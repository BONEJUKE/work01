package com.example.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.scheduler.AgendaAggregator

class AgendaViewModelFactory(
    private val aggregator: AgendaAggregator,
    private val reminderOrchestrator: ReminderOrchestrator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgendaViewModel::class.java)) {
            return AgendaViewModel(aggregator, reminderOrchestrator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${'$'}{modelClass.name}")
    }
}
