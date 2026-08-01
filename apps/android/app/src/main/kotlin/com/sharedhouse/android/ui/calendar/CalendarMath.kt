package com.sharedhouse.android.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarDateRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    init {
        require(!endInclusive.isBefore(start)) { "Calendar range end must not precede start." }
    }

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)
}

data class CalendarDaySummary(
    val date: LocalDate,
    val events: List<CalendarEventUi>,
    val eventCount: Int = events.size,
    val maintenanceCount: Int = events.count { it.type == CalendarEventType.MAINTENANCE },
    val appointmentCount: Int = events.count { it.type == CalendarEventType.APPOINTMENT },
)

object CalendarPeriodCalculator {
    fun rangeFor(
        view: CalendarView,
        anchorDate: LocalDate,
        firstDayOfWeek: DayOfWeek,
    ): CalendarDateRange = when (view) {
        CalendarView.WEEK -> {
            val start = anchorDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            CalendarDateRange(start, start.plusDays(6))
        }

        CalendarView.MONTH -> {
            val month = YearMonth.from(anchorDate)
            CalendarDateRange(month.atDay(1), month.atEndOfMonth())
        }

        CalendarView.QUARTER -> {
            val firstMonth = ((anchorDate.monthValue - 1) / 3) * 3 + 1
            val start = LocalDate.of(anchorDate.year, firstMonth, 1)
            CalendarDateRange(start, start.plusMonths(3).minusDays(1))
        }

        CalendarView.YEAR -> CalendarDateRange(
            LocalDate.of(anchorDate.year, 1, 1),
            LocalDate.of(anchorDate.year, 12, 31),
        )
    }

    fun moveAnchor(
        view: CalendarView,
        anchorDate: LocalDate,
        direction: CalendarPeriodDirection,
    ): LocalDate {
        val amount = if (direction == CalendarPeriodDirection.NEXT) 1L else -1L
        return when (view) {
            CalendarView.WEEK -> anchorDate.plusWeeks(amount)
            CalendarView.MONTH -> anchorDate.plusMonths(amount)
            CalendarView.QUARTER -> anchorDate.plusMonths(amount * 3)
            CalendarView.YEAR -> anchorDate.plusYears(amount)
        }
    }

    /** Dates required to render a complete week-aligned month grid. */
    fun monthGridDates(
        month: YearMonth,
        firstDayOfWeek: DayOfWeek,
    ): List<LocalDate> {
        val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val lastDay = month.atEndOfMonth()
        val lastDayOfWeek = firstDayOfWeek.minus(1)
        val end = lastDay.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))
        return generateSequence(start) { current ->
            current.plusDays(1).takeUnless { it.isAfter(end) }
        }.toList()
    }

    fun summaries(
        dates: Iterable<LocalDate>,
        events: List<CalendarEventUi>,
    ): List<CalendarDaySummary> = dates.map { date ->
        CalendarDaySummary(
            date = date,
            events = events
                .filter { event -> event.occursOn(date) }
                .sortedWith(compareBy<CalendarEventUi> { it.startTime != null }.thenBy { it.startTime }),
        )
    }
}

fun CalendarEventUi.occursOn(date: LocalDate): Boolean = this.date == date

data class CalendarDraftInput(
    val title: String,
    val description: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAllDay: Boolean,
    val type: CalendarEventType,
    val reminderMinutesBefore: Int?,
)

enum class CalendarDraftError {
    TITLE_REQUIRED,
    TITLE_TOO_LONG,
    DESCRIPTION_TOO_LONG,
    END_NOT_AFTER_START,
    REMINDER_OUT_OF_RANGE,
}

data class CalendarDraftValidation(
    val errors: Set<CalendarDraftError>,
    val draft: CalendarEventDraft?,
) {
    val isValid: Boolean get() = errors.isEmpty() && draft != null
}

object CalendarDraftValidator {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_DESCRIPTION_LENGTH = 1_000

    fun validate(input: CalendarDraftInput): CalendarDraftValidation {
        val errors = buildSet {
            if (input.title.isBlank()) add(CalendarDraftError.TITLE_REQUIRED)
            if (input.title.trim().length > MAX_TITLE_LENGTH) add(CalendarDraftError.TITLE_TOO_LONG)
            if (input.description.length > MAX_DESCRIPTION_LENGTH) {
                add(CalendarDraftError.DESCRIPTION_TOO_LONG)
            }
            if (!input.isAllDay && !input.endTime.isAfter(input.startTime)) {
                add(CalendarDraftError.END_NOT_AFTER_START)
            }
            if (input.reminderMinutesBefore != null && input.reminderMinutesBefore !in 0..10_080) {
                add(CalendarDraftError.REMINDER_OUT_OF_RANGE)
            }
        }
        if (errors.isNotEmpty()) return CalendarDraftValidation(errors, null)

        return CalendarDraftValidation(
            errors = emptySet(),
            draft = CalendarEventDraft(
                title = input.title.trim(),
                description = input.description.trim().ifBlank { null },
                type = input.type,
                date = input.date,
                startTime = input.startTime.takeUnless { input.isAllDay },
                endTime = input.endTime.takeUnless { input.isAllDay },
                reminderMinutesBefore = input.reminderMinutesBefore,
            ),
        )
    }
}

/** Pure navigation reducer that the application shell can use or mirror in its ViewModel. */
object CalendarUiReducer {
    fun reduce(
        state: CalendarUiState,
        action: CalendarAction,
        today: LocalDate = LocalDate.now(state.zoneId),
    ): CalendarUiState = when (action) {
        is CalendarAction.ChangeView -> state.copy(
            view = action.view,
            anchorDate = state.selectedDate,
        )

        is CalendarAction.MovePeriod -> state.copy(
            anchorDate = CalendarPeriodCalculator.moveAnchor(
                state.view,
                state.anchorDate,
                action.direction,
            ),
            selectedDate = CalendarPeriodCalculator.moveAnchor(
                state.view,
                state.selectedDate,
                action.direction,
            ),
        )

        is CalendarAction.SelectDate -> state.copy(
            anchorDate = action.date,
            selectedDate = action.date,
        )

        CalendarAction.GoToToday -> state.copy(
            anchorDate = today,
            selectedDate = today,
        )

        else -> state
    }
}
