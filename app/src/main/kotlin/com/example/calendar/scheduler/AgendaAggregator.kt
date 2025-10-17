package com.example.calendar.scheduler

import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.EventRepository
import com.example.calendar.data.Recurrence
import com.example.calendar.data.RecurrenceRule
import com.example.calendar.data.Task
import com.example.calendar.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Produces a combined agenda of events and tasks for a selected period.
 */
class AgendaAggregator(
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository
) {
    fun observeAgenda(period: AgendaPeriod): Flow<AgendaSnapshot> = when (period) {
        is AgendaPeriod.Day -> observeDay(period.date)
        is AgendaPeriod.Week -> observeWeek(period.start)
        is AgendaPeriod.Month -> observeMonth(period.year, period.month)
    }

    private fun observeDay(date: LocalDate): Flow<AgendaSnapshot> = combine(
        taskRepository.observeTasksForDay(date),
        eventRepository.observeEventsForDay(date)
    ) { tasks, events ->
            AgendaSnapshot(
                rangeStart = date,
                tasks = tasks,
                events = events.expandRecurringInstances(date, date)
            )
    }

    private fun observeWeek(start: LocalDate): Flow<AgendaSnapshot> {
        val end = start.plusDays(6)
        return combine(
            taskRepository.observeTasksForWeek(start),
            eventRepository.observeEventsForRange(start, end)
        ) { tasks, events ->
            AgendaSnapshot(
                rangeStart = start,
                tasks = tasks,
                events = events.expandRecurringInstances(start, end)
            )
        }.map { snapshot -> snapshot.copy(rangeEnd = end) }
    }

    private fun observeMonth(year: Int, month: Int): Flow<AgendaSnapshot> {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        return combine(
            taskRepository.observeTasksForMonth(year, month),
            eventRepository.observeEventsForRange(firstDay, lastDay)
        ) { tasks, events ->
            AgendaSnapshot(
                rangeStart = firstDay,
                tasks = tasks,
                events = events.expandRecurringInstances(firstDay, lastDay)
            )
        }.map { snapshot -> snapshot.copy(rangeEnd = lastDay) }
    }
}

data class AgendaSnapshot(
    val rangeStart: LocalDate,
    val tasks: List<Task>,
    val events: List<CalendarEvent>,
    val rangeEnd: LocalDate = rangeStart
) {
    val overdueTasks: List<Task> = tasks.filter { it.isOverdue() }
    val completedCount: Int = tasks.count { it.status.isDone() }
    val pendingCount: Int = tasks.size - completedCount
    val conflictingEventIds: Set<UUID> = events.resolveConflictingEventIds()
}

private fun List<CalendarEvent>.resolveConflictingEventIds(): Set<UUID> {
    if (size <= 1) return emptySet()

    val sortedByStart = sortedBy { it.start }
    val conflicts = mutableSetOf<UUID>()

    for (index in sortedByStart.indices) {
        val current = sortedByStart[index]
        for (otherIndex in index + 1 until sortedByStart.size) {
            val other = sortedByStart[otherIndex]
            if (!current.end.isAfter(other.start)) {
                break
            }

            if (current.start.isBefore(other.end) && other.start.isBefore(current.end)) {
                conflicts += current.id
                conflicts += other.id
            }
        }
    }

    return conflicts
}

private fun List<CalendarEvent>.expandRecurringInstances(
    rangeStart: LocalDate,
    rangeEnd: LocalDate
): List<CalendarEvent> {
    if (isEmpty()) return emptyList()

    val startBoundary = rangeStart.atStartOfDay()
    val endBoundaryExclusive = rangeEnd.plusDays(1).atStartOfDay()
    val expanded = mutableListOf<CalendarEvent>()

    for (event in this) {
        val recurrence = event.recurrence
        if (recurrence == null) {
            if (event.end > startBoundary && event.start < endBoundaryExclusive) {
                expanded += event
            }
            continue
        }

        val interval = recurrence.interval.coerceAtLeast(1)
        val maxOccurrences = recurrence.occurrences?.coerceAtLeast(1) ?: Int.MAX_VALUE
        val indexOffset = firstRelevantOccurrenceIndex(event.start, startBoundary, recurrence.rule, interval)
            .coerceAtMost(maxOccurrences - 1)

        var occurrenceIndex = indexOffset
        var currentStart = addOccurrences(event.start, recurrence.rule, interval, occurrenceIndex)
        var currentEnd = addOccurrences(event.end, recurrence.rule, interval, occurrenceIndex)

        // Skip occurrences that still end before the visible window.
        while (occurrenceIndex < maxOccurrences && currentEnd <= startBoundary) {
            occurrenceIndex += 1
            if (occurrenceIndex >= maxOccurrences) break
            currentStart = addOccurrences(event.start, recurrence.rule, interval, occurrenceIndex)
            currentEnd = addOccurrences(event.end, recurrence.rule, interval, occurrenceIndex)
        }

        while (occurrenceIndex < maxOccurrences && currentStart < endBoundaryExclusive) {
            if (currentEnd > startBoundary) {
                val override = event.recurrenceExceptions.firstOrNull { exception ->
                    exception.matches(currentStart)
                }
                val occurrence = if (override != null) {
                    event.applyOverride(override, currentStart, currentEnd)
                } else {
                    event.copy(start = currentStart, end = currentEnd)
                }
                if (occurrence != null) {
                    expanded += occurrence
                }
            }
            occurrenceIndex += 1
            if (occurrenceIndex >= maxOccurrences) break
            currentStart = addOccurrences(event.start, recurrence.rule, interval, occurrenceIndex)
            currentEnd = addOccurrences(event.end, recurrence.rule, interval, occurrenceIndex)
        }
    }

    return expanded.sortedBy { it.start }
}

private fun firstRelevantOccurrenceIndex(
    eventStart: LocalDateTime,
    rangeStart: LocalDateTime,
    rule: RecurrenceRule,
    interval: Int
): Int {
    if (!eventStart.isBefore(rangeStart)) return 0

    val unitsBetween = when (rule) {
        RecurrenceRule.Daily -> ChronoUnit.DAYS.between(eventStart, rangeStart)
        RecurrenceRule.Weekly -> ChronoUnit.WEEKS.between(eventStart, rangeStart)
        RecurrenceRule.Monthly -> ChronoUnit.MONTHS.between(eventStart, rangeStart)
        RecurrenceRule.Yearly -> ChronoUnit.YEARS.between(eventStart, rangeStart)
    }

    if (unitsBetween <= 0) return 0

    val intervalLong = interval.toLong()
    val tentativeIndex = (unitsBetween / intervalLong).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return tentativeIndex.coerceAtLeast(0)
}

private fun addOccurrences(
    dateTime: LocalDateTime,
    rule: RecurrenceRule,
    interval: Int,
    occurrences: Int
): LocalDateTime {
    if (occurrences <= 0) return dateTime

    val step = occurrences.toLong() * interval.coerceAtLeast(1)
    return when (rule) {
        RecurrenceRule.Daily -> dateTime.plusDays(step)
        RecurrenceRule.Weekly -> dateTime.plusWeeks(step)
        RecurrenceRule.Monthly -> dateTime.plusMonths(step)
        RecurrenceRule.Yearly -> dateTime.plusYears(step)
    }
}
