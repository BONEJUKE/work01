package com.example.calendar.reminder

import com.example.calendar.data.Reminder
import java.time.LocalDateTime

/**
 * Abstraction over Android's alarm APIs to simplify testing and reuse.
 */
interface ReminderScheduler {
    fun scheduleReminder(
        id: String,
        triggerAt: LocalDateTime,
        reminder: Reminder,
        payload: ReminderPayload
    )

    fun cancelReminder(id: String)
}

data class ReminderPayload(
    val title: String,
    val message: String,
    val deepLink: String,
    val allowSnooze: Boolean
)
