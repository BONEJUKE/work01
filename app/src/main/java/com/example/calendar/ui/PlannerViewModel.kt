package com.example.calendar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.PlannerDatabase
import com.example.calendar.data.PlannerTaskEntity
import com.example.calendar.data.RepeatCadence
import com.example.calendar.domain.AlarmScheduler
import com.example.calendar.domain.PlannerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class PlannerScope { DAY, WEEK, MONTH }

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedScope: PlannerScope = PlannerScope.DAY,
    val tasks: List<PlannerTaskEntity> = emptyList()
)

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlannerRepository(
        PlannerDatabase.getInstance(application).plannerTaskDao()
    )
    private val alarmScheduler = AlarmScheduler(application)

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState

    private var taskCollectionJob: Job? = null

    init {
        refreshTasks()
    }

    fun selectDate(newDate: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = newDate)
        refreshTasks()
    }

    fun selectScope(scope: PlannerScope) {
        _uiState.value = _uiState.value.copy(selectedScope = scope)
        refreshTasks()
    }

    fun toggleTaskCompletion(task: PlannerTaskEntity) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun addTask(
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime,
        reminderMinutesBefore: Int,
        repeatCadence: RepeatCadence
    ) {
        viewModelScope.launch {
            val dueDateTime = LocalDateTime.of(date, time)
            val dueMillis = dueDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val entity = PlannerTaskEntity(
                title = title,
                description = description,
                scheduledAtEpochMillis = dueMillis,
                reminderMinutesBefore = reminderMinutesBefore,
                repeatCadence = repeatCadence
            )
            val taskId = repository.saveTask(entity)
            val persistedTask = entity.copy(id = taskId)
            scheduleAlarm(persistedTask)
        }
    }

    private fun scheduleAlarm(task: PlannerTaskEntity) {
        val triggerTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(task.scheduledAtEpochMillis),
            ZoneId.systemDefault()
        ).minusMinutes(task.reminderMinutesBefore.toLong()).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val safeTrigger = triggerTime.coerceAtLeast(System.currentTimeMillis())
        alarmScheduler.schedule(task, safeTrigger)
    }

    private fun refreshTasks() {
        val (start, end) = getCurrentRange()
        taskCollectionJob?.cancel()
        taskCollectionJob = viewModelScope.launch {
            repository.observeTasks(start, end).collectLatest { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks)
            }
        }
    }

    private fun getCurrentRange(): Pair<Long, Long> {
        val state = _uiState.value
        val zone = ZoneId.systemDefault()
        return when (state.selectedScope) {
            PlannerScope.DAY -> {
                val start = state.selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = state.selectedDate.plusDays(1).atStartOfDay(zone).minusNanos(1)
                    .toInstant().toEpochMilli()
                start to end
            }

            PlannerScope.WEEK -> {
                val startOfWeek = state.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = state.selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val start = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = endOfWeek.plusDays(1).atStartOfDay(zone).minusNanos(1)
                    .toInstant().toEpochMilli()
                start to end
            }

            PlannerScope.MONTH -> {
                val startOfMonth = state.selectedDate.withDayOfMonth(1)
                val endOfMonth = startOfMonth.plusMonths(1).minusDays(1)
                val start = startOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = endOfMonth.plusDays(1).atStartOfDay(zone).minusNanos(1)
                    .toInstant().toEpochMilli()
                start to end
            }
        }
    }
}
