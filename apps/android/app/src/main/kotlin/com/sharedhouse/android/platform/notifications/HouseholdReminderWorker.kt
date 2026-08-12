package com.sharedhouse.android.platform.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sharedhouse.android.BuildConfig
import com.sharedhouse.android.MainActivity
import com.sharedhouse.android.R
import com.sharedhouse.android.platform.security.AndroidKeystoreSessionStore
import com.sharedhouse.android.platform.security.SessionLoadResult
import com.sharedhouse.android.platform.security.SessionSaveResult
import com.sharedhouse.android.preferences.NotificationPreferences
import com.sharedhouse.android.ui.money.ExpenseStatus
import com.sharedhouse.android.ui.money.ExpenseUi
import com.sharedhouse.android.ui.tasks.HouseholdTaskUi
import com.sharedhouse.android.ui.tasks.TaskStatus
import com.sharedhouse.network.ApiResult
import com.sharedhouse.network.HouseholdTaskActionDto
import com.sharedhouse.network.SharedHouseApiClient
import com.sharedhouse.network.createSharedHouseHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TYPE_TASK = "task"
private const val TYPE_MONEY = "money"
private const val ACTION_START = "start"
private const val ACTION_COMPLETE = "complete"
private const val REMINDER_WORK_TAG_PREFIX = "sharedhouse-reminders:"

class HouseholdReminderScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun replaceScheduledReminders(
        householdId: String,
        zoneId: ZoneId,
        preferences: NotificationPreferences,
        expenses: List<ExpenseUi>,
        tasks: List<HouseholdTaskUi>,
        now: Instant = Instant.now(),
    ) {
        val tag = REMINDER_WORK_TAG_PREFIX + householdId
        workManager.cancelAllWorkByTag(tag)
        if (SharedHouseNotifications.permissionStatus(context) != NotificationPermissionStatus.GRANTED) return

        if (preferences.money) {
            expenses.asSequence()
                .filter { it.status == ExpenseStatus.APPROVED && it.dueDate >= now.atZone(zoneId).toLocalDate() }
                .forEach { expense ->
                    enqueue(
                        tag = tag,
                        uniqueName = "sharedhouse-reminder-money:${expense.id}",
                        triggerAt = reminderInstant(
                            expense.dueDate.atTime(9, 0),
                            zoneId,
                            preferences,
                        ),
                        now = now,
                        data = workDataOf(
                            ReminderWorker.KEY_TYPE to TYPE_MONEY,
                            ReminderWorker.KEY_ID to expense.id,
                            ReminderWorker.KEY_TITLE to expense.title,
                            ReminderWorker.KEY_DESCRIPTION to context.getString(R.string.notification_money_due_body),
                            ReminderWorker.KEY_HOUSEHOLD_ID to householdId,
                        ),
                    )
                }
        }
        if (preferences.tasks) {
            tasks.asSequence()
                .filter { it.isMine && it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
                .forEach { task ->
                    enqueue(
                        tag = tag,
                        uniqueName = "sharedhouse-reminder-task:${task.id}",
                        triggerAt = reminderInstant(
                            task.dueDate.atTime(task.dueTime?.let(::parseTime) ?: LocalTime.of(9, 0)),
                            zoneId,
                            preferences,
                        ),
                        now = now,
                        data = workDataOf(
                            ReminderWorker.KEY_TYPE to TYPE_TASK,
                            ReminderWorker.KEY_ID to task.id,
                            ReminderWorker.KEY_TITLE to task.title,
                            ReminderWorker.KEY_DESCRIPTION to context.getString(R.string.notification_task_due_body),
                            ReminderWorker.KEY_HOUSEHOLD_ID to householdId,
                            ReminderWorker.KEY_VERSION to task.version,
                            ReminderWorker.KEY_CAN_START to task.canStart,
                            ReminderWorker.KEY_CAN_COMPLETE to task.canComplete,
                        ),
                    )
                }
        }
    }

    private fun enqueue(
        tag: String,
        uniqueName: String,
        triggerAt: Instant,
        now: Instant,
        data: androidx.work.Data,
    ) {
        if (triggerAt.isBefore(now.minusSeconds(60))) return
        val delay = Duration.between(now, triggerAt).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag(tag)
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }
}

internal fun reminderInstant(
    dueAt: LocalDateTime,
    zoneId: ZoneId,
    preferences: NotificationPreferences,
): Instant {
    var reminder = dueAt.minusMinutes(preferences.leadTime.minutes.toLong())
    val quiet = preferences.quietHours
    if (quiet.contains(reminder.hour * 60 + reminder.minute)) {
        val quietEnd = LocalTime.of(quiet.endMinutes / 60, quiet.endMinutes % 60)
        reminder = if (quiet.startMinutes < quiet.endMinutes) {
            reminder.toLocalDate().atTime(quietEnd)
        } else if (reminder.hour * 60 + reminder.minute >= quiet.startMinutes) {
            reminder.toLocalDate().plusDays(1).atTime(quietEnd)
        } else {
            reminder.toLocalDate().atTime(quietEnd)
        }
    }
    return reminder.atZone(zoneId).toInstant()
}

class ReminderWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        if (SharedHouseNotifications.permissionStatus(applicationContext) != NotificationPermissionStatus.GRANTED) {
            return Result.success()
        }
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val description = inputData.getString(KEY_DESCRIPTION) ?: return Result.failure()
        val householdId = inputData.getString(KEY_HOUSEHOLD_ID) ?: return Result.failure()
        val notificationId = stableNotificationId(type, id)
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = if (type == TYPE_TASK) SharedHouseNotifications.CHANNEL_TASKS
        else SharedHouseNotifications.CHANNEL_MONEY
        val publicVersion = Notification.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(description)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        val builder = Notification.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setOnlyAlertOnce(true)

        if (type == TYPE_TASK) {
            val version = inputData.getInt(KEY_VERSION, 0)
            if (inputData.getBoolean(KEY_CAN_START, false)) {
                builder.addAction(taskAction(R.string.notification_action_start, householdId, id, version, ACTION_START, notificationId))
            }
            if (inputData.getBoolean(KEY_CAN_COMPLETE, false)) {
                builder.addAction(taskAction(R.string.notification_action_complete, householdId, id, version, ACTION_COMPLETE, notificationId))
            }
        }
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(notificationId, builder.build())
        return Result.success()
    }

    private fun taskAction(
        label: Int,
        householdId: String,
        taskId: String,
        version: Int,
        action: String,
        notificationId: Int,
    ): Notification.Action {
        val intent = Intent(applicationContext, HouseholdNotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionWorker.KEY_HOUSEHOLD_ID, householdId)
            putExtra(NotificationActionWorker.KEY_TASK_ID, taskId)
            putExtra(NotificationActionWorker.KEY_VERSION, version)
            putExtra(NotificationActionWorker.KEY_ACTION, action)
            putExtra(NotificationActionWorker.KEY_NOTIFICATION_ID, notificationId)
            if (action == ACTION_COMPLETE) {
                putExtra(
                    NotificationActionWorker.KEY_NOTE,
                    applicationContext.getString(R.string.notification_action_complete_note),
                )
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            stableNotificationId(action, taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, applicationContext.getString(label), pendingIntent).build()
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_HOUSEHOLD_ID = "householdId"
        const val KEY_VERSION = "version"
        const val KEY_CAN_START = "canStart"
        const val KEY_CAN_COMPLETE = "canComplete"
    }
}

class HouseholdNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = notificationActionPayload(intent) ?: return
        val request = OneTimeWorkRequestBuilder<NotificationActionWorker>()
            .setInputData(payload)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "sharedhouse-notification-action:${payload.getString(NotificationActionWorker.KEY_TASK_ID)}:${payload.getInt(NotificationActionWorker.KEY_VERSION, 0)}:${payload.getString(NotificationActionWorker.KEY_ACTION)}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

internal fun notificationActionPayload(intent: Intent): androidx.work.Data? {
    val householdId = intent.getStringExtra(NotificationActionWorker.KEY_HOUSEHOLD_ID) ?: return null
    val taskId = intent.getStringExtra(NotificationActionWorker.KEY_TASK_ID) ?: return null
    val action = intent.getStringExtra(NotificationActionWorker.KEY_ACTION) ?: return null
    val version = intent.getIntExtra(NotificationActionWorker.KEY_VERSION, 0)
    val notificationId = intent.getIntExtra(NotificationActionWorker.KEY_NOTIFICATION_ID, 0)
    val note = intent.getStringExtra(NotificationActionWorker.KEY_NOTE)
    if (!isValidNotificationAction(householdId, taskId, version, action)) return null
    return workDataOf(
        NotificationActionWorker.KEY_HOUSEHOLD_ID to householdId,
        NotificationActionWorker.KEY_TASK_ID to taskId,
        NotificationActionWorker.KEY_VERSION to version,
        NotificationActionWorker.KEY_ACTION to action,
        NotificationActionWorker.KEY_NOTIFICATION_ID to notificationId,
        NotificationActionWorker.KEY_NOTE to note,
    )
}

internal fun isValidNotificationAction(
    householdId: String,
    taskId: String,
    version: Int,
    action: String,
): Boolean =
    isUuid(householdId) &&
        isUuid(taskId) &&
        version >= 1 &&
        action in setOf(ACTION_START, ACTION_COMPLETE)

class NotificationActionWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val householdId = inputData.getString(KEY_HOUSEHOLD_ID) ?: return Result.failure()
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val note = inputData.getString(KEY_NOTE)
        val version = inputData.getInt(KEY_VERSION, 0)
        if (!isValidNotificationAction(householdId, taskId, version, action)) {
            return Result.failure()
        }
        if (action == ACTION_COMPLETE && note.isNullOrBlank()) return Result.failure()
        val sessionStore = AndroidKeystoreSessionStore(applicationContext)
        val restored = sessionStore.load() as? SessionLoadResult.Restored ?: return Result.failure()
        var session = restored.session
        val httpClient = createSharedHouseHttpClient(OkHttp.create())
        val api = SharedHouseApiClient(httpClient, BuildConfig.API_BASE_URL)
        try {
            var response = api.actOnHouseholdTask(
                session.accessToken,
                householdId,
                taskId,
                version,
                UUID.randomUUID().toString(),
                HouseholdTaskActionDto(action = action, note = note),
            )
            if (response is ApiResult.Failure && (response.status == 401 || response.code == "SESSION_INVALID")) {
                val refreshed = api.refresh(session.refreshToken)
                if (refreshed !is ApiResult.Success || sessionStore.save(refreshed.value) != SessionSaveResult.SAVED) {
                    return Result.failure()
                }
                session = refreshed.value
                response = api.actOnHouseholdTask(
                    session.accessToken,
                    householdId,
                    taskId,
                    version,
                    UUID.randomUUID().toString(),
                    HouseholdTaskActionDto(action = action, note = note),
                )
            }
            if (response !is ApiResult.Success) {
                val failure = response as ApiResult.Failure
                return if ((failure.status ?: 0) >= 500) Result.retry() else Result.failure()
            }
            applicationContext.getSystemService(NotificationManager::class.java)
                .cancel(inputData.getInt(KEY_NOTIFICATION_ID, 0))
            return Result.success()
        } finally {
            httpClient.close()
        }
    }

    companion object {
        const val KEY_HOUSEHOLD_ID = "householdId"
        const val KEY_TASK_ID = "taskId"
        const val KEY_VERSION = "version"
        const val KEY_ACTION = "action"
        const val KEY_NOTIFICATION_ID = "notificationId"
        const val KEY_NOTE = "note"
    }
}

private fun parseTime(value: String): LocalTime = runCatching { LocalTime.parse(value) }
    .getOrDefault(LocalTime.of(9, 0))

private fun stableNotificationId(kind: String, id: String): Int = ("$kind:$id".hashCode() and Int.MAX_VALUE)

private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess
