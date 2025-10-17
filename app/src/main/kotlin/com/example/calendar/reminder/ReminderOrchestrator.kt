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
        val baseId = "task-${task.id}"
        val dueAt = task.dueAt ?: run {
            cancelByBaseId(baseId)
            return
        }
        val schedule = buildSchedule(
            baseId = baseId,
            title = task.title,
            targetDateTime = dueAt,
            reminders = task.reminders,
            deepLink = "app://task/${task.id}",
            allowSnooze = true,
            taskId = task.id.toString()
        )
        replaceSchedule(baseId, schedule)
    }

    fun scheduleForEvent(event: CalendarEvent) {
        val baseId = "event-${event.id}"
        val schedule = buildSchedule(
            baseId = baseId,
            title = event.title,
            targetDateTime = event.start,
            reminders = event.reminders,
            deepLink = "app://event/${event.id}",
            allowSnooze = false,
            taskId = null
        )
        replaceSchedule(baseId, schedule)
    }

    fun cancelForTask(task: Task) = cancelByBaseId("task-${task.id}")
    fun cancelForEvent(event: CalendarEvent) = cancelByBaseId("event-${event.id}")

    internal fun cancelByBaseId(baseId: String) {
        val stored = store.read(baseId)
        if (stored.isEmpty()) {
            scheduler.cancelReminder(baseId)
        } else {
            stored.forEach { scheduler.cancelReminder(it.id) }
        }
        store.remove(baseId)
    }

    internal fun ensureScheduledForTask(task: Task): Boolean {
        val baseId = "task-${task.id}"
        val dueAt = task.dueAt ?: run {
            cancelByBaseId(baseId)
            return false
        }
        if (task.reminders.isEmpty()) {
            cancelByBaseId(baseId)
            return false
        }

        val schedule = buildSchedule(
            baseId = baseId,
            title = task.title,
            targetDateTime = dueAt,
            reminders = task.reminders,
            deepLink = "app://task/${task.id}",
            allowSnooze = true,
            taskId = task.id.toString()
        )

        if (schedule.isEmpty()) {
            cancelByBaseId(baseId)
            return false
        }

        if (!store.read(baseId).contentEquals(schedule)) {
            replaceSchedule(baseId, schedule)
        }

        return true
    }

    internal fun ensureScheduledForEvent(event: CalendarEvent): Boolean {
        val baseId = "event-${event.id}"
        if (event.reminders.isEmpty()) {
            cancelByBaseId(baseId)
            return false
        }

        val schedule = buildSchedule(
            baseId = baseId,
            title = event.title,
            targetDateTime = event.start,
            reminders = event.reminders,
            deepLink = "app://event/${event.id}",
            allowSnooze = false,
            taskId = null
        )

        if (schedule.isEmpty()) {
            cancelByBaseId(baseId)
            return false
        }

        if (!store.read(baseId).contentEquals(schedule)) {
            replaceSchedule(baseId, schedule)
        }

        return true
    }

    private fun replaceSchedule(baseId: String, schedule: List<StoredReminder>) {
        cancelByBaseId(baseId)
        if (schedule.isEmpty()) {
            return
        }
        schedule.forEach { record ->
            scheduler.scheduleReminder(
                id = record.id,
                triggerAt = record.triggerAt,
                reminder = record.reminder,
                payload = record.payload
            )
        }
        store.write(baseId, schedule)
    }

    private fun buildSchedule(
        baseId: String,
        title: String,
        targetDateTime: LocalDateTime,
        reminders: List<Reminder>,
        deepLink: String,
        allowSnooze: Boolean,
        taskId: String?
    ): List<StoredReminder> {
        val now = ZonedDateTime.now(zoneId)
        return buildList {
            reminders.forEachIndexed { index, reminder ->
                val trigger = targetDateTime.minusMinutes(reminder.minutesBefore)
                val zonedTrigger = trigger.atZone(zoneId)
                if (zonedTrigger.isAfter(now)) {
                    val id = "$baseId-$index"
                    val payload = ReminderPayload(
                        title = title,
                        message = "${reminder.minutesBefore}분 전에 알림",
                        deepLink = deepLink,
                        allowSnooze = allowSnooze && reminder.allowSnooze,
                        taskId = taskId,
                        baseId = baseId
                    )
                    add(
                        StoredReminder(
                            id = id,
                            triggerAt = zonedTrigger.toLocalDateTime(),
                            reminder = reminder,
                            payload = payload
                        )
                    )
                }
            }
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

private fun List<StoredReminder>.contentEquals(other: List<StoredReminder>): Boolean {
    if (size != other.size) return false
    return sortedBy { it.id }.zip(other.sortedBy { it.id }).all { (first, second) ->
        first.id == second.id &&
            first.triggerAt == second.triggerAt &&
            first.reminder == second.reminder &&
            first.payload == second.payload
    }
}
