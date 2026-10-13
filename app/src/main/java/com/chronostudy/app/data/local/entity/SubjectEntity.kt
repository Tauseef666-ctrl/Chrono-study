package com.chronostudy.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["name"], unique = false), Index("courseId")],
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#FFB020",
    val iconKey: String = "book",
    val targetMinutesPerWeek: Int? = null,
    /** Optional owning course / programme. Null means "unassigned". */
    val courseId: Long? = null,
    val semester: Int? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    /** True when this subject is seeded sample data that the user can freely edit or delete. */
    val isSample: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
