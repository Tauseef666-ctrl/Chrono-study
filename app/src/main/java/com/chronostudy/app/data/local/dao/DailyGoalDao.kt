package com.chronostudy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chronostudy.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(goals: List<DailyGoalEntity>)

    @Query("SELECT * FROM daily_goals WHERE epochDay = :epochDay")
    suspend fun getByDay(epochDay: Long): DailyGoalEntity?

    @Query("SELECT * FROM daily_goals WHERE epochDay = :epochDay")
    fun observeByDay(epochDay: Long): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals")
    suspend fun getAll(): List<DailyGoalEntity>

    @Query("DELETE FROM daily_goals")
    suspend fun deleteAll()
}
