package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.Task
import com.example.calendar.scheduler.AgendaAggregator
import com.example.calendar.scheduler.AgendaSnapshot
import com.example.calendar.reminder.ReminderOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AgendaViewModel(
    private val aggregator: AgendaAggregator,
    private val reminderOrchestrator: ReminderOrchestrator
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
            val toggled = task.toggleCompletion()
            if (!toggled.status.isDone() && toggled.dueAt != null) {
                reminderOrchestrator.scheduleForTask(toggled)
            }
            _state.value = _state.value.updateTask(toggled)
        }
    }

    private fun observePeriod() {
        _period.onEach { period ->
            aggregator.observeAgenda(period)
                .onEach { snapshot ->
                    _state.value = _state.value.copy(
                        snapshot = snapshot,
                        isLoading = false,
                        error = null
                    )
                }
                .launchIn(viewModelScope)
        }.launchIn(viewModelScope)
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
}
