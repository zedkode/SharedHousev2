import { describe, expect, it } from 'vitest';

import { buildConnectionString } from './database.js';
import { readWorkerEnvironment } from './environment.js';

describe('worker environment', () => {
  it('requires an explicit database and applies bounded defaults', () => {
    expect(readWorkerEnvironment({ DATABASE_URL: 'postgresql://worker@db/sharedhouse' })).toEqual({
      databaseUrl: 'postgresql://worker@db/sharedhouse',
      databasePassword: null,
      pollIntervalMs: 60_000,
      batchSize: 100,
    });
    expect(() => readWorkerEnvironment({})).toThrow('DATABASE_URL is required.');
  });

  it('injects a secret password without exposing it as a separate setting', () => {
    expect(buildConnectionString('postgresql://worker@db/sharedhouse', 'secret')).toBe(
      'postgresql://worker:secret@db/sharedhouse',
    );
  });
});
