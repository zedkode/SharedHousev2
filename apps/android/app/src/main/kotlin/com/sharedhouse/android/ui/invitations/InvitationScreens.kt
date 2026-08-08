package com.sharedhouse.android.ui.invitations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.app.AppUiState
import com.sharedhouse.android.ui.app.FormField
import com.sharedhouse.android.ui.components.FormScreenScaffold
import com.sharedhouse.android.ui.components.StatusMessage
import com.sharedhouse.android.ui.components.localized
import com.sharedhouse.network.HouseholdInvitationDto

@Composable
fun InvitationJoinScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onTokenChange: (String) -> Unit,
    onPreview: () -> Unit,
    onAccept: () -> Unit,
) {
    BackHandler(enabled = !state.isSubmitting, onBack = onBack)
    FormScreenScaffold(
        title = R.string.invitation_join_title,
        description = R.string.invitation_join_description,
        onBack = onBack,
    ) {
        if (state.isSubmitting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let {
            StatusMessage(message = it, isError = true, correlationId = state.correlationId)
        }
        state.notice?.let {
            StatusMessage(message = it, isError = false, correlationId = null)
        }
        OutlinedTextField(
            value = state.invitation.tokenInput,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.invitation_code_label)) },
            supportingText = {
                Text(
                    state.fieldErrors[FormField.InvitationToken]?.localized()
                        ?: stringResource(R.string.invitation_code_support),
                )
            },
            isError = state.fieldErrors[FormField.InvitationToken] != null,
            enabled = !state.isSubmitting,
            singleLine = true,
        )
        OutlinedButton(
            onClick = onPreview,
            enabled = !state.isSubmitting && state.invitation.tokenInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(R.string.invitation_preview_action))
        }
        state.invitation.preview?.let { preview ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = preview.householdName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(
                            R.string.invitation_preview_role,
                            localizedInvitationRole(preview.role),
                        ),
                    )
                    Text(
                        text = stringResource(
                            if (preview.emailRestricted) {
                                R.string.invitation_email_restricted
                            } else {
                                R.string.invitation_email_open
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.invitation_expires, preview.expiresAt),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Button(
                onClick = onAccept,
                enabled = !state.isSubmitting && preview.status == "pending",
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Outlined.GroupAdd, contentDescription = null)
                Text(
                    stringResource(R.string.invitation_accept_action),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
fun InvitationManagerScreen(
    state: AppUiState,
    canInviteAdmins: Boolean,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onCreate: () -> Unit,
    onShare: (String) -> Unit,
    onRevoke: (String) -> Unit,
) {
    BackHandler(enabled = !state.isSubmitting, onBack = onBack)
    FormScreenScaffold(
        title = R.string.invitation_manage_title,
        description = R.string.invitation_manage_description,
        onBack = onBack,
    ) {
        if (state.isSubmitting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let {
            StatusMessage(message = it, isError = true, correlationId = state.correlationId)
        }
        state.notice?.let {
            StatusMessage(message = it, isError = false, correlationId = null)
        }
        Text(
            text = stringResource(R.string.invitation_create_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        OutlinedTextField(
            value = state.invitation.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.invitation_email_label)) },
            supportingText = {
                Text(
                    state.fieldErrors[FormField.InvitationEmail]?.localized()
                        ?: stringResource(R.string.invitation_email_support),
                )
            },
            isError = state.fieldErrors[FormField.InvitationEmail] != null,
            enabled = !state.isSubmitting,
            singleLine = true,
        )
        Text(stringResource(R.string.invitation_role_label), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canInviteAdmins) {
                InvitationRoleChip("admin", state.invitation.role, onRoleChange)
            }
            InvitationRoleChip("member", state.invitation.role, onRoleChange)
            InvitationRoleChip("read_only", state.invitation.role, onRoleChange)
        }
        Button(
            onClick = onCreate,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.invitation_create_action))
        }
        state.invitation.createdToken?.let { token ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.invitation_secret_once),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SelectionContainer {
                        Text(token, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = { onShare(token) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text(
                            stringResource(R.string.invitation_share_action),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.invitation_existing_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (!state.isSubmitting && state.invitation.invitations.isEmpty()) {
            Text(
                text = stringResource(R.string.invitation_existing_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.invitation.invitations.forEach { invitation ->
            InvitationCard(invitation = invitation, onRevoke = onRevoke)
        }
    }
}

@Composable
private fun InvitationRoleChip(
    role: String,
    selectedRole: String,
    onRoleChange: (String) -> Unit,
) {
    FilterChip(
        selected = role == selectedRole,
        onClick = { onRoleChange(role) },
        label = { Text(localizedInvitationRole(role)) },
    )
}

@Composable
private fun InvitationCard(
    invitation: HouseholdInvitationDto,
    onRevoke: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                invitation.email ?: stringResource(R.string.invitation_any_email),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.invitation_preview_role, localizedInvitationRole(invitation.role)))
            Text(
                stringResource(R.string.invitation_status, localizedInvitationStatus(invitation.status)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.invitation_expires, invitation.expiresAt),
                style = MaterialTheme.typography.bodySmall,
            )
            if (invitation.status == "pending") {
                OutlinedButton(
                    onClick = { onRevoke(invitation.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.invitation_revoke_action))
                }
            }
        }
    }
}

@Composable
private fun localizedInvitationRole(role: String): String = stringResource(
    when (role) {
        "admin" -> R.string.househub_role_admin
        "read_only" -> R.string.househub_role_read_only
        else -> R.string.househub_role_member
    },
)

@Composable
private fun localizedInvitationStatus(status: String): String = stringResource(
    when (status) {
        "accepted" -> R.string.invitation_status_accepted
        "revoked" -> R.string.invitation_status_revoked
        "expired" -> R.string.invitation_status_expired
        else -> R.string.invitation_status_pending
    },
)
