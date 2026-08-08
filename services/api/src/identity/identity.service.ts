import { Injectable } from '@nestjs/common';
import type {
  AccountSummary,
  AccountDeletionResult,
  RefreshSessionRequest,
  RegisterRequest,
  RegistrationAccepted,
  ResendEmailVerificationRequest,
  SessionResponse,
  SignInRequest,
  VerifyEmailRequest,
} from '@sharedhouse/contracts';

import { newUuidV7 } from '../common/uuid-v7.js';
import { readApiEnvironment } from '../config/environment.js';
import { VerificationEmailCodec } from '../email/verification-email-codec.js';
import { ApiProblemException, validationProblem } from '../http/api-problem.exception.js';
import { PasswordService } from '../security/password.service.js';
import { TokenService } from '../security/token.service.js';
import { IdentityRepository } from './identity.repository.js';

const ACCESS_TOKEN_LIFETIME_MS = 15 * 60 * 1000;
const REFRESH_TOKEN_LIFETIME_MS = 30 * 24 * 60 * 60 * 1000;
const VERIFICATION_LIFETIME_MS = 15 * 60 * 1000;
const VERIFICATION_RESEND_COOLDOWN_MS = 60 * 1000;

@Injectable()
export class IdentityService {
  constructor(
    private readonly repository: IdentityRepository,
    private readonly passwords: PasswordService,
    private readonly tokens: TokenService,
    private readonly verificationEmails: VerificationEmailCodec,
  ) {}

  async register(request: RegisterRequest): Promise<RegistrationAccepted> {
    const passwordViolations = this.passwords.validatePolicy(request.password);
    if (passwordViolations.length > 0) {
      throw validationProblem(
        passwordViolations.map((message) => ({ field: 'password', message })),
      );
    }

    const occurredAt = new Date();
    const credential = await this.passwords.hashPassword(request.password);
    const verificationCode = this.tokens.createVerificationCode();
    const verificationChallengeId = newUuidV7(occurredAt.getTime());
    const verificationExpiresAt = new Date(
      occurredAt.getTime() + VERIFICATION_LIFETIME_MS,
    ).toISOString();
    const verificationEmail = this.verificationEmails.prepare({
      challengeId: verificationChallengeId,
      recipientEmail: request.email,
      locale: request.preferredLocale,
      code: verificationCode,
      expiresAt: verificationExpiresAt,
      occurredAt: occurredAt.toISOString(),
    });
    const created = await this.repository.createRegistration({
      userId: newUuidV7(occurredAt.getTime()),
      email: request.email,
      displayName: request.displayName,
      preferredLocale: request.preferredLocale,
      passwordAlgorithm: credential.algorithm,
      passwordSaltBase64: credential.saltBase64,
      passwordHashBase64: credential.hashBase64,
      verificationChallengeId,
      verificationCodeHash: this.tokens.hash(verificationCode),
      verificationExpiresAt,
      marketingConsent: request.marketingConsent,
      occurredAt: occurredAt.toISOString(),
      ...(verificationEmail === undefined ? {} : { verificationEmail }),
    });
    const environment = readApiEnvironment(process.env);

    return {
      verificationRequired: true,
      ...(created && environment.exposeDevelopmentVerificationCode
        ? { developmentVerificationCode: verificationCode }
        : {}),
    };
  }

  async verifyEmail(request: VerifyEmailRequest): Promise<SessionResponse> {
    const occurredAt = new Date().toISOString();
    const result = await this.repository.verifyEmail(
      request.email,
      this.tokens.hash(request.code),
      occurredAt,
    );

    if (result.status !== 'verified') {
      throw new ApiProblemException(
        result.status === 'invalid'
          ? {
              status: 400,
              code: 'VERIFICATION_CODE_INVALID',
              title: 'The verification code is not valid.',
            }
          : {
              status: 410,
              code: 'VERIFICATION_CODE_EXPIRED',
              title: 'The verification code has expired.',
            },
      );
    }

    return this.createSession(result.account, request.deviceName ?? 'SharedHouse app');
  }

  async resendVerification(request: ResendEmailVerificationRequest): Promise<RegistrationAccepted> {
    const pending = await this.repository.findPendingVerificationAccount(request.email);
    if (pending === null) {
      return { verificationRequired: true };
    }

    const occurredAt = new Date();
    const verificationCode = this.tokens.createVerificationCode();
    const verificationChallengeId = newUuidV7(occurredAt.getTime());
    const verificationExpiresAt = new Date(
      occurredAt.getTime() + VERIFICATION_LIFETIME_MS,
    ).toISOString();
    const verificationEmail = this.verificationEmails.prepare({
      challengeId: verificationChallengeId,
      recipientEmail: request.email,
      locale: pending.preferredLocale,
      code: verificationCode,
      expiresAt: verificationExpiresAt,
      occurredAt: occurredAt.toISOString(),
    });
    const replaced = await this.repository.replaceVerificationChallenge({
      userId: pending.userId,
      verificationChallengeId,
      verificationCodeHash: this.tokens.hash(verificationCode),
      verificationExpiresAt,
      cooldownBefore: new Date(
        occurredAt.getTime() - VERIFICATION_RESEND_COOLDOWN_MS,
      ).toISOString(),
      occurredAt: occurredAt.toISOString(),
      ...(verificationEmail === undefined ? {} : { verificationEmail }),
    });
    const environment = readApiEnvironment(process.env);
    return {
      verificationRequired: true,
      ...(replaced && environment.exposeDevelopmentVerificationCode
        ? { developmentVerificationCode: verificationCode }
        : {}),
    };
  }

  async signIn(request: SignInRequest): Promise<SessionResponse> {
    const credential = await this.repository.findCredentialByEmail(request.email);
    if (credential === null) {
      await this.passwords.consumeDummyVerification(request.password);
      throw invalidCredentials();
    }

    const passwordMatches = await this.passwords.verifyPassword(request.password, credential);
    if (!passwordMatches) {
      throw invalidCredentials();
    }
    if (credential.status === 'pending_verification' || !credential.account.emailVerified) {
      throw new ApiProblemException({
        status: 403,
        code: 'EMAIL_VERIFICATION_REQUIRED',
        title: 'Verify the email address before signing in.',
      });
    }
    if (credential.status !== 'active') {
      throw new ApiProblemException({
        status: 403,
        code: 'ACCOUNT_UNAVAILABLE',
        title: 'This account is not available.',
      });
    }

    return this.createSession(credential.account, request.deviceName ?? 'SharedHouse app');
  }

  async refresh(request: RefreshSessionRequest): Promise<SessionResponse> {
    const accessToken = this.tokens.createAccessToken();
    const refreshToken = this.tokens.createRefreshToken();
    const occurredAt = new Date();
    const accessTokenExpiresAt = new Date(
      occurredAt.getTime() + ACCESS_TOKEN_LIFETIME_MS,
    ).toISOString();
    const refreshTokenExpiresAt = new Date(
      occurredAt.getTime() + REFRESH_TOKEN_LIFETIME_MS,
    ).toISOString();
    const result = await this.repository.rotateRefreshToken({
      currentRefreshTokenHash: this.tokens.hash(request.refreshToken),
      nextAccessTokenHash: this.tokens.hash(accessToken),
      nextRefreshTokenHash: this.tokens.hash(refreshToken),
      accessExpiresAt: accessTokenExpiresAt,
      refreshExpiresAt: refreshTokenExpiresAt,
      occurredAt: occurredAt.toISOString(),
    });

    if (result.status !== 'rotated') {
      throw new ApiProblemException({
        status: 401,
        code: result.status === 'reused' ? 'REFRESH_TOKEN_REUSED' : 'SESSION_INVALID',
        title: 'Sign in again to continue.',
      });
    }

    return {
      accessToken,
      refreshToken,
      accessTokenExpiresAt,
      refreshTokenExpiresAt,
      account: result.principal.account,
    };
  }

  async signOut(accessToken: string): Promise<void> {
    await this.repository.revokeSession(this.tokens.hash(accessToken), new Date().toISOString());
  }

  async deleteAccount(userId: string, password: string): Promise<AccountDeletionResult> {
    const credential = await this.repository.findCredentialByUserId(userId);
    if (
      credential === null ||
      credential.status !== 'active' ||
      !(await this.passwords.verifyPassword(password, credential))
    ) {
      throw recentAuthenticationRequired();
    }
    return this.completeAccountDeletion(userId);
  }

  async deleteAccountByCredentials(
    email: string,
    password: string,
  ): Promise<AccountDeletionResult> {
    const credential = await this.repository.findCredentialByEmail(email);
    if (
      credential === null ||
      credential.status !== 'active' ||
      !(await this.passwords.verifyPassword(password, credential))
    ) {
      if (credential === null) await this.passwords.consumeDummyVerification(password);
      throw recentAuthenticationRequired();
    }
    return this.completeAccountDeletion(credential.account.id);
  }

  private async completeAccountDeletion(userId: string): Promise<AccountDeletionResult> {
    const result = await this.repository.deleteAccount(userId, new Date().toISOString());
    if (result.status === 'owner_transfer_required') {
      throw new ApiProblemException({
        status: 409,
        code: 'ACCOUNT_DELETION_OWNER_TRANSFER_REQUIRED',
        title:
          'Transfer ownership or remove the other household members before deleting the account.',
      });
    }
    return { status: 'completed', closedHouseholdIds: [...result.closedHouseholdIds] };
  }

  private async createSession(
    account: AccountSummary,
    deviceName: string,
  ): Promise<SessionResponse> {
    const accessToken = this.tokens.createAccessToken();
    const refreshToken = this.tokens.createRefreshToken();
    const occurredAt = new Date();
    const accessTokenExpiresAt = new Date(
      occurredAt.getTime() + ACCESS_TOKEN_LIFETIME_MS,
    ).toISOString();
    const refreshTokenExpiresAt = new Date(
      occurredAt.getTime() + REFRESH_TOKEN_LIFETIME_MS,
    ).toISOString();

    await this.repository.createSession({
      sessionId: newUuidV7(occurredAt.getTime()),
      familyId: newUuidV7(occurredAt.getTime()),
      userId: account.id,
      deviceName,
      accessTokenHash: this.tokens.hash(accessToken),
      refreshTokenHash: this.tokens.hash(refreshToken),
      accessExpiresAt: accessTokenExpiresAt,
      refreshExpiresAt: refreshTokenExpiresAt,
      occurredAt: occurredAt.toISOString(),
    });

    return {
      accessToken,
      refreshToken,
      accessTokenExpiresAt,
      refreshTokenExpiresAt,
      account,
    };
  }
}

function recentAuthenticationRequired(): ApiProblemException {
  return new ApiProblemException({
    status: 401,
    code: 'RECENT_AUTHENTICATION_REQUIRED',
    title: 'Confirm the current password to delete this account.',
  });
}

function invalidCredentials(): ApiProblemException {
  return new ApiProblemException({
    status: 401,
    code: 'INVALID_CREDENTIALS',
    title: 'The email address or password is not valid.',
  });
}
