package com.example.calendar

import android.app.Application
import com.example.calendar.data.EventRepository
import com.example.calendar.data.TaskRepository
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.scheduler.AgendaAggregator

class CalendarApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = QuickStartAppContainer()
    }
}

interface AppContainer {
    val taskRepository: TaskRepository
    val eventRepository: EventRepository
    val reminderOrchestrator: ReminderOrchestrator
    val agendaAggregator: AgendaAggregator
}
