package com.sharedhouse.android.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarMathTest {
    @Test
    fun `week range honours configured first day`() {
        val range = CalendarPeriodCalculator.rangeFor(
            view = CalendarView.WEEK,
            anchorDate = LocalDate.of(2026, 8, 1),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(LocalDate.of(2026, 7, 27), range.start)
        assertEquals(LocalDate.of(2026, 8, 2), range.endInclusive)
        assertTrue(LocalDate.of(2026, 8, 1) in range)
        assertFalse(LocalDate.of(2026, 8, 3) in range)
    }

    @Test
    fun `month quarter and leap year ranges are inclusive`() {
        assertEquals(
            CalendarDateRange(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)),
            CalendarPeriodCalculator.rangeFor(
                CalendarView.MONTH,
                LocalDate.of(2028, 2, 14),
                DayOfWeek.MONDAY,
            ),
        )
        assertEquals(
            CalendarDateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)),
            CalendarPeriodCalculator.rangeFor(
                CalendarView.QUARTER,
                LocalDate.of(2026, 8, 1),
                DayOfWeek.MONDAY,
            ),
        )
        assertEquals(
            CalendarDateRange(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)),
            CalendarPeriodCalculator.rangeFor(
                CalendarView.YEAR,
                LocalDate.of(2028, 6, 1),
                DayOfWeek.MONDAY,
            ),
        )
    }

    @Test
    fun `period navigation uses view sized steps`() {
        val anchor = LocalDate.of(2026, 8, 15)

        assertEquals(
            LocalDate.of(2026, 8, 22),
            CalendarPeriodCalculator.moveAnchor(
                CalendarView.WEEK,
                anchor,
                CalendarPeriodDirection.NEXT,
            ),
        )
        assertEquals(
            LocalDate.of(2026, 5, 15),
            CalendarPeriodCalculator.moveAnchor(
                CalendarView.QUARTER,
                anchor,
                CalendarPeriodDirection.PREVIOUS,
            ),
        )
        assertEquals(
            LocalDate.of(2027, 8, 15),
            CalendarPeriodCalculator.moveAnchor(
                CalendarView.YEAR,
                anchor,
                CalendarPeriodDirection.NEXT,
            ),
        )
    }

    @Test
    fun `month cells are complete weeks aligned to first day`() {
        val dates = CalendarPeriodCalculator.monthGridDates(
            month = YearMonth.of(2026, 8),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dates.last().dayOfWeek)
        assertEquals(0, dates.size % 7)
        assertTrue(LocalDate.of(2026, 8, 1) in dates)
        assertTrue(LocalDate.of(2026, 8, 31) in dates)
    }

    @Test
    fun `day summaries contain only real matching events and order all day first`() {
        val selectedDate = LocalDate.of(2026, 8, 1)
        val allDay = event(
            id = "all-day",
            date = selectedDate,
            startTime = null,
            type = CalendarEventType.MAINTENANCE,
        )
        val timed = event(
            id = "timed",
            date = selectedDate,
            startTime = LocalTime.of(9, 30),
            type = CalendarEventType.APPOINTMENT,
        )
        val anotherDay = event(
            id = "tomorrow",
            date = selectedDate.plusDays(1),
            startTime = LocalTime.NOON,
        )

        val summary = CalendarPeriodCalculator.summaries(
            dates = listOf(selectedDate),
            events = listOf(timed, anotherDay, allDay),
        ).single()

        assertEquals(listOf("all-day", "timed"), summary.events.map(CalendarEventUi::id))
        assertEquals(2, summary.eventCount)
        assertEquals(1, summary.maintenanceCount)
        assertEquals(1, summary.appointmentCount)
    }

    @Test
    fun `navigation reducer updates selection without changing loaded events`() {
        val event = event(
            id = "persisted",
            date = LocalDate.of(2026, 8, 1),
            startTime = null,
        )
        val state = CalendarUiState(
            view = CalendarView.MONTH,
            anchorDate = LocalDate.of(2026, 8, 1),
            selectedDate = LocalDate.of(2026, 8, 1),
            content = CalendarContent.Ready(listOf(event)),
        )

        val selected = CalendarUiReducer.reduce(
            state,
            CalendarAction.SelectDate(LocalDate.of(2026, 8, 9)),
        )
        val moved = CalendarUiReducer.reduce(
            selected,
            CalendarAction.MovePeriod(CalendarPeriodDirection.NEXT),
        )
        val today = CalendarUiReducer.reduce(
            moved,
            CalendarAction.GoToToday,
            today = LocalDate.of(2026, 8, 20),
        )

        assertEquals(LocalDate.of(2026, 8, 9), selected.selectedDate)
        assertEquals(LocalDate.of(2026, 9, 9), moved.selectedDate)
        assertEquals(LocalDate.of(2026, 8, 20), today.selectedDate)
        assertEquals(state.content, today.content)
    }

    @Test
    fun `wire event types map exactly and unknown values stay honest`() {
        assertEquals("maintenance", CalendarEventType.MAINTENANCE.wireValue)
        assertEquals(
            CalendarEventType.SHOPPING,
            CalendarEventType.fromWireValue("shopping"),
        )
        assertEquals(CalendarEventType.OTHER, CalendarEventType.fromWireValue("unsupported"))
    }

    @Test
    fun `calendar capabilities are restrictive unless application authorizes them`() {
        val state = CalendarUiState()
        val event = event(
            id = "read-only",
            date = LocalDate.of(2026, 8, 1),
            startTime = null,
        )

        assertFalse(state.canCreateEvents)
        assertFalse(event.canEdit)
        assertFalse(event.canDelete)
    }

    private fun event(
        id: String,
        date: LocalDate,
        startTime: LocalTime?,
        type: CalendarEventType = CalendarEventType.OTHER,
    ) = CalendarEventUi(
        id = id,
        title = id,
        date = date,
        startTime = startTime,
        endTime = startTime?.plusHours(1),
        type = type,
        version = 1,
    )
}
