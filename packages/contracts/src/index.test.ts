import { describe, expect, it } from 'vitest';

import { API_VERSION, type Money } from './index.js';

describe('public contracts', () => {
  it('pins the first API contract version', () => {
    expect(API_VERSION).toBe('v1');
  });

  it('represents money using integer minor units and a currency code', () => {
    const money: Money = { minorUnits: 12_345, currency: 'GBP' };

    expect(Number.isSafeInteger(money.minorUnits)).toBe(true);
    expect(money.currency).toMatch(/^[A-Z]{3}$/u);
  });
});
