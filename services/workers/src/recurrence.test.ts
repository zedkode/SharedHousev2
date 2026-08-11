import { describe, expect, it } from 'vitest';

import { equalAllocationMinorUnits, nextOccurrenceDate } from './recurrence.js';

describe('expense recurrence', () => {
  it('keeps the monthly anchor after a short month', () => {
    expect(nextOccurrenceDate('2027-01-31', 'monthly', 31, 1)).toBe('2027-02-28');
    expect(nextOccurrenceDate('2027-02-28', 'monthly', 31, 1)).toBe('2027-03-31');
  });

  it('handles leap years without drifting the yearly anchor', () => {
    expect(nextOccurrenceDate('2028-02-29', 'yearly', 29, 2)).toBe('2029-02-28');
    expect(nextOccurrenceDate('2031-02-28', 'yearly', 29, 2)).toBe('2032-02-29');
  });

  it('moves weekly and quarterly rules using local dates only', () => {
    expect(nextOccurrenceDate('2026-03-29', 'weekly', 29, 3)).toBe('2026-04-05');
    expect(nextOccurrenceDate('2026-11-30', 'quarterly', 30, 11)).toBe('2027-02-28');
  });

  it('reconciles integer allocations exactly and deterministically', () => {
    const allocations = equalAllocationMinorUnits(1_001, 3);
    expect(allocations).toEqual([334n, 334n, 333n]);
    expect(allocations.reduce((sum, value) => sum + value, 0n)).toBe(1_001n);
  });
});
