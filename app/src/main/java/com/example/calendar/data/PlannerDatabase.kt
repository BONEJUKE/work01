package com.example.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PlannerTaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(TaskConverters::class)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun plannerTaskDao(): PlannerTaskDao

    companion object {
        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        fun getInstance(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner-database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
