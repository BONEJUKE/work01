package com.example.calendar.scheduler

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Produces a combined agenda of events and tasks for a selected period.
 */
class AgendaAggregator(
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository
) {
    fun observeAgenda(period: AgendaPeriod): Flow<AgendaSnapshot> = when (period) {
        is AgendaPeriod.Day -> observeDay(period.date)
        is AgendaPeriod.Week -> observeWeek(period.start)
        is AgendaPeriod.Month -> observeMonth(period.year, period.month)
    }

    private fun observeDay(date: LocalDate): Flow<AgendaSnapshot> = combine(
        taskRepository.observeTasksForDay(date),
        eventRepository.observeEventsForDay(date)
    ) { tasks, events ->
        AgendaSnapshot(date, tasks, events)
    }

    private fun observeWeek(start: LocalDate): Flow<AgendaSnapshot> {
        val end = start.plusDays(6)
        return combine(
            taskRepository.observeTasksForWeek(start),
            eventRepository.observeEventsForRange(start, end)
        ) { tasks, events ->
            AgendaSnapshot(start, tasks, events)
        }.map { snapshot -> snapshot.copy(rangeEnd = end) }
    }

    private fun observeMonth(year: Int, month: Int): Flow<AgendaSnapshot> {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        return combine(
            taskRepository.observeTasksForMonth(year, month),
            eventRepository.observeEventsForRange(firstDay, lastDay)
        ) { tasks, events ->
            AgendaSnapshot(firstDay, tasks, events)
        }.map { snapshot -> snapshot.copy(rangeEnd = lastDay) }
    }
}

data class AgendaSnapshot(
    val rangeStart: LocalDate,
    val tasks: List<Task>,
    val events: List<CalendarEvent>,
    val rangeEnd: LocalDate = rangeStart
) {
    val overdueTasks: List<Task> = tasks.filter { it.isOverdue() }
    val completedCount: Int = tasks.count { it.status.isDone() }
    val pendingCount: Int = tasks.size - completedCount
}
