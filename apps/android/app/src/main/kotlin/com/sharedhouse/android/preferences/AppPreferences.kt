package com.sharedhouse.android.preferences

enum class AppearanceMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): AppearanceMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

enum class AppLanguage(val storageValue: String, val languageTag: String?) {
    SYSTEM("system", null),
    ENGLISH("en", "en"),
    ROMANIAN("ro", "ro");

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

enum class TextScale(val storageValue: String, val multiplier: Float) {
    STANDARD("standard", 1f),
    LARGE("large", 1.15f),
    EXTRA_LARGE("extra_large", 1.3f);

    companion object {
        fun fromStorage(value: String?): TextScale =
            entries.firstOrNull { it.storageValue == value } ?: STANDARD
    }
}

enum class ReminderLeadTime(val storageValue: String, val minutes: Int) {
    AT_TIME("at_time", 0),
    FIFTEEN_MINUTES("15_minutes", 15),
    ONE_HOUR("1_hour", 60),
    ONE_DAY("1_day", 24 * 60);

    companion object {
        fun fromStorage(value: String?): ReminderLeadTime =
            entries.firstOrNull { it.storageValue == value } ?: FIFTEEN_MINUTES
    }
}

enum class NotificationCategory(val storageValue: String) {
    MONEY("money"),
    TASKS("tasks"),
    REQUESTS("requests"),
    ANNOUNCEMENTS("announcements"),
    SECURITY("security"),
    SUBSCRIPTION("subscription");
}

data class QuietHours(
    val enabled: Boolean = false,
    val startMinutes: Int = 22 * 60,
    val endMinutes: Int = 7 * 60,
) {
    init {
        require(startMinutes in MINUTES_IN_DAY_RANGE) { "Quiet-hours start must be within a day." }
        require(endMinutes in MINUTES_IN_DAY_RANGE) { "Quiet-hours end must be within a day." }
        require(startMinutes != endMinutes) { "Quiet-hours start and end must differ." }
    }

    fun contains(minuteOfDay: Int): Boolean {
        if (!enabled || minuteOfDay !in MINUTES_IN_DAY_RANGE) return false
        return if (startMinutes < endMinutes) {
            minuteOfDay in startMinutes until endMinutes
        } else {
            minuteOfDay >= startMinutes || minuteOfDay < endMinutes
        }
    }

    companion object {
        val MINUTES_IN_DAY_RANGE = 0 until 24 * 60

        fun fromStorage(enabled: Boolean, startMinutes: Int, endMinutes: Int): QuietHours =
            runCatching { QuietHours(enabled, startMinutes, endMinutes) }
                .getOrDefault(QuietHours(enabled = enabled))
    }
}

data class NotificationPreferences(
    val money: Boolean = true,
    val tasks: Boolean = true,
    val requests: Boolean = true,
    val announcements: Boolean = true,
    val security: Boolean = true,
    val subscription: Boolean = true,
    val quietHours: QuietHours = QuietHours(),
    val leadTime: ReminderLeadTime = ReminderLeadTime.FIFTEEN_MINUTES,
    val sound: Boolean = true,
    val vibration: Boolean = true,
) {
    fun isEnabled(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.MONEY -> money
        NotificationCategory.TASKS -> tasks
        NotificationCategory.REQUESTS -> requests
        NotificationCategory.ANNOUNCEMENTS -> announcements
        NotificationCategory.SECURITY -> security
        NotificationCategory.SUBSCRIPTION -> subscription
    }

    fun withCategory(category: NotificationCategory, enabled: Boolean): NotificationPreferences = when (category) {
        NotificationCategory.MONEY -> copy(money = enabled)
        NotificationCategory.TASKS -> copy(tasks = enabled)
        NotificationCategory.REQUESTS -> copy(requests = enabled)
        NotificationCategory.ANNOUNCEMENTS -> copy(announcements = enabled)
        NotificationCategory.SECURITY -> copy(security = enabled)
        NotificationCategory.SUBSCRIPTION -> copy(subscription = enabled)
    }
}

data class AppPreferences(
    val tutorialCompleted: Boolean = false,
    val tutorialSkipped: Boolean = false,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val textScale: TextScale = TextScale.STANDARD,
    val notifications: NotificationPreferences = NotificationPreferences(),
)

fun formatMinutesOfDay(minutes: Int): String {
    require(minutes in QuietHours.MINUTES_IN_DAY_RANGE)
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}

fun adjustedQuietTime(current: Int, deltaMinutes: Int, disallowed: Int): Int {
    require(current in QuietHours.MINUTES_IN_DAY_RANGE)
    require(disallowed in QuietHours.MINUTES_IN_DAY_RANGE)
    val day = 24 * 60
    var candidate = ((current + deltaMinutes) % day + day) % day
    if (candidate == disallowed) {
        candidate = ((candidate + deltaMinutes) % day + day) % day
    }
    return candidate
}
