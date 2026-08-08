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
}

export interface ClaimedVerificationEmail {
  readonly id: string;
  readonly challengeId: string;
  readonly recipientEmail: string;
  readonly locale: SupportedLocale;
  readonly codeCiphertextBase64: string;
  readonly codeIvBase64: string;
  readonly codeAuthTagBase64: string;
  readonly expiresAt: Date | string;
  readonly attemptCount: number;
}
