package com.chronostudy.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    /** Local date as epoch-day. */
    @PrimaryKey val epochDay: Long,
    val targetMinutes: Int,
)
