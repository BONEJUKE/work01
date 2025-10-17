package com.example.calendar.reminder

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import com.example.calendar.data.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderOrchestratorTest {

    @Test
    fun `schedules reminders for future triggers`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))
        val dueAt = LocalDateTime.of(2099, 1, 1, 9, 0)
        val task = Task(
            title = "Prepare launch",
            status = TaskStatus.Pending,
            dueAt = dueAt,
            period = AgendaPeriod.Day(LocalDate.of(2099, 1, 1)),
            reminders = listOf(
                Reminder(minutesBefore = 10),
                Reminder(minutesBefore = 60, allowSnooze = false)
            )
        )

        orchestrator.scheduleForTask(task)

        assertEquals(2, scheduler.scheduled.size)
        val first = scheduler.scheduled[0]
        assertEquals("task-${task.id}-0", first.id)
        assertEquals(dueAt.minusMinutes(10), first.triggerAt)
        assertEquals("10분 전에 알림", first.payload.message)
        assertTrue(first.payload.allowSnooze)
        assertEquals("app://task/${task.id}", first.payload.deepLink)
        assertEquals("task-${task.id}", first.payload.baseId)
        assertEquals(task.id.toString(), first.payload.taskId)

        val second = scheduler.scheduled[1]
        assertEquals("task-${task.id}-1", second.id)
        assertEquals(dueAt.minusMinutes(60), second.triggerAt)
        assertFalse(second.payload.allowSnooze)
        assertEquals("task-${task.id}", second.payload.baseId)

        val stored = store.read("task-${task.id}")
        assertEquals(2, stored.size)
        assertEquals(first.id, stored[0].id)
    }

    @Test
    fun `skips reminders that would trigger in the past`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))
        val now = LocalDateTime.now(ZoneId.of("UTC"))
        val dueAt = now.plusMinutes(30)
        val task = Task(
            title = "Submit expense report",
            dueAt = dueAt,
            period = AgendaPeriod.Day(dueAt.toLocalDate()),
            reminders = listOf(
                Reminder(minutesBefore = 60),
                Reminder(minutesBefore = 5)
            )
        )

        orchestrator.scheduleForTask(task)

        assertEquals(1, scheduler.scheduled.size)
        val scheduled = scheduler.scheduled.single()
        assertEquals(dueAt.minusMinutes(5), scheduled.triggerAt)
        assertEquals(1, store.read("task-${task.id}").size)
    }

    @Test
    fun `schedules events without snooze support`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))
        val start = LocalDateTime.of(2099, 6, 15, 14, 0)
        val event = CalendarEvent(
            title = "Design review",
            start = start,
            end = start.plusHours(1),
            reminders = listOf(Reminder(minutesBefore = 30, allowSnooze = true))
        )

        orchestrator.scheduleForEvent(event)

        val scheduled = scheduler.scheduled.single()
        assertEquals("event-${event.id}-0", scheduled.id)
        assertEquals(start.minusMinutes(30), scheduled.triggerAt)
        assertFalse(scheduled.payload.allowSnooze)
        assertEquals("app://event/${event.id}", scheduled.payload.deepLink)
        assertEquals("event-${event.id}", scheduled.payload.baseId)
        assertEquals(null, scheduled.payload.taskId)
        assertEquals(1, store.read("event-${event.id}").size)
    }

    @Test
    fun `cancels reminders using stored identifiers`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))
        val dueAt = LocalDateTime.of(2099, 1, 1, 9, 0)
        val task = Task(
            title = "Follow up",
            dueAt = dueAt,
            period = AgendaPeriod.Day(LocalDate.of(2099, 1, 1)),
            reminders = listOf(Reminder(minutesBefore = 30))
        )
        val event = CalendarEvent(
            title = "Retro",
            start = LocalDateTime.of(2099, 1, 5, 10, 0),
            end = LocalDateTime.of(2099, 1, 5, 11, 0),
            reminders = listOf(Reminder(minutesBefore = 15))
        )

        orchestrator.scheduleForTask(task)
        orchestrator.scheduleForEvent(event)

        orchestrator.cancelForTask(task)
        orchestrator.cancelForEvent(event)

        assertTrue(store.read("task-${task.id}").isEmpty())
        assertTrue(store.read("event-${event.id}").isEmpty())
        assertEquals(
            listOf("task-${task.id}-0", "event-${event.id}-0"),
            scheduler.cancelled
        )
    }

    @Test
    fun `restores persisted reminders on initialization`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val future = LocalDateTime.of(2099, 1, 1, 8, 0)
        val stale = LocalDateTime.of(2000, 1, 1, 8, 0)
        val taskId = "task-demo"
        store.write(
            taskId,
            listOf(
                StoredReminder(
                    id = "$taskId-0",
                    triggerAt = future,
                    reminder = Reminder(minutesBefore = 30),
                    payload = ReminderPayload(
                        title = "Future",
                        message = "30분 전에 알림",
                        deepLink = "app://task/demo",
                        allowSnooze = true,
                        taskId = "demo",
                        baseId = taskId
                    )
                )
            )
        )
        store.write(
            "event-demo",
            listOf(
                StoredReminder(
                    id = "event-demo-0",
                    triggerAt = stale,
                    reminder = Reminder(minutesBefore = 10),
                    payload = ReminderPayload(
                        title = "Past",
                        message = "10분 전에 알림",
                        deepLink = "app://event/demo",
                        allowSnooze = false,
                        baseId = "event-demo"
                    )
                )
            )
        )

        ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))

        assertEquals(1, scheduler.scheduled.size)
        assertEquals("task-demo-0", scheduler.scheduled.single().id)
        assertTrue(store.read("event-demo").isEmpty())
    }

    @Test
    fun `ensure scheduled for task refreshes stale persisted reminders`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))
        val task = Task(
            title = "Sync reminders",
            dueAt = LocalDateTime.of(2099, 2, 1, 9, 0),
            period = AgendaPeriod.Day(LocalDate.of(2099, 2, 1)),
            reminders = listOf(Reminder(minutesBefore = 10), Reminder(minutesBefore = 30))
        )
        val baseId = "task-${task.id}"
        store.write(
            baseId,
            listOf(
                StoredReminder(
                    id = "$baseId-stale",
                    triggerAt = LocalDateTime.of(2099, 1, 1, 9, 0),
                    reminder = Reminder(minutesBefore = 5),
                    payload = ReminderPayload(
                        title = "Old",
                        message = "old",
                        deepLink = "app://task/${task.id}",
                        allowSnooze = true,
                        taskId = task.id.toString(),
                        baseId = baseId
                    )
                )
            )
        )

        val result = orchestrator.ensureScheduledForTask(task)

        assertTrue(result)
        assertEquals(listOf("$baseId-stale"), scheduler.cancelled)
        assertEquals(2, scheduler.scheduled.size)
        assertEquals(listOf("$baseId-0", "$baseId-1"), scheduler.scheduled.map { it.id })
        assertEquals(2, store.read(baseId).size)
    }

    @Test
    fun `ensure scheduled for event removes reminders when configuration is empty`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))
        val event = CalendarEvent(
            title = "Cloud sync",
            start = LocalDateTime.of(2099, 3, 10, 15, 0),
            end = LocalDateTime.of(2099, 3, 10, 16, 0),
            reminders = listOf(Reminder(minutesBefore = 15))
        )

        orchestrator.scheduleForEvent(event)
        assertEquals(1, store.read("event-${event.id}").size)

        val result = orchestrator.ensureScheduledForEvent(event.copy(reminders = emptyList()))

        assertFalse(result)
        assertTrue(store.read("event-${event.id}").isEmpty())
        assertEquals(listOf("event-${event.id}-0"), scheduler.cancelled)
    }

    @Test
    fun `cancel by base id requests cancellation when no persisted reminders exist`() {
        val scheduler = FakeReminderScheduler()
        val store = FakeReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store, ZoneId.of("UTC"))

        orchestrator.cancelByBaseId("task-missing")

        assertEquals(listOf("task-missing"), scheduler.cancelled)
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val scheduled = mutableListOf<ScheduledReminder>()
        val cancelled = mutableListOf<String>()

        override fun scheduleReminder(
            id: String,
            triggerAt: LocalDateTime,
            reminder: Reminder,
            payload: ReminderPayload
        ) {
            scheduled += ScheduledReminder(id, triggerAt, reminder, payload)
        }

        override fun cancelReminder(id: String) {
            cancelled += id
        }
    }

    private class FakeReminderStore : ReminderStore {
        private val backing = linkedMapOf<String, MutableList<StoredReminder>>()

        override fun write(baseId: String, reminders: List<StoredReminder>) {
            backing[baseId] = reminders.toMutableList()
        }

        override fun read(baseId: String): List<StoredReminder> =
            backing[baseId]?.toList() ?: emptyList()

        override fun readAll(): Map<String, List<StoredReminder>> =
            backing.mapValues { it.value.toList() }

        override fun remove(baseId: String) {
            backing.remove(baseId)
        }
    }

    private data class ScheduledReminder(
        val id: String,
        val triggerAt: LocalDateTime,
        val reminder: Reminder,
        val payload: ReminderPayload
    )
}
