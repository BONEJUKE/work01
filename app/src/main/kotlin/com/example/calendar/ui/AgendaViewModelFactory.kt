package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.calendar.data.EventRepository
import com.example.calendar.data.TaskRepository
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.scheduler.AgendaAggregator

class AgendaViewModelFactory(
    private val aggregator: AgendaAggregator,
    private val reminderOrchestrator: ReminderOrchestrator,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgendaViewModel::class.java)) {
            return AgendaViewModel(
                aggregator = aggregator,
                reminderOrchestrator = reminderOrchestrator,
                taskRepository = taskRepository,
                eventRepository = eventRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${'$'}{modelClass.name}")
    }
}
