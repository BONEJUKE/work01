package com.example.calendar.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface EventRepository {
    fun observeEventsForDay(date: LocalDate): Flow<List<CalendarEvent>>
    fun observeEventsForRange(start: LocalDate, end: LocalDate): Flow<List<CalendarEvent>>

    suspend fun upsert(event: CalendarEvent)
    suspend fun delete(id: java.util.UUID)
}
