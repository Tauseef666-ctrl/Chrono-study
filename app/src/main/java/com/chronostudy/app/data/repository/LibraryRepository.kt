package com.chronostudy.app.data.repository

import com.chronostudy.app.data.local.dao.ChapterDao
import com.chronostudy.app.data.local.dao.CourseDao
import com.chronostudy.app.data.local.dao.SubjectDao
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.domain.model.ChapterProgress
import com.chronostudy.app.domain.model.EntryType
import com.chronostudy.app.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow

class LibraryRepository(
    private val courseDao: CourseDao,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val timeProvider: TimeProvider,
) {
    val courses: Flow<List<CourseEntity>> = courseDao.observeActive()
    val allCourses: Flow<List<CourseEntity>> = courseDao.observeAll()
    val subjects: Flow<List<SubjectEntity>> = subjectDao.observeActive()
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.observeAll()
    val chapters: Flow<List<ChapterEntity>> = chapterDao.observeAll()

    fun chaptersFor(subjectId: Long): Flow<List<ChapterEntity>> =
        chapterDao.observeActiveBySubject(subjectId)

    suspend fun getSubject(id: Long): SubjectEntity? = subjectDao.getById(id)
    suspend fun getChapter(id: Long): ChapterEntity? = chapterDao.getById(id)
    suspend fun getCourse(id: Long): CourseEntity? = courseDao.getById(id)
    suspend fun firstActiveSubject(): SubjectEntity? = subjectDao.firstActive()

    // ---- Courses -------------------------------------------------------------------------

    suspend fun addCourse(
        name: String,
        branch: String = "",
        semester: Int? = null,
        scheme: String = "",
        entryType: EntryType = EntryType.NORMAL,
        notes: String = "",
    ): Long {
        val now = timeProvider.wallClockMillis()
        return courseDao.insert(
            CourseEntity(
                name = name.trim(),
                branch = branch.trim(),
                semester = semester,
                scheme = scheme.trim(),
                entryType = entryType,
                notes = notes,
                sortOrder = courseDao.maxSortOrder() + 1,
                isSample = false,
                isUnverified = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateCourse(course: CourseEntity) =
        courseDao.update(course.copy(updatedAt = timeProvider.wallClockMillis()))

    suspend fun deleteCourse(id: Long) {
        courseDao.getById(id)?.let { courseDao.delete(it) }
    }

    suspend fun setCourseArchived(id: Long, archived: Boolean) =
        courseDao.setArchived(id, archived, timeProvider.wallClockMillis())

    // ---- Subjects ------------------------------------------------------------------------

    suspend fun addSubject(
        name: String,
        description: String = "",
        colorHex: String = "#FFB020",
        iconKey: String = "book",
        courseId: Long? = null,
        semester: Int? = null,
        targetMinutesPerWeek: Int? = null,
    ): Long {
        val now = timeProvider.wallClockMillis()
        return subjectDao.insert(
            SubjectEntity(
                name = name.trim(),
                description = description,
                colorHex = colorHex,
                iconKey = iconKey,
                courseId = courseId,
                semester = semester,
                targetMinutesPerWeek = targetMinutesPerWeek,
                sortOrder = subjectDao.maxSortOrder() + 1,
                isSample = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateSubject(subject: SubjectEntity) =
        subjectDao.update(subject.copy(updatedAt = timeProvider.wallClockMillis()))

    suspend fun deleteSubject(id: Long) {
        subjectDao.getById(id)?.let { subjectDao.delete(it) }
    }

    suspend fun setSubjectArchived(id: Long, archived: Boolean) =
        subjectDao.setArchived(id, archived, timeProvider.wallClockMillis())

    suspend fun reorderSubjects(orderedIds: List<Long>) {
        val now = timeProvider.wallClockMillis()
        orderedIds.forEachIndexed { index, id -> subjectDao.setSortOrder(id, index, now) }
    }

    // ---- Chapters ------------------------------------------------------------------------

    suspend fun addChapter(
        subjectId: Long,
        name: String,
        unitLabel: String = "",
        notes: String = "",
        progress: ChapterProgress = ChapterProgress.NOT_STARTED,
        targetSessions: Int? = null,
    ): Long {
        val now = timeProvider.wallClockMillis()
        return chapterDao.insert(
            ChapterEntity(
                subjectId = subjectId,
                name = name.trim(),
                unitLabel = unitLabel,
                notes = notes,
                progress = progress,
                targetSessions = targetSessions,
                sortOrder = chapterDao.maxSortOrder(subjectId) + 1,
                isUnverified = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateChapter(chapter: ChapterEntity) =
        chapterDao.update(chapter.copy(updatedAt = timeProvider.wallClockMillis()))

    suspend fun deleteChapter(id: Long) {
        chapterDao.getById(id)?.let { chapterDao.delete(it) }
    }

    suspend fun setChapterArchived(id: Long, archived: Boolean) =
        chapterDao.setArchived(id, archived, timeProvider.wallClockMillis())

    suspend fun setChapterProgress(id: Long, progress: ChapterProgress) =
        chapterDao.setProgress(id, progress, timeProvider.wallClockMillis())

    suspend fun reorderChapters(orderedIds: List<Long>) {
        val now = timeProvider.wallClockMillis()
        orderedIds.forEachIndexed { index, id -> chapterDao.setSortOrder(id, index, now) }
    }
}
