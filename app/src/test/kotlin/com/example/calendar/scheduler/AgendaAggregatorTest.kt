package com.example.calendar.scheduler

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import com.example.calendar.data.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AgendaAggregatorTest {

    @Test
    fun `aggregates tasks and events for a day with derived counts`() = runBlocking {
        val date = LocalDate.of(2024, 5, 10)
        val tasks = listOf(
            Task(
                title = "Submit report",
                status = TaskStatus.Completed,
                dueAt = LocalDateTime.of(2024, 5, 10, 9, 0),
                period = AgendaPeriod.Day(date)
            ),
            Task(
                title = "Pay rent",
                dueAt = LocalDateTime.of(2024, 5, 1, 0, 0),
                period = AgendaPeriod.Day(date)
            )
        )
        val events = listOf(
            CalendarEvent(
                title = "Team sync",
                start = LocalDateTime.of(2024, 5, 10, 10, 0),
                end = LocalDateTime.of(2024, 5, 10, 11, 0)
            )
        )
        val aggregator = AgendaAggregator(
            taskRepository = FakeTaskRepository(dayTasks = mapOf(date to tasks)),
            eventRepository = FakeEventRepository(dayEvents = mapOf(date to events))
        )

        val snapshot = aggregator.observeAgenda(AgendaPeriod.Day(date)).first()

        assertEquals(date, snapshot.rangeStart)
        assertEquals(date, snapshot.rangeEnd)
        assertEquals(tasks, snapshot.tasks)
        assertEquals(events, snapshot.events)
        assertEquals(1, snapshot.completedCount)
        assertEquals(1, snapshot.pendingCount)
        assertEquals(listOf(tasks[1]), snapshot.overdueTasks)
    }

    @Test
    fun `aggregates tasks and events for a week`() = runBlocking {
        val start = LocalDate.of(2024, 3, 4)
        val tasks = listOf(
            Task(
                title = "Plan sprint",
                period = AgendaPeriod.Week(start)
            )
        )
        val events = listOf(
            CalendarEvent(
                title = "Demo",
                start = LocalDateTime.of(2024, 3, 7, 14, 0),
                end = LocalDateTime.of(2024, 3, 7, 15, 0)
            )
        )
        val aggregator = AgendaAggregator(
            taskRepository = FakeTaskRepository(weekTasks = mapOf(start to tasks)),
            eventRepository = FakeEventRepository(rangeEvents = mapOf((start to start.plusDays(6)) to events))
        )

        val snapshot = aggregator.observeAgenda(AgendaPeriod.Week(start)).first()

        assertEquals(start, snapshot.rangeStart)
        assertEquals(start.plusDays(6), snapshot.rangeEnd)
        assertEquals(tasks, snapshot.tasks)
        assertEquals(events, snapshot.events)
    }

    @Test
    fun `aggregates tasks and events for a month`() = runBlocking {
        val year = 2024
        val month = 6
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        val tasks = listOf(
            Task(
                title = "Close books",
                period = AgendaPeriod.Month(year, month)
            )
        )
        val events = listOf(
            CalendarEvent(
                title = "Quarterly planning",
                start = LocalDateTime.of(2024, 6, 12, 9, 0),
                end = LocalDateTime.of(2024, 6, 12, 10, 0)
            )
        )
        val aggregator = AgendaAggregator(
            taskRepository = FakeTaskRepository(monthTasks = mapOf((year to month) to tasks)),
            eventRepository = FakeEventRepository(rangeEvents = mapOf((firstDay to lastDay) to events))
        )

        val snapshot = aggregator.observeAgenda(AgendaPeriod.Month(year, month)).first()

        assertEquals(firstDay, snapshot.rangeStart)
        assertEquals(lastDay, snapshot.rangeEnd)
        assertEquals(tasks, snapshot.tasks)
        assertEquals(events, snapshot.events)
        assertTrue(snapshot.overdueTasks.isEmpty())
    }

    private class FakeTaskRepository(
        private val dayTasks: Map<LocalDate, List<Task>> = emptyMap(),
        private val weekTasks: Map<LocalDate, List<Task>> = emptyMap(),
        private val monthTasks: Map<Pair<Int, Int>, List<Task>> = emptyMap()
    ) : TaskRepository {
        override fun observeTasksForDay(date: LocalDate): Flow<List<Task>> =
            MutableStateFlow(dayTasks[date].orEmpty())

        override fun observeTasksForWeek(start: LocalDate): Flow<List<Task>> =
            MutableStateFlow(weekTasks[start].orEmpty())

        override fun observeTasksForMonth(year: Int, month: Int): Flow<List<Task>> =
            MutableStateFlow(monthTasks[year to month].orEmpty())

        override suspend fun upsert(task: Task) = throw UnsupportedOperationException()
        override suspend fun toggleStatus(id: java.util.UUID) = throw UnsupportedOperationException()
        override suspend fun delete(id: java.util.UUID) = throw UnsupportedOperationException()
    }

    private class FakeEventRepository(
        private val dayEvents: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
        private val rangeEvents: Map<Pair<LocalDate, LocalDate>, List<CalendarEvent>> = emptyMap()
    ) : EventRepository {
        override fun observeEventsForDay(date: LocalDate): Flow<List<CalendarEvent>> =
            MutableStateFlow(dayEvents[date].orEmpty())

        override fun observeEventsForRange(start: LocalDate, end: LocalDate): Flow<List<CalendarEvent>> =
            MutableStateFlow(rangeEvents[start to end].orEmpty())

        override suspend fun upsert(event: CalendarEvent) = throw UnsupportedOperationException()
        override suspend fun delete(id: java.util.UUID) = throw UnsupportedOperationException()
    }
}
