package com.chronostudy.app.data.repository

import com.chronostudy.app.data.local.dao.DailyGoalDao
import com.chronostudy.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

class GoalRepository(
    private val dao: DailyGoalDao,
) {
    fun observeByDay(epochDay: Long): Flow<DailyGoalEntity?> = dao.observeByDay(epochDay)

    suspend fun getByDay(epochDay: Long): DailyGoalEntity? = dao.getByDay(epochDay)

    suspend fun setTargetMinutes(epochDay: Long, targetMinutes: Int) =
        dao.upsert(DailyGoalEntity(epochDay = epochDay, targetMinutes = targetMinutes.coerceAtLeast(0)))

    suspend fun getAll(): List<DailyGoalEntity> = dao.getAll()
}
