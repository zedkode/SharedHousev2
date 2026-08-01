import type { AccountSummary, SupportedLocale } from '@sharedhouse/contracts';

import type { PasswordAlgorithm } from '../security/password.service.js';

export interface RegistrationRecord {
  readonly userId: string;
  readonly email: string;
  readonly displayName: string;
  readonly preferredLocale: SupportedLocale;
  readonly passwordAlgorithm: PasswordAlgorithm;
  readonly passwordSaltBase64: string;
  readonly passwordHashBase64: string;
  readonly verificationChallengeId: string;
  readonly verificationCodeHash: string;
  readonly verificationExpiresAt: string;
  readonly marketingConsent: boolean;
  readonly occurredAt: string;
}

export interface UserCredentialRecord {
  readonly account: AccountSummary;
  readonly status: 'pending_verification' | 'active' | 'suspended' | 'deleted';
  readonly algorithm: PasswordAlgorithm;
  readonly saltBase64: string;
  readonly hashBase64: string;
}

export interface AuthenticatedPrincipal {
  readonly sessionId: string;
  readonly familyId: string;
  readonly userId: string;
  readonly account: AccountSummary;
}

export interface SessionTokenRecord {
  readonly sessionId: string;
  readonly familyId: string;
  readonly userId: string;
  readonly deviceName: string;
  readonly accessTokenHash: string;
  readonly refreshTokenHash: string;
  readonly accessExpiresAt: string;
  readonly refreshExpiresAt: string;
  readonly occurredAt: string;
}

export type VerificationResult =
  | { readonly status: 'verified'; readonly account: AccountSummary }
  | { readonly status: 'invalid' | 'expired' };

export type RefreshRotationResult =
  | { readonly status: 'rotated'; readonly principal: AuthenticatedPrincipal }
  | { readonly status: 'invalid' | 'reused' };
