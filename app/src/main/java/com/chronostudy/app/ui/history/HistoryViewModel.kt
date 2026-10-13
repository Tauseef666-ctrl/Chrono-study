package com.chronostudy.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<StudySessionEntity> = emptyList(),
    val subjectNames: Map<Long, SubjectEntity> = emptyMap(),
    val chapterNames: Map<Long, String> = emptyMap(),
)

class HistoryViewModel(
    private val library: LibraryRepository,
    private val sessions: SessionRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(
        sessions.allSessions,
        library.allSubjects,
        library.chapters,
    ) { all, subjects, chapters ->
        HistoryUiState(
            sessions = all.sortedByDescending { it.startWallClockMillis },
            subjectNames = subjects.associateBy { it.id },
            chapterNames = chapters.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun delete(id: Long) = viewModelScope.launch { sessions.deleteSession(id) }

    fun reassign(id: Long, subjectId: Long, chapterId: Long?) =
        viewModelScope.launch { sessions.reassignSession(id, subjectId, chapterId) }

    fun updateNote(session: StudySessionEntity, note: String) =
        viewModelScope.launch { sessions.updateSession(session.copy(note = note)) }
}
