package com.example.calendar.data

import java.time.LocalDateTime
import java.util.UUID

data class CalendarEvent(
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
