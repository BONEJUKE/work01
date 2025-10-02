package com.example.calendar.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface TaskRepository {
    fun observeTasksForDay(date: LocalDate): Flow<List<Task>>
    fun observeTasksForWeek(start: LocalDate): Flow<List<Task>>
    fun observeTasksForMonth(year: Int, month: Int): Flow<List<Task>>

    suspend fun upsert(task: Task)
    suspend fun toggleStatus(id: UUID)
    suspend fun delete(id: UUID)
}
