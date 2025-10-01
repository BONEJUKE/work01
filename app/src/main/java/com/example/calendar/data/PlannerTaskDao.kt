package com.example.calendar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerTaskDao {
    @Query("SELECT * FROM planner_tasks WHERE scheduledAtEpochMillis BETWEEN :startEpoch AND :endEpoch ORDER BY scheduledAtEpochMillis")
    fun observeTasksBetween(startEpoch: Long, endEpoch: Long): Flow<List<PlannerTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: PlannerTaskEntity): Long

    @Update
    suspend fun update(task: PlannerTaskEntity)

    @Query("SELECT * FROM planner_tasks WHERE id = :id")
    suspend fun findById(id: Long): PlannerTaskEntity?

    @Delete
    suspend fun delete(task: PlannerTaskEntity)
}
