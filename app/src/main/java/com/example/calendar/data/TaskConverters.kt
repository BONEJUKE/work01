package com.example.calendar.data

import androidx.room.TypeConverter

class TaskConverters {
    @TypeConverter
    fun fromRepeatCadence(value: RepeatCadence): String = value.name

    @TypeConverter
    fun toRepeatCadence(value: String): RepeatCadence = RepeatCadence.valueOf(value)
}
