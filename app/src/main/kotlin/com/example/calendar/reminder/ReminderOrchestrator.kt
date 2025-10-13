package com.example.calendar.reminder

import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderOrchestrator(
    private val scheduler: ReminderScheduler,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun scheduleForTask(task: Task) {
        val dueAt = task.dueAt ?: return
        scheduleReminders(
            baseId = "task-${task.id}",
            title = task.title,
            targetDateTime = dueAt,
            reminders = task.reminders,
            deepLink = "app://task/${task.id}",
            allowSnooze = true
        )
    }

    fun scheduleForEvent(event: CalendarEvent) {
        scheduleReminders(
            baseId = "event-${event.id}",
            title = event.title,
            targetDateTime = event.start,
            reminders = event.reminders,
            deepLink = "app://event/${event.id}",
            allowSnooze = false
        )
    }

    fun cancelForTask(task: Task) {
        cancelScheduledReminders(
            baseId = "task-${task.id}",
            reminders = task.reminders
        )
    }

    fun cancelForEvent(event: CalendarEvent) {
        cancelScheduledReminders(
            baseId = "event-${event.id}",
            reminders = event.reminders
        )
    }

    private fun scheduleReminders(
        baseId: String,
        title: String,
        targetDateTime: LocalDateTime,
        reminders: List<Reminder>,
        deepLink: String,
        allowSnooze: Boolean
    ) {
        reminders.forEachIndexed { index, reminder ->
            val trigger = targetDateTime.minusMinutes(reminder.minutesBefore)
            val zonedTrigger: ZonedDateTime = trigger.atZone(zoneId)
            if (zonedTrigger.isAfter(ZonedDateTime.now(zoneId))) {
                scheduler.scheduleReminder(
                    id = "$baseId-$index",
                    triggerAt = zonedTrigger.toLocalDateTime(),
                    reminder = reminder,
                    payload = ReminderPayload(
                        title = title,
                        message = "${reminder.minutesBefore}분 전에 알림",
                        deepLink = deepLink,
                        allowSnooze = allowSnooze && reminder.allowSnooze
                    )
                )
            }
        }
    }

    private fun cancelScheduledReminders(
        baseId: String,
        reminders: List<Reminder>
    ) {
        reminders.indices.forEach { index ->
            scheduler.cancelReminder("$baseId-$index")
        }
    }
}
