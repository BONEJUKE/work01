package com.example.calendar.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a to-do item that can appear within daily, weekly, or monthly agenda views.
 */
data class Task(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.Pending,
    val dueAt: LocalDateTime? = null,
    val period: AgendaPeriod,
    val reminders: List<Reminder> = emptyList(),
    val tags: Set<String> = emptySet()
) {
    fun toggleCompletion(): Task = copy(
        status = if (status.isDone()) TaskStatus.Pending else TaskStatus.Completed
    )

    fun isOverdue(currentDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
        val deadline = dueAt ?: return false
        return status != TaskStatus.Completed && deadline.isBefore(currentDateTime)
    }
}

sealed class AgendaPeriod {
    data class Day(val date: LocalDate) : AgendaPeriod()
    data class Week(val start: LocalDate) : AgendaPeriod() {
        val end: LocalDate = start.plusDays(6)
    }
    data class Month(val year: Int, val month: Int) : AgendaPeriod() {
        val firstDay: LocalDate = LocalDate.of(year, month, 1)
        val lastDay: LocalDate = firstDay.plusMonths(1).minusDays(1)
    }
}

/**
 * Reminder offset relative to the task's due time.
 */
data class Reminder(
    val minutesBefore: Long,
    val allowSnooze: Boolean = true
)
