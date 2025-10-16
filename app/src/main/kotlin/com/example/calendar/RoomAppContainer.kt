package com.example.calendar

import android.app.AlarmManager
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
import com.example.calendar.scheduler.AgendaAggregator

/**
 * Production container backed by a Room database so tasks and events survive
 * process death and app restarts.
 */
class RoomAppContainer(
    private val context: Context
) : AppContainer {

    private val database: CalendarDatabase by lazy {
        Room.databaseBuilder(
            context,
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    override val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database.taskDao())
    }

    override val eventRepository: EventRepository by lazy {
        RoomEventRepository(database.calendarEventDao())
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(AlarmManager::class.java)
            ?: context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    override val reminderOrchestrator: ReminderOrchestrator by lazy {
        ReminderOrchestrator(
            AndroidReminderScheduler(
                context = context,
                alarmManager = alarmManager,
                workManager = workManager
            )
        )
    }

    override val agendaAggregator: AgendaAggregator by lazy {
        AgendaAggregator(taskRepository, eventRepository)
    }
}
