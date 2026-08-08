package com.sharedhouse.android.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferencesRepositoryTest {
    @Test
    fun `repository persists tutorial appearance and accessibility choices`() = runTest {
        val repository = AppPreferencesRepository(InMemoryPreferencesDataStore())

        repository.completeTutorial(skipped = true)
        repository.setAppearanceMode(AppearanceMode.DARK)
        repository.setLanguage(AppLanguage.ROMANIAN)
        repository.setDynamicColor(false)
        repository.setReducedMotion(true)
        repository.setHighContrast(true)
        repository.setTextScale(TextScale.EXTRA_LARGE)

        val preferences = repository.preferences.first()
        assertTrue(preferences.tutorialCompleted)
        assertTrue(preferences.tutorialSkipped)
        assertEquals(AppearanceMode.DARK, preferences.appearanceMode)
        assertEquals(AppLanguage.ROMANIAN, preferences.language)
        assertFalse(preferences.dynamicColor)
        assertTrue(preferences.reducedMotion)
        assertTrue(preferences.highContrast)
        assertEquals(TextScale.EXTRA_LARGE, preferences.textScale)
    }

    @Test
    fun `repository persists notification controls without scheduling work`() = runTest {
        val repository = AppPreferencesRepository(InMemoryPreferencesDataStore())

        repository.setNotificationCategory(NotificationCategory.MONEY, false)
        repository.setQuietHoursEnabled(true)
        repository.setQuietHours(startMinutes = 23 * 60, endMinutes = 6 * 60 + 30)
        repository.setReminderLeadTime(ReminderLeadTime.ONE_DAY)
        repository.setNotificationSound(false)
        repository.setNotificationVibration(false)

        val notifications = repository.preferences.first().notifications
        assertFalse(notifications.money)
        assertTrue(notifications.tasks)
        assertTrue(notifications.quietHours.enabled)
        assertEquals(23 * 60, notifications.quietHours.startMinutes)
        assertEquals(6 * 60 + 30, notifications.quietHours.endMinutes)
        assertEquals(ReminderLeadTime.ONE_DAY, notifications.leadTime)
        assertFalse(notifications.sound)
        assertFalse(notifications.vibration)
    }

    @Test
    fun `optional diagnostics and advertising remain off until explicitly enabled`() = runTest {
        val repository = AppPreferencesRepository(InMemoryPreferencesDataStore())

        assertFalse(repository.preferences.first().privacy.analyticsEnabled)
        assertFalse(repository.preferences.first().privacy.crashReportingEnabled)
        assertFalse(repository.preferences.first().privacy.adsEnabled)

        repository.setAnalyticsEnabled(true)
        repository.setCrashReportingEnabled(true)
        repository.setAdsEnabled(true)

        val privacy = repository.preferences.first().privacy
        assertTrue(privacy.analyticsEnabled)
        assertTrue(privacy.crashReportingEnabled)
        assertTrue(privacy.adsEnabled)
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val values = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override val data = values.asStateFlow()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        mutex.withLock {
            transform(values.value).also { values.value = it }
        }
}
