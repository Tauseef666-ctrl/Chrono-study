package com.chronostudy.app.domain.stats

import com.google.common.truth.Truth.assertThat
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.data.local.entity.StudySessionEntity
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatsCalculatorTest {

    private val zone = ZoneOffset.UTC

    private fun wallClock(date: LocalDate, hour: Int = 9): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli() + hour * 3_600_000L

    private fun session(
        subjectId: Long = 1L,
        focusMillis: Long = 30 * 60_000L,
        date: LocalDate,
        state: SessionState = SessionState.COMPLETED,
    ) = StudySessionEntity(
        id = 0,
        subjectId = subjectId,
        chapterId = null,
        plannedDurationMillis = focusMillis,
        actualFocusMillis = focusMillis,
        startWallClockMillis = wallClock(date),
        state = state,
    )

    @Test
    fun onlyCompletedSessionsContribute() {
        val completed = session(date = LocalDate.of(2026, 1, 1))
        val cancelled = session(date = LocalDate.of(2026, 1, 1), state = SessionState.CANCELLED)
        val running = session(date = LocalDate.of(2026, 1, 1), state = SessionState.RUNNING)
        val list = listOf(completed, cancelled, running)

        assertThat(StatsCalculator.totalFocusMillis(list)).isEqualTo(completed.actualFocusMillis)
        assertThat(StatsCalculator.sessionCount(list)).isEqualTo(1)
    }

    @Test
    fun zeroLengthSessionsAreIgnored() {
        val empty = session(focusMillis = 0, date = LocalDate.of(2026, 1, 1))
        assertThat(StatsCalculator.eligible(listOf(empty))).isEmpty()
    }

    @Test
    fun focusOn_sumsOnlyGivenDay() {
        val day1 = LocalDate.of(2026, 2, 10)
        val day2 = LocalDate.of(2026, 2, 11)
        val list = listOf(
            session(focusMillis = 60_000, date = day1),
            session(focusMillis = 30_000, date = day1),
            session(focusMillis = 90_000, date = day2),
        )
        assertThat(StatsCalculator.focusOn(list, day1, zone)).isEqualTo(90_000L)
        assertThat(StatsCalculator.focusOn(list, day2, zone)).isEqualTo(90_000L)
    }

    @Test
    fun currentStreak_countsConsecutiveDaysEndingToday() {
        val today = LocalDate.of(2026, 3, 20)
        val list = listOf(
            session(date = today),
            session(date = today.minusDays(1)),
            session(date = today.minusDays(2)),
            session(date = today.minusDays(4)),
        )
        assertThat(StatsCalculator.currentStreak(list, zone, today)).isEqualTo(3)
    }

    @Test
    fun currentStreak_isNotLostWhenTodayHasNoSessionsYet() {
        val today = LocalDate.of(2026, 3, 20)
        val list = listOf(
            session(date = today.minusDays(1)),
            session(date = today.minusDays(2)),
        )
        assertThat(StatsCalculator.currentStreak(list, zone, today)).isEqualTo(2)
    }

    @Test
    fun longestStreak_findsBestRun() {
        val base = LocalDate.of(2026, 4, 1)
        val list = listOf(
            session(date = base),
            session(date = base.plusDays(1)),
            session(date = base.plusDays(3)),
            session(date = base.plusDays(4)),
            session(date = base.plusDays(5)),
        )
        assertThat(StatsCalculator.longestStreak(list, zone)).isEqualTo(3)
    }

    @Test
    fun focusBySubject_groupsAndSortsDescending() {
        val day = LocalDate.of(2026, 5, 1)
        val list = listOf(
            session(subjectId = 1L, focusMillis = 10_000, date = day),
            session(subjectId = 1L, focusMillis = 5_000, date = day),
            session(subjectId = 2L, focusMillis = 30_000, date = day),
        )
        val bySubject = StatsCalculator.focusBySubject(list)
        assertThat(bySubject.first().subjectId).isEqualTo(2L)
        assertThat(bySubject.first().focusMillis).isEqualTo(30_000L)
        assertThat(bySubject.last().focusMillis).isEqualTo(15_000L)
    }

    @Test
    fun dailyBuckets_groupByCalendarDay() {
        val day = LocalDate.of(2026, 6, 1)
        val list = listOf(
            session(focusMillis = 60_000, date = day, state = SessionState.COMPLETED),
            session(focusMillis = 30_000, date = day, state = SessionState.COMPLETED),
        )
        val buckets = StatsCalculator.dailyBuckets(list, zone)
        assertThat(buckets).hasSize(1)
        assertThat(buckets.first().focusMillis).isEqualTo(90_000L)
        assertThat(buckets.first().sessionCount).isEqualTo(2)
    }
}
