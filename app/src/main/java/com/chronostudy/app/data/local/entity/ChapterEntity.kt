package com.chronostudy.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chronostudy.app.domain.model.ChapterProgress

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId")],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val unitLabel: String = "",
    val progress: ChapterProgress = ChapterProgress.NOT_STARTED,
    val notes: String = "",
    /** Optional revision reminder as epoch-day, null when unset. */
    val revisionEpochDay: Long? = null,
    val targetSessions: Int? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    /** True when the chapter list is a user-editable placeholder rather than verified official data. */
    val isUnverified: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
