package com.example.calendar.ui

import androidx.compose.runtime.Composable
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Task
import com.example.calendar.ui.agenda.AgendaRoute
import com.example.calendar.ui.theme.CalendarTheme

@Composable
fun CalendarApp(
    viewModel: AgendaViewModel,
    onEventClick: (CalendarEvent) -> Unit = {},
    onTaskClick: (Task) -> Unit = {}
) {
    CalendarTheme {
        AgendaRoute(
            viewModel = viewModel,
            onEventClick = onEventClick,
            onTaskClick = onTaskClick
        )
    }
}
