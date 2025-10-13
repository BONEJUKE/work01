package com.example.calendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.UUID

@Dao
interface CalendarEventDao {
    @Query(
        "SELECT * FROM calendar_events WHERE start >= :start AND start < :end ORDER BY start"
    )
    fun observeEventsWithin(start: LocalDateTime, end: LocalDateTime): Flow<List<CalendarEvent>>

    @Query(
        "SELECT * FROM calendar_events WHERE start < :endExclusive AND end > :startInclusive ORDER BY start"
    )
    fun observeEventsIntersecting(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime
    ): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteById(id: UUID)
}
