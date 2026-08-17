package com.sharedhouse.android.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.app.UiMessage

@Composable
fun UiMessage.localized(): String = stringResource(
    when (this) {
        UiMessage.DisplayNameRequired -> R.string.error_display_name
        UiMessage.EmailInvalid -> R.string.error_email
        UiMessage.PasswordRequired -> R.string.error_password_required
        UiMessage.PasswordTooShort -> R.string.error_password_length
        UiMessage.AgeConfirmationRequired -> R.string.error_age_confirmation
        UiMessage.TermsConfirmationRequired -> R.string.error_terms_confirmation
        UiMessage.VerificationCodeInvalidInput -> R.string.error_verification_code_format
        UiMessage.HouseholdNameRequired -> R.string.error_household_name
        UiMessage.CountryCodeInvalid -> R.string.error_country_code
        UiMessage.TimezoneInvalid -> R.string.error_timezone
        UiMessage.CurrencyInvalid -> R.string.error_currency
        UiMessage.CycleAnchorInvalid -> R.string.error_cycle_anchor
        UiMessage.InvalidCredentials -> R.string.error_invalid_credentials
        UiMessage.EmailVerificationRequired -> R.string.error_email_verification_required
        UiMessage.VerificationCodeInvalid -> R.string.error_verification_code_invalid
        UiMessage.VerificationCodeExpired -> R.string.error_verification_code_expired
        UiMessage.AccountUnavailable -> R.string.error_account_unavailable
        UiMessage.SessionExpired -> R.string.error_session_expired
        UiMessage.SecureSessionReset -> R.string.notice_secure_session_reset
        UiMessage.SecureStorageUnavailable -> R.string.error_secure_storage_unavailable
        UiMessage.SessionRestoreNetworkUnavailable -> R.string.notice_session_restore_network
        UiMessage.IdempotencyKeyReused -> R.string.error_idempotency_key
        UiMessage.RequestInvalid -> R.string.error_request_invalid
        UiMessage.RateLimited -> R.string.error_rate_limited
        UiMessage.NetworkUnavailable -> R.string.error_network_unavailable
        UiMessage.ServiceUnavailable -> R.string.error_service_unavailable
        UiMessage.HouseholdLoadFailed -> R.string.error_household_load
        UiMessage.HouseholdVersionConflict -> R.string.error_household_version_conflict
        UiMessage.HouseholdReloaded -> R.string.notice_household_reloaded
        UiMessage.InvitationTokenInvalid -> R.string.error_invitation_token
        UiMessage.InvitationNotFound -> R.string.error_invitation_not_found
        UiMessage.InvitationExpired -> R.string.error_invitation_expired
        UiMessage.InvitationUnavailable -> R.string.error_invitation_unavailable
        UiMessage.InvitationEmailMismatch -> R.string.error_invitation_email_mismatch
        UiMessage.InvitationManageForbidden -> R.string.error_invitation_manage_forbidden
        UiMessage.InvitationRoleForbidden -> R.string.error_invitation_role_forbidden
        UiMessage.InvitationCreated -> R.string.notice_invitation_created
        UiMessage.InvitationAccepted -> R.string.notice_invitation_accepted
        UiMessage.InvitationRevoked -> R.string.notice_invitation_revoked
        UiMessage.RegistrationAccepted -> R.string.notice_registration_accepted
        UiMessage.VerificationCodeSent -> R.string.notice_verification_code_sent
        UiMessage.SignedOut -> R.string.notice_signed_out
        UiMessage.SessionRevocationUnconfirmed -> R.string.notice_session_revocation_unconfirmed
        UiMessage.AccountDeleted -> R.string.notice_account_deleted
        UiMessage.AccountDeletionOwnerTransferRequired -> R.string.error_account_owner_transfer
        UiMessage.RecentAuthenticationRequired -> R.string.error_recent_authentication
        UiMessage.AccountDeletionFailed -> R.string.error_account_deletion
        UiMessage.AccountExportReady -> R.string.notice_account_export_ready
        UiMessage.AccountExportFailed -> R.string.error_account_export
    },
)

@Composable
fun StatusMessage(
    message: UiMessage,
    isError: Boolean,
    correlationId: String?,
    modifier: Modifier = Modifier,
) {
    val container = if (isError) {
        AtmosphereTheme.colorScheme.errorContainer
    } else {
        AtmosphereTheme.colorScheme.primaryContainer
    }
    val content = if (isError) {
        AtmosphereTheme.colorScheme.onErrorContainer
    } else {
        AtmosphereTheme.colorScheme.onPrimaryContainer
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, content.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = content,
            )
            Text(text = message.localized(), color = content, style = AtmosphereTheme.typography.bodyMedium)
            if (!correlationId.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.support_reference, correlationId),
                    color = content,
                    style = AtmosphereTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun FormScreenScaffold(
    @StringRes title: Int,
    @StringRes description: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AtmosphereTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(title),
                            style = AtmosphereTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(description),
                            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                            style = AtmosphereTheme.typography.bodyLarge,
                        )
                        header?.invoke(this)
                    }
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), content = content)
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = AtmosphereTheme.colorScheme.surfaceContainer,
    borderColor: Color = AtmosphereTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = AtmosphereTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
            ),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 0.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = AtmosphereTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
            ),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

