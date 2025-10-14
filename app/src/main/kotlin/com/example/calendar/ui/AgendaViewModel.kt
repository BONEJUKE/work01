package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import com.example.calendar.scheduler.AgendaAggregator
import com.example.calendar.scheduler.AgendaSnapshot
import com.example.calendar.reminder.ReminderOrchestrator
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
    val error: Throwable? = null
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
