package com.example.calendar.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.core.app.AlarmManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.calendar.data.Reminder
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Concrete implementation of [ReminderScheduler] that integrates with Android's
 * [AlarmManager] and Jetpack [WorkManager].
 */
class AndroidReminderScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val workManager: WorkManager,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = clock.zone
) : ReminderScheduler {

    override fun scheduleReminder(
        id: String,
        triggerAt: LocalDateTime,
        reminder: Reminder,
        payload: ReminderPayload
    ) {
        val triggerAtMillis = triggerAt.toZonedDateTime().toInstant().toEpochMilli()
        val requestCode = requestCodeFor(id)

        val pendingIntent = ReminderReceiver.createPendingIntent(
            context = context,
            requestCode = requestCode,
            id = id,
            payload = payload
        )

        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        enqueueOneTimeWorker(id, payload, triggerAt)
    }

    override fun cancelReminder(id: String) {
        val requestCode = requestCodeFor(id)
        val pendingIntent = ReminderReceiver.createPendingIntent(
            context = context,
            requestCode = requestCode,
            id = id,
            payload = null
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        workManager.cancelUniqueWork(ReminderNotificationWorker.uniqueWorkName(id))
    }

    private fun enqueueOneTimeWorker(
        id: String,
        payload: ReminderPayload,
        triggerAt: LocalDateTime
    ) {
        val delay = Duration.between(ZonedDateTime.now(clock), triggerAt.toZonedDateTime())
            .coerceAtLeast(Duration.ZERO)

        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delay)
            .setInputData(ReminderNotificationWorker.createInputData(id, payload))
            .addTag(ReminderNotificationWorker.tag(id))
            .build()

        workManager.enqueueUniqueWork(
            ReminderNotificationWorker.uniqueWorkName(id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun Duration.coerceAtLeast(min: Duration): Duration =
        if (this < min) min else this

    private fun LocalDateTime.toZonedDateTime(): ZonedDateTime =
        atZone(zoneId)

    private fun requestCodeFor(id: String): Int = id.hashCode()
}
