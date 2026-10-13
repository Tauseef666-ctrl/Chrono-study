package com.chronostudy.app.domain.timer

import com.google.common.truth.Truth.assertThat
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.stats.StatsCalculator
import org.junit.Test

class SessionTimerTest {

    private val startElapsed = 1_000_000L
    private val startWall = 1_700_000_000_000L

    private fun newSession(planned: Long = 60_000L) = SessionTimer.start(
        subjectId = 1L,
        chapterId = null,
        plannedDurationMillis = planned,
        nowElapsedRealtime = startElapsed,
        nowWallClock = startWall,
    )

    @Test
    fun start_producesRunningSessionWithZeroFocus() {
        val session = newSession()
        assertThat(session.state).isEqualTo(SessionState.RUNNING)
        assertThat(session.actualFocusMillis).isEqualTo(0L)
        assertThat(session.startWallClockMillis).isEqualTo(startWall)
    }

    @Test
    fun runningDelta_isCountedFromMonotonicClock() {
        val session = newSession()
        val elapsed = SessionTimer.liveElapsedFocusMillis(session, startElapsed + 10_000, startWall + 10_000)
        assertThat(elapsed).isEqualTo(10_000L)
    }

    @Test
    fun pausedTime_isNeverCounted() {
        var session = newSession()
        session = SessionTimer.pause(session, startElapsed + 10_000, startWall + 10_000)

        val whilePaused = SessionTimer.liveElapsedFocusMillis(session, startElapsed + 40_000, startWall + 40_000)
        assertThat(whilePaused).isEqualTo(10_000L)

        session = SessionTimer.resume(session, startElapsed + 40_000, startWall + 40_000)
        assertThat(session.accumulatedPausedMillis).isEqualTo(30_000L)

        val afterResume = SessionTimer.liveElapsedFocusMillis(session, startElapsed + 50_000, startWall + 50_000)
        assertThat(afterResume).isEqualTo(20_000L)
    }

    @Test
    fun finishingNaturally_recordsPlannedDuration() {
        var session = newSession(planned = 60_000L)
        session = SessionTimer.finish(session, startElapsed + 70_000, startWall + 70_000, plannedReached = true)
        assertThat(session.actualFocusMillis).isEqualTo(60_000L)
        assertThat(session.state).isEqualTo(SessionState.COMPLETED)
        assertThat(session.finishedEarly).isFalse()
    }

    @Test
    fun finishingEarly_recordsElapsedTime() {
        var session = newSession(planned = 60_000L)
        session = SessionTimer.finish(session, startElapsed + 20_000, startWall + 20_000, plannedReached = false)
        assertThat(session.actualFocusMillis).isEqualTo(20_000L)
        assertThat(session.finishedEarly).isTrue()
    }

    @Test
    fun cancelledSession_isNotEligibleForStats() {
        var session = newSession()
        session = SessionTimer.pause(session, startElapsed + 15_000, startWall + 15_000)
        session = SessionTimer.cancel(session, startWall + 20_000)
        assertThat(session.state).isEqualTo(SessionState.CANCELLED)
        assertThat(StatsCalculator.eligible(listOf(session))).isEmpty()
    }

    @Test
    fun wallClockJumpingBackwards_doesNotAffectElapsed() {
        val session = newSession()
        val elapsed = SessionTimer.liveElapsedFocusMillis(
            session,
            nowElapsedRealtime = startElapsed + 5_000,
            nowWallClock = startWall - 3_600_000,
        )
        assertThat(elapsed).isEqualTo(5_000L)
    }

    @Test
    fun rebootWithElapsedGoingBackwards_fallsBackToWallClock() {
        val session = newSession()
        val elapsed = SessionTimer.liveElapsedFocusMillis(
            session,
            nowElapsedRealtime = startElapsed - 100,
            nowWallClock = startWall + 8_000,
        )
        assertThat(elapsed).isEqualTo(8_000L)
    }

    @Test
    fun remainingAndProgress_areCappedAtPlanned() {
        val session = newSession(planned = 60_000L)
        assertThat(SessionTimer.remainingMillis(session, startElapsed + 20_000, startWall + 20_000))
            .isEqualTo(40_000L)
        assertThat(SessionTimer.remainingMillis(session, startElapsed + 90_000, startWall + 90_000))
            .isEqualTo(0L)
        assertThat(SessionTimer.progressFraction(session, startElapsed + 30_000, startWall + 30_000))
            .isWithin(0.001f).of(0.5f)
        assertThat(SessionTimer.progressFraction(session, startElapsed + 90_000, startWall + 90_000))
            .isEqualTo(1f)
    }
}
