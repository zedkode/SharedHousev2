import { describe, expect, it } from 'vitest';

import { readApiEnvironment } from '../src/config/environment.js';

describe('API environment', () => {
  it('uses the documented local port when no value is supplied', () => {
    expect(readApiEnvironment({})).toEqual({ port: 3000 });
  });

  it.each(['0', '65536', '3.14', 'not-a-port'])('rejects invalid PORT=%s', (port) => {
    expect(() => readApiEnvironment({ PORT: port })).toThrow(
      'PORT must be an integer between 1 and 65535.',
    );
  });
});
