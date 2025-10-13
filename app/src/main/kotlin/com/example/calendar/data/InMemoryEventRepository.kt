package com.example.calendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID

class InMemoryEventRepository(
    events: List<CalendarEvent> = emptyList()
) : EventRepository {

    private val state = MutableStateFlow(events.associateBy(CalendarEvent::id))

    override fun observeEventsForDay(date: LocalDate): Flow<List<CalendarEvent>> {
        return state.map { current ->
            current.values
                .filter { it.start.toLocalDate() == date }
                .sortedBy(CalendarEvent::start)
        }
    }

    override fun observeEventsForRange(start: LocalDate, end: LocalDate): Flow<List<CalendarEvent>> {
        return state.map { current ->
            current.values
                .filter { event ->
                    val eventDate = event.start.toLocalDate()
                    !eventDate.isBefore(start) && !eventDate.isAfter(end)
                }
                .sortedBy(CalendarEvent::start)
        }
    }

    override suspend fun upsert(event: CalendarEvent) {
        state.update { current -> current + (event.id to event) }
    }

    override suspend fun delete(id: UUID) {
        state.update { current -> current - id }
    }
}

