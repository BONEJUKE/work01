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
import com.example.calendar.reminder.REMINDER_STORE_NAME
import com.example.calendar.reminder.ReminderStore
import com.example.calendar.reminder.ReminderStoreSynchronizer
import com.example.calendar.reminder.SharedPreferencesReminderStore
import com.example.calendar.scheduler.AgendaAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Production container backed by a Room database so tasks and events survive
 * process death and app restarts.
 */
class RoomAppContainer(
    private val context: Context
) : AppContainer {

    private val reminderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: CalendarDatabase by lazy {
        Room.databaseBuilder(
            context,
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    private val reminderStore: ReminderStore by lazy {
        SharedPreferencesReminderStore(
            context.getSharedPreferences(REMINDER_STORE_NAME, Context.MODE_PRIVATE)
        )
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

    private var reminderSynchronizer: ReminderStoreSynchronizer? = null

    override val reminderOrchestrator: ReminderOrchestrator by lazy {
        ReminderOrchestrator(
            AndroidReminderScheduler(
                context = context,
                alarmManager = alarmManager,
                workManager = workManager
            ),
            reminderStore
        ).also { orchestrator ->
            reminderSynchronizer = ReminderStoreSynchronizer(
                taskStream = database.taskDao().observeAllTasks(),
                eventStream = database.calendarEventDao().observeAllEvents(),
                orchestrator = orchestrator,
                store = reminderStore,
                scope = reminderScope
            )
        }
    }

    override val agendaAggregator: AgendaAggregator by lazy {
        AgendaAggregator(taskRepository, eventRepository)
    }
}
