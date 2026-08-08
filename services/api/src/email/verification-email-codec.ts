import { Injectable } from '@nestjs/common';
import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';

import { newUuidV7 } from '../common/uuid-v7.js';
import { readApiEnvironment } from '../config/environment.js';
import type {
  ClaimedVerificationEmail,
  PreparedVerificationEmail,
} from './verification-email.types.js';

const ALGORITHM = 'aes-256-gcm';

@Injectable()
export class VerificationEmailCodec {
  private readonly key: Buffer | null;

  constructor() {
    const environment = readApiEnvironment(process.env);
    this.key =
      environment.emailProvider === 'resend'
        ? Buffer.from(requireConfiguredKey(environment.emailOutboxEncryptionKeyBase64), 'base64')
        : null;
  }

  get isEnabled(): boolean {
    return this.key !== null;
  }

  prepare(input: {
    readonly challengeId: string;
    readonly recipientEmail: string;
    readonly locale: PreparedVerificationEmail['locale'];
    readonly code: string;
    readonly expiresAt: string;
    readonly occurredAt: string;
  }): PreparedVerificationEmail | undefined {
    if (this.key === null) {
      return undefined;
    }

    const iv = randomBytes(12);
    const cipher = createCipheriv(ALGORITHM, this.key, iv);
    cipher.setAAD(authenticatedContext(input.challengeId, input.recipientEmail, input.expiresAt));
    const ciphertext = Buffer.concat([cipher.update(input.code, 'utf8'), cipher.final()]);

    return {
      outboxId: newUuidV7(),
      challengeId: input.challengeId,
      recipientEmail: input.recipientEmail,
      locale: input.locale,
      codeCiphertextBase64: ciphertext.toString('base64'),
      codeIvBase64: iv.toString('base64'),
      codeAuthTagBase64: cipher.getAuthTag().toString('base64'),
      expiresAt: input.expiresAt,
      occurredAt: input.occurredAt,
    };
  }

  decrypt(message: ClaimedVerificationEmail): string {
    if (this.key === null) {
      throw new Error('Verification email delivery is disabled.');
    }

    const expiresAt = new Date(message.expiresAt).toISOString();
    const decipher = createDecipheriv(
      ALGORITHM,
      this.key,
      Buffer.from(message.codeIvBase64, 'base64'),
    );
    decipher.setAAD(authenticatedContext(message.challengeId, message.recipientEmail, expiresAt));
    decipher.setAuthTag(Buffer.from(message.codeAuthTagBase64, 'base64'));
    return Buffer.concat([
      decipher.update(Buffer.from(message.codeCiphertextBase64, 'base64')),
      decipher.final(),
    ]).toString('utf8');
  }
}

function authenticatedContext(challengeId: string, email: string, expiresAt: string): Buffer {
  return Buffer.from(`sharedhouse-verification-v1\n${challengeId}\n${email}\n${expiresAt}`, 'utf8');
}

function requireConfiguredKey(value: string | null): string {
  if (value === null) {
    throw new Error('Verification email encryption key is unavailable.');
  }
  return value;
}
