package com.sharedhouse.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.calendar.CalendarEventType
import com.sharedhouse.android.ui.calendar.CalendarEventUi
import com.sharedhouse.android.ui.components.GlassCard
import java.time.format.DateTimeFormatter

import java.time.format.FormatStyle

/**
 * Modern authenticated dashboard. Every value is supplied by the application layer; feature
 * areas without a data source are labelled as unavailable instead of displaying zero totals.
 */
@Composable
fun HouseholdDashboardScreen(
    model: HouseholdDashboardUiModel,
    onOpenCalendar: () -> Unit,
    onRetryCalendar: () -> Unit,
    onEditHousehold: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
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
            item {
                DashboardHero(model)
            }
            item {
                DashboardSectionTitle(
                    title = stringResource(R.string.dashboard_quick_actions_title),
                    supporting = stringResource(R.string.dashboard_quick_actions_supporting),
                )
            }
            item {
                QuickActions(
                    onOpenCalendar = onOpenCalendar,
                    onEditHousehold = onEditHousehold,
                    onOpenGuides = onOpenGuides,
                    onOpenSettings = onOpenSettings,
                )
            }
            item {
                CalendarOverviewCard(
                    content = model.calendar,
                    onOpenCalendar = onOpenCalendar,
                    onRetry = onRetryCalendar,
                )
            }
            item {
                DashboardSectionTitle(
                    title = stringResource(R.string.dashboard_workspace_title),
                    supporting = stringResource(R.string.dashboard_workspace_supporting),
                )
            }
            item {
                FeatureReadinessGrid(
                    onOpenMoney = onOpenMoney,
                    onOpenTasks = onOpenTasks,
                    onOpenGuides = onOpenGuides,
                )
            }
            item {
                GuidanceCard(onOpenGuides = onOpenGuides)
            }
        }
    }
}

@Composable
private fun DashboardHero(model: HouseholdDashboardUiModel) {
    val hasName = model.accountDisplayName.isNotBlank()
    val householdName = model.householdName.takeIf(String::isNotBlank)
        ?: stringResource(R.string.dashboard_household_name_unavailable)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            ) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Home, contentDescription = null)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (hasName) {
                        stringResource(R.string.dashboard_greeting_named, model.accountDisplayName)
                    } else {
                        stringResource(R.string.dashboard_greeting_generic)
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.dashboard_household_context, householdName),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.dashboard_configured_household_badge)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun DashboardSectionTitle(
    title: String,
    supporting: String,
) {
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
private fun QuickActions(
    onOpenCalendar: () -> Unit,
    onEditHousehold: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 620.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(
                    title = R.string.dashboard_action_calendar,
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = onOpenCalendar,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    title = R.string.dashboard_action_edit_household,
                    icon = Icons.Outlined.Edit,
                    onClick = onEditHousehold,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    title = R.string.dashboard_action_guides,
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = onOpenGuides,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    title = R.string.dashboard_action_settings,
                    icon = Icons.Outlined.Settings,
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        title = R.string.dashboard_action_calendar,
                        icon = Icons.Outlined.CalendarMonth,
                        onClick = onOpenCalendar,
                        modifier = Modifier.weight(1f),
                    )
                    QuickAction(
                        title = R.string.dashboard_action_edit_household,
                        icon = Icons.Outlined.Edit,
                        onClick = onEditHousehold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        title = R.string.dashboard_action_guides,
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        onClick = onOpenGuides,
                        modifier = Modifier.weight(1f),
                    )
                    QuickAction(
                        title = R.string.dashboard_action_settings,
                        icon = Icons.Outlined.Settings,
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuickAction(
    @StringRes title: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalendarOverviewCard(
    content: DashboardCalendarContent,
    onOpenCalendar: () -> Unit,
    onRetry: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Today, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_calendar_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = calendarStatusText(content),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        when (content) {
            DashboardCalendarContent.Loading -> CalendarLoading()
            DashboardCalendarContent.Error -> CalendarError(onRetry)
            is DashboardCalendarContent.Ready -> CalendarReady(content.events)
        }

        Button(
            onClick = onOpenCalendar,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.dashboard_calendar_open))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun calendarStatusText(content: DashboardCalendarContent): String = when (content) {
    DashboardCalendarContent.Loading -> stringResource(R.string.dashboard_calendar_loading_short)
    DashboardCalendarContent.Error -> stringResource(R.string.dashboard_calendar_error_short)
    is DashboardCalendarContent.Ready -> pluralStringResource(
        R.plurals.dashboard_calendar_event_count,
        content.events.size,
        content.events.size,
    )
}

@Composable
private fun CalendarLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.dashboard_calendar_loading),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CalendarError(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                Text(
                    text = stringResource(R.string.dashboard_calendar_error),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text(
                    text = stringResource(R.string.dashboard_calendar_retry),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarReady(events: List<CalendarEventUi>) {
    if (events.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.dashboard_calendar_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }

    val visibleEvents = remember(events) {
        events.sortedWith(compareBy<CalendarEventUi>({ it.date }, { it.startTime })).take(3)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visibleEvents.forEachIndexed { index, event ->
            DashboardEventRow(event)
            if (index != visibleEvents.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (events.size > visibleEvents.size) {
            Text(
                text = pluralStringResource(
                    R.plurals.dashboard_calendar_more_events,
                    events.size - visibleEvents.size,
                    events.size - visibleEvents.size,
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DashboardEventRow(event: CalendarEventUi) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    val date = remember(event.date, dateFormatter) { dateFormatter.format(event.date) }
    val time = event.startTime?.let(timeFormatter::format)
        ?: stringResource(R.string.dashboard_calendar_all_day)
    val description = stringResource(
        R.string.dashboard_calendar_event_accessibility,
        event.title,
        date,
        time,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(eventTypeIcon(event.type), contentDescription = null)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.dashboard_calendar_event_when, date, time),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun eventTypeIcon(type: CalendarEventType): ImageVector = when (type) {
    CalendarEventType.HOUSEHOLD -> Icons.Outlined.Home
    CalendarEventType.MAINTENANCE -> Icons.Outlined.Settings
    CalendarEventType.APPOINTMENT -> Icons.Outlined.Schedule
    CalendarEventType.SHOPPING -> Icons.Outlined.AccountBalanceWallet
    CalendarEventType.OTHER -> Icons.Outlined.CalendarMonth
}

@Composable
private fun FeatureReadinessGrid(
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenGuides: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 760.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureReadinessCard(
                    title = R.string.dashboard_money_title,
                    description = R.string.dashboard_money_unavailable,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onOpenMoney,
                    modifier = Modifier.weight(1f),
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_tasks_title,
                    description = R.string.dashboard_tasks_unavailable,
                    icon = Icons.Outlined.Checklist,
                    onClick = onOpenTasks,
                    modifier = Modifier.weight(1f),
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_requests_title,
                    description = R.string.dashboard_requests_unavailable,
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onOpenGuides,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureReadinessCard(
                    title = R.string.dashboard_money_title,
                    description = R.string.dashboard_money_unavailable,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onOpenMoney,
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_tasks_title,
                    description = R.string.dashboard_tasks_unavailable,
                    icon = Icons.Outlined.Checklist,
                    onClick = onOpenTasks,
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_requests_title,
                    description = R.string.dashboard_requests_unavailable,
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onOpenGuides,
                )
            }
        }
    }
}

@Composable
private fun FeatureReadinessCard(
    @StringRes title: Int,
    @StringRes description: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_not_connected_badge),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.dashboard_feature_learn_more),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun GuidanceCard(onOpenGuides: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
            Text(
                text = stringResource(R.string.dashboard_guidance_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.dashboard_guidance_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onOpenGuides) {
                Text(stringResource(R.string.dashboard_action_guides))
            }
        }
    }
}
