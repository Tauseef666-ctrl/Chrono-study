package com.chronostudy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chronostudy.app.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

data class SubjectFocusTotal(
    val subjectId: Long,
    val totalMillis: Long,
)

data class ChapterFocusTotal(
    val chapterId: Long,
    val totalMillis: Long,
)

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insert(session: StudySessionEntity): Long

    @Update
    suspend fun update(session: StudySessionEntity)

    @Delete
    suspend fun delete(session: StudySessionEntity)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getById(id: Long): StudySessionEntity?

    @Query("SELECT * FROM study_sessions ORDER BY startWallClockMillis DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE state = 'COMPLETED' ORDER BY startWallClockMillis DESC")
    fun observeCompleted(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE state IN ('RUNNING','PAUSED') ORDER BY startWallClockMillis DESC LIMIT 1")
    suspend fun getActiveSession(): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE state IN ('RUNNING','PAUSED') ORDER BY startWallClockMillis DESC LIMIT 1")
    fun observeActiveSession(): Flow<StudySessionEntity?>

    @Query("SELECT * FROM study_sessions WHERE state = 'COMPLETED' AND startWallClockMillis >= :startMillis AND startWallClockMillis < :endMillis ORDER BY startWallClockMillis DESC")
    suspend fun getCompletedBetween(startMillis: Long, endMillis: Long): List<StudySessionEntity>

    @Query("SELECT COALESCE(SUM(actualFocusMillis), 0) FROM study_sessions WHERE state = 'COMPLETED'")
    fun observeTotalFocusMillis(): Flow<Long>

    @Query("SELECT COALESCE(SUM(actualFocusMillis), 0) FROM study_sessions WHERE state = 'COMPLETED' AND startWallClockMillis >= :startMillis AND startWallClockMillis < :endMillis")
    fun observeFocusMillisBetween(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT subjectId, COALESCE(SUM(actualFocusMillis), 0) AS totalMillis FROM study_sessions WHERE state = 'COMPLETED' GROUP BY subjectId")
    fun observeFocusBySubject(): Flow<List<SubjectFocusTotal>>

    @Query("SELECT chapterId, COALESCE(SUM(actualFocusMillis), 0) AS totalMillis FROM study_sessions WHERE state = 'COMPLETED' AND chapterId IS NOT NULL GROUP BY chapterId")
    fun observeFocusByChapter(): Flow<List<ChapterFocusTotal>>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE state = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE state = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Query("SELECT COALESCE(SUM(actualFocusMillis), 0) FROM study_sessions WHERE state = 'COMPLETED'")
    suspend fun totalFocusMillis(): Long

    @Query("SELECT MAX(actualFocusMillis) FROM study_sessions WHERE state = 'COMPLETED'")
    suspend fun longestSessionMillis(): Long?

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}
