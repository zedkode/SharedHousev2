import type {
  CalendarEventConfiguration,
  CalendarEventType,
  CreateHouseholdInvitationRequest,
  DeleteAccountRequest,
  ExportAccountRequest,
  HouseholdConfiguration,
  HouseholdInvitationRole,
  RefreshSessionRequest,
  ResendEmailVerificationRequest,
  RegisterRequest,
  SignInRequest,
  SupportedLocale,
  VerifyEmailRequest,
} from '@sharedhouse/contracts';

import { validationProblem, type FieldViolation } from './api-problem.exception.js';

type JsonObject = Readonly<Record<string, unknown>>;

export function parseRegisterRequest(value: unknown): RegisterRequest {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'email',
    'password',
    'displayName',
    'preferredLocale',
    'ageConfirmed',
    'termsAccepted',
    'marketingConsent',
  ]);
  const violations: FieldViolation[] = [];
  const email = readEmail(body.email, 'email', violations);
  const password = readString(body.password, 'password', 1, 128, violations, false);
  const displayName = readString(body.displayName, 'displayName', 1, 80, violations, true);
  const preferredLocale = readLocale(body.preferredLocale, violations);
  const ageConfirmed = readRequiredTrue(body.ageConfirmed, 'ageConfirmed', violations);
  const termsAccepted = readRequiredTrue(body.termsAccepted, 'termsAccepted', violations);
  const marketingConsent = readBoolean(body.marketingConsent, 'marketingConsent', violations);
  throwIfViolations(violations);

  return {
    email,
    password,
    displayName,
    preferredLocale,
    ageConfirmed,
    termsAccepted,
    marketingConsent,
  };
}

export function parseVerifyEmailRequest(value: unknown): VerifyEmailRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['email', 'code', 'deviceName']);
  const violations: FieldViolation[] = [];
  const email = readEmail(body.email, 'email', violations);
  const code = readString(body.code, 'code', 8, 8, violations, false);
  if (!/^[0-9]{8}$/u.test(code)) {
    violations.push({ field: 'code', message: 'Use the 8-digit verification code.' });
  }
  const deviceName = readOptionalString(body.deviceName, 'deviceName', 1, 80, violations);
  throwIfViolations(violations);
  return { email, code, ...(deviceName === undefined ? {} : { deviceName }) };
}

export function parseResendEmailVerificationRequest(
  value: unknown,
): ResendEmailVerificationRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['email']);
  const violations: FieldViolation[] = [];
  const email = readEmail(body.email, 'email', violations);
  throwIfViolations(violations);
  return { email };
}

export function parseSignInRequest(value: unknown): SignInRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['email', 'password', 'deviceName']);
  const violations: FieldViolation[] = [];
  const email = readEmail(body.email, 'email', violations);
  const password = readString(body.password, 'password', 1, 128, violations, false);
  const deviceName = readOptionalString(body.deviceName, 'deviceName', 1, 80, violations);
  throwIfViolations(violations);
  return { email, password, ...(deviceName === undefined ? {} : { deviceName }) };
}

export function parseRefreshRequest(value: unknown): RefreshSessionRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['refreshToken']);
  const violations: FieldViolation[] = [];
  const refreshToken = readString(body.refreshToken, 'refreshToken', 40, 256, violations, false);
  throwIfViolations(violations);
  return { refreshToken };
}

export function parseDeleteAccountRequest(value: unknown): DeleteAccountRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['password', 'confirmation']);
  const violations: FieldViolation[] = [];
  const password = readString(body.password, 'password', 1, 128, violations, false);
  const confirmation = readString(body.confirmation, 'confirmation', 6, 6, violations, false);
  if (confirmation !== 'DELETE') {
    violations.push({ field: 'confirmation', message: 'Type DELETE to confirm.' });
  }
  throwIfViolations(violations);
  return { password, confirmation: 'DELETE' };
}

export function parseExportAccountRequest(value: unknown): ExportAccountRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['password']);
  const violations: FieldViolation[] = [];
  const password = readString(body.password, 'password', 1, 128, violations, false);
  throwIfViolations(violations);
  return { password };
}

export function parsePublicDeleteAccountRequest(
  value: unknown,
): DeleteAccountRequest & { email: string } {
  const body = readObject(value);
  assertAllowedKeys(body, ['email', 'password', 'confirmation']);
  const violations: FieldViolation[] = [];
  const email = readEmail(body.email, 'email', violations);
  const password = readString(body.password, 'password', 1, 128, violations, false);
  const confirmation = readString(body.confirmation, 'confirmation', 6, 6, violations, false);
  if (confirmation !== 'DELETE') {
    violations.push({ field: 'confirmation', message: 'Type DELETE to confirm.' });
  }
  throwIfViolations(violations);
  return { email, password, confirmation: 'DELETE' };
}

export function parseHouseholdConfiguration(value: unknown): HouseholdConfiguration {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'name',
    'countryCode',
    'timezone',
    'currency',
    'firstDayOfWeek',
    'cycleType',
    'cycleAnchor',
  ]);
  const violations: FieldViolation[] = [];
  const name = readString(body.name, 'name', 1, 100, violations, true);
  const countryCode = readString(
    body.countryCode,
    'countryCode',
    2,
    2,
    violations,
    true,
  ).toUpperCase();
  if (!/^[A-Z]{2}$/u.test(countryCode)) {
    violations.push({ field: 'countryCode', message: 'Use a two-letter country code.' });
  }
  const timezone = readString(body.timezone, 'timezone', 1, 64, violations, true);
  if (!isTimeZone(timezone)) {
    violations.push({ field: 'timezone', message: 'Use a valid IANA timezone.' });
  }
  const currency = readString(body.currency, 'currency', 3, 3, violations, true).toUpperCase();
  if (!isCurrency(currency)) {
    violations.push({ field: 'currency', message: 'Use a valid three-letter currency code.' });
  }
  const firstDayOfWeek = readFirstDayOfWeek(body.firstDayOfWeek, violations);
  const cycleType = readCycleType(body.cycleType, violations);
  const cycleAnchor = readString(body.cycleAnchor, 'cycleAnchor', 10, 10, violations, true);
  if (!isIsoDate(cycleAnchor)) {
    violations.push({ field: 'cycleAnchor', message: 'Use a valid date in YYYY-MM-DD format.' });
  }
  throwIfViolations(violations);

  return {
    name,
    countryCode,
    timezone,
    currency,
    firstDayOfWeek,
    cycleType,
    cycleAnchor,
  };
}

export function parseCreateHouseholdInvitation(value: unknown): CreateHouseholdInvitationRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['role', 'email']);
  const violations: FieldViolation[] = [];
  const role = readInvitationRole(body.role, violations);
  const email =
    body.email === undefined || body.email === null || body.email === ''
      ? null
      : readEmail(body.email, 'email', violations);
  throwIfViolations(violations);
  return { role, email };
}

export interface CalendarDateRange {
  readonly from: string;
  readonly to: string;
}

export function parseCalendarEventConfiguration(value: unknown): CalendarEventConfiguration {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'title',
    'description',
    'type',
    'date',
    'startTime',
    'endTime',
    'reminderMinutesBefore',
  ]);
  const violations: FieldViolation[] = [];
  const title = readString(body.title, 'title', 1, 120, violations, true);
  const description = readOptionalNullableString(
    body.description,
    'description',
    1,
    1000,
    violations,
  );
  const type = readCalendarEventType(body.type, violations);
  const date = readString(body.date, 'date', 10, 10, violations, true);
  if (!isIsoDate(date)) {
    violations.push({ field: 'date', message: 'Use a valid date in YYYY-MM-DD format.' });
  }
  const startTime = readOptionalTime(body.startTime, 'startTime', violations);
  const endTime = readOptionalTime(body.endTime, 'endTime', violations);
  if (typeof endTime === 'string' && typeof startTime !== 'string') {
    violations.push({ field: 'endTime', message: 'Provide a start time before an end time.' });
  }
  if (typeof startTime === 'string' && typeof endTime === 'string' && endTime <= startTime) {
    violations.push({ field: 'endTime', message: 'End time must be later than start time.' });
  }
  const reminderMinutesBefore = readOptionalInteger(
    body.reminderMinutesBefore,
    'reminderMinutesBefore',
    0,
    10_080,
    violations,
  );
  throwIfViolations(violations);

  return {
    title,
    type,
    date,
    ...(description === undefined ? {} : { description }),
    ...(startTime === undefined ? {} : { startTime }),
    ...(endTime === undefined ? {} : { endTime }),
    ...(reminderMinutesBefore === undefined ? {} : { reminderMinutesBefore }),
  };
}

export function parseCalendarDateRange(
  fromValue: string | undefined,
  toValue: string | undefined,
): CalendarDateRange {
  const violations: FieldViolation[] = [];
  const from = readDateQuery(fromValue, 'from', violations);
  const to = readDateQuery(toValue, 'to', violations);

  if (isIsoDate(from) && isIsoDate(to)) {
    const fromEpoch = Date.parse(`${from}T00:00:00.000Z`);
    const toEpoch = Date.parse(`${to}T00:00:00.000Z`);
    const rangeDays = Math.floor((toEpoch - fromEpoch) / MillisecondsPerDay) + 1;
    if (rangeDays < 1) {
      violations.push({ field: 'to', message: 'The end date cannot be before the start date.' });
    } else if (rangeDays > MaximumCalendarRangeDays) {
      violations.push({
        field: 'to',
        message: `Calendar queries can include at most ${String(MaximumCalendarRangeDays)} days.`,
      });
    }
  }

  throwIfViolations(violations);
  return { from, to };
}

function readObject(value: unknown): JsonObject {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw validationProblem([{ field: '$', message: 'Expected a JSON object.' }]);
  }
  return value as JsonObject;
}

function assertAllowedKeys(body: JsonObject, allowedKeys: readonly string[]): void {
  const allowed = new Set(allowedKeys);
  const unknownKeys = Object.keys(body).filter((key) => !allowed.has(key));
  if (unknownKeys.length > 0) {
    throw validationProblem(
      unknownKeys.map((field) => ({ field, message: 'This field is not supported.' })),
    );
  }
}

function readString(
  value: unknown,
  field: string,
  minimum: number,
  maximum: number,
  violations: FieldViolation[],
  trim: boolean,
): string {
  if (typeof value !== 'string') {
    violations.push({ field, message: 'Expected text.' });
    return '';
  }
  const result = trim ? value.trim() : value;
  const length = Array.from(result).length;
  if (length < minimum || length > maximum) {
    violations.push({
      field,
      message: `Use between ${String(minimum)} and ${String(maximum)} characters.`,
    });
  }
  return result;
}

function readOptionalString(
  value: unknown,
  field: string,
  minimum: number,
  maximum: number,
  violations: FieldViolation[],
): string | undefined {
  if (value === undefined) {
    return undefined;
  }
  return readString(value, field, minimum, maximum, violations, true);
}

function readOptionalNullableString(
  value: unknown,
  field: string,
  minimum: number,
  maximum: number,
  violations: FieldViolation[],
): string | null | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return readString(value, field, minimum, maximum, violations, true);
}

function readOptionalTime(
  value: unknown,
  field: string,
  violations: FieldViolation[],
): string | null | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  if (typeof value !== 'string' || !/^(?:[01][0-9]|2[0-3]):[0-5][0-9]$/u.test(value)) {
    violations.push({ field, message: 'Use 24-hour time in HH:mm format.' });
    return undefined;
  }
  return value;
}

function readOptionalInteger(
  value: unknown,
  field: string,
  minimum: number,
  maximum: number,
  violations: FieldViolation[],
): number | null | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  if (!Number.isSafeInteger(value) || (value as number) < minimum || (value as number) > maximum) {
    violations.push({
      field,
      message: `Use a whole number between ${String(minimum)} and ${String(maximum)}.`,
    });
    return undefined;
  }
  return value as number;
}

function readDateQuery(
  value: string | undefined,
  field: string,
  violations: FieldViolation[],
): string {
  if (value === undefined || !isIsoDate(value)) {
    violations.push({ field, message: 'Use a valid date in YYYY-MM-DD format.' });
    return '';
  }
  return value;
}

function readEmail(value: unknown, field: string, violations: FieldViolation[]): string {
  const email = readString(value, field, 3, 254, violations, true).toLocaleLowerCase('en-US');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(email)) {
    violations.push({ field, message: 'Use a valid email address.' });
  }
  return email;
}

function readLocale(value: unknown, violations: FieldViolation[]): SupportedLocale {
  if (value === 'en' || value === 'ro') {
    return value;
  }
  violations.push({ field: 'preferredLocale', message: 'Choose English or Romanian.' });
  return 'en';
}

function readBoolean(value: unknown, field: string, violations: FieldViolation[]): boolean {
  if (typeof value === 'boolean') {
    return value;
  }
  violations.push({ field, message: 'Expected true or false.' });
  return false;
}

function readRequiredTrue(value: unknown, field: string, violations: FieldViolation[]): true {
  if (value !== true) {
    violations.push({ field, message: 'This confirmation is required.' });
  }
  return true;
}

function readFirstDayOfWeek(value: unknown, violations: FieldViolation[]): 1 | 6 | 7 {
  if (value === 1 || value === 6 || value === 7) {
    return value;
  }
  violations.push({ field: 'firstDayOfWeek', message: 'Choose Monday, Saturday, or Sunday.' });
  return 1;
}

function readCycleType(
  value: unknown,
  violations: FieldViolation[],
): HouseholdConfiguration['cycleType'] {
  if (value === 'weekly' || value === 'fourteen_day' || value === 'calendar_month') {
    return value;
  }
  violations.push({ field: 'cycleType', message: 'Choose a supported billing cycle.' });
  return 'calendar_month';
}

function readInvitationRole(value: unknown, violations: FieldViolation[]): HouseholdInvitationRole {
  if (value === 'admin' || value === 'member' || value === 'read_only') {
    return value;
  }
  violations.push({
    field: 'role',
    message: 'Choose admin, member, or read_only.',
  });
  return 'member';
}

function readCalendarEventType(value: unknown, violations: FieldViolation[]): CalendarEventType {
  if (
    value === 'household' ||
    value === 'maintenance' ||
    value === 'appointment' ||
    value === 'shopping' ||
    value === 'other'
  ) {
    return value;
  }
  violations.push({ field: 'type', message: 'Choose a supported calendar event type.' });
  return 'other';
}

function isTimeZone(value: string): boolean {
  try {
    new Intl.DateTimeFormat('en-GB', { timeZone: value }).format();
    return true;
  } catch {
    return false;
  }
}

function isCurrency(value: string): boolean {
  if (!/^[A-Z]{3}$/u.test(value)) {
    return false;
  }
  try {
    new Intl.NumberFormat('en-GB', { style: 'currency', currency: value }).format(0);
    return true;
  } catch {
    return false;
  }
}

function isIsoDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/u.test(value)) {
    return false;
  }
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

function throwIfViolations(violations: readonly FieldViolation[]): void {
  if (violations.length > 0) {
    throw validationProblem(violations);
  }
}

const MillisecondsPerDay = 86_400_000;
const MaximumCalendarRangeDays = 370;
