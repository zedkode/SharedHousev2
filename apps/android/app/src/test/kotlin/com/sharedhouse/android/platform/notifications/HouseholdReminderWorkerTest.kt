package com.sharedhouse.android.platform.notifications

import com.sharedhouse.android.preferences.NotificationPreferences
import com.sharedhouse.android.preferences.QuietHours
import com.sharedhouse.android.preferences.ReminderLeadTime
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HouseholdReminderWorkerTest {
    @Test
    fun `reminder lead time uses the household timezone`() {
        val result = reminderInstant(
            dueAt = LocalDateTime.parse("2026-10-25T09:00:00"),
            zoneId = ZoneId.of("Europe/London"),
            preferences = NotificationPreferences(leadTime = ReminderLeadTime.ONE_HOUR),
        )

        assertEquals("2026-10-25T08:00:00Z", result.toString())
    }

    @Test
    fun `quiet hours defer an overnight reminder to their configured end`() {
        val result = reminderInstant(
            dueAt = LocalDateTime.parse("2026-08-12T06:00:00"),
            zoneId = ZoneId.of("Europe/London"),
            preferences = NotificationPreferences(
                leadTime = ReminderLeadTime.AT_TIME,
                quietHours = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60),
            ),
        )

        assertEquals("2026-08-12T06:00:00Z", result.toString())
    }

    @Test
    fun `notification action accepts only scoped versioned task commands`() {
        assertTrue(
            isValidNotificationAction(
                householdId = "10000000-0000-7000-8000-000000000001",
                taskId = "10000000-0000-7000-8000-000000000002",
                version = 3,
                action = "complete",
            ),
        )
        assertFalse(
            isValidNotificationAction(
                householdId = "10000000-0000-7000-8000-000000000001",
                taskId = "10000000-0000-7000-8000-000000000002",
                version = 3,
                action = "cancel",
            ),
        )
    }
}
