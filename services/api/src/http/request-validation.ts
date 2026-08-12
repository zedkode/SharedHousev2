import type {
  CalendarEventConfiguration,
  CalendarEventType,
  UpdateBillingRosterRequest,
  CreateHouseholdInvitationRequest,
  DeleteAccountRequest,
  ExpenseCategory,
  ExpenseConfiguration,
  ExpensePaymentActionRequest,
  ExpensePaymentDeclarationRequest,
  ExpensePaymentMethod,
  ExpenseTemplateCadence,
  ExpenseTemplateConfiguration,
  ArchiveExpenseTemplateRequest,
  ExportAccountRequest,
  HouseholdConfiguration,
  HouseholdMemberActionRequest,
  HouseholdInvitationRole,
  HouseholdTaskActionRequest,
  HouseholdTaskConfiguration,
  HouseholdTaskPriority,
  HouseholdTaskRecurrenceCadence,
  RefreshSessionRequest,
  ResendEmailVerificationRequest,
  RegisterRequest,
  ReverseExpenseRequest,
  SignInRequest,
  SupportedLocale,
  VerifyEmailRequest,
} from '@sharedhouse/contracts';
import {
  EXPENSE_CATEGORIES,
  EXPENSE_PAYMENT_METHODS,
  EXPENSE_TEMPLATE_CADENCES,
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

export function parseHouseholdMemberAction(value: unknown): HouseholdMemberActionRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['action', 'role', 'reason']);
  const violations: FieldViolation[] = [];
  const actions = ['change_role', 'suspend', 'reactivate', 'remove', 'transfer_ownership'] as const;
  const action =
    typeof body.action === 'string' && actions.includes(body.action as (typeof actions)[number])
      ? (body.action as (typeof actions)[number])
      : 'change_role';
  if (body.action !== action) {
    violations.push({ field: 'action', message: 'Choose a supported member action.' });
  }
  const role =
    body.role === undefined || body.role === null
      ? body.role
      : readInvitationRole(body.role, violations);
  const reason = readOptionalNullableString(body.reason, 'reason', 3, 240, violations);
  if (action === 'change_role' && role == null) {
    violations.push({ field: 'role', message: 'Choose the new member role.' });
  }
  if (action !== 'change_role' && role !== undefined && role !== null) {
    violations.push({ field: 'role', message: 'Role is only valid for change_role.' });
  }
  throwIfViolations(violations);
  return {
    action,
    ...(role === undefined ? {} : { role }),
    ...(reason === undefined ? {} : { reason }),
  };
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

export function parseExpenseConfiguration(value: unknown): ExpenseConfiguration {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'title',
    'supplierName',
    'category',
    'customCategoryName',
    'amount',
    'dueDate',
    'notes',
  ]);
  const violations: FieldViolation[] = [];
  const title = readString(body.title, 'title', 1, 120, violations, true);
  const supplierName = readOptionalNullableString(
    body.supplierName,
    'supplierName',
    1,
    120,
    violations,
  );
  const category = readExpenseCategory(body.category, violations);
  const customCategoryName = readOptionalNullableString(
    body.customCategoryName,
    'customCategoryName',
    1,
    60,
    violations,
  );
  validateCustomCategory(category, customCategoryName, violations);
  const amountBody =
    typeof body.amount === 'object' && body.amount !== null && !Array.isArray(body.amount)
      ? (body.amount as JsonObject)
      : null;
  if (amountBody === null) {
    violations.push({ field: 'amount', message: 'Expected a money object.' });
  } else {
    assertAllowedKeys(amountBody, ['minorUnits', 'currency']);
  }
  const minorUnits = readRequiredInteger(
    amountBody?.minorUnits,
    'amount.minorUnits',
    1,
    999_999_999_999,
    violations,
  );
  const currency = readString(
    amountBody?.currency,
    'amount.currency',
    3,
    3,
    violations,
    true,
  ).toUpperCase();
  if (!isCurrency(currency)) {
    violations.push({ field: 'amount.currency', message: 'Use a valid currency code.' });
  }
  const dueDate = readString(body.dueDate, 'dueDate', 10, 10, violations, true);
  if (!isIsoDate(dueDate)) {
    violations.push({ field: 'dueDate', message: 'Use a valid date in YYYY-MM-DD format.' });
  }
  const notes = readOptionalNullableString(body.notes, 'notes', 1, 1000, violations);
  throwIfViolations(violations);
  return {
    title,
    ...(supplierName === undefined ? {} : { supplierName }),
    category,
    customCategoryName: category === 'custom' ? (customCategoryName ?? null) : null,
    amount: { minorUnits, currency },
    dueDate,
    ...(notes === undefined ? {} : { notes }),
  };
}

export function parseReviseExpenseRequest(value: unknown): {
  readonly configuration: ExpenseConfiguration;
  readonly reason: string;
} {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'title',
    'supplierName',
    'category',
    'customCategoryName',
    'amount',
    'dueDate',
    'notes',
    'reason',
  ]);
  const violations: FieldViolation[] = [];
  const reason = readString(body.reason, 'reason', 3, 500, violations, true);
  throwIfViolations(violations);
  const configuration = Object.fromEntries(
    Object.entries(body).filter(([key]) => key !== 'reason'),
  );
  return { configuration: parseExpenseConfiguration(configuration), reason };
}

export function parseBillingRosterUpdate(value: unknown): UpdateBillingRosterRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['couples']);
  const violations: FieldViolation[] = [];
  const rawCouples = Array.isArray(body.couples) ? body.couples : [];
  if (!Array.isArray(body.couples)) {
    violations.push({ field: 'couples', message: 'Expected a list of couples.' });
    throwIfViolations(violations);
  }
  if (rawCouples.length > 50) {
    violations.push({
      field: 'couples',
      message: 'A billing roster can contain at most 50 couples.',
    });
  }
  const couples = rawCouples.slice(0, 50).map((item: unknown, index: number) => {
    const couple = readObject(item);
    assertAllowedKeys(couple, ['primaryMembershipId', 'partnerMembershipId', 'partnerDisplayName']);
    const primaryMembershipId = readString(
      couple.primaryMembershipId,
      `couples.${String(index)}.primaryMembershipId`,
      36,
      36,
      violations,
      true,
    );
    const partnerMembershipId = readOptionalNullableString(
      couple.partnerMembershipId,
      `couples.${String(index)}.partnerMembershipId`,
      36,
      36,
      violations,
    );
    const partnerDisplayName = readOptionalNullableString(
      couple.partnerDisplayName,
      `couples.${String(index)}.partnerDisplayName`,
      1,
      80,
      violations,
    );
    if (!isUuid(primaryMembershipId))
      violations.push({
        field: `couples.${String(index)}.primaryMembershipId`,
        message: 'Choose an active household member.',
      });
    if (partnerMembershipId != null && !isUuid(partnerMembershipId))
      violations.push({
        field: `couples.${String(index)}.partnerMembershipId`,
        message: 'Choose an active household member.',
      });
    if ((partnerMembershipId == null) === (partnerDisplayName == null))
      violations.push({
        field: `couples.${String(index)}`,
        message: 'Choose either an existing member or name one partner without app access.',
      });
    return {
      primaryMembershipId,
      ...(partnerMembershipId === undefined ? {} : { partnerMembershipId }),
      ...(partnerDisplayName === undefined ? {} : { partnerDisplayName }),
    };
  });
  throwIfViolations(violations);
  return { couples };
}

export function parseHouseholdTaskConfiguration(value: unknown): HouseholdTaskConfiguration {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'title',
    'instructions',
    'zone',
    'priority',
    'dueDate',
    'dueTime',
    'estimatedMinutes',
    'assigneeMembershipId',
    'recurrenceCadence',
    'recurrenceEndsOn',
  ]);
  const violations: FieldViolation[] = [];
  const title = readString(body.title, 'title', 1, 120, violations, true);
  const instructions = readOptionalNullableString(
    body.instructions,
    'instructions',
    1,
    2000,
    violations,
  );
  const zone = readOptionalNullableString(body.zone, 'zone', 1, 80, violations);
  const priority = readHouseholdTaskPriority(body.priority, violations);
  const dueDate = readString(body.dueDate, 'dueDate', 10, 10, violations, true);
  if (!isIsoDate(dueDate))
    violations.push({ field: 'dueDate', message: 'Use a valid date in YYYY-MM-DD format.' });
  const dueTime = readOptionalTime(body.dueTime, 'dueTime', violations);
  const estimatedMinutes = readOptionalInteger(
    body.estimatedMinutes,
    'estimatedMinutes',
    5,
    1440,
    violations,
  );
  const assigneeMembershipId = readString(
    body.assigneeMembershipId,
    'assigneeMembershipId',
    36,
    36,
    violations,
    true,
  );
  if (!isUuid(assigneeMembershipId))
    violations.push({
      field: 'assigneeMembershipId',
      message: 'Use a valid active membership identifier.',
    });
  const recurrenceCadence = readOptionalNullableString(
    body.recurrenceCadence,
    'recurrenceCadence',
    6,
    16,
    violations,
  );
  if (
    recurrenceCadence != null &&
    !['weekly', 'fortnightly', 'monthly'].includes(recurrenceCadence)
  ) {
    violations.push({
      field: 'recurrenceCadence',
      message: 'Choose weekly, every two weeks, or monthly.',
    });
  }
  const recurrenceEndsOn = readOptionalNullableString(
    body.recurrenceEndsOn,
    'recurrenceEndsOn',
    10,
    10,
    violations,
  );
  if (recurrenceEndsOn != null && !isIsoDate(recurrenceEndsOn)) {
    violations.push({
      field: 'recurrenceEndsOn',
      message: 'Use a valid date in YYYY-MM-DD format.',
    });
  }
  if (recurrenceEndsOn != null && isIsoDate(dueDate) && recurrenceEndsOn < dueDate) {
    violations.push({
      field: 'recurrenceEndsOn',
      message: 'The final date cannot be before the first task.',
    });
  }
  if (recurrenceCadence == null && recurrenceEndsOn != null) {
    violations.push({
      field: 'recurrenceEndsOn',
      message: 'Choose a recurrence before setting its final date.',
    });
  }
  throwIfViolations(violations);
  return {
    title,
    ...(instructions === undefined ? {} : { instructions }),
    ...(zone === undefined ? {} : { zone }),
    priority,
    dueDate,
    ...(dueTime === undefined ? {} : { dueTime }),
    ...(estimatedMinutes === undefined ? {} : { estimatedMinutes }),
    assigneeMembershipId,
    ...(recurrenceCadence === undefined
      ? {}
      : {
          recurrenceCadence: recurrenceCadence as HouseholdTaskRecurrenceCadence | null,
        }),
    ...(recurrenceEndsOn === undefined ? {} : { recurrenceEndsOn }),
  };
}

export function parseHouseholdTaskAction(value: unknown): HouseholdTaskActionRequest {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'action',
    'note',
    'requestId',
    'requestedAssigneeMembershipId',
    'requestedDueDate',
    'requestedDueTime',
  ]);
  const violations: FieldViolation[] = [];
  const allowedActions = [
    'start',
    'complete',
    'reopen',
    'cancel',
    'stop_recurrence',
    'request_help',
    'request_swap',
    'request_postpone',
    'report_issue',
    'approve_request',
    'reject_request',
  ] as const;
  const action =
    typeof body.action === 'string' &&
    allowedActions.includes(body.action as (typeof allowedActions)[number])
      ? (body.action as (typeof allowedActions)[number])
      : 'start';
  if (body.action !== action)
    violations.push({ field: 'action', message: 'Choose a supported task action.' });
  const note = readOptionalNullableString(body.note, 'note', 3, 1000, violations);
  const requestId = readOptionalNullableString(body.requestId, 'requestId', 36, 36, violations);
  const requestedAssigneeMembershipId = readOptionalNullableString(
    body.requestedAssigneeMembershipId,
    'requestedAssigneeMembershipId',
    36,
    36,
    violations,
  );
  const requestedDueDate = readOptionalNullableString(
    body.requestedDueDate,
    'requestedDueDate',
    10,
    10,
    violations,
  );
  const requestedDueTime = readOptionalTime(body.requestedDueTime, 'requestedDueTime', violations);
  if (requestId != null && !isUuid(requestId))
    violations.push({ field: 'requestId', message: 'Use a valid request identifier.' });
  if (requestedAssigneeMembershipId != null && !isUuid(requestedAssigneeMembershipId))
    violations.push({
      field: 'requestedAssigneeMembershipId',
      message: 'Use a valid membership identifier.',
    });
  if (requestedDueDate != null && !isIsoDate(requestedDueDate))
    violations.push({
      field: 'requestedDueDate',
      message: 'Use a valid date in YYYY-MM-DD format.',
    });
  if (
    [
      'complete',
      'cancel',
      'stop_recurrence',
      'request_help',
      'request_swap',
      'request_postpone',
      'report_issue',
      'reject_request',
    ].includes(action) &&
    note == null
  ) {
    violations.push({
      field: 'note',
      message: 'Explain this task action in at least 3 characters.',
    });
  }
  if ((action === 'approve_request' || action === 'reject_request') && requestId == null)
    violations.push({ field: 'requestId', message: 'Choose the pending request.' });
  if (action === 'request_swap' && requestedAssigneeMembershipId == null)
    violations.push({
      field: 'requestedAssigneeMembershipId',
      message: 'Choose the proposed replacement.',
    });
  if (action === 'request_postpone' && requestedDueDate == null)
    violations.push({ field: 'requestedDueDate', message: 'Choose the requested later date.' });
  throwIfViolations(violations);
  return {
    action,
    ...(note === undefined ? {} : { note }),
    ...(requestId === undefined ? {} : { requestId }),
    ...(requestedAssigneeMembershipId === undefined ? {} : { requestedAssigneeMembershipId }),
    ...(requestedDueDate === undefined ? {} : { requestedDueDate }),
    ...(requestedDueTime === undefined ? {} : { requestedDueTime }),
  };
}

export function parseExpenseTemplateConfiguration(value: unknown): ExpenseTemplateConfiguration {
  const body = readObject(value);
  assertAllowedKeys(body, [
    'title',
    'category',
    'customCategoryName',
    'amount',
    'cadence',
    'nextDueDate',
    'endsOn',
    'notes',
  ]);
  const violations: FieldViolation[] = [];
  const title = readString(body.title, 'title', 1, 120, violations, true);
  const category = readExpenseCategory(body.category, violations);
  const customCategoryName = readOptionalNullableString(
    body.customCategoryName,
    'customCategoryName',
    1,
    60,
    violations,
  );
  validateCustomCategory(category, customCategoryName, violations);
  const amountBody =
    typeof body.amount === 'object' && body.amount !== null && !Array.isArray(body.amount)
      ? (body.amount as JsonObject)
      : null;
  if (amountBody === null)
    violations.push({ field: 'amount', message: 'Expected a money object.' });
  else assertAllowedKeys(amountBody, ['minorUnits', 'currency']);
  const minorUnits = readRequiredInteger(
    amountBody?.minorUnits,
    'amount.minorUnits',
    1,
    999_999_999_999,
    violations,
  );
  const currency = readString(
    amountBody?.currency,
    'amount.currency',
    3,
    3,
    violations,
    true,
  ).toUpperCase();
  if (!isCurrency(currency))
    violations.push({ field: 'amount.currency', message: 'Use a valid currency code.' });
  const cadence = readExpenseTemplateCadence(body.cadence, violations);
  const nextDueDate = readString(body.nextDueDate, 'nextDueDate', 10, 10, violations, true);
  if (!isIsoDate(nextDueDate)) {
    violations.push({ field: 'nextDueDate', message: 'Use a valid date in YYYY-MM-DD format.' });
  }
  const endsOn = readOptionalNullableString(body.endsOn, 'endsOn', 10, 10, violations);
  if (endsOn != null && !isIsoDate(endsOn)) {
    violations.push({ field: 'endsOn', message: 'Use a valid date in YYYY-MM-DD format.' });
  } else if (endsOn != null && isIsoDate(nextDueDate) && endsOn < nextDueDate) {
    violations.push({
      field: 'endsOn',
      message: 'The final date cannot be before the first due date.',
    });
  }
  const notes = readOptionalNullableString(body.notes, 'notes', 1, 1000, violations);
  throwIfViolations(violations);
  return {
    title,
    category,
    customCategoryName: category === 'custom' ? (customCategoryName ?? null) : null,
    amount: { minorUnits, currency },
    cadence,
    nextDueDate,
    ...(endsOn === undefined ? {} : { endsOn }),
    ...(notes === undefined ? {} : { notes }),
  };
}

export function parseArchiveExpenseTemplateRequest(value: unknown): ArchiveExpenseTemplateRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['reason']);
  const violations: FieldViolation[] = [];
  const reason = readString(body.reason, 'reason', 3, 500, violations, true);
  throwIfViolations(violations);
  return { reason };
}

export function parseReverseExpenseRequest(value: unknown): ReverseExpenseRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['reason']);
  const violations: FieldViolation[] = [];
  const reason = readString(body.reason, 'reason', 3, 500, violations, true);
  throwIfViolations(violations);
  return { reason };
}

export function parseExpensePaymentDeclarationRequest(
  value: unknown,
): ExpensePaymentDeclarationRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['method', 'paidAt', 'reference', 'note']);
  const violations: FieldViolation[] = [];
  const method = readExpensePaymentMethod(body.method, violations);
  const paidAt = readString(body.paidAt, 'paidAt', 20, 35, violations, true);
  if (!isIsoInstant(paidAt)) {
    violations.push({
      field: 'paidAt',
      message: 'Use an ISO 8601 date-time with a UTC or numeric offset.',
    });
  }
  const reference = readOptionalNullableString(body.reference, 'reference', 1, 120, violations);
  const note = readOptionalNullableString(body.note, 'note', 1, 500, violations);
  throwIfViolations(violations);
  return {
    method,
    paidAt,
    ...(reference === undefined ? {} : { reference }),
    ...(note === undefined ? {} : { note }),
  };
}

export function parseExpensePaymentActionRequest(value: unknown): ExpensePaymentActionRequest {
  const body = readObject(value);
  assertAllowedKeys(body, ['reason']);
  const violations: FieldViolation[] = [];
  const reason = readString(body.reason, 'reason', 3, 500, violations, true);
  throwIfViolations(violations);
  return { reason };
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

function readRequiredInteger(
  value: unknown,
  field: string,
  minimum: number,
  maximum: number,
  violations: FieldViolation[],
): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value)) {
    violations.push({ field, message: 'Expected a whole number.' });
    return minimum;
  }
  if (value < minimum || value > maximum) {
    violations.push({
      field,
      message: `Use a value between ${String(minimum)} and ${String(maximum)}.`,
    });
  }
  return value;
}

function readExpenseCategory(value: unknown, violations: FieldViolation[]): ExpenseCategory {
  if (typeof value === 'string' && EXPENSE_CATEGORIES.includes(value as ExpenseCategory)) {
    return value as ExpenseCategory;
  }
  violations.push({ field: 'category', message: 'Choose a supported expense category.' });
  return 'other';
}

function readHouseholdTaskPriority(
  value: unknown,
  violations: FieldViolation[],
): HouseholdTaskPriority {
  if (value === 'low' || value === 'normal' || value === 'high') return value;
  violations.push({ field: 'priority', message: 'Choose low, normal, or high priority.' });
  return 'normal';
}

function readExpenseTemplateCadence(
  value: unknown,
  violations: FieldViolation[],
): ExpenseTemplateCadence {
  if (
    typeof value === 'string' &&
    EXPENSE_TEMPLATE_CADENCES.includes(value as ExpenseTemplateCadence)
  ) {
    return value as ExpenseTemplateCadence;
  }
  violations.push({
    field: 'cadence',
    message: 'Choose weekly, every two weeks, monthly, quarterly, or yearly.',
  });
  return 'monthly';
}

function readExpensePaymentMethod(
  value: unknown,
  violations: FieldViolation[],
): ExpensePaymentMethod {
  if (
    typeof value === 'string' &&
    EXPENSE_PAYMENT_METHODS.includes(value as ExpensePaymentMethod)
  ) {
    return value as ExpensePaymentMethod;
  }
  violations.push({ field: 'method', message: 'Choose a supported payment method.' });
  return 'other';
}

function validateCustomCategory(
  category: ExpenseCategory,
  customCategoryName: string | null | undefined,
  violations: FieldViolation[],
): void {
  if (category === 'custom' && (customCategoryName === undefined || customCategoryName === null)) {
    violations.push({ field: 'customCategoryName', message: 'Name the custom category.' });
  }
  if (category !== 'custom' && customCategoryName !== undefined && customCategoryName !== null) {
    violations.push({
      field: 'customCategoryName',
      message: 'Custom category text requires category custom.',
    });
  }
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

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(value);
}

function isIsoInstant(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})$/u.test(value)) {
    return false;
  }
  return !Number.isNaN(Date.parse(value));
}

function throwIfViolations(violations: readonly FieldViolation[]): void {
  if (violations.length > 0) {
    throw validationProblem(violations);
  }
}

const MillisecondsPerDay = 86_400_000;
const MaximumCalendarRangeDays = 370;
