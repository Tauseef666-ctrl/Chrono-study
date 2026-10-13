package com.chronostudy.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.alert.AlertPlayer
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BreakUiState(
    val running: Boolean = false,
    val isLongBreak: Boolean = false,
    val remainingMillis: Long = 0L,
    val totalMillis: Long = 0L,
    val nextSubjectName: String? = null,
) {
    val progress: Float
        get() = if (totalMillis <= 0L) 0f else (1f - remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
}

class BreakViewModel(
    library: LibraryRepository,
    planner: PlannerRepository,
    private val settingsRepository: SettingsRepository,
    private val alertPlayer: AlertPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow(BreakUiState())
    val state: StateFlow<BreakUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    val nextUp = combine(
        planner.observeNextUpcoming(LocalDate.now().toEpochDay()),
        library.allSubjects,
    ) { task, subjects ->
        task?.let { t -> subjects.firstOrNull { it.id == t.subjectId }?.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun start(minutes: Int, isLong: Boolean) {
        timerJob?.cancel()
        val total = minutes.coerceAtLeast(1) * 60_000L
        _state.value = BreakUiState(
            running = true,
            isLongBreak = isLong,
            remainingMillis = total,
            totalMillis = total,
            nextSubjectName = nextUp.value,
        )
        timerJob = viewModelScope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(500)
                remaining -= 500
                _state.value = _state.value.copy(remainingMillis = remaining.coerceAtLeast(0))
            }
            playAlert(focus = false)
            _state.value = _state.value.copy(running = false, remainingMillis = 0)
        }
    }

    fun extend(minutes: Int) {
        val current = _state.value
        if (!current.running) return
        val total = current.totalMillis + minutes * 60_000L
        val remaining = current.remainingMillis + minutes * 60_000L
        _state.value = current.copy(totalMillis = total, remainingMillis = remaining)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var rem = remaining
            while (rem > 0) {
                delay(500)
                rem -= 500
                _state.value = _state.value.copy(remainingMillis = rem.coerceAtLeast(0))
            }
            playAlert(focus = false)
            _state.value = _state.value.copy(running = false, remainingMillis = 0)
        }
    }

    fun skip() {
        timerJob?.cancel()
        _state.value = BreakUiState(nextSubjectName = nextUp.value)
    }

    fun stop() {
        timerJob?.cancel()
        alertPlayer.stop()
        _state.value = BreakUiState(nextSubjectName = nextUp.value)
    }

    private fun playAlert(focus: Boolean) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            alertPlayer.play(
                mode = settings.alertMode,
                soundKey = if (focus) settings.focusSoundKey else settings.breakSoundKey,
                vibrationKey = settings.vibrationPatternKey,
                repeat = settings.repeatAlert,
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
