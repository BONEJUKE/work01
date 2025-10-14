package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.calendar.ui.AgendaViewModel
import com.example.calendar.ui.AgendaViewModelFactory
import com.example.calendar.ui.CalendarApp

class MainActivity : ComponentActivity() {
    private val viewModel: AgendaViewModel by viewModels {
        val app = application as CalendarApplication
        AgendaViewModelFactory(
            aggregator = app.container.agendaAggregator,
            reminderOrchestrator = app.container.reminderOrchestrator,
            taskRepository = app.container.taskRepository,
            eventRepository = app.container.eventRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarApp(viewModel = viewModel)
        }
    }
}
