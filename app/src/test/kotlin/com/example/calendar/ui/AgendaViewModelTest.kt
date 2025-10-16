package com.example.calendar.ui

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.InMemoryEventRepository
import com.example.calendar.data.InMemoryTaskRepository
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import com.example.calendar.data.TaskStatus
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.ui.CompletedTaskFilter
import com.example.calendar.reminder.ReminderPayload
import com.example.calendar.reminder.ReminderScheduler
import com.example.calendar.reminder.ReminderStore
import com.example.calendar.reminder.StoredReminder
import com.example.calendar.scheduler.AgendaAggregator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2024, 1, 15)

    private lateinit var task: Task
    private lateinit var event: CalendarEvent
    private val weekStart = today
    private val monthPeriod = AgendaPeriod.Month(today.year, today.monthValue)

    private lateinit var scheduler: RecordingReminderScheduler
    private lateinit var store: InMemoryReminderStore
    private lateinit var viewModel: AgendaViewModel
    private lateinit var weekTask: Task
    private lateinit var monthTask: Task
    private lateinit var weekEvent: CalendarEvent
    private lateinit var monthEvent: CalendarEvent

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        task = Task(
            id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            title = "Write status update",
            description = "Collect highlights before stand-up",
            status = TaskStatus.Pending,
            dueAt = LocalDateTime.of(today, LocalTime.of(10, 0)),
            period = AgendaPeriod.Day(today),
            reminders = listOf(Reminder(minutesBefore = 30))
        )
        event = CalendarEvent(
            id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            title = "Weekly planning",
            start = LocalDateTime.of(today, LocalTime.of(9, 0)),
            end = LocalDateTime.of(today, LocalTime.of(10, 0)),
            reminders = listOf(Reminder(minutesBefore = 60))
        )

        weekTask = Task(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            title = "Prep weekly sync",
            period = AgendaPeriod.Week(weekStart),
            dueAt = LocalDateTime.of(weekStart.plusDays(2), LocalTime.of(14, 30))
        )
        monthTask = Task(
            id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
            title = "Publish roadmap",
            period = monthPeriod,
            dueAt = LocalDateTime.of(today.withDayOfMonth(25), LocalTime.of(17, 0))
        )
        weekEvent = CalendarEvent(
            id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
            title = "Leadership sync",
            start = LocalDateTime.of(weekStart.plusDays(3), LocalTime.of(16, 0)),
            end = LocalDateTime.of(weekStart.plusDays(3), LocalTime.of(17, 0))
        )
        monthEvent = CalendarEvent(
            id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
            title = "Board review",
            start = LocalDateTime.of(today.withDayOfMonth(28), LocalTime.of(9, 0)),
            end = LocalDateTime.of(today.withDayOfMonth(28), LocalTime.of(10, 0))
        )

        val taskRepository = InMemoryTaskRepository(listOf(task, weekTask, monthTask))
        val eventRepository = InMemoryEventRepository(listOf(event, weekEvent, monthEvent))
        val aggregator = AgendaAggregator(taskRepository, eventRepository)
        scheduler = RecordingReminderScheduler()
        store = InMemoryReminderStore()
        val orchestrator = ReminderOrchestrator(scheduler, store)

        viewModel = AgendaViewModel(
            aggregator = aggregator,
            reminderOrchestrator = orchestrator,
            taskRepository = taskRepository,
            eventRepository = eventRepository
        )

        viewModel.setPeriod(AgendaPeriod.Day(today))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `agenda snapshot populates tasks and events for selected period`() = runTest(dispatcher) {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        val snapshot = requireNotNull(state.snapshot)
        assertEquals(1, snapshot.tasks.size)
        assertEquals(1, snapshot.events.size)
        assertEquals(task.id, snapshot.tasks.single().id)
        assertEquals(event.id, snapshot.events.single().id)
    }

    @Test
    fun `toggling a task updates status and reminder scheduling`() = runTest(dispatcher) {
        advanceUntilIdle()

        val initialTask = requireNotNull(viewModel.state.value.snapshot).tasks.single()
        assertFalse(initialTask.status.isDone())

        viewModel.toggleTask(initialTask)
        advanceUntilIdle()

        val completed = requireNotNull(viewModel.state.value.snapshot).tasks.single()
        assertTrue(completed.status.isDone())
        assertEquals(listOf("task-${task.id}"), scheduler.canceled)

        viewModel.toggleTask(completed)
        advanceUntilIdle()

        val reopened = requireNotNull(viewModel.state.value.snapshot).tasks.single()
        assertFalse(reopened.status.isDone())
        assertTrue(scheduler.scheduled.any { it.id.startsWith("task-${task.id}-") })
    }

    @Test
    fun `deleting an event removes it from snapshot and cancels reminders`() = runTest(dispatcher) {
        advanceUntilIdle()

        val currentEvent = requireNotNull(viewModel.state.value.snapshot).events.single()
        viewModel.deleteEvent(currentEvent)
        advanceUntilIdle()

        val updated = viewModel.state.value.snapshot
        assertNotNull(updated)
        assertTrue(updated!!.events.isEmpty())
        assertEquals(listOf("event-${event.id}"), scheduler.canceled)
    }

    @Test
    fun `switching to week period loads aggregated snapshot`() = runTest(dispatcher) {
        viewModel.setPeriod(AgendaPeriod.Week(weekStart))
        advanceUntilIdle()

        val snapshot = requireNotNull(viewModel.state.value.snapshot)
        assertEquals(weekStart, snapshot.rangeStart)
        assertEquals(weekStart.plusDays(6), snapshot.rangeEnd)
        assertEquals(listOf(weekTask), snapshot.tasks)
        assertEquals(listOf(weekEvent), snapshot.events)
        assertEquals(1, snapshot.pendingCount)
    }

    @Test
    fun `switching to month period loads aggregated snapshot`() = runTest(dispatcher) {
        viewModel.setPeriod(monthPeriod)
        advanceUntilIdle()

        val snapshot = requireNotNull(viewModel.state.value.snapshot)
        assertEquals(today.withDayOfMonth(1), snapshot.rangeStart)
        assertEquals(today.withDayOfMonth(today.lengthOfMonth()), snapshot.rangeEnd)
        assertEquals(listOf(monthTask), snapshot.tasks)
        assertEquals(listOf(monthEvent), snapshot.events)
        assertEquals(1, snapshot.pendingCount)
    }

    @Test
    fun `cycling completed task filter rotates modes`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertEquals(CompletedTaskFilter.All, viewModel.state.value.filters.completedTaskFilter)

        viewModel.cycleCompletedTaskFilter()
        assertEquals(CompletedTaskFilter.HideCompleted, viewModel.state.value.filters.completedTaskFilter)

        viewModel.cycleCompletedTaskFilter()
        assertEquals(CompletedTaskFilter.CompletedOnly, viewModel.state.value.filters.completedTaskFilter)

        viewModel.cycleCompletedTaskFilter()
        assertEquals(CompletedTaskFilter.All, viewModel.state.value.filters.completedTaskFilter)
    }

    @Test
    fun `toggle show recurring events updates filter state`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertTrue(viewModel.state.value.filters.showRecurringEvents)
        viewModel.toggleShowRecurringEvents()
        assertFalse(viewModel.state.value.filters.showRecurringEvents)
    }

    private class RecordingReminderScheduler : ReminderScheduler {
        data class Scheduled(val id: String, val payload: ReminderPayload)

        val scheduled = mutableListOf<Scheduled>()
        val canceled = mutableListOf<String>()

        override fun scheduleReminder(
            id: String,
            triggerAt: LocalDateTime,
            reminder: Reminder,
            payload: ReminderPayload
        ) {
            scheduled += Scheduled(id, payload)
        }

        override fun cancelReminder(id: String) {
            canceled += id
        }
    }

    private class InMemoryReminderStore : ReminderStore {
        private val backing = linkedMapOf<String, MutableList<StoredReminder>>()

        override fun write(baseId: String, reminders: List<StoredReminder>) {
            if (reminders.isEmpty()) {
                remove(baseId)
            } else {
                backing[baseId] = reminders.toMutableList()
            }
        }

        override fun read(baseId: String): List<StoredReminder> =
            backing[baseId]?.toList() ?: emptyList()

        override fun readAll(): Map<String, List<StoredReminder>> =
            backing.mapValues { it.value.toList() }

        override fun remove(baseId: String) {
            backing.remove(baseId)
        }
    }
}
