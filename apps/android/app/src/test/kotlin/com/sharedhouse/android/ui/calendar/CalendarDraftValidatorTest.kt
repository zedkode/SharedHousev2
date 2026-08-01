package com.sharedhouse.android.ui.calendar

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarDraftValidatorTest {
    @Test
    fun `valid timed input produces normalized backend aligned draft`() {
        val validation = CalendarDraftValidator.validate(
            input = validInput().copy(
                title = "  Boiler inspection  ",
                description = "  Annual service  ",
                reminderMinutesBefore = 60,
            ),
        )

        assertTrue(validation.isValid)
        assertEquals("Boiler inspection", validation.draft?.title)
        assertEquals("Annual service", validation.draft?.description)
        assertEquals(LocalTime.of(9, 0), validation.draft?.startTime)
        assertEquals(LocalTime.of(10, 0), validation.draft?.endTime)
        assertEquals(60, validation.draft?.reminderMinutesBefore)
    }

    @Test
    fun `all day input omits start and end times`() {
        val validation = CalendarDraftValidator.validate(
            input = validInput().copy(
                isAllDay = true,
                startTime = LocalTime.of(23, 0),
                endTime = LocalTime.of(1, 0),
            ),
        )

        assertTrue(validation.isValid)
        assertNull(validation.draft?.startTime)
        assertNull(validation.draft?.endTime)
    }

    @Test
    fun `blank or oversized text is rejected without draft`() {
        val validation = CalendarDraftValidator.validate(
            input = validInput().copy(
                title = " ",
                description = "x".repeat(CalendarDraftValidator.MAX_DESCRIPTION_LENGTH + 1),
            ),
        )

        assertFalse(validation.isValid)
        assertNull(validation.draft)
        assertEquals(
            setOf(
                CalendarDraftError.TITLE_REQUIRED,
                CalendarDraftError.DESCRIPTION_TOO_LONG,
            ),
            validation.errors,
        )
    }

    @Test
    fun `timed event must end later on the same date`() {
        val validation = CalendarDraftValidator.validate(
            input = validInput().copy(
                startTime = LocalTime.of(18, 0),
                endTime = LocalTime.of(17, 59),
            ),
        )

        assertEquals(setOf(CalendarDraftError.END_NOT_AFTER_START), validation.errors)
        assertNull(validation.draft)
    }

    @Test
    fun `reminder outside supported seven day window is rejected`() {
        val validation = CalendarDraftValidator.validate(
            input = validInput().copy(reminderMinutesBefore = 10_081),
        )

        assertEquals(setOf(CalendarDraftError.REMINDER_OUT_OF_RANGE), validation.errors)
        assertNull(validation.draft)
    }

    private fun validInput() = CalendarDraftInput(
        title = "Boiler inspection",
        description = "Annual service",
        date = LocalDate.of(2026, 8, 4),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        isAllDay = false,
        type = CalendarEventType.MAINTENANCE,
        reminderMinutesBefore = null,
    )
}
