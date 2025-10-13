package com.example.calendar.data

import java.time.LocalDate
import java.util.UUID

class RoomEventRepository(private val eventDao: CalendarEventDao) : EventRepository {
    override fun observeEventsForDay(date: LocalDate) =
        eventDao.observeEventsWithin(date.atStartOfDay(), date.plusDays(1).atStartOfDay())

    override fun observeEventsForRange(start: LocalDate, end: LocalDate) =
        eventDao.observeEventsIntersecting(
            startInclusive = start.atStartOfDay(),
            endExclusive = end.plusDays(1).atStartOfDay()
        )

    override suspend fun upsert(event: CalendarEvent) {
        validateEvent(event)
        eventDao.upsert(event)
    }

    override suspend fun delete(id: UUID) {
        eventDao.deleteById(id)
    }

    private fun validateEvent(event: CalendarEvent) {
        require(!event.end.isBefore(event.start)) { "Event end time must be after start time" }
    }
}
