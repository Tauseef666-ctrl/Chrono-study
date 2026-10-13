package com.chronostudy.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.stats.StatsCalculator
import com.chronostudy.app.domain.time.TimeProvider
import com.chronostudy.app.domain.timer.SessionTimer
import com.chronostudy.app.session.SessionCoordinator
import com.chronostudy.app.ui.common.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class FocusTimerUiState(
    val session: StudySessionEntity? = null,
    val subjectName: String? = null,
    val chapterName: String? = null,
    val remainingMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val plannedMillis: Long = 0L,
    val progress: Float = 0f,
    val isPaused: Boolean = false,
    val todayTotalMillis: Long = 0L,
    val todaySubjectMillis: Long = 0L,
)

class FocusTimerViewModel(
    library: LibraryRepository,
    sessions: SessionRepository,
    private val coordinator: SessionCoordinator,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private data class Names(val subjects: List<SubjectEntity>, val chapters: List<ChapterEntity>)

    private val names = combine(library.subjects, library.chapters) { s, c -> Names(s, c) }

    val state: StateFlow<FocusTimerUiState> = combine(
        sessions.activeSession,
        ticker(1_000),
        names,
        sessions.completedSessions,
    ) { session, wall, names, completed ->
        if (session == null) {
            FocusTimerUiState()
        } else {
            val elapsed = SessionTimer.cappedElapsedFocusMillis(
                session,
                timeProvider.elapsedRealtimeMillis(),
                wall,
            )
            val today = LocalDate.now(zone)
            FocusTimerUiState(
                session = session,
                subjectName = names.subjects.firstOrNull { it.id == session.subjectId }?.name,
                chapterName = session.chapterId?.let { cid -> names.chapters.firstOrNull { it.id == cid }?.name },
                remainingMillis = SessionTimer.remainingMillis(
                    session,
                    timeProvider.elapsedRealtimeMillis(),
                    wall,
                ),
                elapsedMillis = elapsed,
                plannedMillis = session.plannedDurationMillis,
                progress = SessionTimer.progressFraction(
                    session,
                    timeProvider.elapsedRealtimeMillis(),
                    wall,
                ),
                isPaused = session.state == SessionState.PAUSED,
                todayTotalMillis = StatsCalculator.focusOn(completed, today, zone),
                todaySubjectMillis = StatsCalculator.focusOn(completed, today, zone, session.subjectId),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusTimerUiState())

    init {
        viewModelScope.launch {
            while (true) {
                state.value.session
                    ?.takeIf { it.state == SessionState.RUNNING }
                    ?.let { coordinator.completeIfDue(it.id) }
                delay(1_000)
            }
        }
    }

    fun togglePause() {
        val session = state.value.session ?: return
        viewModelScope.launch {
            if (session.state == SessionState.RUNNING) {
                coordinator.pause(session.id)
            } else {
                coordinator.resume(session.id)
            }
        }
    }

    fun finishEarly() {
        val session = state.value.session ?: return
        viewModelScope.launch { coordinator.finishEarly(session.id) }
    }

    fun cancel() {
        val session = state.value.session ?: return
        viewModelScope.launch { coordinator.cancel(session.id) }
    }
}

private fun StatsCalculator.focusOn(
    sessions: List<StudySessionEntity>,
    date: LocalDate,
    zone: ZoneId,
    subjectId: Long,
): Long = sessions
    .filter { it.subjectId == subjectId }
    .let { focusOn(it, date, zone) }
