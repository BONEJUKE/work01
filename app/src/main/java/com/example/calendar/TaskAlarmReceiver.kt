package com.example.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.calendar.data.PlannerDatabase
import com.example.calendar.data.PlannerTaskEntity
import com.example.calendar.data.RepeatCadence
import com.example.calendar.domain.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""
        val repeatCadence = intent.getStringExtra(EXTRA_TASK_REPEAT)?.let {
            runCatching { RepeatCadence.valueOf(it) }.getOrDefault(RepeatCadence.NONE)
        } ?: RepeatCadence.NONE

        notifyUser(context, taskId, title, description)
        if (taskId >= 0) {
            rescheduleIfNeeded(context, taskId, repeatCadence)
        }
    }

    private fun notifyUser(context: Context, taskId: Long, title: String, description: String) {
        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(taskId.toInt(), notification)
        }
    }

    private fun rescheduleIfNeeded(context: Context, taskId: Long, repeatCadence: RepeatCadence) {
        if (repeatCadence == RepeatCadence.NONE) return

        val database = PlannerDatabase.getInstance(context)
        val scheduler = AlarmScheduler(context)
        CoroutineScope(Dispatchers.IO).launch {
            val task = database.plannerTaskDao().findById(taskId) ?: return@launch
            val nextOccurrence = computeNextOccurrence(task, repeatCadence)
            val updatedTask = task.copy(
                scheduledAtEpochMillis = nextOccurrence,
                isCompleted = false
            )
            database.plannerTaskDao().upsert(updatedTask)
            scheduler.schedule(updatedTask, nextOccurrence)
        }
    }

    private fun computeNextOccurrence(task: PlannerTaskEntity, cadence: RepeatCadence): Long {
        val currentDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(task.scheduledAtEpochMillis),
            ZoneId.systemDefault()
        )

        val nextDateTime = when (cadence) {
            RepeatCadence.DAILY -> currentDateTime.plusDays(1)
            RepeatCadence.WEEKLY -> currentDateTime.plusWeeks(1)
            RepeatCadence.MONTHLY -> currentDateTime.plusMonths(1)
            RepeatCadence.NONE -> currentDateTime
        }

        return nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_TASK_REPEAT = "extra_task_repeat"
    }
}
