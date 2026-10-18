package com.chronostudy.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.time.TimeProvider
import com.chronostudy.app.domain.timer.SessionTimer
import com.chronostudy.app.session.SessionCoordinator
import com.chronostudy.app.ui.common.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SubjectOption(
    val id: Long,
    val name: String,
    val colorHex: String,
    val courseName: String?,
    val semester: Int?,
)

data class ChapterOption(
    val id: Long,
    val name: String,
    val unitLabel: String,
)

data class FocusSetupUiState(
    val subjects: List<SubjectOption> = emptyList(),
    val chapters: List<ChapterOption> = emptyList(),
    val selectedSubjectId: Long? = null,
    val selectedChapterId: Long? = null,
    val durationMinutes: Int = 25,
    val note: String = "",
    val runningSessionId: Long? = null,
    val runningSubjectName: String? = null,
    val runningChapterName: String? = null,
    val runningRemainingMillis: Long = 0L,
    val runningIsPaused: Boolean = false,
) {
    val canStart: Boolean get() = selectedSubjectId != null && runningSessionId == null
    val hasRunningSession: Boolean get() = runningSessionId != null
    val selectedSubject: SubjectOption? get() = subjects.firstOrNull { it.id == selectedSubjectId }
}

class FocusSetupViewModel(
    private val library: LibraryRepository,
    private val coordinator: SessionCoordinator,
    settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val selectedSubject = MutableStateFlow<Long?>(null)
    private val selectedChapter = MutableStateFlow<Long?>(null)
    private val duration = MutableStateFlow(25)
    private val note = MutableStateFlow("")

    private val baseState: StateFlow<FocusSetupUiState> = combine(
        library.subjects,
        library.courses,
        library.chapters,
        combine(selectedSubject, selectedChapter, duration, note) { s, c, d, n -> Selection(s, c, d, n) },
        settingsRepository.settings,
    ) { subjects, courses, chapters, selection, settings ->
        val courseMap = courses.associateBy { it.id }
        val subjectOptions = subjects.map { it.toOption(courseMap) }
        val chapterOptions = chapters
            .filter { it.subjectId == selection.subjectId && !it.isArchived }
            .sortedBy { it.sortOrder }
            .map { ChapterOption(it.id, it.name, it.unitLabel) }
        FocusSetupUiState(
            subjects = subjectOptions,
            chapters = chapterOptions,
            selectedSubjectId = selection.subjectId,
            selectedChapterId = selection.chapterId,
            durationMinutes = if (selection.duration <= 0) settings.defaultFocusMinutes else selection.duration,
            note = selection.note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSetupUiState())

    private val runningInfo: StateFlow<RunningSessionInfo?> = combine(
        coordinator.activeSessionFlow,
        library.subjects,
        library.chapters,
        ticker(1_000),
    ) { session, subjects, chapters, _ ->
        if (session == null) {
            null
        } else {
            RunningSessionInfo(
                id = session.id,
                subjectName = subjects.firstOrNull { it.id == session.subjectId }?.name,
                chapterName = session.chapterId?.let { cid -> chapters.firstOrNull { it.id == cid }?.name },
                remainingMillis = SessionTimer.remainingMillis(
                    session,
                    timeProvider.elapsedRealtimeMillis(),
                    timeProvider.wallClockMillis(),
                ),
                isPaused = session.state == SessionState.PAUSED,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<FocusSetupUiState> = combine(baseState, runningInfo) { base, running ->
        base.copy(
            runningSessionId = running?.id,
            runningSubjectName = running?.subjectName,
            runningChapterName = running?.chapterName,
            runningRemainingMillis = running?.remainingMillis ?: 0L,
            runningIsPaused = running?.isPaused ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSetupUiState())

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            duration.value = settings.defaultFocusMinutes
            val subjectId = settings.lastSubjectId ?: library.firstActiveSubject()?.id
            selectedSubject.value = subjectId
            selectedChapter.value = settings.lastChapterId
        }
    }

    fun selectSubject(id: Long) {
        if (selectedSubject.value != id) {
            selectedSubject.value = id
            selectedChapter.value = null
        }
    }

    fun selectChapter(id: Long?) {
        selectedChapter.value = if (selectedChapter.value == id) null else id
    }

    fun setDuration(minutes: Int) {
        duration.value = minutes.coerceIn(1, 240)
    }

    fun setNote(value: String) {
        note.value = value
    }

    fun start() = startWith(
        subjectId = selectedSubject.value,
        chapterId = selectedChapter.value,
        minutes = state.value.durationMinutes,
    )

    fun startTask(subjectId: Long, chapterId: Long?, minutes: Int, taskId: Long) = startWith(
        subjectId = subjectId,
        chapterId = chapterId,
        minutes = minutes,
        plannerTaskId = taskId,
    )

    private fun startWith(subjectId: Long?, chapterId: Long?, minutes: Int, plannerTaskId: Long? = null) {
        val subject = subjectId ?: return
        viewModelScope.launch {
            if (coordinator.activeSession() != null) return@launch
            coordinator.startSession(
                subjectId = subject,
                chapterId = chapterId,
                plannedDurationMillis = minutes.coerceAtLeast(1) * 60_000L,
                plannerTaskId = plannerTaskId,
                note = note.value,
            )
        }
    }
}

private data class RunningSessionInfo(
    val id: Long,
    val subjectName: String?,
    val chapterName: String?,
    val remainingMillis: Long,
    val isPaused: Boolean,
)

private data class Selection(
    val subjectId: Long?,
    val chapterId: Long?,
    val duration: Int,
    val note: String,
)

private fun SubjectEntity.toOption(courseMap: Map<Long, CourseEntity>) = SubjectOption(
    id = id,
    name = name,
    colorHex = colorHex,
    courseName = courseId?.let { courseMap[it]?.name },
    semester = semester,
)
