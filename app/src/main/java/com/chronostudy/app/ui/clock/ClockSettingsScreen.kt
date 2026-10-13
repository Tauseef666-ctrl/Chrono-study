package com.chronostudy.app.ui.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.data.settings.ClockAlignment
import com.chronostudy.app.data.settings.ClockDateFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.Divider
import com.chronostudy.app.ui.components.Pill
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.components.ToggleRow
import com.chronostudy.app.ui.settings.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun ClockSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader(title = "Clock display", onBack = onBack)

        SectionHeader("Format")
        ChronoCard {
            Column {
                ToggleRow("24-hour time", settings.use24Hour, viewModel::set24Hour)
                Divider()
                ToggleRow("Show seconds", settings.showSeconds, viewModel::setShowSeconds)
                Divider()
                ToggleRow("Show date", settings.showDate, viewModel::setShowDate)
            }
        }

        if (settings.showDate) {
            Column {
                SectionHeader("Date style")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockDateFormat.all.forEach { format ->
                        Pill(
                            text = formatLabel(format),
                            selected = settings.dateFormat == format,
                            onClick = { viewModel.setDateFormat(format) },
                        )
                    }
                }
            }
        }

        Gap()
        SectionHeader("Widgets on the clock")
        ChronoCard {
            Column {
                ToggleRow("Show battery", settings.showBattery, viewModel::setShowBattery)
                Divider()
                ToggleRow("Show study timer", settings.showStudyTimer, viewModel::setShowStudyTimer)
                Divider()
                ToggleRow("Show subject & chapter", settings.showSubjectChapter, viewModel::setShowSubjectChapter)
                Divider()
                ToggleRow("Show goal progress", settings.showGoalProgress, viewModel::setShowGoalProgress)
                Divider()
                ToggleRow("Show next task", settings.showNextTask, viewModel::setShowNextTask)
            }
        }

        Gap()
        SectionHeader("Layout")
        ChronoCard {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        text = "Centered",
                        selected = settings.clockAlignment == ClockAlignment.CENTER,
                        onClick = { viewModel.setClockAlignment(ClockAlignment.CENTER) },
                    )
                    Pill(
                        text = "Left aligned",
                        selected = settings.clockAlignment == ClockAlignment.START,
                        onClick = { viewModel.setClockAlignment(ClockAlignment.START) },
                    )
                }
                Gap()
                Text("Clock size", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.clockScale,
                    onValueChange = viewModel::setClockScale,
                    valueRange = 0.8f..1.4f,
                    onValueChangeFinished = { },
                )
            }
        }

        Gap()
        SectionHeader("Brightness & dimming")
        ChronoCard {
            Column {
                ToggleRow("Low brightness mode", settings.lowBrightness, viewModel::setLowBrightness)
                Divider()
                ToggleRow("Auto-dim when idle", settings.autoDimEnabled, viewModel::setAutoDimEnabled)
            }
        }

        if (settings.autoDimEnabled || settings.lowBrightness) {
            Gap()
            ChronoCard {
                Column {
                    if (settings.autoDimEnabled) {
                        Text(
                            "Dim after ${settings.autoDimAfterMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = settings.autoDimAfterMinutes.toFloat(),
                            onValueChange = { viewModel.setAutoDimAfterMinutes(it.roundToInt()) },
                            valueRange = 1f..60f,
                        )
                        Divider()
                    }
                    Text("Dim level", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.dimLevel,
                        onValueChange = viewModel::setDimLevel,
                        valueRange = 0.05f..0.8f,
                    )
                }
            }
        }

        Gap()
        Gap()
    }
}

@Composable
private fun Gap() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
}

private fun formatLabel(format: ClockDateFormat): String = when (format) {
    ClockDateFormat.DayMonth -> "12 Oct"
    ClockDateFormat.MonthDay -> "Oct 12"
    else -> "Mon, 12 Oct"
}
