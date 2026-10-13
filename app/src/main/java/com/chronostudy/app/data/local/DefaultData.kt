package com.chronostudy.app.data.local

import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.domain.model.EntryType

/**
 * First-run seed data.
 *
 * Everything here is **editable sample data** ([CourseEntity.isSample] / [SubjectEntity.isSample]
 * and [ChapterEntity.isUnverified]). It is intentionally generic and must never be presented as
 * a verified official syllabus. The subject names come from the initial Chrono Study subject
 * list; chapter names are placeholders the student is expected to rename.
 */
object DefaultData {

    private const val CSE = "Computer Science & Engineering"
    private const val SCHEME = "2020"

    private val baseSubjectNames = listOf(
        "Computer Networking",
        "Operating System",
        "Database Management System (DBMS)",
        "Internet and Web Technology",
        "Concept of Programming Using C",
    )

    private val subjectColors = listOf(
        "#FFB020",
        "#4C8DFF",
        "#2FBF9F",
        "#9B7BFF",
        "#4CC38A",
    )

    suspend fun seedIfEmpty(db: ChronoDatabase) {
        if (db.courseDao().count() > 0 || db.subjectDao().count() > 0) return
        val now = System.currentTimeMillis()

        val normalCourseId = db.courseDao().insert(
            CourseEntity(
                name = "Diploma in $CSE",
                branch = "CSE",
                semester = 4,
                scheme = SCHEME,
                entryType = EntryType.NORMAL,
                notes = "Sample course — edit to match your official programme and semester.",
                sortOrder = 0,
                isSample = true,
                isUnverified = true,
                createdAt = now,
                updatedAt = now,
            ),
        )

        val lateralCourseId = db.courseDao().insert(
            CourseEntity(
                name = "Diploma in $CSE — Lateral Entry",
                branch = "CSE",
                semester = 3,
                scheme = SCHEME,
                entryType = EntryType.LATERAL,
                notes = "Sample lateral-entry course — add or rename subjects to match your syllabus.",
                sortOrder = 1,
                isSample = true,
                isUnverified = true,
                createdAt = now,
                updatedAt = now,
            ),
        )

        seedSubjectsForCourse(db, normalCourseId, 0, now)
        seedSubjectsForCourse(db, lateralCourseId, 100, now)
    }

    private suspend fun seedSubjectsForCourse(
        db: ChronoDatabase,
        courseId: Long,
        sortOffset: Int,
        now: Long,
    ) {
        baseSubjectNames.forEachIndexed { index, name ->
            val subjectId = db.subjectDao().insert(
                SubjectEntity(
                    name = name,
                    description = "Sample subject — edit or delete freely.",
                    colorHex = subjectColors[index % subjectColors.size],
                    iconKey = "book",
                    courseId = courseId,
                    sortOrder = sortOffset + index,
                    isSample = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            seedSampleChapters(db, subjectId, now)
        }
    }

    private suspend fun seedSampleChapters(db: ChronoDatabase, subjectId: Long, now: Long) {
        val chapters = (1..5).map { unit ->
            ChapterEntity(
                subjectId = subjectId,
                name = "Unit $unit",
                unitLabel = "Unit $unit (sample — edit me)",
                sortOrder = unit - 1,
                isUnverified = true,
                notes = "",
                createdAt = now,
                updatedAt = now,
            )
        }
        db.chapterDao().insertAll(chapters)
    }
}
