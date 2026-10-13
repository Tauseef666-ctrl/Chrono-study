package com.chronostudy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.domain.model.ChapterProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Insert
    suspend fun insert(chapter: ChapterEntity): Long

    @Insert
    suspend fun insertAll(chapters: List<ChapterEntity>): List<Long>

    @Update
    suspend fun update(chapter: ChapterEntity)

    @Delete
    suspend fun delete(chapter: ChapterEntity)

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY sortOrder ASC, name ASC")
    fun observeBySubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveBySubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY subjectId ASC, sortOrder ASC")
    fun observeAll(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Long): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE subjectId = :subjectId")
    suspend fun countBySubject(subjectId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM chapters WHERE subjectId = :subjectId")
    suspend fun maxSortOrder(subjectId: Long): Int

    @Query("UPDATE chapters SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAt: Long)

    @Query("UPDATE chapters SET progress = :progress, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setProgress(id: Long, progress: ChapterProgress, updatedAt: Long)

    @Query("UPDATE chapters SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("DELETE FROM chapters")
    suspend fun deleteAll()
}
