package com.sharedhouse.android.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val PREFERENCES_FILE_NAME = "sharedhouse_app_preferences"

private val Context.sharedHousePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_FILE_NAME,
)

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.sharedHousePreferencesDataStore)

    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map(::decodePreferences)

    suspend fun completeTutorial(skipped: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TUTORIAL_COMPLETED] = true
            preferences[Keys.TUTORIAL_SKIPPED] = skipped
        }
    }

    suspend fun showTutorialAgain() {
        dataStore.edit { preferences ->
            preferences[Keys.TUTORIAL_COMPLETED] = false
            preferences[Keys.TUTORIAL_SKIPPED] = false
        }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) = update(Keys.APPEARANCE_MODE, mode.storageValue)

    suspend fun setDynamicColor(enabled: Boolean) = update(Keys.DYNAMIC_COLOR, enabled)

    suspend fun setLanguage(language: AppLanguage) = update(Keys.LANGUAGE, language.storageValue)

    suspend fun setReducedMotion(enabled: Boolean) = update(Keys.REDUCED_MOTION, enabled)

    suspend fun setHighContrast(enabled: Boolean) = update(Keys.HIGH_CONTRAST, enabled)

    suspend fun setTextScale(scale: TextScale) = update(Keys.TEXT_SCALE, scale.storageValue)

    suspend fun setNotificationCategory(category: NotificationCategory, enabled: Boolean) {
        update(Keys.notificationCategory(category), enabled)
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) = update(Keys.QUIET_HOURS_ENABLED, enabled)

    suspend fun setQuietHours(startMinutes: Int, endMinutes: Int) {
        QuietHours(enabled = true, startMinutes = startMinutes, endMinutes = endMinutes)
        dataStore.edit { preferences ->
            preferences[Keys.QUIET_HOURS_START] = startMinutes
            preferences[Keys.QUIET_HOURS_END] = endMinutes
        }
    }

    suspend fun setReminderLeadTime(leadTime: ReminderLeadTime) =
        update(Keys.REMINDER_LEAD_TIME, leadTime.storageValue)

    suspend fun setNotificationSound(enabled: Boolean) = update(Keys.NOTIFICATION_SOUND, enabled)

    suspend fun setNotificationVibration(enabled: Boolean) = update(Keys.NOTIFICATION_VIBRATION, enabled)

    suspend fun setAnalyticsEnabled(enabled: Boolean) = update(Keys.ANALYTICS_ENABLED, enabled)

    suspend fun setCrashReportingEnabled(enabled: Boolean) = update(Keys.CRASH_REPORTING_ENABLED, enabled)

    suspend fun setAdsEnabled(enabled: Boolean) = update(Keys.ADS_ENABLED, enabled)

    private suspend fun update(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    private suspend fun update(key: Preferences.Key<String>, value: String) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    private object Keys {
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val TUTORIAL_SKIPPED = booleanPreferencesKey("tutorial_skipped")
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("language")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val NOTIFICATION_MONEY = booleanPreferencesKey("notification_money")
        val NOTIFICATION_TASKS = booleanPreferencesKey("notification_tasks")
        val NOTIFICATION_REQUESTS = booleanPreferencesKey("notification_requests")
        val NOTIFICATION_ANNOUNCEMENTS = booleanPreferencesKey("notification_announcements")
        val NOTIFICATION_SECURITY = booleanPreferencesKey("notification_security")
        val NOTIFICATION_SUBSCRIPTION = booleanPreferencesKey("notification_subscription")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val REMINDER_LEAD_TIME = stringPreferencesKey("reminder_lead_time")
        val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        val NOTIFICATION_VIBRATION = booleanPreferencesKey("notification_vibration")
        val ANALYTICS_ENABLED = booleanPreferencesKey("privacy_analytics_enabled")
        val CRASH_REPORTING_ENABLED = booleanPreferencesKey("privacy_crash_reporting_enabled")
        val ADS_ENABLED = booleanPreferencesKey("privacy_ads_enabled")

        fun notificationCategory(category: NotificationCategory): Preferences.Key<Boolean> = when (category) {
            NotificationCategory.MONEY -> NOTIFICATION_MONEY
            NotificationCategory.TASKS -> NOTIFICATION_TASKS
            NotificationCategory.REQUESTS -> NOTIFICATION_REQUESTS
            NotificationCategory.ANNOUNCEMENTS -> NOTIFICATION_ANNOUNCEMENTS
            NotificationCategory.SECURITY -> NOTIFICATION_SECURITY
            NotificationCategory.SUBSCRIPTION -> NOTIFICATION_SUBSCRIPTION
        }
    }

    private companion object {
        fun decodePreferences(preferences: Preferences): AppPreferences {
            val quietHours = QuietHours.fromStorage(
                enabled = preferences[Keys.QUIET_HOURS_ENABLED] ?: false,
                startMinutes = preferences[Keys.QUIET_HOURS_START] ?: 22 * 60,
                endMinutes = preferences[Keys.QUIET_HOURS_END] ?: 7 * 60,
            )
            return AppPreferences(
                tutorialCompleted = preferences[Keys.TUTORIAL_COMPLETED] ?: false,
                tutorialSkipped = preferences[Keys.TUTORIAL_SKIPPED] ?: false,
                appearanceMode = AppearanceMode.fromStorage(preferences[Keys.APPEARANCE_MODE]),
                dynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
                language = AppLanguage.fromStorage(preferences[Keys.LANGUAGE]),
                reducedMotion = preferences[Keys.REDUCED_MOTION] ?: false,
                highContrast = preferences[Keys.HIGH_CONTRAST] ?: false,
                textScale = TextScale.fromStorage(preferences[Keys.TEXT_SCALE]),
                notifications = NotificationPreferences(
                    money = preferences[Keys.NOTIFICATION_MONEY] ?: true,
                    tasks = preferences[Keys.NOTIFICATION_TASKS] ?: true,
                    requests = preferences[Keys.NOTIFICATION_REQUESTS] ?: true,
                    announcements = preferences[Keys.NOTIFICATION_ANNOUNCEMENTS] ?: true,
                    security = preferences[Keys.NOTIFICATION_SECURITY] ?: true,
                    subscription = preferences[Keys.NOTIFICATION_SUBSCRIPTION] ?: true,
                    quietHours = quietHours,
                    leadTime = ReminderLeadTime.fromStorage(preferences[Keys.REMINDER_LEAD_TIME]),
                    sound = preferences[Keys.NOTIFICATION_SOUND] ?: true,
                    vibration = preferences[Keys.NOTIFICATION_VIBRATION] ?: true,
                ),
                privacy = PrivacyPreferences(
                    analyticsEnabled = preferences[Keys.ANALYTICS_ENABLED] ?: false,
                    crashReportingEnabled = preferences[Keys.CRASH_REPORTING_ENABLED] ?: false,
                    adsEnabled = preferences[Keys.ADS_ENABLED] ?: false,
                ),
            )
        }
    }
}
