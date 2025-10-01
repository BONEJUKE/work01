package com.example.calendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "planner_tasks")
@TypeConverters(TaskConverters::class)
data class PlannerTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val scheduledAtEpochMillis: Long,
    val reminderMinutesBefore: Int,
    val repeatCadence: RepeatCadence,
    val isCompleted: Boolean = false
)

enum class RepeatCadence { NONE, DAILY, WEEKLY, MONTHLY }
