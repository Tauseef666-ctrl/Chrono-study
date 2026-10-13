package com.chronostudy.app.data.repository

import com.chronostudy.app.data.local.dao.PlannerTaskDao
import com.chronostudy.app.data.local.entity.PlannerTaskEntity
import com.chronostudy.app.domain.model.TaskStatus
import com.chronostudy.app.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow

class PlannerRepository(
    private val dao: PlannerTaskDao,
    private val timeProvider: TimeProvider,
) {
    fun observeByDay(epochDay: Long): Flow<List<PlannerTaskEntity>> = dao.observeByDay(epochDay)
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<PlannerTaskEntity>> =
        dao.observeBetween(startDay, endDay)

    fun observeNextUpcoming(fromDay: Long): Flow<PlannerTaskEntity?> = dao.observeNextUpcoming(fromDay)

    suspend fun getById(id: Long): PlannerTaskEntity? = dao.getById(id)
    suspend fun getByDay(epochDay: Long): List<PlannerTaskEntity> = dao.getByDay(epochDay)

    suspend fun addTask(
        subjectId: Long,
        chapterId: Long?,
        plannedEpochDay: Long,
        startMinuteOfDay: Int? = null,
        endMinuteOfDay: Int? = null,
        plannedDurationMinutes: Int = 0,
        note: String = "",
    ): Long {
        val now = timeProvider.wallClockMillis()
        return dao.insert(
            PlannerTaskEntity(
                subjectId = subjectId,
                chapterId = chapterId,
                plannedEpochDay = plannedEpochDay,
                startMinuteOfDay = startMinuteOfDay,
                endMinuteOfDay = endMinuteOfDay,
                plannedDurationMinutes = plannedDurationMinutes,
                note = note,
                sortOrder = dao.maxSortOrder(plannedEpochDay) + 1,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateTask(task: PlannerTaskEntity) =
        dao.update(task.copy(updatedAt = timeProvider.wallClockMillis()))

    suspend fun deleteTask(task: PlannerTaskEntity) = dao.delete(task)

    suspend fun setStatus(id: Long, status: TaskStatus) =
        dao.setStatus(id, status, timeProvider.wallClockMillis())

    suspend fun moveToDay(id: Long, epochDay: Long) {
        val task = dao.getById(id) ?: return
        dao.update(
            task.copy(
                plannedEpochDay = epochDay,
                sortOrder = dao.maxSortOrder(epochDay) + 1,
                updatedAt = timeProvider.wallClockMillis(),
            ),
        )
    }

    suspend fun reorder(orderedIds: List<Long>) {
        val now = timeProvider.wallClockMillis()
        orderedIds.forEachIndexed { index, id -> dao.setSortOrder(id, index, now) }
    }

    suspend fun deleteAll() = dao.deleteAll()
}
