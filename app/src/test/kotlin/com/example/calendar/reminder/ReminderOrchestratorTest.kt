package com.example.calendar.reminder

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import com.example.calendar.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderOrchestratorTest {

    @Test
    fun `schedules reminders for future triggers`() {
        val scheduler = FakeReminderScheduler()
        val orchestrator = ReminderOrchestrator(scheduler, ZoneId.of("UTC"))
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

        val second = scheduler.scheduled[1]
        assertEquals("task-${task.id}-1", second.id)
        assertEquals(dueAt.minusMinutes(60), second.triggerAt)
        assertFalse(second.payload.allowSnooze)
    }

    @Test
    fun `skips reminders that would trigger in the past`() {
        val scheduler = FakeReminderScheduler()
        val orchestrator = ReminderOrchestrator(scheduler, ZoneId.of("UTC"))
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
    }

    @Test
    fun `schedules events without snooze support`() {
        val scheduler = FakeReminderScheduler()
        val orchestrator = ReminderOrchestrator(scheduler, ZoneId.of("UTC"))
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
    }

    @Test
    fun `cancels reminders using composed identifiers`() {
        val scheduler = FakeReminderScheduler()
        val orchestrator = ReminderOrchestrator(scheduler, ZoneId.of("UTC"))
        val task = Task(
            title = "Follow up",
            period = AgendaPeriod.Day(LocalDate.now())
        )
        val event = CalendarEvent(
            title = "Retro",
            start = LocalDateTime.of(2025, 1, 5, 10, 0),
            end = LocalDateTime.of(2025, 1, 5, 11, 0)
        )

        orchestrator.cancelForTask(task)
        orchestrator.cancelForEvent(event)

        assertEquals(listOf("task-${task.id}", "event-${event.id}"), scheduler.cancelled)
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val scheduled = mutableListOf<ScheduledReminder>()
        val cancelled = mutableListOf<String>()

        override fun scheduleReminder(id: String, triggerAt: LocalDateTime, reminder: Reminder, payload: ReminderPayload) {
            scheduled += ScheduledReminder(id, triggerAt, reminder, payload)
        }

        override fun cancelReminder(id: String) {
            cancelled += id
        }
    }

    private data class ScheduledReminder(
        val id: String,
        val triggerAt: LocalDateTime,
        val reminder: Reminder,
        val payload: ReminderPayload
    )
}
