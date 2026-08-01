package com.sharedhouse.android.platform.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.sharedhouse.android.R
import com.sharedhouse.android.preferences.NotificationCategory
import com.sharedhouse.android.preferences.NotificationPreferences

enum class NotificationPermissionStatus {
    GRANTED,
    NOT_GRANTED,
    BLOCKED_BY_SYSTEM,
}

enum class LocalTestNotificationResult {
    POSTED,
    RUNTIME_PERMISSION_REQUIRED,
    APP_NOTIFICATIONS_BLOCKED,
    TASK_CATEGORY_DISABLED,
    TASK_CHANNEL_BLOCKED,
}

object SharedHouseNotifications {
    const val CHANNEL_MONEY = "sharedhouse.money"
    const val CHANNEL_TASKS = "sharedhouse.tasks"
    const val CHANNEL_REQUESTS = "sharedhouse.requests"
    const val CHANNEL_ANNOUNCEMENTS = "sharedhouse.announcements"
    const val CHANNEL_SECURITY = "sharedhouse.security"
    const val CHANNEL_SUBSCRIPTION = "sharedhouse.subscription"

    private const val CHANNEL_GROUP = "sharedhouse.household_activity"
    private const val LOCAL_TEST_NOTIFICATION_ID = 71_001

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CHANNEL_GROUP,
                context.getString(R.string.notification_channel_group),
            ),
        )
        manager.createNotificationChannels(
            listOf(
                channel(
                    context,
                    CHANNEL_MONEY,
                    R.string.notification_channel_money,
                    R.string.notification_channel_money_description,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                channel(
                    context,
                    CHANNEL_TASKS,
                    R.string.notification_channel_tasks,
                    R.string.notification_channel_tasks_description,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                channel(
                    context,
                    CHANNEL_REQUESTS,
                    R.string.notification_channel_requests,
                    R.string.notification_channel_requests_description,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                channel(
                    context,
                    CHANNEL_ANNOUNCEMENTS,
                    R.string.notification_channel_announcements,
                    R.string.notification_channel_announcements_description,
                    NotificationManager.IMPORTANCE_LOW,
                ),
                channel(
                    context,
                    CHANNEL_SECURITY,
                    R.string.notification_channel_security,
                    R.string.notification_channel_security_description,
                    NotificationManager.IMPORTANCE_HIGH,
                ),
                channel(
                    context,
                    CHANNEL_SUBSCRIPTION,
                    R.string.notification_channel_subscription,
                    R.string.notification_channel_subscription_description,
                    NotificationManager.IMPORTANCE_LOW,
                ),
            ),
        )
    }

    fun permissionStatus(context: Context): NotificationPermissionStatus {
        val manager = context.getSystemService(NotificationManager::class.java)
        return classifyNotificationPermission(
            runtimePermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            runtimePermissionGranted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            appNotificationsEnabled = manager.areNotificationsEnabled(),
        )
    }

    fun postLocalTest(
        context: Context,
        preferences: NotificationPreferences,
    ): LocalTestNotificationResult {
        if (!preferences.isEnabled(NotificationCategory.TASKS)) {
            return LocalTestNotificationResult.TASK_CATEGORY_DISABLED
        }
        when (permissionStatus(context)) {
            NotificationPermissionStatus.NOT_GRANTED -> {
                return LocalTestNotificationResult.RUNTIME_PERMISSION_REQUIRED
            }
            NotificationPermissionStatus.BLOCKED_BY_SYSTEM -> {
                return LocalTestNotificationResult.APP_NOTIFICATIONS_BLOCKED
            }
            NotificationPermissionStatus.GRANTED -> Unit
        }
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_TASKS)?.importance == NotificationManager.IMPORTANCE_NONE) {
            return LocalTestNotificationResult.TASK_CHANNEL_BLOCKED
        }
        val notification = Notification.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_body))
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        manager.notify(LOCAL_TEST_NOTIFICATION_ID, notification)
        return LocalTestNotificationResult.POSTED
    }

    fun openSystemSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun channelId(category: NotificationCategory): String = when (category) {
        NotificationCategory.MONEY -> CHANNEL_MONEY
        NotificationCategory.TASKS -> CHANNEL_TASKS
        NotificationCategory.REQUESTS -> CHANNEL_REQUESTS
        NotificationCategory.ANNOUNCEMENTS -> CHANNEL_ANNOUNCEMENTS
        NotificationCategory.SECURITY -> CHANNEL_SECURITY
        NotificationCategory.SUBSCRIPTION -> CHANNEL_SUBSCRIPTION
    }

    private fun channel(
        context: Context,
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        importance: Int,
    ): NotificationChannel = NotificationChannel(
        id,
        context.getString(nameRes),
        importance,
    ).apply {
        group = CHANNEL_GROUP
        description = context.getString(descriptionRes)
        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        setShowBadge(id != CHANNEL_SUBSCRIPTION)
    }
}

fun classifyNotificationPermission(
    runtimePermissionRequired: Boolean,
    runtimePermissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
): NotificationPermissionStatus = when {
    runtimePermissionRequired && !runtimePermissionGranted -> NotificationPermissionStatus.NOT_GRANTED
    !appNotificationsEnabled -> NotificationPermissionStatus.BLOCKED_BY_SYSTEM
    else -> NotificationPermissionStatus.GRANTED
}
