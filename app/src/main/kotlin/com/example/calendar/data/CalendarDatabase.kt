package com.example.calendar.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Task::class, CalendarEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CalendarTypeConverters::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        const val DATABASE_NAME: String = "calendar.db"
    }
}
