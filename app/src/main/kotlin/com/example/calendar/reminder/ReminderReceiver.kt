package com.example.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Broadcast receiver triggered by [AlarmManager] to enqueue the reminder notification work.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val deepLink = intent.getStringExtra(EXTRA_DEEP_LINK).orEmpty()
        val allowSnooze = intent.getBooleanExtra(EXTRA_ALLOW_SNOOZE, false)

        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInputData(
                ReminderNotificationWorker.createInputData(
                    id = reminderId,
                    payload = ReminderPayload(
                        title = title,
                        message = message,
                        deepLink = deepLink,
                        allowSnooze = allowSnooze
                    )
                )
            )
            .addTag(ReminderNotificationWorker.tag(reminderId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ReminderNotificationWorker.uniqueWorkName(reminderId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    companion object {
        private const val ACTION_REMINDER = "com.example.calendar.action.SHOW_REMINDER"
        private const val URI_SCHEME = "calendar"

        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_MESSAGE = "extra_message"
        private const val EXTRA_DEEP_LINK = "extra_deep_link"
        private const val EXTRA_ALLOW_SNOOZE = "extra_allow_snooze"

        fun createPendingIntent(
            context: Context,
            requestCode: Int,
            id: String,
            payload: ReminderPayload?
        ) = PendingIntentProvider.getBroadcast(
            context = context,
            requestCode = requestCode,
            intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_REMINDER
                data = Uri.parse("$URI_SCHEME://reminder/$id")
                putExtra(EXTRA_ID, id)
                if (payload != null) {
                    putExtra(EXTRA_TITLE, payload.title)
                    putExtra(EXTRA_MESSAGE, payload.message)
                    putExtra(EXTRA_DEEP_LINK, payload.deepLink)
                    putExtra(EXTRA_ALLOW_SNOOZE, payload.allowSnooze)
                }
            }
        )
    }
}

private object PendingIntentProvider {
    fun getBroadcast(context: Context, requestCode: Int, intent: Intent) =
        android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
}
