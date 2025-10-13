package com.example.calendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID

/**
 * Simple in-memory implementation that exposes reactive flows for the agenda
 * screens. It keeps the API identical to the Room repository so we can swap
 * the storage layer later without touching the UI.
 */
class InMemoryTaskRepository(
    tasks: List<Task> = emptyList()
) : TaskRepository {

    private val state = MutableStateFlow(tasks.associateBy(Task::id))

    override fun observeTasksForDay(date: LocalDate): Flow<List<Task>> =
        observeByPeriod(AgendaPeriod.Day(date))

    override fun observeTasksForWeek(start: LocalDate): Flow<List<Task>> =
        observeByPeriod(AgendaPeriod.Week(start))

    override fun observeTasksForMonth(year: Int, month: Int): Flow<List<Task>> =
        observeByPeriod(AgendaPeriod.Month(year, month))

    private fun observeByPeriod(period: AgendaPeriod): Flow<List<Task>> {
        return state.map { tasks ->
            tasks.values
                .filter { it.period == period }
                .sortedWith(compareBy({ it.dueAt == null }, { it.dueAt }))
        }
    }

    override suspend fun upsert(task: Task) {
        state.update { current -> current + (task.id to task) }
    }

    override suspend fun toggleStatus(id: UUID) {
        state.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to existing.toggleCompletion())
        }
    }

    override suspend fun delete(id: UUID) {
        state.update { current -> current - id }
    }
}

