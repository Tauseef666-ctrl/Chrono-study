package com.chronostudy.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.settings.ChronoSettings
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    settingsRepository: SettingsRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    val settings: StateFlow<ChronoSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChronoSettings())

    val activeSession = sessionRepository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
