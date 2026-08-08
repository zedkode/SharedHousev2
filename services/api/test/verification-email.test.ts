import { afterEach, describe, expect, it, vi } from 'vitest';

import { ResendEmailClient } from '../src/email/resend-email.client.js';
import { VerificationEmailCodec } from '../src/email/verification-email-codec.js';
import type { ClaimedVerificationEmail } from '../src/email/verification-email.types.js';

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe('verification email delivery', () => {
  it('encrypts the code with authenticated context and decrypts it for delivery', () => {
    configureEmailEnvironment();
    const codec = new VerificationEmailCodec();
    const prepared = codec.prepare({
      challengeId: '019c0000-0000-7000-8000-000000000001',
      recipientEmail: 'person@example.test',
      locale: 'en',
      code: '12345678',
      expiresAt: '2026-08-08T12:15:00.000Z',
      occurredAt: '2026-08-08T12:00:00.000Z',
    });

    expect(prepared).toBeDefined();
    expect(prepared?.codeCiphertextBase64).not.toContain('12345678');
    expect(codec.decrypt(asClaimed(requirePrepared(prepared)))).toBe('12345678');

    expect(() =>
      codec.decrypt({
        ...asClaimed(requirePrepared(prepared)),
        recipientEmail: 'attacker@example.test',
      }),
    ).toThrow();
  });

  it('sends a localized code through Resend with an idempotency key', async () => {
    configureEmailEnvironment();
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 'provider-message-1' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const client = new ResendEmailClient();
    const message = sampleClaimedMessage();

    await expect(client.sendVerification(message, '87654321')).resolves.toBe('provider-message-1');
    expect(fetchMock).toHaveBeenCalledOnce();
    const request = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(request.headers).toMatchObject({
      Authorization: 'Bearer synthetic-resend-key',
      'Idempotency-Key': message.id,
    });
    expect(typeof request.body).toBe('string');
    expect(request.body as string).toContain('87654321');
  });

  it('classifies Resend throttling as retryable without exposing response content', async () => {
    configureEmailEnvironment();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('sensitive', { status: 429 })));
    const client = new ResendEmailClient();

    await expect(client.sendVerification(sampleClaimedMessage(), '87654321')).rejects.toMatchObject(
      { safeCode: 'resend_http_429', retryable: true },
    );
  });
});

function configureEmailEnvironment(): void {
  vi.stubEnv('NODE_ENV', 'test');
  vi.stubEnv('EMAIL_PROVIDER', 'resend');
  vi.stubEnv('EMAIL_FROM', 'SharedHouse <verify@mail.dohotstudio.com>');
  vi.stubEnv('RESEND_API_KEY', 'synthetic-resend-key');
  vi.stubEnv('EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64', Buffer.alloc(32, 9).toString('base64'));
}

function requirePrepared(
  value: ReturnType<VerificationEmailCodec['prepare']>,
): NonNullable<ReturnType<VerificationEmailCodec['prepare']>> {
  if (value === undefined) throw new Error('Expected an encrypted email payload.');
  return value;
}

function asClaimed(
  prepared: NonNullable<ReturnType<VerificationEmailCodec['prepare']>>,
): ClaimedVerificationEmail {
  return {
    id: prepared.outboxId,
    challengeId: prepared.challengeId,
    recipientEmail: prepared.recipientEmail,
    locale: prepared.locale,
    codeCiphertextBase64: prepared.codeCiphertextBase64,
    codeIvBase64: prepared.codeIvBase64,
    codeAuthTagBase64: prepared.codeAuthTagBase64,
    expiresAt: prepared.expiresAt,
    attemptCount: 1,
  };
}

function sampleClaimedMessage(): ClaimedVerificationEmail {
  return {
    id: '019c0000-0000-7000-8000-000000000010',
    challengeId: '019c0000-0000-7000-8000-000000000011',
    recipientEmail: 'person@example.test',
    locale: 'ro',
    codeCiphertextBase64: 'ciphertext',
    codeIvBase64: 'iv',
    codeAuthTagBase64: 'tag',
    expiresAt: '2026-08-08T12:15:00.000Z',
    attemptCount: 1,
  };
}
