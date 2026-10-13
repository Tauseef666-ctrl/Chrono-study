package com.chronostudy.app.ui.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.PlannerTaskEntity
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.data.settings.ChronoSettings
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.stats.StatsCalculator
import com.chronostudy.app.domain.time.TimeProvider
import com.chronostudy.app.domain.timer.SessionTimer
import com.chronostudy.app.ui.common.ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class ClockUiState(
    val now: LocalDateTime = LocalDateTime.now(),
    val settings: ChronoSettings = ChronoSettings(),
    val activeSession: StudySessionEntity? = null,
    val subjectName: String? = null,
    val chapterName: String? = null,
    val remainingMillis: Long = 0L,
    val progress: Float = 0f,
    val todayFocusMillis: Long = 0L,
    val nextTask: PlannerTaskEntity? = null,
    val nextTaskSubjectName: String? = null,
) {
    val hasActiveSession: Boolean get() = activeSession != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClockViewModel(
    library: LibraryRepository,
    sessions: SessionRepository,
    planner: PlannerRepository,
    settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val nextTaskFlow = flow {
        while (true) {
            emit(LocalDate.now(zone).toEpochDay())
            delay(30_000)
        }
    }.distinctUntilChanged().flatMapLatest { day ->
        planner.observeNextUpcoming(day)
    }

    private data class Bundle(
        val active: StudySessionEntity?,
        val completed: List<StudySessionEntity>,
        val nextTask: PlannerTaskEntity?,
        val subjects: List<SubjectEntity>,
        val chapters: List<ChapterEntity>,
    )

    private val dataFlow = combine(
        sessions.activeSession,
        sessions.completedSessions,
        nextTaskFlow,
        library.subjects,
        library.chapters,
    ) { active, completed, nextTask, subjects, chapters ->
        Bundle(active, completed, nextTask, subjects, chapters)
    }

    val state: StateFlow<ClockUiState> = combine(
        ticker(1_000),
        settingsRepository.settings,
        dataFlow,
    ) { wall, settings, bundle ->
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(wall), zone)
        val active = bundle.active

        val subjectName: String?
        val chapterName: String?
        if (active != null) {
            subjectName = bundle.subjects.firstOrNull { it.id == active.subjectId }?.name
            chapterName = active.chapterId?.let { cid -> bundle.chapters.firstOrNull { it.id == cid }?.name }
        } else {
            subjectName = settings.lastSubjectId?.let { id -> bundle.subjects.firstOrNull { it.id == id }?.name }
            chapterName = settings.lastChapterId?.let { id -> bundle.chapters.firstOrNull { it.id == id }?.name }
        }

        ClockUiState(
            now = now,
            settings = settings,
            activeSession = active,
            subjectName = subjectName,
            chapterName = chapterName,
            remainingMillis = active?.let {
                SessionTimer.remainingMillis(it, timeProvider.elapsedRealtimeMillis(), wall)
            } ?: 0L,
            progress = active?.let {
                SessionTimer.progressFraction(it, timeProvider.elapsedRealtimeMillis(), wall)
            } ?: 0f,
            todayFocusMillis = StatsCalculator.focusOn(bundle.completed, now.toLocalDate(), zone),
            nextTask = bundle.nextTask,
            nextTaskSubjectName = bundle.nextTask?.let { task ->
                bundle.subjects.firstOrNull { it.id == task.subjectId }?.name
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClockUiState())
}
