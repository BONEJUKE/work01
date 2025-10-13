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

        if (allowSnooze) {
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
        } else {
            builder.setOnlyAlertOnce(true)
        }

        NotificationManagerCompat.from(applicationContext).notify(reminderId.hashCode(), builder.build())

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "calendar_reminders"
        private const val CHANNEL_NAME = "Calendar reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for scheduled calendar reminders"

        private const val KEY_ID = "key_id"
        private const val KEY_TITLE = "key_title"
        private const val KEY_MESSAGE = "key_message"
        private const val KEY_DEEP_LINK = "key_deep_link"
        private const val KEY_ALLOW_SNOOZE = "key_allow_snooze"

        fun createInputData(id: String, payload: ReminderPayload): Data =
            Data.Builder()
                .putString(KEY_ID, id)
                .putString(KEY_TITLE, payload.title)
                .putString(KEY_MESSAGE, payload.message)
                .putString(KEY_DEEP_LINK, payload.deepLink)
                .putBoolean(KEY_ALLOW_SNOOZE, payload.allowSnooze)
                .build()

        fun uniqueWorkName(id: String): String = "reminder-notification-$id"

        fun tag(id: String): String = "reminder-tag-$id"
    }
}
