package com.sharedhouse.android.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferencesTest {
    @Test
    fun `overnight quiet hours include late night and early morning`() {
        val quietHours = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60)

        assertTrue(quietHours.contains(23 * 60))
        assertTrue(quietHours.contains(6 * 60 + 59))
        assertFalse(quietHours.contains(12 * 60))
    }

    @Test
    fun `disabled quiet hours never suppress a minute`() {
        val quietHours = QuietHours(enabled = false, startMinutes = 9 * 60, endMinutes = 17 * 60)

        assertFalse(quietHours.contains(12 * 60))
    }

    @Test
    fun `invalid persisted quiet hours fall back safely`() {
        val quietHours = QuietHours.fromStorage(
            enabled = true,
            startMinutes = -1,
            endMinutes = 2_000,
        )

        assertEquals(22 * 60, quietHours.startMinutes)
        assertEquals(7 * 60, quietHours.endMinutes)
        assertTrue(quietHours.enabled)
    }

    @Test
    fun `time adjustment wraps and skips the disallowed boundary`() {
        assertEquals(23 * 60 + 30, adjustedQuietTime(current = 0, deltaMinutes = -30, disallowed = 7 * 60))
        assertEquals(8 * 60, adjustedQuietTime(current = 7 * 60, deltaMinutes = 30, disallowed = 7 * 60 + 30))
    }

    @Test
    fun `category updates affect only the requested category`() {
        val notifications = NotificationPreferences().withCategory(NotificationCategory.MONEY, false)

        assertFalse(notifications.money)
        assertTrue(notifications.tasks)
        assertTrue(notifications.security)
    }

    @Test
    fun `unknown enum storage values use conservative defaults`() {
        assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromStorage("sepia"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorage("fr"))
        assertEquals(TextScale.STANDARD, TextScale.fromStorage("huge"))
        assertEquals(ReminderLeadTime.FIFTEEN_MINUTES, ReminderLeadTime.fromStorage("five_days"))
    }
}
