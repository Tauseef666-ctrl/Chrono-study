package com.chronostudy.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chronostudy.app.domain.model.TaskStatus

@Entity(
    tableName = "planner_tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId"), Index("chapterId"), Index("plannedEpochDay")],
)
data class PlannerTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val chapterId: Long? = null,
    /** Local date as epoch-day (days since 1970-01-01). */
    val plannedEpochDay: Long,
    /** Optional start of day in minutes (0..1439). */
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val plannedDurationMinutes: Int = 0,
    val status: TaskStatus = TaskStatus.PLANNED,
    val note: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
