package com.chronostudy.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chronostudy.app.domain.model.SessionState

/**
 * A recorded focus session.
 *
 * Elapsed focus time is measured with a monotonic clock ([lastResumeElapsedMillis]) so it never
 * jumps when the device wall clock changes. Wall-clock timestamps are kept for calendar grouping
 * and display only. [lastResumeWallClockMillis] lets the session be reconstructed after a reboot
 * (when the monotonic clock resets).
 */
@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId"), Index("chapterId"), Index("startWallClockMillis")],
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val chapterId: Long? = null,
    val plannedDurationMillis: Long,
    /** Finalized eligible active focus time (excludes the currently running segment and pauses). */
    val actualFocusMillis: Long = 0,
    val startWallClockMillis: Long,
    val endWallClockMillis: Long? = null,
    val accumulatedPausedMillis: Long = 0,
    val state: SessionState = SessionState.RUNNING,
    val note: String = "",
    val plannerTaskId: Long? = null,
    /** elapsedRealtime captured when the current running segment started; null while paused. */
    val lastResumeElapsedMillis: Long? = null,
    /** wall clock captured when the current running segment started; used for reboot recovery. */
    val lastResumeWallClockMillis: Long? = null,
    /** elapsedRealtime captured when the session was paused; null while running. */
    val pausedAtElapsedMillis: Long? = null,
    /** True when the session ended before its planned duration. */
    val finishedEarly: Boolean = false,
    /** True when the session's time was reconstructed after a reboot rather than measured. */
    val recovered: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
