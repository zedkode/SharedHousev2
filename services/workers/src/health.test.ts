import { describe, expect, it } from 'vitest';

import { createWorkerHealth } from './health.js';

describe('worker health', () => {
  it('creates a deterministic structured startup report', () => {
    const checkedAt = new Date('2026-07-25T12:00:00.000Z');

    expect(createWorkerHealth(checkedAt)).toEqual({
      status: 'ok',
      service: 'workers',
      apiVersion: 'v1',
      checkedAt: '2026-07-25T12:00:00.000Z',
    });
  });
});
