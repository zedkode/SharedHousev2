package com.sharedhouse.android.ui.app

import com.sharedhouse.android.ui.calendar.CalendarUiState
import com.sharedhouse.network.AccountDto
import com.sharedhouse.network.AccountExportDto
import com.sharedhouse.network.HouseholdDto
import com.sharedhouse.network.HouseholdInvitationDto
import com.sharedhouse.network.HouseholdInvitationPreviewDto

enum class AppRoute(val path: String) {
    Welcome("welcome"),
    Register("register"),
    VerifyEmail("verify-email"),
    SignIn("sign-in"),
    HouseholdGate("household-gate"),
    HouseholdChoice("household-choice"),
    HouseholdSetup("household-setup"),
    InvitationJoin("invitation-join"),
    InvitationManage("invitation-manage"),
    Home("home"),
}

enum class HouseholdEditorMode {
    Create,
    Edit,
}

enum class FormField {
    Email,
    Password,
    DisplayName,
    VerificationCode,
    AgeConfirmation,
    TermsConfirmation,
    HouseholdName,
    CountryCode,
    Timezone,
    Currency,
    CycleAnchor,
    InvitationToken,
    InvitationEmail,
}

enum class UiMessage {
    DisplayNameRequired,
    EmailInvalid,
    PasswordRequired,
    PasswordTooShort,
    AgeConfirmationRequired,
    TermsConfirmationRequired,
    VerificationCodeInvalidInput,
    HouseholdNameRequired,
    CountryCodeInvalid,
    TimezoneInvalid,
    CurrencyInvalid,
    CycleAnchorInvalid,
    InvalidCredentials,
    EmailVerificationRequired,
    VerificationCodeInvalid,
    VerificationCodeExpired,
    AccountUnavailable,
    SessionExpired,
    SecureSessionReset,
    SecureStorageUnavailable,
    SessionRestoreNetworkUnavailable,
    IdempotencyKeyReused,
    RequestInvalid,
    RateLimited,
    NetworkUnavailable,
    ServiceUnavailable,
    HouseholdLoadFailed,
    HouseholdVersionConflict,
    HouseholdReloaded,
    InvitationTokenInvalid,
    InvitationNotFound,
    InvitationExpired,
    InvitationUnavailable,
    InvitationEmailMismatch,
    InvitationManageForbidden,
    InvitationRoleForbidden,
    InvitationCreated,
    InvitationAccepted,
    InvitationRevoked,
    RegistrationAccepted,
    VerificationCodeSent,
    SignedOut,
    SessionRevocationUnconfirmed,
    AccountDeleted,
    AccountDeletionOwnerTransferRequired,
    RecentAuthenticationRequired,
    AccountDeletionFailed,
    AccountExportReady,
    AccountExportFailed,
}

data class AuthFormState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val verificationCode: String = "",
    val ageConfirmed: Boolean = false,
    val termsAccepted: Boolean = false,
    val marketingConsent: Boolean = false,
)

data class HouseholdFormState(
    val name: String = "",
    val countryCode: String,
    val timezone: String,
    val currency: String,
    val firstDayOfWeek: Int,
    val cycleType: String = "calendar_month",
    val cycleAnchor: String,
)

data class InvitationUiState(
    val tokenInput: String = "",
    val email: String = "",
    val role: String = "member",
    val preview: HouseholdInvitationPreviewDto? = null,
    val invitations: List<HouseholdInvitationDto> = emptyList(),
    val createdToken: String? = null,
)

data class AppUiState(
    val route: AppRoute = AppRoute.Welcome,
    val auth: AuthFormState = AuthFormState(),
    val household: HouseholdFormState,
    val isRestoringSession: Boolean = true,
    val canRetrySessionRestore: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: UiMessage? = null,
    val notice: UiMessage? = null,
    val fieldErrors: Map<FormField, UiMessage> = emptyMap(),
    val correlationId: String? = null,
    val developmentVerificationCode: String? = null,
    val account: AccountDto? = null,
    val accountExport: AccountExportDto? = null,
    val households: List<HouseholdDto> = emptyList(),
    val selectedHousehold: HouseholdDto? = null,
    val householdEditorMode: HouseholdEditorMode = HouseholdEditorMode.Create,
    val calendar: CalendarUiState = CalendarUiState(),
    val invitation: InvitationUiState = InvitationUiState(),
)
