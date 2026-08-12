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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import com.sharedhouse.android.ui.atmosphere.AssistChip
import com.sharedhouse.android.ui.atmosphere.AmbientBackground
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.Card
import com.sharedhouse.android.ui.atmosphere.CardDefaults
import com.sharedhouse.android.ui.atmosphere.CircularProgressIndicator
import com.sharedhouse.android.ui.atmosphere.FilledTonalButton
import com.sharedhouse.android.ui.atmosphere.HorizontalDivider
import com.sharedhouse.android.ui.atmosphere.Icon
import com.sharedhouse.android.ui.atmosphere.PremiumHeroCard
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.atmosphere.Surface
import com.sharedhouse.android.ui.atmosphere.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.sharedhouse.android.ui.chat.ChatConnection
import com.sharedhouse.android.ui.chat.ChatUiState
import com.sharedhouse.android.ui.icons.SharedHouseIcons
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Currency

import java.time.format.FormatStyle

/**
 * Modern authenticated dashboard. Every value is supplied by the application layer; feature
 * areas without a data source are labelled as unavailable instead of displaying zero totals.
 */
@Composable
fun HouseholdDashboardScreen(
    model: HouseholdDashboardUiModel,
    chat: ChatUiState,
    onOpenChat: () -> Unit,
    onOpenCalendar: () -> Unit,
    onRetryCalendar: () -> Unit,
    onEditHousehold: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AmbientBackground(
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardHero(
                    model = model,
                    onOpenMoney = onOpenMoney,
                    onOpenTasks = onOpenTasks,
                    onOpenCalendar = onOpenCalendar,
                    onOpenRequests = onOpenRequests,
                )
            }
            item { ChatResumeCard(chat, onOpenChat) }
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
                    onOpenSettings = onOpenSettings,
                    onOpenMoney = onOpenMoney,
                    onOpenTasks = onOpenTasks,
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
                    money = model.money,
                    tasks = model.tasks,
                    onOpenMoney = onOpenMoney,
                    onOpenTasks = onOpenTasks,
                    onOpenRequests = onOpenRequests,
                )
            }
            item {
                GuidanceCard(onOpenGuides = onOpenGuides)
            }
        }
    }
}

@Composable
private fun ChatResumeCard(chat: ChatUiState, onOpenChat: () -> Unit) {
    val latest = chat.messages.lastOrNull()
    Surface(
        onClick = onOpenChat,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AtmosphereTheme.colorScheme.cardLevel2,
        contentColor = AtmosphereTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = AtmosphereTheme.shapes.medium,
                color = AtmosphereTheme.colorScheme.primary.copy(alpha = .14f),
                contentColor = AtmosphereTheme.colorScheme.primary,
                border = BorderStroke(1.dp, AtmosphereTheme.colorScheme.primary.copy(alpha = .28f)),
            ) { Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(SharedHouseIcons.Chat, null, Modifier.size(22.dp)) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(if (chat.connection == ChatConnection.LIVE) R.string.dashboard_chat_live else R.string.dashboard_chat_open),
                    style = AtmosphereTheme.typography.labelSmall,
                    color = if (chat.connection == ChatConnection.LIVE) AtmosphereTheme.colorScheme.statusPositive else AtmosphereTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    latest?.let { stringResource(R.string.dashboard_chat_message, it.senderDisplayName, it.body) }
                        ?: stringResource(R.string.dashboard_chat_empty),
                    style = AtmosphereTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
        }
    }
}

@Composable
private fun DashboardHero(
    model: HouseholdDashboardUiModel,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenRequests: () -> Unit,
) {
    val hasName = model.accountDisplayName.isNotBlank()
    val householdName = model.householdName.takeIf(String::isNotBlank)
        ?: stringResource(R.string.dashboard_household_name_unavailable)

    val dueValue = when (val money = model.money) {
        is DashboardMoneyContent.Ready -> dashboardMoney(money.amountDueMinor, money.currency)
        DashboardMoneyContent.Loading -> "…"
        DashboardMoneyContent.Error -> "—"
    }
    val taskValue = (model.tasks as? DashboardTasksContent.Ready)?.activeCount?.toString() ?: "—"
    val eventValue = (model.calendar as? DashboardCalendarContent.Ready)?.events?.size?.toString() ?: "—"
    val requestValue = (model.tasks as? DashboardTasksContent.Ready)?.pendingRequests?.toString() ?: "—"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PremiumHeroCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenMoney,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = if (hasName) {
                            stringResource(R.string.dashboard_greeting_named, model.accountDisplayName)
                        } else {
                            stringResource(R.string.dashboard_greeting_generic)
                        },
                        style = AtmosphereTheme.typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.dashboard_household_context, householdName),
                        style = AtmosphereTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = .75f),
                    )
                }
                Surface(
                    shape = AtmosphereTheme.shapes.medium,
                    color = Color.White.copy(alpha = .14f),
                    contentColor = Color.White,
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(SharedHouseIcons.House, null, Modifier.size(22.dp))
                    }
                }
            }
            Text(
                text = stringResource(R.string.dashboard_metric_due),
                style = AtmosphereTheme.typography.labelMedium,
                color = Color.White.copy(alpha = .75f),
            )
            Text(
                text = dueValue,
                style = AtmosphereTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardMetric(taskValue, stringResource(R.string.dashboard_metric_tasks), SharedHouseIcons.Tasks, onOpenTasks, Modifier.weight(1f))
            DashboardMetric(eventValue, stringResource(R.string.dashboard_metric_events), SharedHouseIcons.Calendar, onOpenCalendar, Modifier.weight(1f))
            DashboardMetric(
                requestValue,
                stringResource(R.string.dashboard_metric_requests),
                SharedHouseIcons.Pending,
                onOpenRequests,
                Modifier.weight(1f),
                attention = requestValue !in setOf("0", "—"),
            )
        }
    }
}

@Composable
private fun DashboardMetric(
    value: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    attention: Boolean = false,
) {
    val container = when {
        prominent -> AtmosphereTheme.colorScheme.primaryContainer
        attention -> AtmosphereTheme.colorScheme.tertiaryContainer
        else -> AtmosphereTheme.colorScheme.surfaceContainer
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (prominent) 84.dp else 70.dp),
        shape = if (prominent) AtmosphereTheme.shapes.large else AtmosphereTheme.shapes.medium,
        color = container,
        border = BorderStroke(1.dp, if (prominent) AtmosphereTheme.colorScheme.primary.copy(alpha = .42f) else AtmosphereTheme.colorScheme.outlineVariant),
        shadowElevation = if (prominent) 7.dp else 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = AtmosphereTheme.shapes.small,
                color = if (prominent) AtmosphereTheme.colorScheme.primary.copy(alpha = .15f) else AtmosphereTheme.colorScheme.secondaryContainer,
                contentColor = if (attention) AtmosphereTheme.colorScheme.tertiary else AtmosphereTheme.colorScheme.primary,
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(17.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(value, style = if (prominent) AtmosphereTheme.typography.headlineMedium else AtmosphereTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, style = AtmosphereTheme.typography.labelSmall, color = AtmosphereTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun dashboardMoney(amountMinor: Long, currencyCode: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(amountMinor, currencyCode, locale) {
        runCatching {
            NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance(currencyCode) }
                .format(amountMinor / 100.0)
        }.getOrDefault("$currencyCode ${amountMinor / 100}")
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
private fun QuickActions(
    onOpenCalendar: () -> Unit,
    onEditHousehold: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(R.string.dashboard_action_add_expense, Icons.Outlined.AddCard, onOpenMoney, Modifier.weight(1f), prominent = true)
            QuickAction(R.string.dashboard_action_add_task, Icons.Outlined.AddTask, onOpenTasks, Modifier.weight(1f), prominent = true)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 600.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(R.string.dashboard_action_record_payment, Icons.Outlined.Payments, onOpenMoney, Modifier.weight(1f))
                    QuickAction(R.string.dashboard_action_add_bill, Icons.AutoMirrored.Outlined.ReceiptLong, onOpenMoney, Modifier.weight(1f))
                    QuickAction(R.string.dashboard_action_calendar, Icons.Outlined.CalendarMonth, onOpenCalendar, Modifier.weight(1f))
                    QuickAction(R.string.dashboard_action_household_settings, Icons.Outlined.AdminPanelSettings, onEditHousehold, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction(R.string.dashboard_action_record_payment, Icons.Outlined.Payments, onOpenMoney, Modifier.weight(1f))
                        QuickAction(R.string.dashboard_action_add_bill, Icons.AutoMirrored.Outlined.ReceiptLong, onOpenMoney, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction(R.string.dashboard_action_calendar, Icons.Outlined.CalendarMonth, onOpenCalendar, Modifier.weight(1f))
                        QuickAction(R.string.dashboard_action_household_settings, Icons.Outlined.AdminPanelSettings, onEditHousehold, Modifier.weight(1f))
                    }
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
    prominent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (prominent) 72.dp else 76.dp),
        shape = if (prominent) AtmosphereTheme.shapes.large else AtmosphereTheme.shapes.medium,
        color = if (prominent) AtmosphereTheme.colorScheme.primaryContainer else AtmosphereTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (prominent) AtmosphereTheme.colorScheme.primary.copy(alpha = .38f) else AtmosphereTheme.colorScheme.outlineVariant),
        shadowElevation = if (prominent) 5.dp else 1.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (prominent) 12.dp else 7.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Surface(
                shape = AtmosphereTheme.shapes.small,
                color = AtmosphereTheme.colorScheme.secondaryContainer,
                contentColor = AtmosphereTheme.colorScheme.secondary,
            ) { Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(17.dp)) } }
            Text(
                text = stringResource(title),
                style = if (prominent) AtmosphereTheme.typography.labelLarge else AtmosphereTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
                color = AtmosphereTheme.colorScheme.secondaryContainer,
                contentColor = AtmosphereTheme.colorScheme.onSecondaryContainer,
                shape = AtmosphereTheme.shapes.large,
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Today, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_calendar_title),
                    style = AtmosphereTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = calendarStatusText(content),
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    style = AtmosphereTheme.typography.bodyMedium,
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
            color = AtmosphereTheme.colorScheme.onSurfaceVariant,
            style = AtmosphereTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CalendarError(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        color = AtmosphereTheme.colorScheme.errorContainer,
        contentColor = AtmosphereTheme.colorScheme.onErrorContainer,
        shape = AtmosphereTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                Text(
                    text = stringResource(R.string.dashboard_calendar_error),
                    style = AtmosphereTheme.typography.bodyMedium,
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
            color = AtmosphereTheme.colorScheme.surfaceContainerLow,
            shape = AtmosphereTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = AtmosphereTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.dashboard_calendar_empty),
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    style = AtmosphereTheme.typography.bodyMedium,
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
                HorizontalDivider(color = AtmosphereTheme.colorScheme.outlineVariant)
            }
        }
        if (events.size > visibleEvents.size) {
            Text(
                text = pluralStringResource(
                    R.plurals.dashboard_calendar_more_events,
                    events.size - visibleEvents.size,
                    events.size - visibleEvents.size,
                ),
                color = AtmosphereTheme.colorScheme.primary,
                style = AtmosphereTheme.typography.labelLarge,
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
            color = AtmosphereTheme.colorScheme.tertiaryContainer,
            contentColor = AtmosphereTheme.colorScheme.onTertiaryContainer,
            shape = AtmosphereTheme.shapes.medium,
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(eventTypeIcon(event.type), contentDescription = null)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = event.title,
                style = AtmosphereTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.dashboard_calendar_event_when, date, time),
                color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                style = AtmosphereTheme.typography.bodySmall,
            )
        }
    }
}

private fun eventTypeIcon(type: CalendarEventType): ImageVector = when (type) {
    CalendarEventType.HOUSEHOLD -> Icons.Outlined.Home
    CalendarEventType.MAINTENANCE -> Icons.Outlined.Settings
    CalendarEventType.APPOINTMENT -> Icons.Outlined.Schedule
    CalendarEventType.SHOPPING -> Icons.Outlined.AccountBalanceWallet
    CalendarEventType.MONEY -> Icons.Outlined.AccountBalanceWallet
    CalendarEventType.TASK -> Icons.Outlined.Checklist
    CalendarEventType.OTHER -> Icons.Outlined.CalendarMonth
}

@Composable
private fun FeatureReadinessGrid(
    money: DashboardMoneyContent,
    tasks: DashboardTasksContent,
    onOpenMoney: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenRequests: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 760.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureReadinessCard(
                    title = R.string.dashboard_money_title,
                    description = dashboardMoneyDescription(money),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onOpenMoney,
                    connected = money is DashboardMoneyContent.Ready,
                    modifier = Modifier.weight(1f),
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_tasks_title,
                    description = dashboardTasksDescription(tasks),
                    icon = Icons.Outlined.Checklist,
                    onClick = onOpenTasks,
                    connected = tasks is DashboardTasksContent.Ready,
                    modifier = Modifier.weight(1f),
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_requests_title,
                    description = dashboardRequestsDescription(tasks),
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onOpenRequests,
                    connected = tasks is DashboardTasksContent.Ready,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureReadinessCard(
                    title = R.string.dashboard_money_title,
                    description = dashboardMoneyDescription(money),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onOpenMoney,
                    connected = money is DashboardMoneyContent.Ready,
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_tasks_title,
                    description = dashboardTasksDescription(tasks),
                    icon = Icons.Outlined.Checklist,
                    onClick = onOpenTasks,
                    connected = tasks is DashboardTasksContent.Ready,
                )
                FeatureReadinessCard(
                    title = R.string.dashboard_requests_title,
                    description = dashboardRequestsDescription(tasks),
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onOpenRequests,
                    connected = tasks is DashboardTasksContent.Ready,
                )
            }
        }
    }
}

@Composable
private fun FeatureReadinessCard(
    @StringRes title: Int,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    connected: Boolean = false,
) {
    val statusColor = if (connected) {
        AtmosphereTheme.colorScheme.statusPositive
    } else {
        AtmosphereTheme.colorScheme.statusAttention
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AtmosphereTheme.colorScheme.cardLevel1,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = AtmosphereTheme.colorScheme.primary.copy(alpha = .15f),
                contentColor = AtmosphereTheme.colorScheme.primary,
                shape = AtmosphereTheme.shapes.small,
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, Modifier.size(20.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = stringResource(title), style = AtmosphereTheme.typography.titleMedium)
                Text(
                    text = description,
                    color = AtmosphereTheme.colorScheme.onSurfaceVariant,
                    style = AtmosphereTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = statusColor.copy(alpha = .14f),
                contentColor = statusColor,
                shape = AtmosphereTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (connected) SharedHouseIcons.Approved else SharedHouseIcons.Pending,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(if (connected) R.string.dashboard_connected_badge else R.string.dashboard_not_connected_badge),
                        style = AtmosphereTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun dashboardTasksDescription(content: DashboardTasksContent): String = when (content) {
    DashboardTasksContent.Loading -> stringResource(R.string.dashboard_tasks_loading)
    DashboardTasksContent.Error -> stringResource(R.string.dashboard_tasks_error)
    is DashboardTasksContent.Ready -> {
        val active = pluralStringResource(
            R.plurals.dashboard_active_tasks_count,
            content.activeCount,
            content.activeCount,
        )
        val pending = pluralStringResource(
            R.plurals.tasks_pending_count,
            content.pendingRequests,
            content.pendingRequests,
        )
        content.nextMineTitle?.let {
            stringResource(R.string.dashboard_tasks_next, it, active, pending)
        } ?: stringResource(R.string.dashboard_tasks_ready, active, pending)
    }
}

@Composable
private fun dashboardMoneyDescription(content: DashboardMoneyContent): String = when (content) {
    DashboardMoneyContent.Loading -> stringResource(R.string.dashboard_money_loading)
    DashboardMoneyContent.Error -> stringResource(R.string.dashboard_money_error)
    is DashboardMoneyContent.Ready -> stringResource(
        R.string.dashboard_money_ready,
        dashboardMoney(content.amountDueMinor, content.currency),
        content.outstandingCount,
    )
}

@Composable
private fun dashboardRequestsDescription(content: DashboardTasksContent): String = when (content) {
    DashboardTasksContent.Loading -> stringResource(R.string.dashboard_requests_loading)
    DashboardTasksContent.Error -> stringResource(R.string.dashboard_requests_error)
    is DashboardTasksContent.Ready -> pluralStringResource(
        R.plurals.dashboard_requests_ready,
        content.pendingRequests,
        content.pendingRequests,
    )
}

@Composable
private fun GuidanceCard(onOpenGuides: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AtmosphereTheme.colorScheme.tertiaryContainer,
        contentColor = AtmosphereTheme.colorScheme.onTertiaryContainer,
        shape = AtmosphereTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
            Text(
                text = stringResource(R.string.dashboard_guidance_title),
                style = AtmosphereTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.dashboard_guidance_description),
                style = AtmosphereTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onOpenGuides) {
                Text(stringResource(R.string.dashboard_action_guides))
            }
        }
    }
}
