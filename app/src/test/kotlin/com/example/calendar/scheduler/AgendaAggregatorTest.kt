package com.example.calendar.scheduler

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Recurrence
import com.example.calendar.data.RecurrenceRule
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
import java.time.LocalTime

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
        val start = LocalDate.of(2000, 1, 3)
        val overdue = Task(
            title = "File paperwork",
            dueAt = LocalDateTime.of(2000, 1, 4, 9, 0),
            period = AgendaPeriod.Week(start)
        )
        val completed = Task(
            title = "Plan sprint",
            status = TaskStatus.Completed,
            dueAt = LocalDateTime.of(2000, 1, 6, 10, 0),
            period = AgendaPeriod.Week(start)
        )
        val upcoming = Task(
            title = "Prep demo",
            dueAt = LocalDateTime.of(2100, 1, 5, 11, 0),
            period = AgendaPeriod.Week(start)
        )
        val tasks = listOf(overdue, completed, upcoming)
        val events = listOf(
            CalendarEvent(
                title = "Demo",
                start = LocalDateTime.of(2000, 1, 7, 14, 0),
                end = LocalDateTime.of(2000, 1, 7, 15, 0)
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
        assertEquals(1, snapshot.completedCount)
        assertEquals(2, snapshot.pendingCount)
        assertEquals(listOf(overdue), snapshot.overdueTasks)
    }

    @Test
    fun `aggregates tasks and events for a month`() = runBlocking {
        val year = 2000
        val month = 6
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        val overdue = Task(
            title = "Close books",
            dueAt = LocalDateTime.of(2000, 6, 15, 9, 0),
            period = AgendaPeriod.Month(year, month)
        )
        val pending = Task(
            title = "Publish roadmap",
            dueAt = LocalDateTime.of(2100, 6, 1, 9, 0),
            period = AgendaPeriod.Month(year, month)
        )
        val tasks = listOf(overdue, pending)
        val events = listOf(
            CalendarEvent(
                title = "Quarterly planning",
                start = LocalDateTime.of(2000, 6, 12, 9, 0),
                end = LocalDateTime.of(2000, 6, 12, 10, 0)
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
        assertEquals(0, snapshot.completedCount)
        assertEquals(2, snapshot.pendingCount)
        assertEquals(listOf(overdue), snapshot.overdueTasks)
    }

    @Test
    fun `marks overlapping events as conflicts`() = runBlocking {
        val date = LocalDate.of(2024, 5, 11)
        val first = CalendarEvent(
            title = "Project kickoff",
            start = LocalDateTime.of(2024, 5, 11, 9, 0),
            end = LocalDateTime.of(2024, 5, 11, 10, 0)
        )
        val overlapping = CalendarEvent(
            title = "Design review",
            start = LocalDateTime.of(2024, 5, 11, 9, 30),
            end = LocalDateTime.of(2024, 5, 11, 10, 30)
        )
        val separate = CalendarEvent(
            title = "Lunch & learn",
            start = LocalDateTime.of(2024, 5, 11, 12, 0),
            end = LocalDateTime.of(2024, 5, 11, 13, 0)
        )
        val aggregator = AgendaAggregator(
            taskRepository = FakeTaskRepository(dayTasks = mapOf(date to emptyList())),
            eventRepository = FakeEventRepository(dayEvents = mapOf(date to listOf(first, overlapping, separate)))
        )

        val snapshot = aggregator.observeAgenda(AgendaPeriod.Day(date)).first()

        assertEquals(setOf(first.id, overlapping.id), snapshot.conflictingEventIds)
        assertTrue(snapshot.conflictingEventIds.contains(first.id))
        assertTrue(snapshot.conflictingEventIds.contains(overlapping.id))
        assertTrue(separate.id !in snapshot.conflictingEventIds)
    }

    @Test
    fun `expands recurring events within requested range`() = runBlocking {
        val start = LocalDate.of(2024, 6, 3)
        val end = start.plusDays(6)
        val recurring = CalendarEvent(
            title = "Daily stand-up",
            start = LocalDateTime.of(start, LocalTime.of(9, 30)),
            end = LocalDateTime.of(start, LocalTime.of(9, 45)),
            recurrence = Recurrence(rule = RecurrenceRule.Daily, interval = 1, occurrences = 5)
        )
        val aggregator = AgendaAggregator(
            taskRepository = FakeTaskRepository(weekTasks = mapOf(start to emptyList())),
            eventRepository = FakeEventRepository(
                rangeEvents = mapOf((start to end) to listOf(recurring))
            )
        )

        val snapshot = aggregator.observeAgenda(AgendaPeriod.Week(start)).first()

        val occurrences = snapshot.events
        assertEquals(5, occurrences.size)
        val expectedDates = (0 until 5).map { start.plusDays(it.toLong()) }
        assertEquals(expectedDates, occurrences.map { it.start.toLocalDate() })
    }

    @Test
    fun `stops expanding recurring events beyond configured occurrences`() = runBlocking {
        val periodStart = LocalDate.of(2024, 7, 1)
        val periodEnd = periodStart.plusMonths(1).minusDays(1)
        val recurring = CalendarEvent(
            title = "Monthly check-in",
            start = LocalDateTime.of(periodStart.minusMonths(1), LocalTime.of(8, 0)),
            end = LocalDateTime.of(periodStart.minusMonths(1), LocalTime.of(9, 0)),
            recurrence = Recurrence(rule = RecurrenceRule.Monthly, interval = 1, occurrences = 2)
        )
        val aggregator = AgendaAggregator(
            taskRepository = FakeTaskRepository(monthTasks = mapOf((periodStart.year to periodStart.monthValue) to emptyList())),
            eventRepository = FakeEventRepository(
                rangeEvents = mapOf((periodStart to periodEnd) to listOf(recurring))
            )
        )

        val snapshot = aggregator.observeAgenda(AgendaPeriod.Month(periodStart.year, periodStart.monthValue)).first()

        // Only the second occurrence should appear in the selected month.
        assertEquals(1, snapshot.events.size)
        assertEquals(periodStart, snapshot.events.single().start.toLocalDate())
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
