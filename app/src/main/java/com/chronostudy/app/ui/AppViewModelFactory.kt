package com.chronostudy.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chronostudy.app.ChronoApplication
import com.chronostudy.app.di.AppContainer
import com.chronostudy.app.ui.analytics.AnalyticsViewModel
import com.chronostudy.app.ui.clock.ClockViewModel
import com.chronostudy.app.ui.focus.BreakViewModel
import com.chronostudy.app.ui.focus.FocusSetupViewModel
import com.chronostudy.app.ui.focus.FocusTimerViewModel
import com.chronostudy.app.ui.history.HistoryViewModel
import com.chronostudy.app.ui.home.HomeViewModel
import com.chronostudy.app.ui.library.LibraryViewModel
import com.chronostudy.app.ui.main.MainViewModel
import com.chronostudy.app.ui.planner.PlannerViewModel
import com.chronostudy.app.ui.settings.BackupViewModel
import com.chronostudy.app.ui.settings.SettingsViewModel

val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        with(container()) {
            MainViewModel(settingsRepository, sessionRepository)
        }
    }
    initializer {
        with(container()) {
            ClockViewModel(libraryRepository, sessionRepository, plannerRepository, settingsRepository, timeProvider)
        }
    }
    initializer {
        with(container()) {
            HomeViewModel(libraryRepository, sessionRepository, plannerRepository, settingsRepository)
        }
    }
    initializer {
        with(container()) {
            FocusSetupViewModel(libraryRepository, sessionCoordinator, settingsRepository, timeProvider)
        }
    }
    initializer {
        with(container()) {
            FocusTimerViewModel(libraryRepository, sessionRepository, sessionCoordinator, timeProvider)
        }
    }
    initializer {
        with(container()) {
            BreakViewModel(libraryRepository, plannerRepository, settingsRepository, alertPlayer)
        }
    }
    initializer {
        with(container()) {
            LibraryViewModel(libraryRepository)
        }
    }
    initializer {
        with(container()) {
            PlannerViewModel(libraryRepository, plannerRepository, goalRepository, sessionRepository)
        }
    }
    initializer {
        with(container()) {
            HistoryViewModel(libraryRepository, sessionRepository)
        }
    }
    initializer {
        with(container()) {
            AnalyticsViewModel(libraryRepository, sessionRepository)
        }
    }
    initializer {
        with(container()) {
            SettingsViewModel(settingsRepository, alertPlayer, sessionRepository, plannerRepository)
        }
    }
    initializer {
        with(container()) {
            BackupViewModel(backupRepository)
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as ChronoApplication).container
