import { describe, expect, it } from 'vitest';

import { buildPostgresConnectionString } from '../src/database/database.service.js';

describe('PostgreSQL connection configuration', () => {
  it('injects and URL-encodes a file-backed password', () => {
    expect(
      buildPostgresConnectionString(
        'postgresql://sharedhouse@postgres:5432/sharedhouse',
        'base64/+value=',
      ),
    ).toBe('postgresql://sharedhouse:base64%2F+value%3D@postgres:5432/sharedhouse');
  });

  it('preserves a direct connection URL when no separate password exists', () => {
    const url = 'postgresql://sharedhouse:direct@postgres:5432/sharedhouse';
    expect(buildPostgresConnectionString(url, null)).toBe(url);
  });

  it('rejects a non-PostgreSQL URL when injecting a password', () => {
    expect(() => buildPostgresConnectionString('https://postgres/sharedhouse', 'secret')).toThrow(
      'DATABASE_URL must use the postgres or postgresql protocol.',
    );
  });
});
