package com.example.calendar.domain

import com.example.calendar.data.PlannerTaskDao
import com.example.calendar.data.PlannerTaskEntity
import kotlinx.coroutines.flow.Flow

class PlannerRepository(private val taskDao: PlannerTaskDao) {
    fun observeTasks(startEpoch: Long, endEpoch: Long): Flow<List<PlannerTaskEntity>> =
        taskDao.observeTasksBetween(startEpoch, endEpoch)

    suspend fun saveTask(task: PlannerTaskEntity): Long = taskDao.upsert(task)

    suspend fun findTaskById(id: Long): PlannerTaskEntity? = taskDao.findById(id)

    suspend fun update(task: PlannerTaskEntity) = taskDao.update(task)
}
