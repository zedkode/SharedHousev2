import type { SupportedLocale } from '@sharedhouse/contracts';

export interface PreparedVerificationEmail {
  readonly outboxId: string;
  readonly challengeId: string;
  readonly recipientEmail: string;
  readonly locale: SupportedLocale;
  readonly codeCiphertextBase64: string;
  readonly codeIvBase64: string;
  readonly codeAuthTagBase64: string;
  readonly expiresAt: string;
  readonly occurredAt: string;
  readonly messageKind?: 'email_verification' | 'email_change_verification';
}

export interface ClaimedVerificationEmail {
  readonly id: string;
  readonly challengeId: string;
  readonly recipientEmail: string;
  readonly locale: SupportedLocale;
  readonly messageKind?:
    | 'email_verification'
    | 'email_change_verification'
    | 'email_change_warning'
    | 'password_changed';
  readonly codeCiphertextBase64: string | null;
  readonly codeIvBase64: string | null;
  readonly codeAuthTagBase64: string | null;
  readonly expiresAt: Date | string;
  readonly attemptCount: number;
}
