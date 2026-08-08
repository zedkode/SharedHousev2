import { Injectable, Logger, type OnModuleDestroy, type OnModuleInit } from '@nestjs/common';

import { DatabaseService } from '../database/database.service.js';
import { EmailDeliveryError, ResendEmailClient } from './resend-email.client.js';
import { VerificationEmailCodec } from './verification-email-codec.js';
import type { ClaimedVerificationEmail } from './verification-email.types.js';

const POLL_INTERVAL_MS = 5_000;
const STALE_LOCK_MS = 5 * 60 * 1000;
const MAX_ATTEMPTS = 8;
const MAX_BATCH_SIZE = 10;

@Injectable()
export class VerificationEmailDispatcher implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(VerificationEmailDispatcher.name);
  private timer: NodeJS.Timeout | null = null;
  private draining = false;

  constructor(
    private readonly database: DatabaseService,
    private readonly codec: VerificationEmailCodec,
    private readonly resend: ResendEmailClient,
  ) {}

  onModuleInit(): void {
    if (!this.codec.isEnabled) {
      return;
    }
    this.timer = setInterval(() => void this.drain(), POLL_INTERVAL_MS);
    this.timer.unref();
    void this.drain();
  }

  onModuleDestroy(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  private async drain(): Promise<void> {
    if (this.draining) return;
    this.draining = true;
    try {
      for (let index = 0; index < MAX_BATCH_SIZE; index += 1) {
        const message = await this.claimNext();
        if (message === null) break;
        await this.deliver(message);
      }
    } catch (error: unknown) {
      this.logger.error({
        event: 'verification_email_dispatch_failed',
        error: safeErrorName(error),
      });
    } finally {
      this.draining = false;
    }
  }

  private async claimNext(): Promise<ClaimedVerificationEmail | null> {
    const now = new Date();
    const staleBefore = new Date(now.getTime() - STALE_LOCK_MS).toISOString();
    const rows = await this.database.query<ClaimedVerificationEmail>(
      `UPDATE verification_email_outbox
       SET status = 'sending',
           attempt_count = attempt_count + 1,
           locked_at = $1,
           updated_at = $1
       WHERE id = (
         SELECT id
         FROM verification_email_outbox
         WHERE attempt_count < $2
           AND available_at <= $1
           AND (
             status = 'pending'
             OR (status = 'sending' AND locked_at < $3)
           )
         ORDER BY available_at, created_at
         FOR UPDATE SKIP LOCKED
         LIMIT 1
       )
       RETURNING
         id,
         challenge_id AS "challengeId",
         recipient_email AS "recipientEmail",
         locale,
         code_ciphertext_base64 AS "codeCiphertextBase64",
         code_iv_base64 AS "codeIvBase64",
         code_auth_tag_base64 AS "codeAuthTagBase64",
         expires_at AS "expiresAt",
         attempt_count AS "attemptCount"`,
      [now.toISOString(), MAX_ATTEMPTS, staleBefore],
    );
    return rows[0] ?? null;
  }

  private async deliver(message: ClaimedVerificationEmail): Promise<void> {
    if (new Date(message.expiresAt).getTime() <= Date.now()) {
      await this.markFailed(message, 'verification_code_expired', false);
      return;
    }

    try {
      const code = this.codec.decrypt(message);
      if (!/^[0-9]{8}$/u.test(code)) {
        throw new EmailDeliveryError('verification_code_payload_invalid', false);
      }
      const providerMessageId = await this.resend.sendVerification(message, code);
      await this.database.query(
        `UPDATE verification_email_outbox
         SET status = 'sent',
             sent_at = $2,
             provider_message_id = $3,
             last_error_code = NULL,
             code_ciphertext_base64 = NULL,
             code_iv_base64 = NULL,
             code_auth_tag_base64 = NULL,
             locked_at = NULL,
             updated_at = $2
         WHERE id = $1 AND status = 'sending'`,
        [message.id, new Date().toISOString(), providerMessageId],
      );
      this.logger.log({ event: 'verification_email_sent', outboxId: message.id });
    } catch (error: unknown) {
      const deliveryError =
        error instanceof EmailDeliveryError
          ? error
          : new EmailDeliveryError('verification_email_internal_error', true);
      await this.markFailed(message, deliveryError.safeCode, deliveryError.retryable);
    }
  }

  private async markFailed(
    message: ClaimedVerificationEmail,
    safeCode: string,
    retryable: boolean,
  ): Promise<void> {
    const canRetry = retryable && message.attemptCount < MAX_ATTEMPTS;
    const delaySeconds = Math.min(30 * 2 ** Math.max(0, message.attemptCount - 1), 3_600);
    const availableAt = new Date(Date.now() + delaySeconds * 1000).toISOString();
    await this.database.query(
      `UPDATE verification_email_outbox
       SET status = $2,
           available_at = $3,
           last_error_code = $4,
           code_ciphertext_base64 = CASE WHEN $2 = 'dead' THEN NULL ELSE code_ciphertext_base64 END,
           code_iv_base64 = CASE WHEN $2 = 'dead' THEN NULL ELSE code_iv_base64 END,
           code_auth_tag_base64 = CASE WHEN $2 = 'dead' THEN NULL ELSE code_auth_tag_base64 END,
           locked_at = NULL,
           updated_at = $5
       WHERE id = $1 AND status = 'sending'`,
      [message.id, canRetry ? 'pending' : 'dead', availableAt, safeCode, new Date().toISOString()],
    );
    this.logger.warn({
      event: canRetry ? 'verification_email_retry_scheduled' : 'verification_email_dead',
      outboxId: message.id,
      attempt: message.attemptCount,
      errorCode: safeCode,
    });
  }
}

function safeErrorName(error: unknown): string {
  return error instanceof Error ? error.name : 'UnknownError';
}
