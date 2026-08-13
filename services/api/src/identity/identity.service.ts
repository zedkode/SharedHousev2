import { Injectable } from '@nestjs/common';
import type {
  AccountSummary,
  AccountDeletionResult,
  AccountExport,
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
    if (credential?.status !== 'active') {
      throw recentAuthenticationRequired();
    }
    if (!(await this.passwords.verifyPassword(password, credential))) {
      throw recentAuthenticationRequired();
    }
    return this.completeAccountDeletion(userId);
  }

  async exportAccount(userId: string, password: string): Promise<AccountExport> {
    const credential = await this.repository.findCredentialByUserId(userId);
    if (credential?.status !== 'active') {
      throw new ApiProblemException({
        status: 401,
        code: 'RECENT_AUTHENTICATION_REQUIRED',
        title: 'Confirm the current password to export this account.',
      });
    }
    if (!(await this.passwords.verifyPassword(password, credential))) {
      throw new ApiProblemException({
        status: 401,
        code: 'RECENT_AUTHENTICATION_REQUIRED',
        title: 'Confirm the current password to export this account.',
      });
    }
    return this.repository.exportAccount(userId, new Date().toISOString());
  }

  async updateDisplayName(userId: string, displayName: string): Promise<AccountSummary> {
    const normalized = displayName.trim().replace(/\s+/gu, ' ');
    if (Array.from(normalized).length < 2 || Array.from(normalized).length > 80)
      throw validationProblem([
        { field: 'displayName', message: 'Use between 2 and 80 characters.' },
      ]);
    const account = await this.repository.updateDisplayName(
      userId,
      normalized,
      new Date().toISOString(),
    );
    if (account === null) throw invalidCredentials();
    return account;
  }

  async changePassword(input: {
    readonly userId: string;
    readonly sessionId: string;
    readonly currentPassword: string;
    readonly newPassword: string;
    readonly revokeOtherSessions: boolean;
  }): Promise<AccountSummary> {
    const current = await this.repository.findCredentialByUserId(input.userId);
    if (
      current?.status !== 'active' ||
      !(await this.passwords.verifyPassword(input.currentPassword, current))
    )
      throw recentAuthenticationRequired('Confirm the current password to change it.');
    const violations = this.passwords.validatePolicy(input.newPassword);
    if (violations.length > 0)
      throw validationProblem(violations.map((message) => ({ field: 'newPassword', message })));
    if (await this.passwords.verifyPassword(input.newPassword, current))
      throw validationProblem([
        { field: 'newPassword', message: 'Choose a password different from the current one.' },
      ]);
    const credential = await this.passwords.hashPassword(input.newPassword);
    const account = await this.repository.changePassword({
      userId: input.userId,
      sessionId: input.sessionId,
      credential,
      revokeOtherSessions: input.revokeOtherSessions,
      occurredAt: new Date().toISOString(),
    });
    if (account === null) throw invalidCredentials();
    return account;
  }

  async requestEmailChange(input: {
    readonly userId: string;
    readonly newEmail: string;
    readonly currentPassword: string;
  }): Promise<{
    readonly status: 'verification_required';
    readonly account: AccountSummary;
    readonly developmentVerificationCode?: string;
  }> {
    const credential = await this.repository.findCredentialByUserId(input.userId);
    if (
      credential?.status !== 'active' ||
      !(await this.passwords.verifyPassword(input.currentPassword, credential))
    )
      throw recentAuthenticationRequired(
        'Confirm the current password to change the email address.',
      );
    const email = input.newEmail.trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(email) || email.length > 254)
      throw validationProblem([{ field: 'newEmail', message: 'Use a valid email address.' }]);
    if (email === credential.account.email)
      throw validationProblem([
        { field: 'newEmail', message: 'Use an email address different from the current one.' },
      ]);
    const occurredAt = new Date();
    const code = this.tokens.createVerificationCode();
    const changeId = newUuidV7(occurredAt.getTime());
    const expiresAt = new Date(occurredAt.getTime() + VERIFICATION_LIFETIME_MS).toISOString();
    const verificationEmail = this.verificationEmails.prepare({
      challengeId: changeId,
      recipientEmail: email,
      locale: credential.account.preferredLocale,
      code,
      expiresAt,
      occurredAt: occurredAt.toISOString(),
      messageKind: 'email_change_verification',
    });
    const result = await this.repository.requestEmailChange({
      userId: input.userId,
      changeId,
      newEmail: email,
      codeHash: this.tokens.hash(code),
      expiresAt,
      occurredAt: occurredAt.toISOString(),
      ...(verificationEmail === undefined ? {} : { verificationEmail }),
    });
    if (result === 'conflict')
      throw new ApiProblemException({
        status: 409,
        code: 'EMAIL_ALREADY_IN_USE',
        title: 'That email address is already in use.',
      });
    if (result === 'not_found') throw invalidCredentials();
    const environment = readApiEnvironment(process.env);
    return {
      status: 'verification_required',
      account: credential.account,
      ...(environment.exposeDevelopmentVerificationCode
        ? { developmentVerificationCode: code }
        : {}),
    };
  }

  async confirmEmailChange(userId: string, code: string): Promise<AccountSummary> {
    if (!/^[0-9]{8}$/u.test(code))
      throw validationProblem([{ field: 'code', message: 'Use the 8-digit verification code.' }]);
    const result = await this.repository.confirmEmailChange(
      userId,
      this.tokens.hash(code),
      new Date().toISOString(),
    );
    if (result === 'expired')
      throw new ApiProblemException({
        status: 410,
        code: 'EMAIL_CHANGE_EXPIRED',
        title: 'The email-change code has expired.',
      });
    if (result === 'invalid')
      throw new ApiProblemException({
        status: 400,
        code: 'EMAIL_CHANGE_CODE_INVALID',
        title: 'The email-change code is not valid.',
      });
    return result;
  }

  async deleteAccountByCredentials(
    email: string,
    password: string,
  ): Promise<AccountDeletionResult> {
    const credential = await this.repository.findCredentialByEmail(email);
    if (credential?.status !== 'active') {
      if (credential === null) await this.passwords.consumeDummyVerification(password);
      throw recentAuthenticationRequired();
    }
    if (!(await this.passwords.verifyPassword(password, credential))) {
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
    return {
      status: 'completed',
      closedHouseholdIds: [...result.closedHouseholdIds],
      transferredHouseholdIds: [...result.transferredHouseholdIds],
    };
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

function recentAuthenticationRequired(
  title = 'Confirm the current password to delete this account.',
): ApiProblemException {
  return new ApiProblemException({
    status: 401,
    code: 'RECENT_AUTHENTICATION_REQUIRED',
    title,
  });
}

function invalidCredentials(): ApiProblemException {
  return new ApiProblemException({
    status: 401,
    code: 'INVALID_CREDENTIALS',
    title: 'The email address or password is not valid.',
  });
}
