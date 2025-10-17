package com.example.calendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.calendar.CalendarApplication
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        val application = context.applicationContext as? CalendarApplication
        if (application == null) {
            pendingResult.finish()
            return
        }
        val container = application.container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_COMPLETE -> {
                        intent.getStringExtra(EXTRA_TASK_ID)
                            ?.let(UUID::fromString)
                            ?.let { taskId ->
                                container.taskRepository.markComplete(taskId)
                                val baseId = intent.getStringExtra(EXTRA_BASE_ID)
                                    ?: "task-$taskId"
                                container.reminderOrchestrator.cancelByBaseId(baseId)
                            }
                        NotificationManagerCompat.from(context)
                            .cancel(reminderId.hashCode())
                    }
                    ACTION_SNOOZE -> {
                        if (intent.getBooleanExtra(EXTRA_ALLOW_SNOOZE, false)) {
                            val snoozeMinutes = intent.getLongExtra(
                                EXTRA_SNOOZE_MINUTES,
                                ReminderPayload.DEFAULT_SNOOZE_MINUTES
                            )
                            val payload = ReminderPayload(
                                title = intent.getStringExtra(EXTRA_TITLE) ?: "",
                                message = intent.getStringExtra(EXTRA_MESSAGE) ?: "",
                                deepLink = intent.getStringExtra(EXTRA_DEEP_LINK) ?: "",
                                allowSnooze = true,
                                taskId = intent.getStringExtra(EXTRA_TASK_ID),
                                baseId = intent.getStringExtra(EXTRA_BASE_ID)
                                    ?: intent.getStringExtra(EXTRA_TASK_ID)?.let { "task-$it" }
                                    ?: reminderId,
                                snoozeMinutes = snoozeMinutes
                            )
                            val snoozedId = reminderId + "-snoozed"
                            val work = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
                                .setInputData(
                                    ReminderNotificationWorker.createInputData(snoozedId, payload)
                                )
                                .setInitialDelay(snoozeMinutes, TimeUnit.MINUTES)
                                .addTag(ReminderNotificationWorker.tag(snoozedId))
                                .build()
                            WorkManager.getInstance(context).enqueueUniqueWork(
                                ReminderNotificationWorker.uniqueWorkName(snoozedId),
                                ExistingWorkPolicy.REPLACE,
                                work
                            )
                        }
                        NotificationManagerCompat.from(context)
                            .cancel(reminderId.hashCode())
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.example.calendar.reminder.action.COMPLETE"
        const val ACTION_SNOOZE = "com.example.calendar.reminder.action.SNOOZE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ALLOW_SNOOZE = "extra_allow_snooze"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_DEEP_LINK = "extra_deep_link"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_BASE_ID = "extra_base_id"
    }
}
