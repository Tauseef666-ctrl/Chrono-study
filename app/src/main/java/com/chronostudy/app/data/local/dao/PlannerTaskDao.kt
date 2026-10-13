package com.chronostudy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chronostudy.app.data.local.entity.PlannerTaskEntity
import com.chronostudy.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerTaskDao {
    @Insert
    suspend fun insert(task: PlannerTaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<PlannerTaskEntity>): List<Long>

    @Update
    suspend fun update(task: PlannerTaskEntity)

    @Delete
    suspend fun delete(task: PlannerTaskEntity)

    @Query("SELECT * FROM planner_tasks WHERE id = :id")
    suspend fun getById(id: Long): PlannerTaskEntity?

    @Query("SELECT * FROM planner_tasks WHERE plannedEpochDay = :epochDay ORDER BY sortOrder ASC, startMinuteOfDay ASC")
    fun observeByDay(epochDay: Long): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE plannedEpochDay BETWEEN :startDay AND :endDay ORDER BY plannedEpochDay ASC, sortOrder ASC")
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<PlannerTaskEntity>>

    @Query("SELECT * FROM planner_tasks WHERE status != 'COMPLETED' AND status != 'SKIPPED' AND plannedEpochDay >= :fromDay ORDER BY plannedEpochDay ASC, startMinuteOfDay ASC LIMIT 1")
    fun observeNextUpcoming(fromDay: Long): Flow<PlannerTaskEntity?>

    @Query("SELECT * FROM planner_tasks WHERE plannedEpochDay = :epochDay ORDER BY sortOrder ASC")
    suspend fun getByDay(epochDay: Long): List<PlannerTaskEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM planner_tasks WHERE plannedEpochDay = :epochDay")
    suspend fun maxSortOrder(epochDay: Long): Int

    @Query("UPDATE planner_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: TaskStatus, updatedAt: Long)

    @Query("UPDATE planner_tasks SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("DELETE FROM planner_tasks")
    suspend fun deleteAll()
}
