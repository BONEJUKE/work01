package com.example.calendar.reminder

import android.content.Context
import android.test.mock.MockContext
import com.example.calendar.data.Reminder
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderReschedulerTest {

    private val testContext = object : MockContext() {
        override fun getApplicationContext(): Context = this
    }

    @Test
    fun restoreSchedulesPersistedReminders() {
        val storedReminder = StoredReminder(
            id = "task-1-0",
            triggerAt = LocalDateTime.now().plusMinutes(30),
            reminder = Reminder(minutesBefore = 15, allowSnooze = true),
            payload = ReminderPayload(
                title = "회의",
                message = "15분 전에 알림",
                deepLink = "app://task/1",
                allowSnooze = true
            )
        )
        val fakeStore = FakeReminderStore(mutableMapOf("task-1" to listOf(storedReminder)))
        val scheduled = mutableListOf<Triple<String, LocalDateTime, ReminderPayload>>()
        val fakeScheduler = object : ReminderScheduler {
            override fun scheduleReminder(
                id: String,
                triggerAt: LocalDateTime,
                reminder: Reminder,
                payload: ReminderPayload
            ) {
                scheduled += Triple(id, triggerAt, payload)
            }

            override fun cancelReminder(id: String) {}
        }

        var schedulerContext: Context? = null
        var storeContext: Context? = null

        val rescheduler = ReminderRescheduler(
            context = testContext,
            schedulerFactory = { context ->
                schedulerContext = context
                fakeScheduler
            },
            storeFactory = { context ->
                storeContext = context
                fakeStore
            }
        )

        rescheduler.restore()

        assertEquals(testContext, schedulerContext)
        assertEquals(testContext, storeContext)
        assertEquals(1, scheduled.size)
        val (id, triggerAt, payload) = scheduled.single()
        assertEquals(storedReminder.id, id)
        assertEquals(storedReminder.triggerAt, triggerAt)
        assertEquals(storedReminder.payload, payload)
        assertTrue(fakeStore.lastWrite.isNotEmpty())
    }
}

private class FakeReminderStore(
    private val reminders: MutableMap<String, List<StoredReminder>>
) : ReminderStore {

    var lastWrite: Map<String, List<StoredReminder>> = emptyMap()
        private set

    override fun write(baseId: String, reminders: List<StoredReminder>) {
        lastWrite = mapOf(baseId to reminders)
        if (reminders.isEmpty()) {
            this.reminders.remove(baseId)
        } else {
            this.reminders[baseId] = reminders
        }
    }

    override fun read(baseId: String): List<StoredReminder> = reminders[baseId].orEmpty()

    override fun readAll(): Map<String, List<StoredReminder>> = reminders.toMap()

    override fun remove(baseId: String) {
        reminders.remove(baseId)
    }
}
