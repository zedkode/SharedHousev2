package com.sharedhouse.android.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
internal fun CalendarPeriodView(
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.view) {
        CalendarView.WEEK -> WeekTimeline(
            state = state,
            events = events,
            onDateSelected = onDateSelected,
            modifier = modifier,
        )

        CalendarView.MONTH -> MonthCalendar(
            month = YearMonth.from(state.anchorDate),
            state = state,
            events = events,
            onDateSelected = onDateSelected,
            modifier = modifier,
        )

        CalendarView.QUARTER -> QuarterCalendar(
            state = state,
            events = events,
            onDateSelected = onDateSelected,
            modifier = modifier,
        )

        CalendarView.YEAR -> YearCalendar(
            state = state,
            events = events,
            onDateSelected = onDateSelected,
            modifier = modifier,
        )
    }
}

@Composable
private fun WeekTimeline(
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = remember(state.anchorDate, state.firstDayOfWeek) {
        CalendarPeriodCalculator.rangeFor(
            CalendarView.WEEK,
            state.anchorDate,
            state.firstDayOfWeek,
        )
    }
    val dates = remember(range) { (0L..6L).map(range.start::plusDays) }
    val summaries = remember(dates, events, state.zoneId) {
        CalendarPeriodCalculator.summaries(dates, events)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = summaries.size,
            key = { summaries[it].date.toString() },
        ) { index ->
            WeekDayCard(
                summary = summaries[index],
                selected = summaries[index].date == state.selectedDate,
                today = summaries[index].date == LocalDate.now(state.zoneId),
                onClick = { onDateSelected(summaries[index].date) },
            )
        }
    }
}

@Composable
private fun WeekDayCard(
    summary: CalendarDaySummary,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
) {
    val dateDescription = localizedDate(summary.date, FormatStyle.FULL)
    val accessibility = stringResource(
        R.string.calendar_day_accessibility,
        dateDescription,
        summary.eventCount,
        summary.maintenanceCount,
        summary.appointmentCount,
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility
                this.selected = selected
                role = Role.Button
            }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            width = if (today) 2.dp else 1.dp,
            color = if (today) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = localizedWeekday(summary.date),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = localizedDate(summary.date, FormatStyle.MEDIUM),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (today) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_today),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            if (summary.events.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_day_no_events),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                summary.events.take(3).forEach { event ->
                    CompactEventRow(event)
                }
                if (summary.events.size > 3) {
                    Text(
                        text = stringResource(
                            R.string.calendar_more_events,
                            summary.events.size - 3,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dates = remember(month, state.firstDayOfWeek) {
        CalendarPeriodCalculator.monthGridDates(month, state.firstDayOfWeek)
    }
    val summaries = remember(dates, events, state.zoneId) {
        CalendarPeriodCalculator.summaries(dates, events)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        WeekdayHeader(state.firstDayOfWeek)
        summaries.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { summary ->
                    CalendarDayCell(
                        summary = summary,
                        inPrimaryMonth = summary.date.month == month.month,
                        selected = summary.date == state.selectedDate,
                        today = summary.date == LocalDate.now(state.zoneId),
                        onClick = { onDateSelected(summary.date) },
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuarterCalendar(
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = remember(state.anchorDate) {
        CalendarPeriodCalculator.rangeFor(
            CalendarView.QUARTER,
            state.anchorDate,
            state.firstDayOfWeek,
        )
    }
    val months = remember(range) {
        (0L..2L).map { YearMonth.from(range.start).plusMonths(it) }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            count = months.size,
            key = { months[it].toString() },
        ) { index ->
            MiniMonthCard(
                month = months[index],
                state = state,
                events = events,
                onDateSelected = onDateSelected,
            )
        }
    }
}

@Composable
private fun YearCalendar(
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val months = remember(state.anchorDate.year) {
        (1..12).map { month -> YearMonth.of(state.anchorDate.year, month) }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(months, key = YearMonth::toString) { month ->
            MiniMonthCard(
                month = month,
                state = state,
                events = events,
                onDateSelected = onDateSelected,
                compact = true,
            )
        }
    }
}

@Composable
private fun MiniMonthCard(
    month: YearMonth,
    state: CalendarUiState,
    events: List<CalendarEventUi>,
    onDateSelected: (LocalDate) -> Unit,
    compact: Boolean = false,
) {
    val dates = remember(month, state.firstDayOfWeek) {
        CalendarPeriodCalculator.monthGridDates(month, state.firstDayOfWeek)
    }
    val summaries = remember(dates, events, state.zoneId) {
        CalendarPeriodCalculator.summaries(dates, events)
    }
    val eventCount = summaries
        .filter { YearMonth.from(it.date) == month }
        .sumOf(CalendarDaySummary::eventCount)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localizedMonth(month),
                    modifier = Modifier.semantics { heading() },
                    fontWeight = FontWeight.SemiBold,
                    style = if (compact) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                )
                if (eventCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = eventCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            WeekdayHeader(state.firstDayOfWeek, compact = true)
            summaries.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { summary ->
                        MiniCalendarDayCell(
                            summary = summary,
                            inPrimaryMonth = YearMonth.from(summary.date) == month,
                            selected = summary.date == state.selectedDate,
                            today = summary.date == LocalDate.now(state.zoneId),
                            onClick = { onDateSelected(summary.date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(
    firstDayOfWeek: DayOfWeek,
    compact: Boolean = false,
) {
    val days = remember(firstDayOfWeek) {
        (0L..6L).map(firstDayOfWeek::plus)
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = localizedDayOfWeek(day, narrow = compact),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    summary: CalendarDaySummary,
    inPrimaryMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateDescription = localizedDate(summary.date, FormatStyle.FULL)
    val accessibility = stringResource(
        R.string.calendar_day_accessibility,
        dateDescription,
        summary.eventCount,
        summary.maintenanceCount,
        summary.appointmentCount,
    )
    Surface(
        modifier = modifier
            .padding(2.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility
                this.selected = selected
                role = Role.Button
            }
            .clickable(onClick = onClick),
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        shape = MaterialTheme.shapes.medium,
        border = if (today) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = summary.date.dayOfMonth.toString(),
                color = if (inPrimaryMonth) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.eventCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EventDot(summary.events.first().type)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = summary.eventCount.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCalendarDayCell(
    summary: CalendarDaySummary,
    inPrimaryMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateDescription = localizedDate(summary.date, FormatStyle.FULL)
    val accessibility = stringResource(
        R.string.calendar_day_accessibility,
        dateDescription,
        summary.eventCount,
        summary.maintenanceCount,
        summary.appointmentCount,
    )
    Box(
        modifier = modifier
            .heightIn(min = 30.dp)
            .padding(1.dp)
            .semantics {
                contentDescription = accessibility
                this.selected = selected
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected || today) {
            Surface(
                modifier = Modifier.size(28.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                shape = CircleShape,
                border = if (today) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            ) {}
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = summary.date.dayOfMonth.toString(),
                color = if (inPrimaryMonth) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
                style = MaterialTheme.typography.labelSmall,
            )
            if (summary.eventCount > 0) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .then(Modifier),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ) {}
                }
            }
        }
    }
}

@Composable
internal fun CompactEventRow(event: CalendarEventUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = event.type.icon,
            contentDescription = null,
            tint = eventTypeColor(event.type),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = calendarEventTimeLabel(event),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = event.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun EventDot(type: CalendarEventType) {
    Surface(
        modifier = Modifier.size(7.dp),
        color = eventTypeColor(type),
        shape = CircleShape,
    ) {}
}

internal val CalendarEventType.icon: ImageVector
    get() = when (this) {
        CalendarEventType.HOUSEHOLD -> Icons.Outlined.Home
        CalendarEventType.MAINTENANCE -> Icons.Outlined.Build
        CalendarEventType.APPOINTMENT -> Icons.Outlined.Event
        CalendarEventType.SHOPPING -> Icons.Outlined.ShoppingCart
        CalendarEventType.OTHER -> Icons.Outlined.MoreHoriz
    }

@Composable
internal fun eventTypeColor(type: CalendarEventType): Color = when (type) {
    CalendarEventType.HOUSEHOLD -> MaterialTheme.colorScheme.primary
    CalendarEventType.MAINTENANCE -> MaterialTheme.colorScheme.tertiary
    CalendarEventType.APPOINTMENT -> MaterialTheme.colorScheme.secondary
    CalendarEventType.SHOPPING -> MaterialTheme.colorScheme.primary
    CalendarEventType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun calendarPeriodTitle(
    view: CalendarView,
    anchorDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
): String {
    val locale = currentJavaLocale()
    return when (view) {
        CalendarView.WEEK -> {
            val range = CalendarPeriodCalculator.rangeFor(view, anchorDate, firstDayOfWeek)
            val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
            stringResource(
                R.string.calendar_week_range,
                range.start.format(formatter),
                range.endInclusive.format(formatter),
                range.endInclusive.year,
            )
        }

        CalendarView.MONTH -> YearMonth.from(anchorDate).format(
            DateTimeFormatter.ofPattern("LLLL yyyy", locale),
        ).replaceFirstChar { it.titlecase(locale) }

        CalendarView.QUARTER -> {
            val quarter = ((anchorDate.monthValue - 1) / 3) + 1
            stringResource(R.string.calendar_quarter_title, quarter, anchorDate.year)
        }

        CalendarView.YEAR -> anchorDate.year.toString()
    }
}

@Composable
internal fun calendarEventTimeLabel(event: CalendarEventUi): String =
    event.startTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentJavaLocale()))
        ?: stringResource(R.string.calendar_all_day)

@Composable
internal fun localizedDate(date: LocalDate, style: FormatStyle): String =
    date.format(DateTimeFormatter.ofLocalizedDate(style).withLocale(currentJavaLocale()))

@Composable
private fun localizedWeekday(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE", currentJavaLocale()))
        .replaceFirstChar { it.titlecase(currentJavaLocale()) }

@Composable
private fun localizedMonth(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("LLLL", currentJavaLocale()))
        .replaceFirstChar { it.titlecase(currentJavaLocale()) }

@Composable
private fun localizedDayOfWeek(day: DayOfWeek, narrow: Boolean): String {
    val anchor = LocalDate.of(2024, 1, 1).with(TemporalAdjusters.nextOrSame(day))
    val pattern = if (narrow) "EEEEE" else "EEE"
    return anchor.format(DateTimeFormatter.ofPattern(pattern, currentJavaLocale()))
        .replaceFirstChar { it.titlecase(currentJavaLocale()) }
}

@Composable
internal fun currentJavaLocale(): Locale {
    return LocalLocale.current.platformLocale
}
