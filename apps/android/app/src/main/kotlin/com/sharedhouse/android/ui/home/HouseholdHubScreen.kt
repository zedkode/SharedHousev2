package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.AlertDialog
import com.sharedhouse.android.ui.atmosphere.AmbientBackground
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.FilledTonalButton
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.atmosphere.DropdownMenu
import com.sharedhouse.android.ui.atmosphere.DropdownMenuItem
import com.sharedhouse.android.ui.atmosphere.DepthIconBadge
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.OutlinedButton
import com.sharedhouse.android.ui.atmosphere.PremiumHeroCard
import com.sharedhouse.android.ui.atmosphere.Surface
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.icons.SharedHouseIcons
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * Household hub backed solely by the current server-confirmed household and membership model.
 */
@Composable
fun HouseholdHubScreen(
    model: HouseholdHubUiModel,
    onEditHousehold: () -> Unit,
    onManageInvitations: () -> Unit,
    onManageCosts: () -> Unit,
    onScheduleTasks: () -> Unit,
    onJoinHousehold: () -> Unit,
    onSelectHousehold: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHouseholdSettings: () -> Unit,
    onOpenGuides: () -> Unit,
    onSignOut: () -> Unit,
    onRetryMembers: () -> Unit,
    onMemberAction: (HouseholdMemberCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    var roleTarget by remember { mutableStateOf<HouseholdMemberUi?>(null) }
    var pendingAction by remember { mutableStateOf<PendingMemberAction?>(null) }
    AmbientBackground(
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HouseholdHubHeader(model) }
            item {
                HubSectionTitle(
                    title = stringResource(R.string.house_members_title),
                    supporting = stringResource(R.string.house_members_supporting),
                )
            }
            item {
                HouseholdMembersPanel(
                    state = model.memberState,
                    onRetry = onRetryMembers,
                    onChangeRole = { roleTarget = it },
                    onConfirm = { member, action ->
                        pendingAction = PendingMemberAction(member, action)
                    },
                    onDirectAction = onMemberAction,
                )
            }
            if (model.households.size > 1) {
                item {
                    HubSectionTitle(
                        title = stringResource(R.string.household_switcher_title),
                        supporting = stringResource(R.string.household_switcher_description),
                    )
                }
                items(
                    count = model.households.size,
                    key = { index -> model.households[index].id },
                ) { index ->
                    val household = model.households[index]
                    HouseholdSwitcherCard(household, onSelectHousehold)
                }
            }
            item {
                HubSectionTitle(
                    title = stringResource(R.string.househub_configuration_title),
                    supporting = stringResource(R.string.househub_configuration_supporting),
                )
            }
            item { HouseholdConfigurationGrid(model) }
            item { HouseholdCycleCard(model) }
            item {
                HubSectionTitle(
                    title = stringResource(R.string.househub_actions_title),
                    supporting = stringResource(R.string.househub_actions_supporting),
                )
            }
            item {
                HouseholdActionPanel(
                    canManageHousehold = model.householdRole == "owner" ||
                        model.householdRole == "admin",
                    onEditHousehold = onEditHousehold,
                    onManageInvitations = onManageInvitations,
                    onManageCosts = onManageCosts,
                    onScheduleTasks = onScheduleTasks,
                    onJoinHousehold = onJoinHousehold,
                    onOpenSettings = onOpenSettings,
                    onOpenHouseholdSettings = onOpenHouseholdSettings,
                    onOpenGuides = onOpenGuides,
                )
            }
            item {
                AccountPanel(
                    displayName = model.accountDisplayName,
                    onSignOut = onSignOut,
                )
            }
        }
    }
    roleTarget?.let { member ->
        RolePickerDialog(
            member = member,
            onDismiss = { roleTarget = null },
            onRole = { role ->
                onMemberAction(
                    HouseholdMemberCommand(member.membershipId, member.version, "change_role", role),
                )
                roleTarget = null
            },
        )
    }
    pendingAction?.let { pending ->
        MemberConfirmationDialog(
            pending = pending,
            onDismiss = { pendingAction = null },
            onConfirm = {
                onMemberAction(pending.command)
                pendingAction = null
            },
        )
    }
}

private data class PendingMemberAction(
    val member: HouseholdMemberUi,
    val command: HouseholdMemberCommand,
)

@Composable
private fun HouseholdMembersPanel(
    state: HouseholdMembersUiState,
    onRetry: () -> Unit,
    onChangeRole: (HouseholdMemberUi) -> Unit,
    onConfirm: (HouseholdMemberUi, HouseholdMemberCommand) -> Unit,
    onDirectAction: (HouseholdMemberCommand) -> Unit,
) {
    when (val content = state.content) {
        HouseholdMembersContent.Loading -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AtmosphereTheme.colorScheme.surfaceContainerLow,
            shape = AtmosphereTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(stringResource(R.string.house_members_loading))
            }
        }
        HouseholdMembersContent.Error -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.errorContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.house_members_load_error),
                    color = AtmosphereTheme.colorScheme.onErrorContainer,
                )
                FilledTonalButton(onClick = onRetry) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text(stringResource(R.string.house_members_retry), Modifier.padding(start = 8.dp))
                }
            }
        }
        is HouseholdMembersContent.Ready -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.problem?.let { problem ->
                Surface(
                    color = AtmosphereTheme.colorScheme.errorContainer,
                    contentColor = AtmosphereTheme.colorScheme.onErrorContainer,
                    shape = AtmosphereTheme.shapes.large,
                ) {
                    Text(
                        stringResource(
                            if (problem == HouseholdMembersProblem.VERSION_CONFLICT) {
                                R.string.house_members_version_conflict
                            } else {
                                R.string.house_members_action_failed
                            },
                        ),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            content.members.forEach { member ->
                MemberCard(
                    member = member,
                    busy = state.mutatingMembershipId == member.membershipId,
                    onChangeRole = { onChangeRole(member) },
                    onDirectAction = onDirectAction,
                    onConfirm = { command -> onConfirm(member, command) },
                )
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: HouseholdMemberUi,
    busy: Boolean,
    onChangeRole: () -> Unit,
    onDirectAction: (HouseholdMemberCommand) -> Unit,
    onConfirm: (HouseholdMemberCommand) -> Unit,
) {
    val roleLabel = localizedRole(member.role)
    val statusLabel = localizedMembershipStatus(member.status)
    var menuExpanded by remember(member.membershipId) { mutableStateOf(false) }
    val hasActions = member.canChangeRole || member.canSuspend || member.canReactivate ||
        member.canRemove || member.canTransferOwnership
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AtmosphereTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            if (member.isCurrentUser) 2.dp else 1.dp,
            if (member.isCurrentUser) AtmosphereTheme.colorScheme.primary else AtmosphereTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HouseholdMemberAvatar(member)
            Column(Modifier.weight(1f)) {
                Text(member.displayName, style = AtmosphereTheme.typography.titleMedium)
                Text(
                    "$roleLabel · $statusLabel",
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    style = AtmosphereTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.house_members_joined, localizedMemberDate(member.joinedAt)),
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    style = AtmosphereTheme.typography.labelSmall,
                )
            }
            if (member.isCurrentUser) {
                Text(
                    stringResource(R.string.house_members_current),
                    color = AtmosphereTheme.colorScheme.primary,
                    style = AtmosphereTheme.typography.labelSmall,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            } else if (hasActions) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            SharedHouseIcons.More,
                            contentDescription = stringResource(R.string.house_members_more_actions, member.displayName),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (member.canChangeRole) DropdownMenuItem(
                            text = { Text(stringResource(R.string.house_members_change_role)) },
                            onClick = { menuExpanded = false; onChangeRole() },
                        )
                        if (member.canReactivate) DropdownMenuItem(
                            text = { Text(stringResource(R.string.house_members_reactivate)) },
                            onClick = { menuExpanded = false; onDirectAction(member.command("reactivate")) },
                        )
                        if (member.canSuspend) DropdownMenuItem(
                            text = { Text(stringResource(R.string.house_members_suspend)) },
                            onClick = { menuExpanded = false; onConfirm(member.command("suspend")) },
                        )
                        if (member.canTransferOwnership) DropdownMenuItem(
                            text = { Text(stringResource(R.string.house_members_transfer)) },
                            onClick = { menuExpanded = false; onConfirm(member.command("transfer_ownership")) },
                        )
                        if (member.canRemove) DropdownMenuItem(
                            text = { Text(stringResource(R.string.house_members_remove), color = AtmosphereTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onConfirm(member.command("remove")) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseholdMemberAvatar(member: HouseholdMemberUi) {
    val gradients = listOf(
        listOf(Color(0xFF7C3AED), Color(0xFFEC4899)),
        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6)),
        listOf(Color(0xFFEC4899), Color(0xFFF97316)),
        listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)),
    )
    val colors = gradients[(member.membershipId.hashCode() and Int.MAX_VALUE) % gradients.size]
    val initials = member.displayName
        .trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifBlank { "?" }
    val ringPadding = if (member.isCurrentUser) 2.dp else 0.dp
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                brush = if (member.isCurrentUser) {
                    Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFFEC4899)))
                } else {
                    Brush.linearGradient(colors)
                },
                shape = CircleShape,
            )
            .padding(ringPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (member.isCurrentUser) AtmosphereTheme.colorScheme.background else Color.Transparent,
                    shape = CircleShape,
                )
                .padding(if (member.isCurrentUser) 2.dp else 0.dp)
                .background(Brush.linearGradient(colors), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = AtmosphereTheme.typography.labelLarge,
            )
        }
    }
}

private fun HouseholdMemberUi.command(action: String) = HouseholdMemberCommand(membershipId, version, action)

@Composable
private fun RolePickerDialog(member: HouseholdMemberUi, onDismiss: () -> Unit, onRole: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.house_members_choose_role, member.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if ("admin" in member.assignableRoles) {
                    RoleOption("admin", R.string.househub_role_admin, R.string.house_members_role_admin_help, onRole)
                }
                if ("member" in member.assignableRoles) {
                    RoleOption("member", R.string.househub_role_member, R.string.house_members_role_member_help, onRole)
                }
                if ("read_only" in member.assignableRoles) {
                    RoleOption("read_only", R.string.househub_role_read_only, R.string.house_members_role_read_only_help, onRole)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.house_members_cancel)) } },
    )
}

@Composable
private fun RoleOption(role: String, @StringRes title: Int, @StringRes help: Int, onRole: (String) -> Unit) {
    OutlinedButton(onClick = { onRole(role) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(title))
            Text(stringResource(help), style = AtmosphereTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MemberConfirmationDialog(pending: PendingMemberAction, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val message = when (pending.command.action) {
        "suspend" -> R.string.house_members_confirm_suspend
        "remove" -> R.string.house_members_confirm_remove
        else -> R.string.house_members_confirm_transfer
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.house_members_confirm_title)) },
        text = { Text(stringResource(message, pending.member.displayName)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.house_members_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.house_members_cancel)) } },
    )
}

@Composable
private fun localizedMemberDate(value: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(value, locale) {
        runCatching {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                .format(Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate())
        }.getOrDefault(value)
    }
}

@Composable
private fun HouseholdSwitcherCard(
    household: HouseholdOptionUi,
    onSelect: (String) -> Unit,
) {
    Card(
        onClick = { onSelect(household.id) },
        enabled = !household.selected,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (household.selected) {
                AtmosphereTheme.colorScheme.primaryContainer
            } else {
                AtmosphereTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(household.name, style = AtmosphereTheme.typography.titleMedium)
                Text(
                    localizedRole(household.role),
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    style = AtmosphereTheme.typography.bodyMedium,
                )
            }
            if (household.selected) {
                Text(
                    stringResource(R.string.household_switcher_current),
                    style = AtmosphereTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HouseholdHubHeader(model: HouseholdHubUiModel) {
    val name = model.householdName.takeIf(String::isNotBlank)
        ?: stringResource(R.string.househub_name_unavailable)
    PremiumHeroCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                DepthIconBadge(
                    icon = SharedHouseIcons.House,
                    contentDescription = null,
                    hero = true,
                    iconSize = 23.dp,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name,
                        style = AtmosphereTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.househub_subtitle),
                        style = AtmosphereTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = .75f),
                        maxLines = 2,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HubStatusPill(
                    icon = SharedHouseIcons.People,
                    text = localizedRole(model.householdRole),
                )
                HubStatusPill(
                    icon = SharedHouseIcons.Approved,
                    text = localizedMembershipStatus(model.membershipStatus),
                )
            }
        }
    }
}

@Composable
private fun HubStatusPill(icon: ImageVector, text: String) {
    Surface(
        color = Color.White.copy(alpha = .14f),
        contentColor = Color.White,
        shape = AtmosphereTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = text, style = AtmosphereTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HubSectionTitle(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = AtmosphereTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = supporting,
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            style = AtmosphereTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HouseholdConfigurationGrid(model: HouseholdHubUiModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 620.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigurationCard(
                    title = R.string.househub_country,
                    value = localizedCountry(model.countryCode),
                    icon = Icons.Outlined.Public,
                    modifier = Modifier.weight(1f),
                )
                ConfigurationCard(
                    title = R.string.househub_timezone,
                    value = model.timezone,
                    icon = Icons.Outlined.Schedule,
                    modifier = Modifier.weight(1f),
                )
                ConfigurationCard(
                    title = R.string.househub_currency,
                    value = localizedCurrency(model.currencyCode),
                    icon = Icons.Outlined.MonetizationOn,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigurationCard(
                    title = R.string.househub_country,
                    value = localizedCountry(model.countryCode),
                    icon = Icons.Outlined.Public,
                )
                ConfigurationCard(
                    title = R.string.househub_timezone,
                    value = model.timezone,
                    icon = Icons.Outlined.Schedule,
                )
                ConfigurationCard(
                    title = R.string.househub_currency,
                    value = localizedCurrency(model.currencyCode),
                    icon = Icons.Outlined.MonetizationOn,
                )
            }
        }
    }
}

@Composable
private fun ConfigurationCard(
    @StringRes title: Int,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = AtmosphereTheme.colorScheme.primaryContainer, contentColor = AtmosphereTheme.colorScheme.primary, shape = AtmosphereTheme.shapes.small) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, Modifier.size(19.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(title), color = AtmosphereTheme.colorScheme.onSurfaceVariant, style = AtmosphereTheme.typography.labelSmall)
                Text(text = value, style = AtmosphereTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HouseholdCycleCard(model: HouseholdHubUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = AtmosphereTheme.colorScheme.primaryContainer,
                    contentColor = AtmosphereTheme.colorScheme.onPrimaryContainer,
                    shape = AtmosphereTheme.shapes.medium,
                ) {
                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.househub_cycle_title),
                        style = AtmosphereTheme.typography.titleMedium,
                    )
                    Text(
                        text = localizedCycle(model.cycleType),
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                        style = AtmosphereTheme.typography.bodyMedium,
                    )
                }
            }
            CycleValueRow(
                label = stringResource(R.string.househub_first_day),
                value = localizedFirstDay(model.firstDayOfWeek),
            )
            CycleValueRow(
                label = stringResource(R.string.househub_cycle_anchor),
                value = localizedAnchorDate(model.cycleAnchor),
            )
            Text(
                text = stringResource(R.string.househub_cycle_guidance),
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                style = AtmosphereTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CycleValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            style = AtmosphereTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = AtmosphereTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun HouseholdActionPanel(
    canManageHousehold: Boolean,
    onEditHousehold: () -> Unit,
    onManageInvitations: () -> Unit,
    onManageCosts: () -> Unit,
    onScheduleTasks: () -> Unit,
    onJoinHousehold: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHouseholdSettings: () -> Unit,
    onOpenGuides: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (canManageHousehold) {
                Button(onClick = onOpenHouseholdSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Settings, contentDescription = null)
                    Text(text = stringResource(R.string.household_settings_title), modifier = Modifier.padding(start = 8.dp))
                }
                Button(onClick = onEditHousehold, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text(
                        text = stringResource(R.string.househub_edit),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = onManageInvitations, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.GroupAdd, contentDescription = null)
                    Text(
                        text = stringResource(R.string.invitation_manage_action),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = onManageCosts, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.MonetizationOn, contentDescription = null)
                    Text(
                        text = stringResource(R.string.househub_manage_costs),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = onScheduleTasks, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Checklist, contentDescription = null)
                    Text(
                        text = stringResource(R.string.househub_schedule_tasks),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            OutlinedButton(onClick = onJoinHousehold, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
                Text(
                    text = stringResource(R.string.invitation_join_another_action),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Text(
                    text = stringResource(R.string.househub_settings),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
            OutlinedButton(onClick = onOpenGuides, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                Text(
                    text = stringResource(R.string.househub_guides),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AccountPanel(displayName: String, onSignOut: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AtmosphereTheme.colorScheme.surfaceContainer,
        shape = AtmosphereTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.househub_account_title),
                        style = AtmosphereTheme.typography.titleMedium,
                    )
                    Text(
                        text = displayName.takeIf(String::isNotBlank)
                            ?: stringResource(R.string.househub_account_name_unavailable),
                        color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                        style = AtmosphereTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                text = stringResource(R.string.househub_signout_guidance),
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                style = AtmosphereTheme.typography.bodySmall,
            )
            TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Text(
                    text = stringResource(R.string.househub_signout),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun localizedCountry(countryCode: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(countryCode, locale) {
        val code = countryCode.trim().uppercase(Locale.ROOT)
        val displayName = runCatching { Locale.Builder().setRegion(code).build().getDisplayCountry(locale) }
            .getOrNull()
            .orEmpty()
        if (displayName.isBlank()) code else "$displayName ($code)"
    }
}

@Composable
private fun localizedCurrency(currencyCode: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(currencyCode, locale) {
        val code = currencyCode.trim().uppercase(Locale.ROOT)
        val displayName = runCatching { Currency.getInstance(code).getDisplayName(locale) }
            .getOrNull()
            .orEmpty()
        if (displayName.isBlank()) code else "$displayName ($code)"
    }
}

@Composable
private fun localizedAnchorDate(value: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(value, locale) {
        runCatching {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            formatter.format(LocalDate.parse(value))
        }.getOrDefault(value)
    }
}

@Composable
private fun localizedFirstDay(day: Int): String = stringResource(
    when (day) {
        1 -> R.string.househub_day_monday
        6 -> R.string.househub_day_saturday
        7 -> R.string.househub_day_sunday
        else -> R.string.househub_value_unrecognized
    },
)

@Composable
private fun localizedCycle(cycle: String): String = stringResource(
    when (cycle.lowercase(Locale.ROOT)) {
        "weekly" -> R.string.househub_cycle_weekly
        "fourteen_day" -> R.string.househub_cycle_fourteen_day
        "calendar_month" -> R.string.househub_cycle_calendar_month
        else -> R.string.househub_value_unrecognized
    },
)

@Composable
private fun localizedRole(role: String): String = stringResource(
    when (role.lowercase(Locale.ROOT)) {
        "owner" -> R.string.househub_role_owner
        "admin" -> R.string.househub_role_admin
        "finance_manager" -> R.string.househub_role_finance
        "chore_manager" -> R.string.househub_role_chore
        "member" -> R.string.househub_role_member
        "read_only" -> R.string.househub_role_read_only
        else -> R.string.househub_value_unrecognized
    },
)

@Composable
private fun localizedMembershipStatus(status: String): String = stringResource(
    when (status.lowercase(Locale.ROOT)) {
        "active" -> R.string.househub_status_active
        "invited" -> R.string.househub_status_invited
        "suspended" -> R.string.househub_status_suspended
        "left" -> R.string.househub_status_left
        "removed" -> R.string.househub_status_removed
        else -> R.string.househub_value_unrecognized
    },
)
