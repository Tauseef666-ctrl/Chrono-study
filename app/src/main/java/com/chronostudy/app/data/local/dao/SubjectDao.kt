package com.chronostudy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chronostudy.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Insert
    suspend fun insert(subject: SubjectEntity): Long

    @Insert
    suspend fun insertAll(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE courseId = :courseId AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeByCourse(courseId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC LIMIT 1")
    suspend fun firstActive(): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM subjects WHERE isArchived = 0")
    suspend fun activeCount(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM subjects")
    suspend fun maxSortOrder(): Int

    @Query("UPDATE subjects SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAt: Long)

    @Query("UPDATE subjects SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}
