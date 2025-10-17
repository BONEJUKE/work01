package com.example.calendar.data

import java.time.LocalDate
import java.util.UUID

class RoomTaskRepository(private val taskDao: TaskDao) : TaskRepository {
    override fun observeTasksForDay(date: LocalDate) =
        taskDao.observeTasksByPeriod(AgendaPeriod.Day(date))

    override fun observeTasksForWeek(start: LocalDate) =
        taskDao.observeTasksByPeriod(AgendaPeriod.Week(start))

    override fun observeTasksForMonth(year: Int, month: Int) =
        taskDao.observeTasksByPeriod(AgendaPeriod.Month(year, month))

    override suspend fun upsert(task: Task) {
        taskDao.upsert(task)
    }

    override suspend fun toggleStatus(id: UUID) {
        val existing = taskDao.findById(id) ?: return
        taskDao.upsert(existing.toggleCompletion())
    }

    override suspend fun markComplete(id: UUID) {
        val existing = taskDao.findById(id) ?: return
        if (existing.status != TaskStatus.Completed) {
            taskDao.updateStatus(id, TaskStatus.Completed)
        }
    }

    override suspend fun delete(id: UUID) {
        taskDao.deleteById(id)
    }
}
