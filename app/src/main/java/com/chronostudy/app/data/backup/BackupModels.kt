package com.chronostudy.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appVersion: String = "1.0.0",
    val exportedAt: Long = 0L,
    val courses: List<CourseDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val chapters: List<ChapterDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val plannerTasks: List<PlannerTaskDto> = emptyList(),
    val dailyGoals: List<DailyGoalDto> = emptyList(),
    val settings: SettingsDto = SettingsDto(),
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class CourseDto(
    val id: Long,
    val name: String,
    val branch: String = "",
    val semester: Int? = null,
    val scheme: String = "",
    val entryType: String = "NORMAL",
    val notes: String = "",
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

@Serializable
data class SubjectDto(
    val id: Long,
    val name: String,
    val description: String = "",
    val colorHex: String = "#FFB020",
    val iconKey: String = "book",
    val targetMinutesPerWeek: Int? = null,
    val courseId: Long? = null,
    val semester: Int? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

@Serializable
data class ChapterDto(
    val id: Long,
    val subjectId: Long,
    val name: String,
    val unitLabel: String = "",
    val progress: String = "NOT_STARTED",
    val notes: String = "",
    val revisionEpochDay: Long? = null,
    val targetSessions: Int? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

@Serializable
data class SessionDto(
    val id: Long,
    val subjectId: Long,
    val chapterId: Long? = null,
    val plannedDurationMillis: Long,
    val actualFocusMillis: Long,
    val startWallClockMillis: Long,
    val endWallClockMillis: Long? = null,
    val accumulatedPausedMillis: Long = 0,
    val state: String = "COMPLETED",
    val note: String = "",
    val plannerTaskId: Long? = null,
    val finishedEarly: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class PlannerTaskDto(
    val id: Long,
    val subjectId: Long,
    val chapterId: Long? = null,
    val plannedEpochDay: Long,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val plannedDurationMinutes: Int = 0,
    val status: String = "PLANNED",
    val note: String = "",
    val sortOrder: Int = 0,
)

@Serializable
data class DailyGoalDto(
    val epochDay: Long,
    val targetMinutes: Int,
)

@Serializable
data class SettingsDto(
    val themePreset: String? = null,
    val accent: String? = null,
    val use24Hour: Boolean? = null,
    val showSeconds: Boolean? = null,
    val showDate: Boolean? = null,
    val showBattery: Boolean? = null,
    val showSubjectChapter: Boolean? = null,
    val defaultFocusMinutes: Int? = null,
    val shortBreakMinutes: Int? = null,
    val longBreakMinutes: Int? = null,
    val alertMode: String? = null,
    val focusSoundKey: String? = null,
    val breakSoundKey: String? = null,
    val vibrationPatternKey: String? = null,
    val dailyGoalMinutes: Int? = null,
    val weeklyGoalMinutes: Int? = null,
)
