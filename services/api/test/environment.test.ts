import { describe, expect, it } from 'vitest';

import { readApiEnvironment } from '../src/config/environment.js';

describe('API environment', () => {
  it('uses the documented local port when no value is supplied', () => {
    expect(readApiEnvironment({})).toEqual({
      port: 3000,
      runtimeEnvironment: 'development',
      databaseUrl: null,
      pgliteDataDirectory: './tmp/sharedhouse-pglite',
      exposeDevelopmentVerificationCode: true,
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
      }).exposeDevelopmentVerificationCode,
    ).toBe(false);

    expect(() =>
      readApiEnvironment({
        NODE_ENV: 'production',
        DATABASE_URL: 'postgresql://example.invalid/sharedhouse',
        AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE: 'true',
      }),
    ).toThrow('Development verification codes cannot be exposed in production.');
  });
});
