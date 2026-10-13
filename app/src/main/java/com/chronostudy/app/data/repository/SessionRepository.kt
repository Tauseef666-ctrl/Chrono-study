package com.chronostudy.app.data.repository

import com.chronostudy.app.data.local.dao.StudySessionDao
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.time.TimeProvider
import com.chronostudy.app.domain.timer.SessionTimer
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val dao: StudySessionDao,
    private val timeProvider: TimeProvider,
) {
    val allSessions: Flow<List<StudySessionEntity>> = dao.observeAll()
    val completedSessions: Flow<List<StudySessionEntity>> = dao.observeCompleted()
    val activeSession: Flow<StudySessionEntity?> = dao.observeActiveSession()
    val totalFocusMillis: Flow<Long> = dao.observeTotalFocusMillis()
    val completedCount: Flow<Int> = dao.observeCompletedCount()
    val focusBySubject = dao.observeFocusBySubject()
    val focusByChapter = dao.observeFocusByChapter()

    suspend fun getActiveSession(): StudySessionEntity? = dao.getActiveSession()
    suspend fun getSession(id: Long): StudySessionEntity? = dao.getById(id)
    suspend fun completedCountOnce(): Int = dao.completedCount()

    suspend fun startSession(
        subjectId: Long,
        chapterId: Long?,
        plannedDurationMillis: Long,
        plannerTaskId: Long? = null,
        note: String = "",
    ): Long {
        val now = timeProvider.wallClockMillis()
        val elapsed = timeProvider.elapsedRealtimeMillis()
        val session = SessionTimer.start(
            subjectId = subjectId,
            chapterId = chapterId,
            plannedDurationMillis = plannedDurationMillis,
            nowElapsedRealtime = elapsed,
            nowWallClock = now,
            plannerTaskId = plannerTaskId,
            note = note,
        )
        return dao.insert(session)
    }

    suspend fun pause(id: Long): Boolean {
        val session = dao.getById(id) ?: return false
        if (session.state != SessionState.RUNNING) return false
        dao.update(
            SessionTimer.pause(
                session,
                timeProvider.elapsedRealtimeMillis(),
                timeProvider.wallClockMillis(),
            ),
        )
        return true
    }

    suspend fun resume(id: Long): Boolean {
        val session = dao.getById(id) ?: return false
        if (session.state != SessionState.PAUSED) return false
        dao.update(
            SessionTimer.resume(
                session,
                timeProvider.elapsedRealtimeMillis(),
                timeProvider.wallClockMillis(),
            ),
        )
        return true
    }

    /**
     * Finalises a session exactly once. Safe to call from the UI, an alarm and a service
     * concurrently: once the session is final it is returned unchanged and never re-recorded.
     *
     * @param plannedReached true when the planned duration elapsed; false records the real
     *   elapsed time (finish-early).
     */
    suspend fun finishSession(id: Long, plannedReached: Boolean): StudySessionEntity? {
        val session = dao.getById(id) ?: return null
        if (session.state.isFinal || !session.state.isActive) return session
        val finished = SessionTimer.finish(
            session,
            timeProvider.elapsedRealtimeMillis(),
            timeProvider.wallClockMillis(),
            plannedReached,
        )
        dao.update(finished)
        return finished
    }

    suspend fun finishEarly(id: Long): StudySessionEntity? = finishSession(id, plannedReached = false)

    suspend fun completeNaturally(id: Long): StudySessionEntity? = finishSession(id, plannedReached = true)

    /** Cancels a session and discards its partial time (never awards planned duration). */
    suspend fun cancel(id: Long): Boolean {
        val session = dao.getById(id) ?: return false
        if (!session.state.isActive) return false
        dao.update(SessionTimer.cancel(session, timeProvider.wallClockMillis()))
        return true
    }

    suspend fun updateSession(session: StudySessionEntity) =
        dao.update(session.copy(updatedAt = timeProvider.wallClockMillis()))

    suspend fun deleteSession(id: Long) = dao.deleteById(id)

    /** Corrects the subject/chapter attribution of a saved session. */
    suspend fun reassignSession(id: Long, subjectId: Long, chapterId: Long?) {
        val session = dao.getById(id) ?: return
        dao.update(
            session.copy(
                subjectId = subjectId,
                chapterId = chapterId,
                updatedAt = timeProvider.wallClockMillis(),
            ),
        )
    }

    suspend fun deleteAll() = dao.deleteAll()
}
