package com.chronostudy.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.alert.AlertPlayer
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.data.settings.ClockAlignment
import com.chronostudy.app.data.settings.ClockDateFormat
import com.chronostudy.app.data.settings.ChronoSettings
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.model.AlertMode
import com.chronostudy.app.domain.model.EarlyFinishBehavior
import com.chronostudy.app.ui.theme.Accent
import com.chronostudy.app.ui.theme.ThemePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val alertPlayer: AlertPlayer,
    private val sessions: SessionRepository,
    private val planner: PlannerRepository,
) : ViewModel() {

    val settings: StateFlow<ChronoSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChronoSettings())

    fun setTheme(value: ThemePreset) = launch { settingsRepository.setTheme(value) }
    fun setAccent(value: Accent?) = launch { settingsRepository.setAccent(value) }
    fun set24Hour(value: Boolean) = launch { settingsRepository.set24Hour(value) }
    fun setShowSeconds(value: Boolean) = launch { settingsRepository.setShowSeconds(value) }
    fun setShowDate(value: Boolean) = launch { settingsRepository.setShowDate(value) }
    fun setDateFormat(value: ClockDateFormat) = launch { settingsRepository.setDateFormat(value) }
    fun setShowBattery(value: Boolean) = launch { settingsRepository.setShowBattery(value) }
    fun setShowStudyTimer(value: Boolean) = launch { settingsRepository.setShowStudyTimer(value) }
    fun setShowSubjectChapter(value: Boolean) = launch { settingsRepository.setShowSubjectChapter(value) }
    fun setShowGoalProgress(value: Boolean) = launch { settingsRepository.setShowGoalProgress(value) }
    fun setShowNextTask(value: Boolean) = launch { settingsRepository.setShowNextTask(value) }
    fun setClockAlignment(value: ClockAlignment) = launch { settingsRepository.setClockAlignment(value) }
    fun setClockScale(value: Float) = launch { settingsRepository.setClockScale(value) }
    fun setLowBrightness(value: Boolean) = launch { settingsRepository.setLowBrightness(value) }
    fun setAutoDimEnabled(value: Boolean) = launch { settingsRepository.setAutoDimEnabled(value) }
    fun setAutoDimAfterMinutes(value: Int) = launch { settingsRepository.setAutoDimAfterMinutes(value) }
    fun setDimLevel(value: Float) = launch { settingsRepository.setDimLevel(value) }

    fun setDefaultFocusMinutes(value: Int) = launch { settingsRepository.setDefaultFocusMinutes(value) }
    fun setShortBreakMinutes(value: Int) = launch { settingsRepository.setShortBreakMinutes(value) }
    fun setLongBreakMinutes(value: Int) = launch { settingsRepository.setLongBreakMinutes(value) }
    fun setAutoStartBreak(value: Boolean) = launch { settingsRepository.setAutoStartBreak(value) }
    fun setAutoStartNextFocus(value: Boolean) = launch { settingsRepository.setAutoStartNextFocus(value) }
    fun setLongBreakEveryN(value: Int) = launch { settingsRepository.setLongBreakEveryN(value) }
    fun setEarlyFinishBehavior(value: EarlyFinishBehavior) = launch { settingsRepository.setEarlyFinishBehavior(value) }

    fun setAlertMode(value: AlertMode) = launch { settingsRepository.setAlertMode(value) }
    fun setFocusSoundKey(value: String) = launch { settingsRepository.setFocusSoundKey(value) }
    fun setBreakSoundKey(value: String) = launch { settingsRepository.setBreakSoundKey(value) }
    fun setVibrationPatternKey(value: String) = launch { settingsRepository.setVibrationPatternKey(value) }
    fun setRepeatAlert(value: Boolean) = launch { settingsRepository.setRepeatAlert(value) }

    fun setDailyGoalMinutes(value: Int) = launch { settingsRepository.setDailyGoalMinutes(value) }
    fun setWeeklyGoalMinutes(value: Int) = launch { settingsRepository.setWeeklyGoalMinutes(value) }
    fun setReminderEnabled(value: Boolean) = launch { settingsRepository.setReminderEnabled(value) }

    fun previewSound(soundKey: String, vibrationKey: String) = launch {
        val current = settingsRepository.settings.first()
        alertPlayer.play(
            mode = current.alertMode,
            soundKey = soundKey,
            vibrationKey = vibrationKey,
            repeat = false,
        )
    }

    fun clearStudyHistory() = launch {
        sessions.deleteAll()
        planner.deleteAll()
    }

    fun resetSettings() = launch { settingsRepository.resetPreferences() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    override fun onCleared() {
        alertPlayer.stop()
        super.onCleared()
    }
}
