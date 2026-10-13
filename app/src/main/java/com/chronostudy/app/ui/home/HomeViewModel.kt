package com.chronostudy.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.data.settings.ChronoSettings
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.stats.StatsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class SubjectFocusRow(
    val subjectId: Long,
    val name: String,
    val colorHex: String,
    val focusMillis: Long,
)

data class HomeUiState(
    val todayFocusMillis: Long = 0L,
    val dailyGoalMinutes: Int = 120,
    val weekFocusMillis: Long = 0L,
    val weeklyGoalMinutes: Int = 600,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalFocusMillis: Long = 0L,
    val completedCount: Int = 0,
    val mostStudiedName: String? = null,
    val subjectFocus: List<SubjectFocusRow> = emptyList(),
    val nextTaskId: Long? = null,
    val nextTaskSubject: String? = null,
    val nextTaskChapter: String? = null,
    val nextTaskMinutes: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    library: LibraryRepository,
    sessions: SessionRepository,
    planner: PlannerRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private val nextTaskFlow = flow {
        while (true) {
            emit(LocalDate.now(zone).toEpochDay())
            delay(30_000)
        }
    }.distinctUntilChanged().flatMapLatest { planner.observeNextUpcoming(it) }

    private data class Bundle(
        val completed: List<StudySessionEntity>,
        val subjects: List<com.chronostudy.app.data.local.entity.SubjectEntity>,
        val chapters: List<com.chronostudy.app.data.local.entity.ChapterEntity>,
        val nextTask: com.chronostudy.app.data.local.entity.PlannerTaskEntity?,
    )

    private val dataFlow = combine(
        sessions.completedSessions,
        library.allSubjects,
        library.chapters,
        nextTaskFlow,
    ) { completed, subjects, chapters, nextTask ->
        Bundle(completed, subjects, chapters, nextTask)
    }

    val state: StateFlow<HomeUiState> = combine(
        dataFlow,
        settingsRepository.settings,
    ) { bundle, settings ->
        buildState(bundle, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(bundle: Bundle, settings: ChronoSettings): HomeUiState {
        val today = LocalDate.now(zone)
        val sessions = bundle.completed
        val subjectNames = bundle.subjects.associate { it.id to it }
        val chapterNames = bundle.chapters.associateBy { it.id }

        val subjectFocus = StatsCalculator.focusBySubject(sessions).map { sf ->
            val subject = subjectNames[sf.subjectId]
            SubjectFocusRow(
                subjectId = sf.subjectId,
                name = subject?.name ?: "Removed subject",
                colorHex = subject?.colorHex ?: "#8A9099",
                focusMillis = sf.focusMillis,
            )
        }

        val mostStudied = subjectFocus.firstOrNull()
        val next = bundle.nextTask

        return HomeUiState(
            todayFocusMillis = StatsCalculator.focusOn(sessions, today, zone),
            dailyGoalMinutes = settings.dailyGoalMinutes,
            weekFocusMillis = StatsCalculator.focusBetween(sessions, today.minusDays(6), today, zone),
            weeklyGoalMinutes = settings.weeklyGoalMinutes,
            currentStreak = StatsCalculator.currentStreak(sessions, zone, today),
            longestStreak = StatsCalculator.longestStreak(sessions, zone),
            totalFocusMillis = StatsCalculator.totalFocusMillis(sessions),
            completedCount = StatsCalculator.sessionCount(sessions),
            mostStudiedName = mostStudied?.name,
            subjectFocus = subjectFocus.take(6),
            nextTaskId = next?.id,
            nextTaskSubject = next?.let { subjectNames[it.subjectId]?.name },
            nextTaskChapter = next?.chapterId?.let { chapterNames[it]?.name },
            nextTaskMinutes = next?.plannedDurationMinutes ?: 0,
        )
    }
}
