package com.example.calendar.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.calendar.R

/**
 * [CoroutineWorker] responsible for showing reminder notifications when alarms fire.
 */
class ReminderNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(KEY_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: ""
        val deepLink = inputData.getString(KEY_DEEP_LINK)
        val allowSnooze = inputData.getBoolean(KEY_ALLOW_SNOOZE, false)
        val taskId = inputData.getString(KEY_TASK_ID)
        val baseId = inputData.getString(KEY_BASE_ID) ?: reminderId
        val snoozeMinutes = inputData.getLong(KEY_SNOOZE_MINUTES, ReminderPayload.DEFAULT_SNOOZE_MINUTES)

        createNotificationChannel()

        val contentIntent = deepLink?.let { link ->
            PendingIntent.getActivity(
                applicationContext,
                reminderId.hashCode(),
                Intent(Intent.ACTION_VIEW, Uri.parse(link)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        if (taskId != null) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    applicationContext.getString(R.string.reminder_action_complete),
                    actionIntent(
                        reminderId = reminderId,
                        action = ReminderActionReceiver.ACTION_COMPLETE,
                        taskId = taskId,
                        baseId = baseId,
                        allowSnooze = allowSnooze,
                        title = title,
                        message = message,
                        deepLink = deepLink,
                        snoozeMinutes = snoozeMinutes
                    )
                ).build()
            )
        }

        if (allowSnooze) {
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
            builder.addAction(
                NotificationCompat.Action.Builder(
                    0,
                    applicationContext.getString(R.string.reminder_action_snooze),
                    actionIntent(
                        reminderId = reminderId,
                        action = ReminderActionReceiver.ACTION_SNOOZE,
                        taskId = taskId,
                        baseId = baseId,
                        allowSnooze = true,
                        title = title,
                        message = message,
                        deepLink = deepLink,
                        snoozeMinutes = snoozeMinutes
                    )
                ).build()
            )
        } else {
            builder.setOnlyAlertOnce(true)
        }

        NotificationManagerCompat.from(applicationContext).notify(reminderId.hashCode(), builder.build())

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelName = applicationContext.getString(R.string.reminder_notification_channel_name)
        val channelDescription = applicationContext.getString(R.string.reminder_notification_channel_description)

        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun actionIntent(
        reminderId: String,
        action: String,
        taskId: String?,
        baseId: String?,
        allowSnooze: Boolean,
        title: String,
        message: String,
        deepLink: String?,
        snoozeMinutes: Long
    ): PendingIntent {
        val intent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderActionReceiver.EXTRA_ALLOW_SNOOZE, allowSnooze)
            putExtra(ReminderActionReceiver.EXTRA_TITLE, title)
            putExtra(ReminderActionReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderActionReceiver.EXTRA_DEEP_LINK, deepLink)
            putExtra(ReminderActionReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(ReminderActionReceiver.EXTRA_BASE_ID, baseId)
        }
        val requestCode = (reminderId + action).hashCode()
        return PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "calendar_reminders"

        private const val KEY_ID = "key_id"
        private const val KEY_TITLE = "key_title"
        private const val KEY_MESSAGE = "key_message"
        private const val KEY_DEEP_LINK = "key_deep_link"
        private const val KEY_ALLOW_SNOOZE = "key_allow_snooze"
        private const val KEY_TASK_ID = "key_task_id"
        private const val KEY_BASE_ID = "key_base_id"
        private const val KEY_SNOOZE_MINUTES = "key_snooze_minutes"

        fun createInputData(id: String, payload: ReminderPayload): Data =
            Data.Builder()
                .putString(KEY_ID, id)
                .putString(KEY_TITLE, payload.title)
                .putString(KEY_MESSAGE, payload.message)
                .putString(KEY_DEEP_LINK, payload.deepLink)
                .putBoolean(KEY_ALLOW_SNOOZE, payload.allowSnooze)
                .putString(KEY_TASK_ID, payload.taskId)
                .putString(KEY_BASE_ID, payload.baseId)
                .putLong(KEY_SNOOZE_MINUTES, payload.snoozeMinutes)
                .build()

        fun uniqueWorkName(id: String): String = "reminder-notification-$id"

        fun tag(id: String): String = "reminder-tag-$id"
    }
}
