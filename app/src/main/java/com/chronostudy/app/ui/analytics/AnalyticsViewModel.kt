package com.chronostudy.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.domain.stats.SessionDailyBucket
import com.chronostudy.app.domain.stats.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

enum class AnalyticsRange(val days: Long?, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
    QUARTER(90, "90 days"),
    ALL(null, "All time"),
}

data class FocusSlice(val name: String, val colorHex: String, val millis: Long)

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val totalMillis: Long = 0L,
    val sessionCount: Int = 0,
    val averageSessionMillis: Long = 0L,
    val activeDays: Int = 0,
    val averagePerActiveDayMillis: Long = 0L,
    val bestDay: LocalDate? = null,
    val bestDayMillis: Long = 0L,
    val buckets: List<SessionDailyBucket> = emptyList(),
    val subjects: List<FocusSlice> = emptyList(),
    val chapters: List<FocusSlice> = emptyList(),
    val plannedMillis: Long = 0L,
    val actualMillis: Long = 0L,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
)

class AnalyticsViewModel(
    library: LibraryRepository,
    sessions: SessionRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val range = MutableStateFlow(AnalyticsRange.WEEK)

    val selectedRange: StateFlow<AnalyticsRange> = range

    val state: StateFlow<AnalyticsUiState> = combine(
        sessions.completedSessions,
        library.allSubjects,
        library.chapters,
        range,
    ) { all, subjects, chapters, selected ->
        val today = LocalDate.now(zone)
        val scoped = if (selected.days == null) {
            all
        } else {
            val from = today.minusDays(selected.days - 1)
            all.filter { !StatsCalculator.toLocalDate(it.startWallClockMillis, zone).isBefore(from) }
        }

        val buckets = StatsCalculator.dailyBuckets(scoped, zone)
            .let { list ->
                if (selected.days == null) list else {
                    val start = today.minusDays(selected.days - 1)
                    list.filter { !it.date.isBefore(start) }
                }
            }
            .sortedBy { it.date }

        val subjectNames = subjects.associateBy { it.id }
        val subjectSlices = StatsCalculator.focusBySubject(scoped).map { sf ->
            val subject = subjectNames[sf.subjectId]
            FocusSlice(
                name = subject?.name ?: "Removed subject",
                colorHex = subject?.colorHex ?: "#8A9099",
                millis = sf.focusMillis,
            )
        }
        val chapterSlices = StatsCalculator.focusByChapter(scoped).map { cf ->
            FocusSlice(
                name = chapters.firstOrNull { it.id == cf.chapterId }?.name ?: "Removed chapter",
                colorHex = "#8A9099",
                millis = cf.focusMillis,
            )
        }

        val activeBuckets = buckets.filter { it.focusMillis > 0 }
        val best = activeBuckets.maxByOrNull { it.focusMillis }
        val (planned, actual) = StatsCalculator.plannedVsActualMillis(scoped)

        AnalyticsUiState(
            range = selected,
            totalMillis = StatsCalculator.totalFocusMillis(scoped),
            sessionCount = StatsCalculator.sessionCount(scoped),
            averageSessionMillis = StatsCalculator.averageSessionMillis(scoped),
            activeDays = activeBuckets.size,
            averagePerActiveDayMillis = if (activeBuckets.isEmpty()) 0L else
                activeBuckets.sumOf { it.focusMillis } / activeBuckets.size,
            bestDay = best?.date,
            bestDayMillis = best?.focusMillis ?: 0L,
            buckets = buckets,
            subjects = subjectSlices,
            chapters = chapterSlices.filter { it.millis > 0 }.take(10),
            plannedMillis = planned,
            actualMillis = actual,
            currentStreak = StatsCalculator.currentStreak(all, zone, today),
            longestStreak = StatsCalculator.longestStreak(all, zone),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun selectRange(value: AnalyticsRange) {
        range.value = value
    }
}
