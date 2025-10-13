package com.example.calendar.reminder

import com.example.calendar.data.Reminder
import java.time.LocalDateTime

/**
 * Placeholder scheduler that keeps the reminder API intact while we focus on
 * shipping a runnable demo. It simply ignores schedule/cancel requests.
 */
class NoOpReminderScheduler : ReminderScheduler {
    override fun scheduleReminder(
        id: String,
        triggerAt: LocalDateTime,
        reminder: Reminder,
        payload: ReminderPayload
    ) {
        // No-op on purpose.
    }

    override fun cancelReminder(id: String) {
        // No-op on purpose.
    }
}

