package com.chronostudy.app.data.backup

import androidx.room.withTransaction
import com.chronostudy.app.data.local.ChronoDatabase
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.DailyGoalEntity
import com.chronostudy.app.data.local.entity.PlannerTaskEntity
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.GoalRepository
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.model.ChapterProgress
import com.chronostudy.app.domain.model.EntryType
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.model.TaskStatus
import com.chronostudy.app.ui.theme.Accent
import com.chronostudy.app.ui.theme.ThemePreset
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

enum class ImportStrategy { MERGE, REPLACE }

data class ImportResult(
    val success: Boolean,
    val message: String,
    val courses: Int = 0,
    val subjects: Int = 0,
    val chapters: Int = 0,
    val sessions: Int = 0,
    val plannerTasks: Int = 0,
    val dailyGoals: Int = 0,
)

class BackupRepository(
    private val database: ChronoDatabase,
    private val library: LibraryRepository,
    private val sessions: SessionRepository,
    private val planner: PlannerRepository,
    private val goals: GoalRepository,
    private val settings: SettingsRepository,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(): String {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            courses = library.allCourses.first().map { it.toDto() },
            subjects = library.allSubjects.first().map { it.toDto() },
            chapters = library.chapters.first().map { it.toDto() },
            sessions = sessions.allSessions.first().map { it.toDto() },
            plannerTasks = planner.observeBetween(Long.MIN_VALUE, Long.MAX_VALUE).first().map { it.toDto() },
            dailyGoals = goals.getAll().map { DailyGoalDto(it.epochDay, it.targetMinutes) },
            settings = settings.settings.first().toDto(),
        )
        return json.encodeToString(BackupData.serializer(), data)
    }

    suspend fun importJson(raw: String, strategy: ImportStrategy): ImportResult {
        val data = try {
            json.decodeFromString(BackupData.serializer(), raw)
        } catch (e: Exception) {
            return ImportResult(false, "Could not read backup file: ${e.message ?: "invalid format"}")
        }

        if (data.formatVersion > BackupData.CURRENT_FORMAT_VERSION) {
            return ImportResult(false, "Backup was created by a newer app version (v${data.formatVersion}).")
        }
        val validationError = validate(data)
        if (validationError != null) return ImportResult(false, validationError)

        return try {
            database.withTransaction {
                if (strategy == ImportStrategy.REPLACE) {
                    database.studySessionDao().deleteAll()
                    database.plannerTaskDao().deleteAll()
                    database.dailyGoalDao().deleteAll()
                    database.chapterDao().deleteAll()
                    database.subjectDao().deleteAll()
                    database.courseDao().deleteAll()
                }
                insertAll(data, strategy)
            }
            applySettings(data.settings)
            ImportResult(
                success = true,
                message = if (strategy == ImportStrategy.REPLACE) "Data replaced." else "Data merged.",
                courses = data.courses.size,
                subjects = data.subjects.size,
                chapters = data.chapters.size,
                sessions = data.sessions.size,
                plannerTasks = data.plannerTasks.size,
                dailyGoals = data.dailyGoals.size,
            )
        } catch (e: Exception) {
            ImportResult(false, "Import failed and was rolled back: ${e.message ?: "unknown error"}")
        }
    }

    private fun validate(data: BackupData): String? {
        if (data.subjects.any { it.name.isBlank() }) return "Backup contains a subject with no name."
        if (data.sessions.any { it.plannedDurationMillis < 0 || it.actualFocusMillis < 0 }) {
            return "Backup contains a session with a negative duration."
        }
        if (data.chapters.any { it.name.isBlank() }) return "Backup contains a chapter with no name."
        return null
    }

    private suspend fun insertAll(data: BackupData, strategy: ImportStrategy) {
        val courseIdMap = HashMap<Long, Long>()
        val subjectIdMap = HashMap<Long, Long>()
        val chapterIdMap = HashMap<Long, Long>()

        data.courses.forEach { dto ->
            val id = database.courseDao().insert(
                CourseEntity(
                    id = if (strategy == ImportStrategy.REPLACE) dto.id else 0,
                    name = dto.name,
                    branch = dto.branch,
                    semester = dto.semester,
                    scheme = dto.scheme,
                    entryType = runCatching { EntryType.valueOf(dto.entryType) }.getOrDefault(EntryType.NORMAL),
                    notes = dto.notes,
                    sortOrder = dto.sortOrder,
                    isArchived = dto.isArchived,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            courseIdMap[dto.id] = id
        }

        data.subjects.forEach { dto ->
            val id = database.subjectDao().insert(
                SubjectEntity(
                    id = if (strategy == ImportStrategy.REPLACE) dto.id else 0,
                    name = dto.name,
                    description = dto.description,
                    colorHex = dto.colorHex,
                    iconKey = dto.iconKey,
                    targetMinutesPerWeek = dto.targetMinutesPerWeek,
                    courseId = dto.courseId?.let { courseIdMap[it] ?: it },
                    semester = dto.semester,
                    sortOrder = dto.sortOrder,
                    isArchived = dto.isArchived,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            subjectIdMap[dto.id] = id
        }

        data.chapters.forEach { dto ->
            val mappedSubject = subjectIdMap[dto.subjectId] ?: return@forEach
            val id = database.chapterDao().insert(
                ChapterEntity(
                    id = if (strategy == ImportStrategy.REPLACE) dto.id else 0,
                    subjectId = mappedSubject,
                    name = dto.name,
                    unitLabel = dto.unitLabel,
                    progress = runCatching { ChapterProgress.valueOf(dto.progress) }
                        .getOrDefault(ChapterProgress.NOT_STARTED),
                    notes = dto.notes,
                    revisionEpochDay = dto.revisionEpochDay,
                    targetSessions = dto.targetSessions,
                    sortOrder = dto.sortOrder,
                    isArchived = dto.isArchived,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            chapterIdMap[dto.id] = id
        }

        data.sessions.forEach { dto ->
            val mappedSubject = subjectIdMap[dto.subjectId] ?: return@forEach
            database.studySessionDao().insert(
                StudySessionEntity(
                    id = if (strategy == ImportStrategy.REPLACE) dto.id else 0,
                    subjectId = mappedSubject,
                    chapterId = dto.chapterId?.let { chapterIdMap[it] },
                    plannedDurationMillis = dto.plannedDurationMillis,
                    actualFocusMillis = dto.actualFocusMillis,
                    startWallClockMillis = dto.startWallClockMillis,
                    endWallClockMillis = dto.endWallClockMillis,
                    accumulatedPausedMillis = dto.accumulatedPausedMillis,
                    state = runCatching { SessionState.valueOf(dto.state) }.getOrDefault(SessionState.COMPLETED),
                    note = dto.note,
                    finishedEarly = dto.finishedEarly,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                ),
            )
        }

        data.plannerTasks.forEach { dto ->
            val mappedSubject = subjectIdMap[dto.subjectId] ?: return@forEach
            database.plannerTaskDao().insert(
                PlannerTaskEntity(
                    id = if (strategy == ImportStrategy.REPLACE) dto.id else 0,
                    subjectId = mappedSubject,
                    chapterId = dto.chapterId?.let { chapterIdMap[it] },
                    plannedEpochDay = dto.plannedEpochDay,
                    startMinuteOfDay = dto.startMinuteOfDay,
                    endMinuteOfDay = dto.endMinuteOfDay,
                    plannedDurationMinutes = dto.plannedDurationMinutes,
                    status = runCatching { TaskStatus.valueOf(dto.status) }.getOrDefault(TaskStatus.PLANNED),
                    note = dto.note,
                    sortOrder = dto.sortOrder,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        data.dailyGoals.forEach { dto ->
            database.dailyGoalDao().upsert(DailyGoalEntity(dto.epochDay, dto.targetMinutes))
        }
    }

    private suspend fun applySettings(dto: SettingsDto) {
        dto.themePreset?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() }?.let { settings.setTheme(it) }
        if (dto.accent != null) {
            val accent = if (dto.accent.isBlank()) null else runCatching { Accent.valueOf(dto.accent) }.getOrNull()
            settings.setAccent(accent)
        }
        dto.use24Hour?.let { settings.set24Hour(it) }
        dto.showSeconds?.let { settings.setShowSeconds(it) }
        dto.showDate?.let { settings.setShowDate(it) }
        dto.showBattery?.let { settings.setShowBattery(it) }
        dto.showSubjectChapter?.let { settings.setShowSubjectChapter(it) }
        dto.defaultFocusMinutes?.let { settings.setDefaultFocusMinutes(it) }
        dto.shortBreakMinutes?.let { settings.setShortBreakMinutes(it) }
        dto.longBreakMinutes?.let { settings.setLongBreakMinutes(it) }
        dto.dailyGoalMinutes?.let { settings.setDailyGoalMinutes(it) }
        dto.weeklyGoalMinutes?.let { settings.setWeeklyGoalMinutes(it) }
        dto.focusSoundKey?.let { settings.setFocusSoundKey(it) }
        dto.breakSoundKey?.let { settings.setBreakSoundKey(it) }
        dto.vibrationPatternKey?.let { settings.setVibrationPatternKey(it) }
    }
}

private fun CourseEntity.toDto() = CourseDto(
    id, name, branch, semester, scheme, entryType.name, notes, sortOrder, isArchived,
)

private fun SubjectEntity.toDto() = SubjectDto(
    id, name, description, colorHex, iconKey, targetMinutesPerWeek, courseId, semester, sortOrder, isArchived,
)

private fun ChapterEntity.toDto() = ChapterDto(
    id, subjectId, name, unitLabel, progress.name, notes, revisionEpochDay, targetSessions, sortOrder, isArchived,
)

private fun StudySessionEntity.toDto() = SessionDto(
    id, subjectId, chapterId, plannedDurationMillis, actualFocusMillis, startWallClockMillis,
    endWallClockMillis, accumulatedPausedMillis, state.name, note, plannerTaskId, finishedEarly, createdAt, updatedAt,
)

private fun PlannerTaskEntity.toDto() = PlannerTaskDto(
    id, subjectId, chapterId, plannedEpochDay, startMinuteOfDay, endMinuteOfDay,
    plannedDurationMinutes, status.name, note, sortOrder,
)

private fun com.chronostudy.app.data.settings.ChronoSettings.toDto() = SettingsDto(
    themePreset = themePreset.name,
    accent = accent?.name,
    use24Hour = use24Hour,
    showSeconds = showSeconds,
    showDate = showDate,
    showBattery = showBattery,
    showSubjectChapter = showSubjectChapter,
    defaultFocusMinutes = defaultFocusMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    alertMode = alertMode.name,
    focusSoundKey = focusSoundKey,
    breakSoundKey = breakSoundKey,
    vibrationPatternKey = vibrationPatternKey,
    dailyGoalMinutes = dailyGoalMinutes,
    weeklyGoalMinutes = weeklyGoalMinutes,
)
