package com.chronostudy.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.data.settings.ClockAlignment
import com.chronostudy.app.data.settings.ClockDateFormat
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.theme.ChronoTheme
import com.chronostudy.app.ui.theme.clockNumerals
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ClockScreen(
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    onFocus: () -> Unit,
    onBreak: () -> Unit,
    viewModel: ClockViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val battery = rememberBatteryState()

    var controlsVisible by remember { mutableStateOf(true) }
    var dimmed by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    ImmersiveSystemBars()
    KeepScreenOn()

    LaunchedEffect(settings.autoDimEnabled, settings.autoDimAfterMinutes) {
        while (true) {
            delay(1_000)
            if (!settings.autoDimEnabled) continue
            val idle = System.currentTimeMillis() - lastInteraction
            if (idle >= settings.autoDimAfterMinutes * 60_000L) dimmed = true
        }
    }

    val dimAlpha = when {
        dimmed -> settings.dimLevel
        settings.lowBrightness -> settings.dimLevel * 0.5f
        else -> 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        lastInteraction = System.currentTimeMillis()
                        if (dimmed) {
                            dimmed = false
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    },
                    onLongPress = {
                        lastInteraction = System.currentTimeMillis()
                        controlsVisible = true
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = if (settings.clockAlignment == ClockAlignment.CENTER) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            if (settings.showDate) {
                Text(
                    text = formatDate(state.now, settings.dateFormat),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = formatTime(state.now, settings.use24Hour, settings.showSeconds),
                style = clockNumerals.copy(fontSize = (96 * settings.clockScale).sp),
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (state.hasActiveSession && settings.showStudyTimer) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Focus · ${DurationFormat.clock(state.remainingMillis)} remaining",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChronoTheme.semantics.accent,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (settings.showSubjectChapter && state.subjectName != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOfNotNull(state.subjectName, state.chapterName).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (settings.showGoalProgress) {
                Spacer(Modifier.height(12.dp))
                val goalMillis = settings.dailyGoalMinutes * 60_000L
                val now = if (goalMillis <= 0L) 0 else (state.todayFocusMillis * 100 / goalMillis)
                Text(
                    text = "Today ${DurationFormat.human(state.todayFocusMillis)} · ${now}% of goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (settings.showNextTask && state.nextTaskSubjectName != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Next: ${state.nextTaskSubjectName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (battery.available && settings.showBattery) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (battery.charging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "${battery.levelPercent}%" + if (battery.charging) " · charging" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (controlsVisible && !dimmed) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                IconButton(onClick = { lastInteraction = System.currentTimeMillis(); onFocus() }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Focus", tint = ChronoTheme.semantics.accent)
                }
                IconButton(onClick = { lastInteraction = System.currentTimeMillis(); onBreak() }) {
                    Icon(Icons.Filled.Coffee, contentDescription = "Break", tint = ChronoTheme.semantics.accent)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Clock settings")
                }
                IconButton(onClick = onExit) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit clock")
                }
            }
        }

        if (dimAlpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha)),
            )
        }
    }
}

@Composable
private fun ImmersiveSystemBars() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context.findActivity() ?: return@DisposableEffect onDispose { }
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private fun formatTime(now: LocalDateTime, use24Hour: Boolean, showSeconds: Boolean): String {
    val pattern = when {
        use24Hour && showSeconds -> "HH:mm:ss"
        use24Hour -> "HH:mm"
        showSeconds -> "h:mm:ss a"
        else -> "h:mm a"
    }
    return now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

private fun formatDate(now: LocalDateTime, format: ClockDateFormat): String {
    val pattern = when (format) {
        ClockDateFormat.DayMonth -> "d MMMM yyyy"
        ClockDateFormat.MonthDay -> "MMMM d, yyyy"
        else -> "EEEE, d MMMM"
    }
    return now.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Composable
private fun KeepScreenOn() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
