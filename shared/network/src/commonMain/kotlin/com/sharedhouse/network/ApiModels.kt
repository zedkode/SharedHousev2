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
