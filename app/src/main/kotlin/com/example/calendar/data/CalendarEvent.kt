package com.example.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String? = null,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val location: String? = null,
    val reminders: List<Reminder> = emptyList(),
    val recurrence: Recurrence? = null
) {
    init {
        require(!end.isBefore(start)) { "Event end time must be after start time" }
    }
}

data class Recurrence(
    val rule: RecurrenceRule,
    val interval: Int = 1,
    val occurrences: Int? = null
)

enum class RecurrenceRule {
    Daily,
    Weekly,
    Monthly,
    Yearly
}
