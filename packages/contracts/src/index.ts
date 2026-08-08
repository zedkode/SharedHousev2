export const API_VERSION = 'v1' as const;

export const SUPPORTED_LOCALES = ['en', 'ro'] as const;
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

export const HOUSEHOLD_CYCLE_TYPES = ['weekly', 'fourteen_day', 'calendar_month'] as const;
export type HouseholdCycleType = (typeof HOUSEHOLD_CYCLE_TYPES)[number];

export const HOUSEHOLD_INVITATION_ROLES = ['admin', 'member', 'read_only'] as const;
export type HouseholdInvitationRole = (typeof HOUSEHOLD_INVITATION_ROLES)[number];
export type HouseholdInvitationStatus = 'pending' | 'accepted' | 'revoked' | 'expired';

export const CALENDAR_EVENT_TYPES = [
  'household',
  'maintenance',
  'appointment',
  'shopping',
  'other',
] as const;
export type CalendarEventType = (typeof CALENDAR_EVENT_TYPES)[number];

export interface ServiceHealth {
  readonly status: 'ok';
  readonly service: 'api' | 'workers';
  readonly apiVersion: typeof API_VERSION;
  readonly checkedAt: string;
}

export interface ProblemDetails {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly code: string;
  readonly correlationId: string;
  readonly detail?: string;
  readonly violations?: readonly {
    readonly field: string;
    readonly message: string;
  }[];
}

export interface Money {
  readonly minorUnits: number;
  readonly currency: string;
}

export interface AccountSummary {
  readonly id: string;
  readonly email: string;
  readonly emailVerified: boolean;
  readonly displayName: string;
  readonly preferredLocale: SupportedLocale;
}

export interface DeleteAccountRequest {
  password: string;
  confirmation: 'DELETE';
}

export interface AccountDeletionResult {
  status: 'completed';
  closedHouseholdIds: string[];
}

export interface ExportAccountRequest {
  password: string;
}

export interface AccountExportConsentRecord {
  purpose: string;
  policyVersion: string;
  granted: boolean;
  recordedAt: string;
}

export interface AccountExportSession {
  deviceName: string;
  authenticatedAt: string;
  lastSeenAt: string;
  revokedAt: string | null;
}

export interface AccountExportInvitation {
  id: string;
  householdId: string;
  role: HouseholdInvitationRole;
  email: string | null;
  status: HouseholdInvitationStatus;
  expiresAt: string;
  createdAt: string;
}

export interface AccountExport {
  formatVersion: '1';
  generatedAt: string;
  account: AccountSummary;
  households: HouseholdSummary[];
  calendarEvents: CalendarEventSummary[];
  consentRecords: AccountExportConsentRecord[];
  sessions: AccountExportSession[];
  invitations: AccountExportInvitation[];
}

export interface RegisterRequest {
  readonly email: string;
  readonly password: string;
  readonly displayName: string;
  readonly preferredLocale: SupportedLocale;
  readonly ageConfirmed: true;
  readonly termsAccepted: true;
  readonly marketingConsent: boolean;
}

export interface RegistrationAccepted {
  readonly verificationRequired: true;
  readonly developmentVerificationCode?: string;
}

export interface ResendEmailVerificationRequest {
  readonly email: string;
}

export interface VerifyEmailRequest {
  readonly email: string;
  readonly code: string;
  readonly deviceName?: string;
}

export interface SignInRequest {
  readonly email: string;
  readonly password: string;
  readonly deviceName?: string;
}

export interface RefreshSessionRequest {
  readonly refreshToken: string;
}

export interface SessionResponse {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly accessTokenExpiresAt: string;
  readonly refreshTokenExpiresAt: string;
  readonly account: AccountSummary;
}

export interface HouseholdConfiguration {
  readonly name: string;
  readonly countryCode: string;
  readonly timezone: string;
  readonly currency: string;
  readonly firstDayOfWeek: 1 | 6 | 7;
  readonly cycleType: HouseholdCycleType;
  readonly cycleAnchor: string;
}

export type CreateHouseholdRequest = HouseholdConfiguration;

export type UpdateHouseholdRequest = HouseholdConfiguration;

export interface HouseholdSummary extends HouseholdConfiguration {
  readonly id: string;
  readonly role: 'owner' | 'admin' | 'member' | 'read_only';
  readonly status: 'active';
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateHouseholdInvitationRequest {
  readonly role: HouseholdInvitationRole;
  readonly email?: string | null;
}

export interface HouseholdInvitationSummary {
  readonly id: string;
  readonly householdId: string;
  readonly householdName: string;
  readonly role: HouseholdInvitationRole;
  readonly email: string | null;
  readonly status: HouseholdInvitationStatus;
  readonly expiresAt: string;
  readonly createdAt: string;
}

export interface HouseholdInvitationCreated extends HouseholdInvitationSummary {
  readonly token: string;
}

export interface HouseholdInvitationPreview {
  readonly householdName: string;
  readonly role: HouseholdInvitationRole;
  readonly emailRestricted: boolean;
  readonly status: 'pending' | 'expired' | 'unavailable';
  readonly expiresAt: string;
}

export interface AcceptHouseholdInvitationResponse {
  readonly household: HouseholdSummary;
}

export interface CalendarEventConfiguration {
  readonly title: string;
  readonly description?: string | null;
  readonly type: CalendarEventType;
  readonly date: string;
  readonly startTime?: string | null;
  readonly endTime?: string | null;
  readonly reminderMinutesBefore?: number | null;
}

export type CreateCalendarEventRequest = CalendarEventConfiguration;

export type UpdateCalendarEventRequest = CalendarEventConfiguration;

export interface CalendarEventSummary {
  readonly id: string;
  readonly householdId: string;
  readonly title: string;
  readonly description: string | null;
  readonly type: CalendarEventType;
  readonly date: string;
  readonly startTime: string | null;
  readonly endTime: string | null;
  readonly reminderMinutesBefore: number | null;
  readonly createdByUserId: string;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}
