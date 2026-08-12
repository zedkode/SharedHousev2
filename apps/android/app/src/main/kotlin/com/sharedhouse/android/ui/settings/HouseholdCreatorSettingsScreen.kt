package com.sharedhouse.android.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SupervisorAccount
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.IconButton
import com.sharedhouse.android.ui.atmosphere.Scaffold
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.theme.AtmosphereTheme

@Composable
fun HouseholdCreatorSettingsScreen(
    householdName: String,
    role: String,
    countryCode: String,
    timezone: String,
    currency: String,
    firstDayOfWeek: Int,
    cycleType: String,
    cycleAnchor: String,
    onBack: () -> Unit,
    onEditHousehold: () -> Unit,
    onManageMembers: () -> Unit,
    onManageInvitations: () -> Unit,
    onManageFinance: () -> Unit,
    onManageChores: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenUserNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authorised = role == "owner" || role == "admin"
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) } },
            title = { Text(stringResource(R.string.household_settings_title), style = AtmosphereTheme.typography.titleLarge) },
        ) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(householdName, style = AtmosphereTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Text(stringResource(if (authorised) R.string.household_settings_description else R.string.household_settings_denied), color = AtmosphereTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (authorised) {
                item { HouseholdControlSummary(countryCode, timezone, currency, firstDayOfWeek, cycleType, cycleAnchor) }
                item { CreatorSectionTitle(R.string.household_settings_section_identity) }
                item { CreatorSetting(Icons.Outlined.HomeWork, R.string.household_settings_identity, R.string.household_settings_identity_description, onEditHousehold) }
                item { CreatorSectionTitle(R.string.household_settings_section_access) }
                item { CreatorSetting(Icons.Outlined.SupervisorAccount, R.string.household_settings_members, R.string.household_settings_members_description, onManageMembers) }
                item { CreatorSetting(Icons.Outlined.GroupAdd, R.string.household_settings_invitations, R.string.household_settings_invitations_description, onManageInvitations) }
                item { CreatorSectionTitle(R.string.household_settings_section_automation) }
                item { CreatorSetting(Icons.Outlined.MonetizationOn, R.string.household_settings_finance, R.string.household_settings_finance_description, onManageFinance) }
                item { CreatorSetting(Icons.Outlined.Checklist, R.string.household_settings_chores, R.string.household_settings_chores_description, onManageChores) }
                item { CreatorSetting(Icons.Outlined.CalendarMonth, R.string.household_settings_calendar, R.string.household_settings_calendar_description, onOpenCalendar) }
                item { CreatorSectionTitle(R.string.household_settings_section_communication) }
                item { CreatorSetting(Icons.Outlined.NotificationsActive, R.string.household_settings_notifications, R.string.household_settings_notifications_description, onOpenUserNotifications) }
            }
        }
    }
}

@Composable
private fun HouseholdControlSummary(
    countryCode: String,
    timezone: String,
    currency: String,
    firstDayOfWeek: Int,
    cycleType: String,
    cycleAnchor: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.household_settings_snapshot), style = AtmosphereTheme.typography.titleMedium)
            Text(stringResource(R.string.household_settings_locale_value, countryCode, timezone), color = AtmosphereTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.household_settings_money_value, currency, cycleType, cycleAnchor), color = AtmosphereTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.household_settings_week_value, firstDayOfWeek), color = AtmosphereTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.household_settings_snapshot_help), style = AtmosphereTheme.typography.bodySmall, color = AtmosphereTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CreatorSectionTitle(title: Int) {
    Text(
        stringResource(title),
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).semantics { heading() },
        style = AtmosphereTheme.typography.titleLarge,
    )
}

@Composable
private fun CreatorSetting(icon: ImageVector, title: Int, description: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AtmosphereTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(27.dp), tint = AtmosphereTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(title), style = AtmosphereTheme.typography.titleMedium)
                Text(stringResource(description), color = AtmosphereTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AtmosphereTheme.colorScheme.onSurfaceVariant)
        }
    }
}
