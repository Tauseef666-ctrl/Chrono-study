package com.chronostudy.app.domain.timer

import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.domain.model.SessionState

/**
 * Pure, testable timer maths and state transitions.
 *
 * All elapsed time is derived from the monotonic clock captured on the session. Wall-clock time
 * is only used as a fallback after a reboot (when the monotonic clock resets). Nothing here
 * decrements a mutable counter, so the result is identical whether the app is foregrounded,
 * rotated, backgrounded or reconstructed.
 */
object SessionTimer {

    /** Elapsed eligible focus time, including the currently running segment if any. */
    fun liveElapsedFocusMillis(
        session: StudySessionEntity,
        nowElapsedRealtime: Long,
        nowWallClock: Long,
    ): Long {
        val base = session.actualFocusMillis
        if (session.state != SessionState.RUNNING) return base
        val resumeElapsed = session.lastResumeElapsedMillis ?: return base
        val delta = if (nowElapsedRealtime >= resumeElapsed) {
            nowElapsedRealtime - resumeElapsed
        } else {
            // Monotonic clock went backwards -> device rebooted. Fall back to wall clock.
            val resumeWall = session.lastResumeWallClockMillis ?: session.startWallClockMillis
            (nowWallClock - resumeWall).coerceAtLeast(0)
        }
        return base + delta.coerceAtLeast(0)
    }

    /** Elapsed focus time capped at the planned duration (used for display of a completed run). */
    fun cappedElapsedFocusMillis(
        session: StudySessionEntity,
        nowElapsedRealtime: Long,
        nowWallClock: Long,
    ): Long = liveElapsedFocusMillis(session, nowElapsedRealtime, nowWallClock)
        .coerceAtMost(session.plannedDurationMillis)

    fun remainingMillis(
        session: StudySessionEntity,
        nowElapsedRealtime: Long,
        nowWallClock: Long,
    ): Long = (session.plannedDurationMillis - cappedElapsedFocusMillis(session, nowElapsedRealtime, nowWallClock))
        .coerceAtLeast(0)

    fun progressFraction(
        session: StudySessionEntity,
        nowElapsedRealtime: Long,
        nowWallClock: Long,
    ): Float {
        if (session.plannedDurationMillis <= 0L) return 0f
        return (cappedElapsedFocusMillis(session, nowElapsedRealtime, nowWallClock).toDouble() /
            session.plannedDurationMillis.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    // ---- Transitions ---------------------------------------------------------------------

    fun start(
        subjectId: Long,
        chapterId: Long?,
        plannedDurationMillis: Long,
        nowElapsedRealtime: Long,
        nowWallClock: Long,
        plannerTaskId: Long? = null,
        note: String = "",
    ): StudySessionEntity = StudySessionEntity(
        subjectId = subjectId,
        chapterId = chapterId,
        plannedDurationMillis = plannedDurationMillis,
        actualFocusMillis = 0,
        startWallClockMillis = nowWallClock,
        accumulatedPausedMillis = 0,
        state = SessionState.RUNNING,
        note = note,
        plannerTaskId = plannerTaskId,
        lastResumeElapsedMillis = nowElapsedRealtime,
        lastResumeWallClockMillis = nowWallClock,
        createdAt = nowWallClock,
        updatedAt = nowWallClock,
    )

    fun pause(session: StudySessionEntity, nowElapsedRealtime: Long, nowWallClock: Long): StudySessionEntity {
        require(session.state == SessionState.RUNNING) { "Only a running session can be paused" }
        val elapsed = liveElapsedFocusMillis(session, nowElapsedRealtime, nowWallClock)
        return session.copy(
            actualFocusMillis = elapsed,
            state = SessionState.PAUSED,
            lastResumeElapsedMillis = null,
            lastResumeWallClockMillis = null,
            pausedAtElapsedMillis = nowElapsedRealtime,
            updatedAt = nowWallClock,
        )
    }

    fun resume(session: StudySessionEntity, nowElapsedRealtime: Long, nowWallClock: Long): StudySessionEntity {
        require(session.state == SessionState.PAUSED) { "Only a paused session can be resumed" }
        val pausedFor = session.pausedAtElapsedMillis?.let { (nowElapsedRealtime - it).coerceAtLeast(0) } ?: 0L
        return session.copy(
            state = SessionState.RUNNING,
            accumulatedPausedMillis = session.accumulatedPausedMillis + pausedFor,
            lastResumeElapsedMillis = nowElapsedRealtime,
            lastResumeWallClockMillis = nowWallClock,
            pausedAtElapsedMillis = null,
            updatedAt = nowWallClock,
        )
    }

    /**
     * Finalises a session. [plannedReached] is true when the planned duration elapsed naturally;
     * otherwise the session finished early and [actualFocusMillis] holds the real elapsed time.
     */
    fun finish(
        session: StudySessionEntity,
        nowElapsedRealtime: Long,
        nowWallClock: Long,
        plannedReached: Boolean,
    ): StudySessionEntity {
        require(session.state == SessionState.RUNNING || session.state == SessionState.PAUSED) {
            "Only an active session can be finished"
        }
        val elapsed = liveElapsedFocusMillis(session, nowElapsedRealtime, nowWallClock)
        val recorded = if (plannedReached) session.plannedDurationMillis else elapsed.coerceAtLeast(0)
        return session.copy(
            actualFocusMillis = recorded,
            state = SessionState.COMPLETED,
            endWallClockMillis = nowWallClock,
            finishedEarly = !plannedReached && recorded < session.plannedDurationMillis,
            lastResumeElapsedMillis = null,
            lastResumeWallClockMillis = null,
            pausedAtElapsedMillis = null,
            updatedAt = nowWallClock,
        )
    }

    fun cancel(session: StudySessionEntity, nowWallClock: Long): StudySessionEntity {
        require(session.state.isActive) { "Only an active session can be cancelled" }
        return session.copy(
            state = SessionState.CANCELLED,
            endWallClockMillis = nowWallClock,
            actualFocusMillis = session.actualFocusMillis,
            lastResumeElapsedMillis = null,
            lastResumeWallClockMillis = null,
            pausedAtElapsedMillis = null,
            updatedAt = nowWallClock,
        )
    }
}
