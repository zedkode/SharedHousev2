package com.sharedhouse.network

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val displayName: String,
    val preferredLocale: String,
)

@Serializable
data class RegisterPayload(
    val email: String,
    val password: String,
    val displayName: String,
    val preferredLocale: String,
    val ageConfirmed: Boolean,
    val termsAccepted: Boolean,
    val marketingConsent: Boolean,
)

@Serializable
data class RegistrationAcceptedDto(
    val verificationRequired: Boolean,
    val developmentVerificationCode: String? = null,
)

@Serializable
data class ResendVerificationPayload(
    val email: String,
)

@Serializable
data class VerifyEmailPayload(
    val email: String,
    val code: String,
    val deviceName: String? = null,
)

@Serializable
data class SignInPayload(
    val email: String,
    val password: String,
    val deviceName: String? = null,
)

@Serializable
data class RefreshSessionPayload(
    val refreshToken: String,
)

@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String,
    val account: AccountDto,
)

@Serializable
data class DeleteAccountPayload(
    val password: String,
    val confirmation: String = "DELETE",
)

@Serializable
data class AccountDeletionResultDto(
    val status: String,
    val closedHouseholdIds: List<String>,
    val transferredHouseholdIds: List<String>,
)

@Serializable
data class ExportAccountPayload(val password: String)

@Serializable
data class AccountExportConsentDto(
    val purpose: String,
    val policyVersion: String,
    val granted: Boolean,
    val recordedAt: String,
)

@Serializable
data class AccountExportSessionDto(
    val deviceName: String,
    val authenticatedAt: String,
    val lastSeenAt: String,
    val revokedAt: String? = null,
)

@Serializable
data class AccountExportInvitationDto(
    val id: String,
    val householdId: String,
    val role: String,
    val email: String? = null,
    val status: String,
    val expiresAt: String,
    val createdAt: String,
)

@Serializable
data class AccountExportDto(
    val formatVersion: String,
    val generatedAt: String,
    val account: AccountDto,
    val households: List<HouseholdDto>,
    val calendarEvents: List<CalendarEventDto>,
    val householdTasks: List<HouseholdTaskDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList(),
    val expenseTemplates: List<ExpenseTemplateDto> = emptyList(),
    val consentRecords: List<AccountExportConsentDto>,
    val sessions: List<AccountExportSessionDto>,
    val invitations: List<AccountExportInvitationDto>,
)

@Serializable
data class HouseholdConfigurationDto(
    val name: String,
    val countryCode: String,
    val timezone: String,
    val currency: String,
    val firstDayOfWeek: Int,
    val cycleType: String,
    val cycleAnchor: String,
)

@Serializable
data class HouseholdDto(
    val id: String,
    val name: String,
    val countryCode: String,
    val timezone: String,
    val currency: String,
    val firstDayOfWeek: Int,
    val cycleType: String,
    val cycleAnchor: String,
    val role: String,
    val status: String,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class HouseholdMemberDto(
    val membershipId: String,
    val userId: String,
    val displayName: String,
    val role: String,
    val status: String,
    val isCurrentUser: Boolean,
    val canChangeRole: Boolean,
    val canSuspend: Boolean,
    val canReactivate: Boolean,
    val canRemove: Boolean,
    val canTransferOwnership: Boolean,
    val assignableRoles: List<String>,
    val joinedAt: String,
    val updatedAt: String,
    val version: Int,
)

@Serializable
data class HouseholdMemberBoardDto(
    val canInvite: Boolean,
    val canEditHousehold: Boolean,
    val members: List<HouseholdMemberDto>,
)

@Serializable
data class HouseholdMemberActionDto(
    val action: String,
    val role: String? = null,
    val reason: String? = null,
)

@Serializable
data class CreateHouseholdInvitationPayload(
    val role: String,
    val email: String? = null,
)

@Serializable
data class HouseholdInvitationDto(
    val id: String,
    val householdId: String,
    val householdName: String,
    val role: String,
    val email: String? = null,
    val status: String,
    val expiresAt: String,
    val createdAt: String,
    val token: String? = null,
)

@Serializable
data class HouseholdInvitationPreviewDto(
    val householdName: String,
    val role: String,
    val emailRestricted: Boolean,
    val status: String,
    val expiresAt: String,
)

@Serializable
data class AcceptHouseholdInvitationDto(
    val household: HouseholdDto,
)

@Serializable
data class CalendarEventConfigurationDto(
    val title: String,
    val description: String? = null,
    val type: String,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val reminderMinutesBefore: Int? = null,
)

@Serializable
data class CalendarEventDto(
    val id: String,
    val householdId: String,
    val title: String,
    val description: String? = null,
    val type: String,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val reminderMinutesBefore: Int? = null,
    val createdByUserId: String,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class HouseholdTaskConfigurationDto(
    val title: String,
    val instructions: String? = null,
    val zone: String? = null,
    val priority: String,
    val dueDate: String,
    val dueTime: String? = null,
    val estimatedMinutes: Int? = null,
    val assigneeMembershipId: String,
)

@Serializable
data class HouseholdTaskMemberDto(
    val membershipId: String,
    val userId: String,
    val displayName: String,
    val role: String,
    val isCurrentUser: Boolean,
)

@Serializable
data class HouseholdTaskRequestDto(
    val id: String,
    val type: String,
    val status: String,
    val reason: String,
    val requestedAssigneeMembershipId: String? = null,
    val requestedDueDate: String? = null,
    val requestedDueTime: String? = null,
    val createdByMembershipId: String,
    val createdByDisplayName: String,
    val resolvedByUserId: String? = null,
    val resolutionNote: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String,
)

@Serializable
data class HouseholdTaskDto(
    val id: String,
    val householdId: String,
    val title: String,
    val instructions: String? = null,
    val zone: String? = null,
    val priority: String,
    val dueDate: String,
    val dueTime: String? = null,
    val estimatedMinutes: Int? = null,
    val assigneeMembershipId: String,
    val assigneeDisplayName: String,
    val status: String,
    val completionNote: String? = null,
    val completedByUserId: String? = null,
    val completedAt: String? = null,
    val requests: List<HouseholdTaskRequestDto> = emptyList(),
    val canManage: Boolean,
    val canStart: Boolean,
    val canComplete: Boolean,
    val canRequest: Boolean,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class HouseholdTaskBoardDto(
    val canCreate: Boolean,
    val members: List<HouseholdTaskMemberDto>,
    val tasks: List<HouseholdTaskDto>,
)

@Serializable
data class HouseholdTaskActionDto(
    val action: String,
    val note: String? = null,
    val requestId: String? = null,
    val requestedAssigneeMembershipId: String? = null,
    val requestedDueDate: String? = null,
    val requestedDueTime: String? = null,
)

@Serializable
data class MoneyDto(
    val minorUnits: Long,
    val currency: String,
)

@Serializable
data class ExpenseConfigurationDto(
    val title: String,
    val category: String,
    val customCategoryName: String? = null,
    val amount: MoneyDto,
    val dueDate: String,
    val notes: String? = null,
)

@Serializable
data class ExpenseAllocationDto(
    val membershipId: String,
    val displayName: String,
    val amount: MoneyDto,
    val roundingAdjustmentMinor: Long,
    val status: String,
    val isCurrentUser: Boolean,
    val paymentDeclarations: List<ExpensePaymentDto> = emptyList(),
    val canDeclarePayment: Boolean = false,
)

@Serializable
data class ExpensePaymentDto(
    val id: String,
    val expenseId: String,
    val allocationMembershipId: String,
    val payerDisplayName: String,
    val amount: MoneyDto,
    val method: String,
    val reference: String? = null,
    val note: String? = null,
    val paidAt: String,
    val status: String,
    val declaredByUserId: String,
    val confirmedByUserId: String? = null,
    val confirmedAt: String? = null,
    val disputedByUserId: String? = null,
    val disputedAt: String? = null,
    val disputeReason: String? = null,
    val reversedByUserId: String? = null,
    val reversedAt: String? = null,
    val reversalReason: String? = null,
    val canConfirm: Boolean,
    val canDispute: Boolean,
    val canReverse: Boolean,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ExpensePaymentDeclarationDto(
    val method: String,
    val paidAt: String,
    val reference: String? = null,
    val note: String? = null,
)

@Serializable
data class ExpensePaymentActionPayload(
    val reason: String,
)

@Serializable
data class ExpenseDto(
    val id: String,
    val householdId: String,
    val title: String,
    val category: String,
    val customCategoryName: String? = null,
    val amount: MoneyDto,
    val dueDate: String,
    val notes: String? = null,
    val sourceTemplateId: String? = null,
    val occurrenceDate: String? = null,
    val splitMethod: String,
    val status: String,
    val allocations: List<ExpenseAllocationDto>,
    val currentUserShare: MoneyDto,
    val createdByUserId: String,
    val canApprove: Boolean,
    val canReverse: Boolean,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ReverseExpensePayload(
    val reason: String,
)

@Serializable
data class ExpenseTemplateConfigurationDto(
    val title: String,
    val category: String,
    val customCategoryName: String? = null,
    val amount: MoneyDto,
    val cadence: String,
    val nextDueDate: String,
    val notes: String? = null,
)

@Serializable
data class ExpenseTemplateDto(
    val id: String,
    val householdId: String,
    val title: String,
    val category: String,
    val customCategoryName: String? = null,
    val amount: MoneyDto,
    val cadence: String,
    val nextDueDate: String,
    val notes: String? = null,
    val status: String,
    val canManage: Boolean,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ArchiveExpenseTemplatePayload(val reason: String)

@Serializable
data class FieldViolationDto(
    val field: String,
    val message: String,
)

@Serializable
data class ProblemDetailsDto(
    val type: String,
    val title: String,
    val status: Int,
    val code: String,
    val correlationId: String,
    val detail: String? = null,
    val violations: List<FieldViolationDto> = emptyList(),
)

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(
        val code: String,
        val title: String,
        val status: Int? = null,
        val correlationId: String? = null,
        val violations: List<FieldViolationDto> = emptyList(),
    ) : ApiResult<Nothing>
}
