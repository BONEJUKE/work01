package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.scheduler.AgendaAggregator
import com.example.calendar.scheduler.AgendaSnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

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

    suspend fun quickAddTask(
        title: String,
        period: AgendaPeriod,
        focusedDay: LocalDate
    ): Result<Unit> {
        val trimmed = title.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be blank"))
        }
        val dueAt = LocalDateTime.of(focusedDay, DEFAULT_TASK_TIME)
        val task = Task(
            title = trimmed,
            dueAt = dueAt,
            period = when (period) {
                is AgendaPeriod.Day -> AgendaPeriod.Day(focusedDay)
                is AgendaPeriod.Week -> period
                is AgendaPeriod.Month -> period
            }
        )

        return runCatching {
            taskRepository.upsert(task)
            reminderOrchestrator.scheduleForTask(task)
        }.onSuccess {
            _state.value = _state.value.copy(
                userMessage = AgendaUserMessage.Success("할 일을 저장했어요")
            )
        }.onFailure {
            _state.value = _state.value.copy(
                userMessage = AgendaUserMessage.Error("할 일을 저장하지 못했습니다.")
            )
        }
    }

    suspend fun quickAddEvent(
        title: String,
        focusedDay: LocalDate
    ): Result<Unit> {
        val trimmed = title.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Event title cannot be blank"))
        }
        val start = LocalDateTime.of(focusedDay, DEFAULT_EVENT_START)
        val event = CalendarEvent(
            title = trimmed,
            start = start,
            end = start.plusHours(DEFAULT_EVENT_DURATION_HOURS)
        )

        return runCatching {
            eventRepository.upsert(event)
            reminderOrchestrator.scheduleForEvent(event)
        }.onSuccess {
            _state.value = _state.value.copy(
                userMessage = AgendaUserMessage.Success("이벤트를 저장했어요")
            )
        }.onFailure {
            _state.value = _state.value.copy(
                userMessage = AgendaUserMessage.Error("이벤트를 저장하지 못했습니다.")
            )
        }
    }

    fun consumeUserMessage() {
        _state.value = _state.value.copy(userMessage = null)
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
    val userMessage: AgendaUserMessage? = null
) {
    fun updateTask(task: Task): AgendaUiState {
        val current = snapshot ?: return this
        val updatedTasks = current.tasks.map { existing ->
            if (existing.id == task.id) task else existing
        }
        return copy(snapshot = current.copy(tasks = updatedTasks))
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

sealed class AgendaUserMessage(val message: String) {
    class Success(message: String) : AgendaUserMessage(message)
    class Error(message: String) : AgendaUserMessage(message)
}

private val DEFAULT_TASK_TIME: LocalTime = LocalTime.of(9, 0)
private val DEFAULT_EVENT_START: LocalTime = LocalTime.of(9, 0)
private const val DEFAULT_EVENT_DURATION_HOURS = 1L
