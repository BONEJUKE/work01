package com.example.calendar.reminder

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.calendar.CalendarApplication
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.Reminder
import com.example.calendar.data.Task
import com.example.calendar.data.TaskStatus
import com.example.calendar.reminder.ReminderActionReceiver.Companion.ACTION_COMPLETE
import com.example.calendar.reminder.ReminderActionReceiver.Companion.ACTION_SNOOZE
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_ALLOW_SNOOZE
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_BASE_ID
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_DEEP_LINK
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_MESSAGE
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_REMINDER_ID
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_SNOOZE_MINUTES
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_TASK_ID
import com.example.calendar.reminder.ReminderActionReceiver.Companion.EXTRA_TITLE
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderActionReceiverTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: CalendarApplication

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        androidx.work.WorkManager.getInstance(application).cancelAllWork().result.get()
    }

    @Test
    fun completeActionMarksTaskDoneAndCancelsWork() = runBlocking {
        val taskDate = LocalDate.now().plusDays(1)
        val task = Task(
            id = UUID.randomUUID(),
            title = "보고서 마감",
            period = AgendaPeriod.Day(taskDate),
            dueAt = LocalDateTime.of(taskDate, LocalTime.of(9, 0)),
            reminders = listOf(Reminder(minutesBefore = 30))
        )
        val taskRepository = application.container.taskRepository
        val orchestrator = application.container.reminderOrchestrator
        taskRepository.upsert(task)
        orchestrator.scheduleForTask(task)

        val reminderId = "task-${task.id}-0"
        val intent = Intent(application, ReminderActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TASK_ID, task.id.toString())
            putExtra(EXTRA_ALLOW_SNOOZE, true)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_MESSAGE, "30분 전에 알림")
            putExtra(EXTRA_DEEP_LINK, "app://task/${task.id}")
            putExtra(EXTRA_SNOOZE_MINUTES, ReminderPayload.DEFAULT_SNOOZE_MINUTES)
            putExtra(EXTRA_BASE_ID, "task-${task.id}")
        }

        ReminderActionReceiver().onReceive(application, intent)

        withTimeout(TimeUnit.SECONDS.toMillis(5)) {
            taskRepository.observeTasksForDay(taskDate).first { tasks ->
                tasks.firstOrNull { it.id == task.id }?.status == TaskStatus.Completed
            }
        }

        val workInfos = androidx.work.WorkManager.getInstance(application)
            .getWorkInfosByTag(ReminderNotificationWorker.tag(reminderId))
            .get()
        assertTrue(workInfos.isEmpty() || workInfos.all { it.state.isFinished })
    }

    @Test
    fun snoozeActionQueuesFollowUpWork() = runBlocking {
        val taskDate = LocalDate.now().plusDays(2)
        val task = Task(
            id = UUID.randomUUID(),
            title = "프레젠테이션 준비",
            period = AgendaPeriod.Day(taskDate),
            dueAt = LocalDateTime.of(taskDate, LocalTime.of(14, 0)),
            reminders = listOf(Reminder(minutesBefore = 45))
        )
        val taskRepository = application.container.taskRepository
        val orchestrator = application.container.reminderOrchestrator
        taskRepository.upsert(task)
        orchestrator.scheduleForTask(task)

        val reminderId = "task-${task.id}-0"
        val intent = Intent(application, ReminderActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TASK_ID, task.id.toString())
            putExtra(EXTRA_ALLOW_SNOOZE, true)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_MESSAGE, "45분 전에 알림")
            putExtra(EXTRA_DEEP_LINK, "app://task/${task.id}")
            putExtra(EXTRA_SNOOZE_MINUTES, 5L)
            putExtra(EXTRA_BASE_ID, "task-${task.id}")
        }

        ReminderActionReceiver().onReceive(application, intent)

        val snoozedTag = ReminderNotificationWorker.tag(reminderId + "-snoozed")
        val workInfos = withTimeout(TimeUnit.SECONDS.toMillis(5)) {
            while (true) {
                val infos = androidx.work.WorkManager.getInstance(application)
                    .getWorkInfosByTag(snoozedTag)
                    .get()
                if (infos.isNotEmpty()) {
                    return@withTimeout infos
                }
                kotlinx.coroutines.delay(100)
            }
        }
        assertEquals(1, workInfos.size)
        assertTrue(workInfos.single().state == androidx.work.WorkInfo.State.ENQUEUED)
    }
}
