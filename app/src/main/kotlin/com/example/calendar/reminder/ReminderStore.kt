package com.example.calendar.reminder

import com.example.calendar.data.Reminder
import java.time.LocalDateTime

/**
 * Persists scheduled reminders so they can be restored after process restarts or device reboots.
 */
interface ReminderStore {
    /**
     * Persists the reminders associated with [baseId]. When [reminders] is empty the entry
     * will be removed from the underlying storage.
     */
    fun write(baseId: String, reminders: List<StoredReminder>)

    /**
     * Returns the reminders associated with [baseId].
     */
    fun read(baseId: String): List<StoredReminder>

    /**
     * Returns all persisted reminders grouped by their base identifier.
     */
    fun readAll(): Map<String, List<StoredReminder>>

    /**
     * Removes the reminders associated with [baseId] from storage.
     */
    fun remove(baseId: String)
}

/**
 * Snapshot of a scheduled reminder persisted by [ReminderStore].
 */
data class StoredReminder(
    val id: String,
    val triggerAt: LocalDateTime,
    val reminder: Reminder,
    val payload: ReminderPayload
)
