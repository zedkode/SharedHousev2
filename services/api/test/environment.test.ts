import { describe, expect, it } from 'vitest';

import { readApiEnvironment } from '../src/config/environment.js';

describe('API environment', () => {
  it('uses the documented local port when no value is supplied', () => {
    expect(readApiEnvironment({})).toEqual({
      port: 3000,
      runtimeEnvironment: 'development',
      databaseUrl: null,
      databasePassword: null,
      pgliteDataDirectory: './tmp/sharedhouse-pglite',
      exposeDevelopmentVerificationCode: true,
      emailProvider: 'disabled',
      emailFrom: null,
      resendApiKey: null,
      emailOutboxEncryptionKeyBase64: null,
    });
  });

  it.each(['0', '65536', '3.14', 'not-a-port'])('rejects invalid PORT=%s', (port) => {
    expect(() => readApiEnvironment({ PORT: port })).toThrow(
      'PORT must be an integer between 1 and 65535.',
    );
  });

  it('uses an isolated in-memory database for tests', () => {
    expect(readApiEnvironment({ NODE_ENV: 'test' }).pgliteDataDirectory).toBe('memory://');
  });

  it('requires PostgreSQL and keeps development verification codes disabled in production', () => {
    expect(() => readApiEnvironment({ NODE_ENV: 'production' })).toThrow(
      'DATABASE_URL is required in production.',
    );

    expect(
      readApiEnvironment({
        NODE_ENV: 'production',
        DATABASE_URL: 'postgresql://example.invalid/sharedhouse',
        EMAIL_PROVIDER: 'resend',
        EMAIL_FROM: 'SharedHouse <verify@mail.dohotstudio.com>',
        RESEND_API_KEY: 'synthetic-test-key',
        EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64: Buffer.alloc(32, 7).toString('base64'),
      }).exposeDevelopmentVerificationCode,
    ).toBe(false);

    expect(() =>
      readApiEnvironment({
        NODE_ENV: 'production',
        DATABASE_URL: 'postgresql://example.invalid/sharedhouse',
        AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE: 'true',
      }),
    ).toThrow('Development verification codes cannot be exposed in production.');

    expect(() =>
      readApiEnvironment({
        NODE_ENV: 'production',
        DATABASE_URL: 'postgresql://example.invalid/sharedhouse',
      }),
    ).toThrow('EMAIL_PROVIDER=resend is required in production.');

    expect(
      readApiEnvironment({
        NODE_ENV: 'production',
        DATABASE_URL: 'postgresql://example.invalid/sharedhouse',
        EMAIL_PROVIDER: 'resend',
        EMAIL_FROM: 'SharedHouse <verify@mail.dohotstudio.com>',
        RESEND_API_KEY: 'synthetic-test-key',
        EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64: Buffer.alloc(32, 7).toString('base64'),
      }),
    ).toMatchObject({
      emailProvider: 'resend',
      emailFrom: 'SharedHouse <verify@mail.dohotstudio.com>',
    });
  });

  it('rejects incomplete or malformed email delivery configuration', () => {
    expect(() => readApiEnvironment({ EMAIL_PROVIDER: 'smtp' })).toThrow(
      'EMAIL_PROVIDER must be disabled or resend.',
    );
    expect(() => readApiEnvironment({ EMAIL_PROVIDER: 'resend' })).toThrow(
      'RESEND_API_KEY is required when EMAIL_PROVIDER=resend.',
    );
    expect(() =>
      readApiEnvironment({
        EMAIL_PROVIDER: 'resend',
        RESEND_API_KEY: 'synthetic-test-key',
        EMAIL_FROM: 'not-an-email',
        EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64: Buffer.alloc(32).toString('base64'),
      }),
    ).toThrow('EMAIL_FROM must contain a valid sender address.');
    expect(() =>
      readApiEnvironment({
        EMAIL_PROVIDER: 'resend',
        RESEND_API_KEY: 'synthetic-test-key',
        EMAIL_FROM: 'SharedHouse <verify@mail.dohotstudio.com>',
        EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64: `${Buffer.alloc(32).toString('base64')}!`,
      }),
    ).toThrow('EMAIL_OUTBOX_ENCRYPTION_KEY_BASE64');
  });
});
