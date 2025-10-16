package com.example.calendar.reminder

import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderOrchestrator(
    private val scheduler: ReminderScheduler,
    private val store: ReminderStore,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    init {
        restorePersistedReminders()
    }

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

    fun cancelForTask(task: Task) = cancelByBaseId("task-${task.id}")
    fun cancelForEvent(event: CalendarEvent) = cancelByBaseId("event-${event.id}")

    private fun cancelByBaseId(baseId: String) {
        val stored = store.read(baseId)
        if (stored.isEmpty()) {
            scheduler.cancelReminder(baseId)
        } else {
            stored.forEach { scheduler.cancelReminder(it.id) }
        }
        store.remove(baseId)
    }

    private fun scheduleReminders(
        baseId: String,
        title: String,
        targetDateTime: LocalDateTime,
        reminders: List<Reminder>,
        deepLink: String,
        allowSnooze: Boolean
    ) {
        cancelByBaseId(baseId)

        val scheduled = buildList {
            reminders.forEachIndexed { index, reminder ->
                val trigger = targetDateTime.minusMinutes(reminder.minutesBefore)
                val zonedTrigger: ZonedDateTime = trigger.atZone(zoneId)
                if (zonedTrigger.isAfter(ZonedDateTime.now(zoneId))) {
                    val id = "$baseId-$index"
                    val payload = ReminderPayload(
                        title = title,
                        message = "${reminder.minutesBefore}분 전에 알림",
                        deepLink = deepLink,
                        allowSnooze = allowSnooze && reminder.allowSnooze
                    )
                    val triggerAt = zonedTrigger.toLocalDateTime()
                    scheduler.scheduleReminder(
                        id = id,
                        triggerAt = triggerAt,
                        reminder = reminder,
                        payload = payload
                    )
                    add(
                        StoredReminder(
                            id = id,
                            triggerAt = triggerAt,
                            reminder = reminder,
                            payload = payload
                        )
                    )
                }
            }
        }

        if (scheduled.isEmpty()) {
            store.remove(baseId)
        } else {
            store.write(baseId, scheduled)
        }
    }

    private fun restorePersistedReminders() {
        val now = LocalDateTime.now(zoneId)
        store.readAll().forEach { (baseId, reminders) ->
            val active = reminders.filter { it.triggerAt.isAfter(now) }
            if (active.isEmpty()) {
                store.remove(baseId)
            } else {
                active.forEach { record ->
                    scheduler.scheduleReminder(
                        id = record.id,
                        triggerAt = record.triggerAt,
                        reminder = record.reminder,
                        payload = record.payload
                    )
                }
                store.write(baseId, active)
            }
        }
    }
}
