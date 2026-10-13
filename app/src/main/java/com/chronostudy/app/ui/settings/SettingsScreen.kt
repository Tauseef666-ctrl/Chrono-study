package com.chronostudy.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.alert.SoundCatalog
import com.chronostudy.app.alert.VibrationCatalog
import com.chronostudy.app.domain.model.AlertMode
import com.chronostudy.app.domain.model.EarlyFinishBehavior
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.Divider
import com.chronostudy.app.ui.components.DurationStepper
import com.chronostudy.app.ui.components.Pill
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.components.SettingRow
import com.chronostudy.app.ui.components.ToggleRow
import com.chronostudy.app.ui.theme.Accent
import com.chronostudy.app.ui.theme.ThemePreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenClockSettings: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)

        SectionHeader("Appearance")
        ChronoCard {
            Column {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreset.entries.forEach { preset ->
                        Pill(
                            text = preset.displayName,
                            selected = settings.themePreset == preset,
                            onClick = { viewModel.setTheme(preset) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Accent", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        text = "Default",
                        selected = settings.accent == null,
                        onClick = { viewModel.setAccent(null) },
                    )
                    Accent.entries.forEach { accent ->
                        Pill(
                            text = accent.displayName,
                            selected = settings.accent == accent,
                            color = accent.color,
                            onClick = { viewModel.setAccent(accent) },
                        )
                    }
                }
            }
        }

        SettingRow(
            title = "Clock display",
            subtitle = "Format, widgets, brightness",
            onClick = onOpenClockSettings,
            trailing = { Text("Open", color = MaterialTheme.colorScheme.primary) },
        )

        SectionHeader("Focus & breaks")
        ChronoCard {
            Column {
                DurationStepper(settings.defaultFocusMinutes, viewModel::setDefaultFocusMinutes, label = "Default focus", step = 5, range = 5..240)
                Divider()
                DurationStepper(settings.shortBreakMinutes, viewModel::setShortBreakMinutes, label = "Short break", step = 1, range = 1..60)
                Divider()
                DurationStepper(settings.longBreakMinutes, viewModel::setLongBreakMinutes, label = "Long break", step = 5, range = 5..120)
                Divider()
                DurationStepper(settings.longBreakEveryN, viewModel::setLongBreakEveryN, label = "Long break every", step = 1, range = 2..10)
            }
        }
        ChronoCard {
            Column {
                ToggleRow("Auto-start break", settings.autoStartBreak, viewModel::setAutoStartBreak)
                Divider()
                ToggleRow("Auto-start next focus", settings.autoStartNextFocus, viewModel::setAutoStartNextFocus)
            }
        }
        ChronoCard {
            Column {
                Text("Finishing early", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        text = "Record elapsed",
                        selected = settings.earlyFinishBehavior == EarlyFinishBehavior.RECORD_ELAPSED,
                        onClick = { viewModel.setEarlyFinishBehavior(EarlyFinishBehavior.RECORD_ELAPSED) },
                    )
                    Pill(
                        text = "Ask me",
                        selected = settings.earlyFinishBehavior == EarlyFinishBehavior.ASK,
                        onClick = { viewModel.setEarlyFinishBehavior(EarlyFinishBehavior.ASK) },
                    )
                }
            }
        }

        SectionHeader("Alerts")
        ChronoCard {
            Column {
                Text("Alert style", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertMode.entries.forEach { mode ->
                        Pill(
                            text = mode.label(),
                            selected = settings.alertMode == mode,
                            onClick = { viewModel.setAlertMode(mode) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ToggleRow("Repeat alert until dismissed", settings.repeatAlert, viewModel::setRepeatAlert)
            }
        }
        ChronoCard {
            Column {
                Text("Focus complete sound", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundCatalog.options.forEach { option ->
                        Pill(
                            text = option.label,
                            selected = settings.focusSoundKey == option.key,
                            onClick = {
                                viewModel.setFocusSoundKey(option.key)
                                viewModel.previewSound(option.key, settings.vibrationPatternKey)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Break complete sound", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundCatalog.options.forEach { option ->
                        Pill(
                            text = option.label,
                            selected = settings.breakSoundKey == option.key,
                            onClick = {
                                viewModel.setBreakSoundKey(option.key)
                                viewModel.previewSound(option.key, settings.vibrationPatternKey)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Vibration", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VibrationCatalog.options.forEach { option ->
                        Pill(
                            text = option.label,
                            selected = settings.vibrationPatternKey == option.key,
                            onClick = {
                                viewModel.setVibrationPatternKey(option.key)
                                viewModel.previewSound(settings.focusSoundKey, option.key)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.previewSound(settings.focusSoundKey, settings.vibrationPatternKey) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Preview alert") }
            }
        }

        SectionHeader("Goals")
        ChronoCard {
            Column {
                DurationStepper(settings.dailyGoalMinutes, viewModel::setDailyGoalMinutes, label = "Daily goal (min)", step = 15, range = 0..1440)
                Divider()
                DurationStepper(settings.weeklyGoalMinutes, viewModel::setWeeklyGoalMinutes, label = "Weekly goal (min)", step = 60, range = 0..10080)
            }
        }

        SectionHeader("Data")
        ChronoCard {
            Column {
                SettingRow(
                    title = "Backup & restore",
                    subtitle = "Export or import your data as JSON",
                    onClick = onOpenBackup,
                    trailing = { Text("Open", color = MaterialTheme.colorScheme.primary) },
                )
                Divider()
                SettingRow(
                    title = "Clear study history",
                    subtitle = "Delete all sessions and planner tasks",
                    onClick = { confirmClear = true },
                )
                Divider()
                SettingRow(
                    title = "Reset settings",
                    subtitle = "Restore default preferences",
                    onClick = { confirmReset = true },
                )
            }
        }

        SectionHeader("About")
        ChronoCard {
            Column {
                Text("Chrono Study", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Offline study clock, focus timer and planner. All data stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear study history?") },
            text = { Text("All focus sessions and planner tasks will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearStudyHistory()
                    confirmClear = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Preferences return to their defaults. Your library and history are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSettings()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

private fun AlertMode.label(): String = when (this) {
    AlertMode.SOUND_AND_VIBRATION -> "Sound + vibration"
    AlertMode.SOUND_ONLY -> "Sound only"
    AlertMode.VIBRATION_ONLY -> "Vibration only"
    AlertMode.SILENT -> "Silent"
}
