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
    val recurrence: Recurrence? = null,
    val recurrenceExceptions: List<RecurrenceException> = emptyList()
) {
    init {
        require(!end.isBefore(start)) { "이벤트 종료 시간은 시작 시간 이후여야 합니다." }
    }

    internal fun applyOverride(
        override: RecurrenceException,
        fallbackStart: LocalDateTime,
        fallbackEnd: LocalDateTime
    ): CalendarEvent? {
        if (override.isCancelled) return null

        val newStart = override.overrideStart ?: fallbackStart
        val newEnd = override.overrideEnd ?: fallbackEnd

        return copy(
            start = newStart,
            end = newEnd,
            title = override.overrideTitle ?: title,
            description = override.overrideDescription ?: description,
            location = override.overrideLocation ?: location,
            recurrence = override.nextRecurrenceOverride ?: recurrence,
            recurrenceExceptions = recurrenceExceptions
        )
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

data class RecurrenceException(
    val originalStart: LocalDateTime,
    val isCancelled: Boolean = false,
    val overrideStart: LocalDateTime? = null,
    val overrideEnd: LocalDateTime? = null,
    val overrideTitle: String? = null,
    val overrideDescription: String? = null,
    val overrideLocation: String? = null,
    val nextRecurrenceOverride: Recurrence? = null
) {
    init {
        if (isCancelled) {
            require(overrideStart == null && overrideEnd == null && overrideTitle == null &&
                overrideDescription == null && overrideLocation == null && nextRecurrenceOverride == null
            ) {
                "취소된 인스턴스에는 덮어쓰기 값을 지정할 수 없습니다."
            }
        }
        if (overrideStart != null && overrideEnd != null) {
            require(!overrideEnd.isBefore(overrideStart)) {
                "덮어쓴 종료 시간은 시작 시간 이후여야 합니다."
            }
        }
    }

    fun matches(occurrenceStart: LocalDateTime): Boolean = occurrenceStart == originalStart
}
