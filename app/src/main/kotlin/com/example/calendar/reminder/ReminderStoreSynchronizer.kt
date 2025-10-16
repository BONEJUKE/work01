package com.example.calendar.reminder

import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Keeps the reminder scheduler and persisted SharedPreferences store in sync with
 * the authoritative Room database. This enables reminders to stay accurate when
 * data is modified by background sync jobs, multiple devices, or different user
 * accounts.
 */
class ReminderStoreSynchronizer(
    taskStream: Flow<List<Task>>,
    eventStream: Flow<List<CalendarEvent>>,
    private val orchestrator: ReminderOrchestrator,
    private val store: ReminderStore,
    scope: CoroutineScope
) {
    private val taskFlow = taskStream
        .map { tasks -> tasks.sortedBy { it.id } }
        .distinctUntilChanged()

    private val eventFlow = eventStream
        .map { events -> events.sortedBy { it.id } }
        .distinctUntilChanged()

    init {
        combine(taskFlow, eventFlow, ::Pair)
            .onEach { (tasks, events) -> synchronize(tasks, events) }
            .launchIn(scope)
    }

    private fun synchronize(tasks: List<Task>, events: List<CalendarEvent>) {
        val activeBaseIds = mutableSetOf<String>()

        tasks.forEach { task ->
            val hasActiveSchedule = orchestrator.ensureScheduledForTask(task)
            if (hasActiveSchedule) {
                activeBaseIds += "task-${task.id}"
            }
        }

        events.forEach { event ->
            val hasActiveSchedule = orchestrator.ensureScheduledForEvent(event)
            if (hasActiveSchedule) {
                activeBaseIds += "event-${event.id}"
            }
        }

        store.readAll()
            .keys
            .filter { it.startsWith("task-") || it.startsWith("event-") }
            .filterNot { it in activeBaseIds }
            .forEach { baseId -> orchestrator.cancelByBaseId(baseId) }
    }
}
