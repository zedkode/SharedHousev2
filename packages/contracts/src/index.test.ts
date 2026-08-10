import { describe, expect, it } from 'vitest';

import {
  API_VERSION,
  CALENDAR_EVENT_TYPES,
  EXPENSE_CATEGORIES,
  EXPENSE_TEMPLATE_CADENCES,
  HOUSEHOLD_CYCLE_TYPES,
  SUPPORTED_LOCALES,
  type CalendarEventConfiguration,
  type HouseholdConfiguration,
  type Money,
} from './index.js';

describe('public contracts', () => {
  it('pins the first API contract version', () => {
    expect(API_VERSION).toBe('v1');
  });

  it('represents money using integer minor units and a currency code', () => {
    const money: Money = { minorUnits: 12_345, currency: 'GBP' };

    expect(Number.isSafeInteger(money.minorUnits)).toBe(true);
    expect(money.currency).toMatch(/^[A-Z]{3}$/u);
  });

  it('pins the initial localisation and household-cycle vocabulary', () => {
    expect(SUPPORTED_LOCALES).toEqual(['en', 'ro']);
    expect(HOUSEHOLD_CYCLE_TYPES).toEqual(['weekly', 'fourteen_day', 'calendar_month']);
  });

  it('pins the one-off calendar vocabulary without implying recurrence', () => {
    expect(CALENDAR_EVENT_TYPES).toEqual([
      'household',
      'maintenance',
      'appointment',
      'shopping',
      'other',
    ]);

    const event: CalendarEventConfiguration = {
      title: 'Boiler service',
      type: 'maintenance',
      date: '2026-08-14',
      startTime: '09:30',
      reminderMinutesBefore: 60,
    };

    expect(event).not.toHaveProperty('recurrence');
  });

  it('represents household dates separately from instants', () => {
    const configuration: HouseholdConfiguration = {
      name: 'Sample home',
      countryCode: 'GB',
      timezone: 'Europe/London',
      currency: 'GBP',
      firstDayOfWeek: 1,
      cycleType: 'calendar_month',
      cycleAnchor: '2026-08-01',
    };

    expect(configuration.cycleAnchor).toMatch(/^\d{4}-\d{2}-\d{2}$/u);
    expect(configuration.timezone).toContain('/');
  });

  it('supports reusable and user-defined household cost vocabulary', () => {
    expect(EXPENSE_CATEGORIES).toContain('custom');
    expect(EXPENSE_TEMPLATE_CADENCES).toEqual(['weekly', 'monthly', 'quarterly', 'yearly']);
  });
});
