package com.example.calendar

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.calendar.data.CalendarDatabase
import com.example.calendar.data.EventRepository
import com.example.calendar.data.RoomEventRepository
import com.example.calendar.data.RoomTaskRepository
import com.example.calendar.data.TaskRepository
import com.example.calendar.reminder.AndroidReminderScheduler
import com.example.calendar.reminder.ReminderOrchestrator
import com.example.calendar.reminder.ReminderScheduler
import com.example.calendar.scheduler.AgendaAggregator

class CalendarApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

interface AppContainer {
    val taskRepository: TaskRepository
    val eventRepository: EventRepository
    val reminderOrchestrator: ReminderOrchestrator
    val agendaAggregator: AgendaAggregator
}

private class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: CalendarDatabase by lazy {
        Room.databaseBuilder(
            context,
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(AlarmManager::class.java)
            ?: throw IllegalStateException("AlarmManager not available")
    }

    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }

    private val reminderScheduler: ReminderScheduler by lazy {
        AndroidReminderScheduler(
            context = context,
            alarmManager = alarmManager,
            workManager = workManager
        )
    }

    override val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database.taskDao())
    }

    override val eventRepository: EventRepository by lazy {
        RoomEventRepository(database.calendarEventDao())
    }

    override val reminderOrchestrator: ReminderOrchestrator by lazy {
        ReminderOrchestrator(reminderScheduler)
    }

    override val agendaAggregator: AgendaAggregator by lazy {
        AgendaAggregator(taskRepository, eventRepository)
    }
}
