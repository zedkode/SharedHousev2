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

export const EXPENSE_CATEGORIES = [
  'rent',
  'electricity',
  'gas',
  'water',
  'internet',
  'council_tax',
  'groceries',
  'household_supplies',
  'maintenance',
  'other',
  'custom',
] as const;
export type ExpenseCategory = (typeof EXPENSE_CATEGORIES)[number];
export type ExpenseStatus = 'proposed' | 'approved' | 'reversed';
export const EXPENSE_TEMPLATE_CADENCES = [
  'weekly',
  'fortnightly',
  'monthly',
  'quarterly',
  'yearly',
] as const;
export type ExpenseTemplateCadence = (typeof EXPENSE_TEMPLATE_CADENCES)[number];
export const EXPENSE_PAYMENT_METHODS = [
  'bank_transfer',
  'cash',
  'card',
  'direct_debit',
  'other',
] as const;
export type ExpensePaymentMethod = (typeof EXPENSE_PAYMENT_METHODS)[number];
export type ExpensePaymentStatus = 'declared' | 'confirmed' | 'disputed' | 'reversed';

export const HOUSEHOLD_TASK_PRIORITIES = ['low', 'normal', 'high'] as const;
export type HouseholdTaskPriority = (typeof HOUSEHOLD_TASK_PRIORITIES)[number];
export const HOUSEHOLD_TASK_RECURRENCE_CADENCES = ['weekly', 'fortnightly', 'monthly'] as const;
export type HouseholdTaskRecurrenceCadence = (typeof HOUSEHOLD_TASK_RECURRENCE_CADENCES)[number];
export type HouseholdTaskStatus = 'open' | 'in_progress' | 'completed' | 'cancelled';
export const HOUSEHOLD_TASK_REQUEST_TYPES = ['help', 'swap', 'postpone', 'issue'] as const;
export type HouseholdTaskRequestType = (typeof HOUSEHOLD_TASK_REQUEST_TYPES)[number];
export type HouseholdTaskRequestStatus = 'pending' | 'approved' | 'rejected' | 'cancelled';

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
  transferredHouseholdIds: string[];
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
  householdTasks: HouseholdTaskSummary[];
  expenses: ExpenseSummary[];
  expenseTemplates: ExpenseTemplateSummary[];
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

export type HouseholdMemberRole = HouseholdSummary['role'];
export type HouseholdMembershipStatus = 'active' | 'suspended' | 'left' | 'removed';

export interface HouseholdMemberSummary {
  readonly membershipId: string;
  readonly userId: string;
  readonly displayName: string;
  readonly role: HouseholdMemberRole;
  readonly status: HouseholdMembershipStatus;
  readonly isCurrentUser: boolean;
  readonly canChangeRole: boolean;
  readonly canSuspend: boolean;
  readonly canReactivate: boolean;
  readonly canRemove: boolean;
  readonly canTransferOwnership: boolean;
  readonly assignableRoles: readonly Exclude<HouseholdMemberRole, 'owner'>[];
  readonly joinedAt: string;
  readonly updatedAt: string;
  readonly version: number;
}

export interface HouseholdMemberBoard {
  readonly canInvite: boolean;
  readonly canEditHousehold: boolean;
  readonly members: readonly HouseholdMemberSummary[];
}

export interface HouseholdChatMessage {
  readonly id: string;
  readonly householdId: string;
  readonly senderMembershipId: string;
  readonly senderUserId: string;
  readonly senderDisplayName: string;
  readonly isCurrentUser: boolean;
  readonly body: string;
  readonly createdAt: string;
}

export interface HouseholdChatPage {
  readonly messages: readonly HouseholdChatMessage[];
  readonly nextCursor: string | null;
}

export interface CreateHouseholdChatMessageRequest {
  readonly body: string;
}

export interface HouseholdMemberActionRequest {
  readonly action: 'change_role' | 'suspend' | 'reactivate' | 'remove' | 'transfer_ownership';
  readonly role?: Exclude<HouseholdMemberRole, 'owner'> | null;
  readonly reason?: string | null;
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

export interface HouseholdTaskConfiguration {
  readonly title: string;
  readonly instructions?: string | null;
  readonly zone?: string | null;
  readonly priority: HouseholdTaskPriority;
  readonly dueDate: string;
  readonly dueTime?: string | null;
  readonly estimatedMinutes?: number | null;
  readonly assigneeMembershipId: string;
  readonly recurrenceCadence?: HouseholdTaskRecurrenceCadence | null;
  readonly recurrenceEndsOn?: string | null;
}

export interface HouseholdTaskMemberSummary {
  readonly membershipId: string;
  readonly userId: string;
  readonly displayName: string;
  readonly role: HouseholdSummary['role'];
  readonly isCurrentUser: boolean;
}

export interface HouseholdTaskRequestSummary {
  readonly id: string;
  readonly type: HouseholdTaskRequestType;
  readonly status: HouseholdTaskRequestStatus;
  readonly reason: string;
  readonly requestedAssigneeMembershipId: string | null;
  readonly requestedDueDate: string | null;
  readonly requestedDueTime: string | null;
  readonly createdByMembershipId: string;
  readonly createdByDisplayName: string;
  readonly resolvedByUserId: string | null;
  readonly resolvedByDisplayName: string | null;
  readonly resolutionNote: string | null;
  readonly resolvedAt: string | null;
  readonly createdAt: string;
}

export interface HouseholdTaskSummary extends HouseholdTaskConfiguration {
  readonly id: string;
  readonly householdId: string;
  readonly instructions: string | null;
  readonly zone: string | null;
  readonly dueTime: string | null;
  readonly estimatedMinutes: number | null;
  readonly assigneeDisplayName: string;
  readonly seriesId: string | null;
  readonly occurrenceDate: string | null;
  readonly recurrenceActive: boolean;
  readonly status: HouseholdTaskStatus;
  readonly completionNote: string | null;
  readonly completedByUserId: string | null;
  readonly completedAt: string | null;
  readonly requests: readonly HouseholdTaskRequestSummary[];
  readonly canManage: boolean;
  readonly canStart: boolean;
  readonly canComplete: boolean;
  readonly canRequest: boolean;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface HouseholdTaskBoard {
  readonly canCreate: boolean;
  readonly members: readonly HouseholdTaskMemberSummary[];
  readonly tasks: readonly HouseholdTaskSummary[];
}

export interface HouseholdTaskActionRequest {
  readonly action:
    | 'start'
    | 'complete'
    | 'reopen'
    | 'cancel'
    | 'stop_recurrence'
    | 'request_help'
    | 'request_swap'
    | 'request_postpone'
    | 'report_issue'
    | 'approve_request'
    | 'reject_request';
  readonly note?: string | null;
  readonly requestId?: string | null;
  readonly requestedAssigneeMembershipId?: string | null;
  readonly requestedDueDate?: string | null;
  readonly requestedDueTime?: string | null;
}

export interface ExpenseConfiguration {
  readonly title: string;
  readonly supplierName?: string | null;
  readonly category: ExpenseCategory;
  readonly customCategoryName?: string | null;
  readonly amount: Money;
  readonly dueDate: string;
  readonly notes?: string | null;
}

export interface BillingRosterMemberSummary {
  readonly membershipId: string;
  readonly displayName: string;
  readonly isCurrentUser: boolean;
}

export interface BillingCoupleConfiguration {
  readonly primaryMembershipId: string;
  readonly partnerMembershipId?: string | null;
  readonly partnerDisplayName?: string | null;
}

export interface BillingCoupleSummary extends BillingCoupleConfiguration {
  readonly id: string;
  readonly primaryDisplayName: string;
  readonly partnerDisplayName: string;
}

export interface BillingRosterSummary {
  readonly householdId: string;
  readonly members: readonly BillingRosterMemberSummary[];
  readonly couples: readonly BillingCoupleSummary[];
  readonly residentCount: number;
  readonly billingUnitCount: number;
  readonly canManage: boolean;
  readonly version: number;
  readonly updatedAt: string;
}

export interface UpdateBillingRosterRequest {
  readonly couples: readonly BillingCoupleConfiguration[];
}

export type CreateExpenseRequest = ExpenseConfiguration;

export interface ExpensePaymentDeclarationRequest {
  readonly method: ExpensePaymentMethod;
  readonly paidAt: string;
  readonly reference?: string | null;
  readonly note?: string | null;
}

export interface ExpensePaymentActionRequest {
  readonly reason: string;
}

export interface ExpensePaymentSummary {
  readonly id: string;
  readonly expenseId: string;
  readonly allocationMembershipId: string;
  readonly payerDisplayName: string;
  readonly amount: Money;
  readonly method: ExpensePaymentMethod;
  readonly reference: string | null;
  readonly note: string | null;
  readonly paidAt: string;
  readonly status: ExpensePaymentStatus;
  readonly declaredByUserId: string;
  readonly declaredByDisplayName: string;
  readonly confirmedByUserId: string | null;
  readonly confirmedByDisplayName: string | null;
  readonly confirmedAt: string | null;
  readonly disputedByUserId: string | null;
  readonly disputedByDisplayName: string | null;
  readonly disputedAt: string | null;
  readonly disputeReason: string | null;
  readonly reversedByUserId: string | null;
  readonly reversedByDisplayName: string | null;
  readonly reversedAt: string | null;
  readonly reversalReason: string | null;
  readonly canConfirm: boolean;
  readonly canDispute: boolean;
  readonly canReverse: boolean;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ExpenseAllocationSummary {
  readonly membershipId: string;
  readonly displayName: string;
  readonly billingUnitType: 'individual' | 'couple';
  readonly participantCount: 1 | 2;
  readonly eligibleMembershipIds: readonly string[];
  readonly amount: Money;
  readonly roundingAdjustmentMinor: number;
  readonly status: 'outstanding' | 'declared' | 'paid' | 'disputed';
  readonly paymentDeclarations: readonly ExpensePaymentSummary[];
  readonly canDeclarePayment: boolean;
  readonly isCurrentUser: boolean;
}

export interface ExpenseSummary extends ExpenseConfiguration {
  readonly id: string;
  readonly householdId: string;
  readonly notes: string | null;
  readonly supplierName: string | null;
  readonly revisionOfExpenseId: string | null;
  readonly supersededByExpenseId: string | null;
  readonly sourceTemplateId: string | null;
  readonly occurrenceDate: string | null;
  readonly splitMethod: 'equal';
  readonly status: ExpenseStatus;
  readonly allocations: readonly ExpenseAllocationSummary[];
  readonly currentUserShare: Money;
  readonly createdByUserId: string;
  readonly canApprove: boolean;
  readonly canReverse: boolean;
  readonly canRevise: boolean;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ReverseExpenseRequest {
  readonly reason: string;
}

export interface ReviseExpenseRequest extends ExpenseConfiguration {
  readonly reason: string;
}

export interface ExpenseTemplateConfiguration {
  readonly title: string;
  readonly category: ExpenseCategory;
  readonly customCategoryName?: string | null;
  readonly amount: Money;
  readonly cadence: ExpenseTemplateCadence;
  readonly nextDueDate: string;
  /** Inclusive final occurrence date. Null means the series continues until an admin archives it. */
  readonly endsOn?: string | null;
  readonly notes?: string | null;
}

export interface ExpenseTemplateSummary extends ExpenseTemplateConfiguration {
  readonly id: string;
  readonly householdId: string;
  readonly customCategoryName: string | null;
  readonly notes: string | null;
  readonly endsOn: string | null;
  readonly status: 'active' | 'archived';
  readonly canManage: boolean;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ArchiveExpenseTemplateRequest {
  readonly reason: string;
}
