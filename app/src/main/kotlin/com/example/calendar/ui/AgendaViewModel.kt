package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.scheduler.AgendaAggregator
import com.example.calendar.scheduler.AgendaSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgendaViewModel(
    private val aggregator: AgendaAggregator,
    private val reminderOrchestrator: ReminderOrchestrator,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _period = MutableStateFlow<AgendaPeriod>(AgendaPeriod.Day(java.time.LocalDate.now()))
    private val _state = MutableStateFlow(AgendaUiState())
    val state: StateFlow<AgendaUiState> = _state.asStateFlow()

    init {
        observePeriod()
    }

    fun setPeriod(period: AgendaPeriod) {
        if (_period.value != period) {
            _period.value = period
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            reminderOrchestrator.cancelForTask(task)
            taskRepository.toggleStatus(task.id)
            val toggled = task.toggleCompletion()
            if (!toggled.status.isDone() && toggled.dueAt != null) {
                reminderOrchestrator.scheduleForTask(toggled)
            }
            _state.value = _state.value.updateTask(toggled)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            reminderOrchestrator.cancelForTask(task)
            taskRepository.delete(task.id)
            _state.value = _state.value.removeTask(task.id)
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            reminderOrchestrator.cancelForEvent(event)
            eventRepository.delete(event.id)
            _state.value = _state.value.removeEvent(event.id)
        }
    }

    suspend fun updateTask(
        original: Task,
        title: String,
        description: String?,
        dueAt: LocalDateTime?
    ): Result<Task> {
        return withContext(viewModelScope.coroutineContext) {
            runCatching {
                val normalizedTitle = title.trim()
                require(normalizedTitle.isNotEmpty()) { "제목을 입력해 주세요." }

                val sanitizedDescription = description?.trim().takeIf { !it.isNullOrBlank() }
                val updated = original.copy(
                    title = normalizedTitle,
                    description = sanitizedDescription,
                    dueAt = dueAt
                )

                reminderOrchestrator.cancelForTask(original)
                taskRepository.upsert(updated)

                if (!updated.status.isDone() && updated.dueAt != null) {
                    reminderOrchestrator.scheduleForTask(updated)
                }

                _state.update { it.updateTask(updated) }
                updated
            }
        }
    }

    suspend fun updateEvent(
        original: CalendarEvent,
        title: String,
        description: String?,
        location: String?,
        start: LocalDateTime,
        end: LocalDateTime
    ): Result<CalendarEvent> {
        return withContext(viewModelScope.coroutineContext) {
            runCatching {
                val normalizedTitle = title.trim()
                require(normalizedTitle.isNotEmpty()) { "제목을 입력해 주세요." }

                require(!end.isBefore(start)) { "종료 시간은 시작 시간 이후여야 합니다." }

                val sanitizedDescription = description?.trim().takeIf { !it.isNullOrBlank() }
                val sanitizedLocation = location?.trim().takeIf { !it.isNullOrBlank() }

                val updated = original.copy(
                    title = normalizedTitle,
                    description = sanitizedDescription,
                    location = sanitizedLocation,
                    start = start,
                    end = end
                )

                reminderOrchestrator.cancelForEvent(original)
                eventRepository.upsert(updated)
                reminderOrchestrator.scheduleForEvent(updated)

                _state.update { it.updateEvent(updated) }
                updated
            }
        }
    }

    fun cycleCompletedTaskFilter() {
        _state.update { current ->
            current.copy(
                filters = current.filters.copy(
                    completedTaskFilter = current.filters.completedTaskFilter.next()
                )
            )
        }
    }

    fun toggleShowRecurringEvents() {
        _state.update { current ->
            current.copy(
                filters = current.filters.copy(
                    showRecurringEvents = !current.filters.showRecurringEvents
                )
            )
        }
    }

    suspend fun quickAddTask(
        title: String,
        focusDate: LocalDate,
        period: AgendaPeriod,
        description: String?,
        dueTime: LocalTime?,
        reminders: List<Reminder>
    ): Result<Task> {
        return withContext(viewModelScope.coroutineContext) {
            runCatching {
                val normalizedTitle = title.trim()
                require(normalizedTitle.isNotEmpty()) { "제목을 입력해 주세요." }

                val sanitizedDescription = description?.trim().takeIf { !it.isNullOrBlank() }
                val dueAt = dueTime?.let { LocalDateTime.of(focusDate, it) }
                require(reminders.all { it.minutesBefore > 0 }) { "리마인더 시점은 1분 이상이어야 합니다." }
                require(dueAt != null || reminders.isEmpty()) { "마감 시간을 설정해야 리마인더를 사용할 수 있어요." }
                val targetPeriod = resolvePeriodForQuickAdd(period, focusDate)

                val task = Task(
                    title = normalizedTitle,
                    description = sanitizedDescription,
                    dueAt = dueAt,
                    period = targetPeriod,
                    reminders = reminders
                )

                taskRepository.upsert(task)
                if (dueAt != null) {
                    reminderOrchestrator.scheduleForTask(task)
                }

                _state.update {
                    it.copy(userMessage = AgendaUserMessage.QuickAddSuccess(QuickAddType.Task))
                }
                task
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        userMessage = AgendaUserMessage.QuickAddFailure(
                            error.message ?: "할 일을 추가하지 못했습니다."
                        )
                    )
                }
            }
        }
    }

    suspend fun quickAddEvent(
        title: String,
        focusDate: LocalDate,
        period: AgendaPeriod,
        description: String?,
        location: String?,
        startTime: LocalTime,
        endTime: LocalTime,
        reminders: List<Reminder>
    ): Result<CalendarEvent> {
        return withContext(viewModelScope.coroutineContext) {
            runCatching {
                val normalizedTitle = title.trim()
                require(normalizedTitle.isNotEmpty()) { "제목을 입력해 주세요." }

                val sanitizedDescription = description?.trim().takeIf { !it.isNullOrBlank() }
                val sanitizedLocation = location?.trim().takeIf { !it.isNullOrBlank() }
                val start = LocalDateTime.of(focusDate, startTime)
                val end = LocalDateTime.of(focusDate, endTime)
                require(!end.isBefore(start)) { "종료 시간이 시작 시간보다 빠를 수 없습니다." }
                require(reminders.all { it.minutesBefore > 0 }) { "리마인더 시점은 1분 이상이어야 합니다." }

                val event = CalendarEvent(
                    title = normalizedTitle,
                    description = sanitizedDescription,
                    start = start,
                    end = end,
                    location = sanitizedLocation,
                    reminders = reminders
                )

                eventRepository.upsert(event)
                reminderOrchestrator.scheduleForEvent(event)

                _state.update {
                    it.copy(userMessage = AgendaUserMessage.QuickAddSuccess(QuickAddType.Event))
                }
                event
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        userMessage = AgendaUserMessage.QuickAddFailure(
                            error.message ?: "일정을 추가하지 못했습니다."
                        )
                    )
                }
            }
        }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    private fun observePeriod() {
        _period
            .flatMapLatest { period ->
                aggregator.observeAgenda(period)
                    .onStart {
                        _state.value = _state.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
            }
            .catch { throwable ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = throwable
                )
            }
            .onEach { snapshot ->
                _state.value = _state.value.copy(
                    snapshot = snapshot,
                    isLoading = false,
                    error = null
                )
            }
            .launchIn(viewModelScope)
    }
}

data class AgendaUiState(
    val snapshot: AgendaSnapshot? = null,
    val isLoading: Boolean = true,
    val error: Throwable? = null,
    val userMessage: AgendaUserMessage? = null,
    val filters: AgendaFilters = AgendaFilters()
) {
    fun updateTask(task: Task): AgendaUiState {
        val current = snapshot ?: return this
        val updatedTasks = current.tasks.map { existing ->
            if (existing.id == task.id) task else existing
        }
        return copy(snapshot = current.copy(tasks = updatedTasks))
    }

    fun updateEvent(event: CalendarEvent): AgendaUiState {
        val current = snapshot ?: return this
        var replaced = false
        val updatedEvents = current.events.map { existing ->
            if (!replaced && existing.id == event.id) {
                replaced = true
                event
            } else {
                existing
            }
        }
        return copy(snapshot = current.copy(events = updatedEvents))
    }

    fun removeTask(id: java.util.UUID): AgendaUiState {
        val current = snapshot ?: return this
        return copy(snapshot = current.copy(tasks = current.tasks.filterNot { it.id == id }))
    }

    fun removeEvent(id: java.util.UUID): AgendaUiState {
        val current = snapshot ?: return this
        return copy(snapshot = current.copy(events = current.events.filterNot { it.id == id }))
    }
}

data class AgendaFilters(
    val completedTaskFilter: CompletedTaskFilter = CompletedTaskFilter.All,
    val showRecurringEvents: Boolean = true
)

enum class CompletedTaskFilter {
    All,
    HideCompleted,
    CompletedOnly;

    fun next(): CompletedTaskFilter = when (this) {
        All -> HideCompleted
        HideCompleted -> CompletedOnly
        CompletedOnly -> All
    }
}

private val DEFAULT_TASK_TIME: LocalTime = LocalTime.of(9, 0)
private val DEFAULT_EVENT_START: LocalTime = LocalTime.of(9, 0)
private const val DEFAULT_EVENT_DURATION_HOURS = 1L

private fun resolvePeriodForQuickAdd(period: AgendaPeriod, focusDate: LocalDate): AgendaPeriod {
    return when (period) {
        is AgendaPeriod.Day -> AgendaPeriod.Day(focusDate)
        is AgendaPeriod.Week -> {
            val startOfWeek = focusDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            AgendaPeriod.Week(startOfWeek)
        }
        is AgendaPeriod.Month -> AgendaPeriod.Month(focusDate.year, focusDate.monthValue)
    }
}

enum class QuickAddType {
    Task,
    Event
}

sealed class AgendaUserMessage {
    data class QuickAddSuccess(val type: QuickAddType) : AgendaUserMessage()
    data class QuickAddFailure(val reason: String) : AgendaUserMessage()
}
