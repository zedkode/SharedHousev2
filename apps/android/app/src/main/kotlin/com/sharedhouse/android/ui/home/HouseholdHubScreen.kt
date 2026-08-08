package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import java.time.LocalDate
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
    onOpenSettings: () -> Unit,
    onOpenGuides: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { HouseholdHubHeader(model) }
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
                    onEditHousehold = onEditHousehold,
                    onOpenSettings = onOpenSettings,
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
}

@Composable
private fun HouseholdHubHeader(model: HouseholdHubUiModel) {
    val name = model.householdName.takeIf(String::isNotBlank)
        ?: stringResource(R.string.househub_name_unavailable)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = MaterialTheme.shapes.large,
            ) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Groups, contentDescription = null)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.househub_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HubStatusPill(
                    icon = Icons.Outlined.Badge,
                    text = localizedRole(model.householdRole),
                )
                HubStatusPill(
                    icon = Icons.Outlined.AccountCircle,
                    text = localizedMembershipStatus(model.membershipStatus),
                )
            }
        }
    }
}

@Composable
private fun HubStatusPill(icon: ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HubSectionTitle(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HouseholdCycleCard(model: HouseholdHubUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.househub_cycle_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = localizedCycle(model.cycleType),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun HouseholdActionPanel(
    onEditHousehold: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuides: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onEditHousehold, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text(
                    text = stringResource(R.string.househub_edit),
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
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
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
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = displayName.takeIf(String::isNotBlank)
                            ?: stringResource(R.string.househub_account_name_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                text = stringResource(R.string.househub_signout_guidance),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
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
