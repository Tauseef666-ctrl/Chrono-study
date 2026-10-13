package com.chronostudy.app.domain.stats

import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.domain.model.SessionState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SessionDailyBucket(
    val date: LocalDate,
    val focusMillis: Long,
    val sessionCount: Int,
)

data class SubjectFocus(
    val subjectId: Long,
    val focusMillis: Long,
)

data class ChapterFocus(
    val chapterId: Long,
    val focusMillis: Long,
)

/**
 * Derives all statistics from saved, eligible sessions. Nothing is cached or stored, so totals
 * are always consistent with the underlying records after corrections or deletions.
 *
 * Only [SessionState.COMPLETED] sessions are eligible. Paused, cancelled and in-flight sessions
 * never contribute to focused-study totals.
 */
object StatsCalculator {

    fun eligible(sessions: List<StudySessionEntity>): List<StudySessionEntity> =
        sessions.filter { it.state == SessionState.COMPLETED && it.actualFocusMillis > 0 }

    fun totalFocusMillis(sessions: List<StudySessionEntity>): Long =
        eligible(sessions).sumOf { it.actualFocusMillis }

    fun sessionCount(sessions: List<StudySessionEntity>): Int = eligible(sessions).size

    fun averageSessionMillis(sessions: List<StudySessionEntity>): Long {
        val count = sessionCount(sessions)
        return if (count == 0) 0L else totalFocusMillis(sessions) / count
    }

    fun focusBySubject(sessions: List<StudySessionEntity>): List<SubjectFocus> =
        eligible(sessions)
            .groupBy { it.subjectId }
            .map { (subjectId, list) -> SubjectFocus(subjectId, list.sumOf { it.actualFocusMillis }) }
            .sortedByDescending { it.focusMillis }

    fun focusByChapter(sessions: List<StudySessionEntity>): List<ChapterFocus> =
        eligible(sessions)
            .filter { it.chapterId != null }
            .groupBy { it.chapterId!! }
            .map { (chapterId, list) -> ChapterFocus(chapterId, list.sumOf { it.actualFocusMillis }) }
            .sortedByDescending { it.focusMillis }

    fun dailyBuckets(sessions: List<StudySessionEntity>, zone: ZoneId): List<SessionDailyBucket> =
        eligible(sessions)
            .groupBy { toLocalDate(it.startWallClockMillis, zone) }
            .map { (date, list) ->
                SessionDailyBucket(
                    date = date,
                    focusMillis = list.sumOf { it.actualFocusMillis },
                    sessionCount = list.size,
                )
            }
            .sortedBy { it.date }

    fun focusOn(sessions: List<StudySessionEntity>, date: LocalDate, zone: ZoneId): Long =
        eligible(sessions)
            .filter { toLocalDate(it.startWallClockMillis, zone) == date }
            .sumOf { it.actualFocusMillis }

    fun focusBetween(
        sessions: List<StudySessionEntity>,
        startInclusive: LocalDate,
        endInclusive: LocalDate,
        zone: ZoneId,
    ): Long = eligible(sessions)
        .filter {
            val d = toLocalDate(it.startWallClockMillis, zone)
            !d.isBefore(startInclusive) && !d.isAfter(endInclusive)
        }
        .sumOf { it.actualFocusMillis }

    fun sessionsBetween(
        sessions: List<StudySessionEntity>,
        startInclusive: LocalDate,
        endInclusive: LocalDate,
        zone: ZoneId,
    ): List<StudySessionEntity> = eligible(sessions).filter {
        val d = toLocalDate(it.startWallClockMillis, zone)
        !d.isBefore(startInclusive) && !d.isAfter(endInclusive)
    }

    /**
     * Consecutive-day streak ending today (or yesterday, so the streak is not lost until a full
     * day is missed). A day counts when it has at least [minMillis] of eligible focus time.
     */
    fun currentStreak(
        sessions: List<StudySessionEntity>,
        zone: ZoneId,
        today: LocalDate,
        minMillis: Long = 1L,
    ): Int {
        val activeDays = eligible(sessions)
            .groupBy { toLocalDate(it.startWallClockMillis, zone) }
            .filterValues { list -> list.sumOf { it.actualFocusMillis } >= minMillis }
            .keys
        if (activeDays.isEmpty()) return 0
        var cursor = if (activeDays.contains(today)) today else today.minusDays(1)
        if (!activeDays.contains(cursor)) return 0
        var streak = 0
        while (activeDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Longest run of consecutive active days anywhere in the history. */
    fun longestStreak(sessions: List<StudySessionEntity>, zone: ZoneId, minMillis: Long = 1L): Int {
        val days = eligible(sessions)
            .groupBy { toLocalDate(it.startWallClockMillis, zone) }
            .filterValues { list -> list.sumOf { it.actualFocusMillis } >= minMillis }
            .keys
            .sorted()
        if (days.isEmpty()) return 0
        var longest = 1
        var run = 1
        for (i in 1 until days.size) {
            run = if (days[i - 1].plusDays(1) == days[i]) run + 1 else 1
            if (run > longest) longest = run
        }
        return longest
    }

    fun mostStudiedSubject(sessions: List<StudySessionEntity>): Long? =
        focusBySubject(sessions).firstOrNull()?.subjectId

    /** Subjects with no eligible focus time in the last [withinDays] days. */
    fun neglectedSubjectIds(
        sessions: List<StudySessionEntity>,
        allSubjectIds: List<Long>,
        recentSubjectIds: Set<Long>,
    ): List<Long> = allSubjectIds.filterNot { recentSubjectIds.contains(it) }

    fun recentSubjectIds(
        sessions: List<StudySessionEntity>,
        zone: ZoneId,
        today: LocalDate,
        withinDays: Long,
    ): Set<Long> {
        val from = today.minusDays(withinDays - 1)
        return eligible(sessions)
            .filter { !toLocalDate(it.startWallClockMillis, zone).isBefore(from) }
            .map { it.subjectId }
            .toSet()
    }

    /** Planned (per planner/task or focused) duration vs. actual, for the given sessions. */
    fun plannedVsActualMillis(sessions: List<StudySessionEntity>): Pair<Long, Long> {
        val list = eligible(sessions)
        return list.sumOf { it.plannedDurationMillis } to list.sumOf { it.actualFocusMillis }
    }

    fun toLocalDate(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
