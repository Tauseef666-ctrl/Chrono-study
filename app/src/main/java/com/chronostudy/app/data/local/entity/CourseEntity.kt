package com.chronostudy.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chronostudy.app.domain.model.EntryType

/**
 * A diploma programme / course a student is enrolled in. Subjects reference a course so the
 * library can be grouped by course and semester, including lateral-entry programmes.
 *
 * Seeded courses are editable samples; [isUnverified] marks data that is not confirmed against
 * the official syllabus.
 */
@Entity(
    tableName = "courses",
    indices = [Index(value = ["name"], unique = false)],
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val branch: String = "",
    val semester: Int? = null,
    /** Curriculum scheme / year label, e.g. "2020". */
    val scheme: String = "",
    val entryType: EntryType = EntryType.NORMAL,
    val notes: String = "",
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val isSample: Boolean = false,
    val isUnverified: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
