package com.chronostudy.app.ui.focus

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.EmptyState
import com.chronostudy.app.ui.components.ProgressRing
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.StatTile
import com.chronostudy.app.ui.theme.ChronoTheme
import com.chronostudy.app.ui.theme.clockNumerals

@Composable
fun FocusTimerScreen(
    onFinished: () -> Unit,
    onDismissed: () -> Unit,
    onOpenBreak: () -> Unit,
    viewModel: FocusTimerViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hadSession by remember { mutableStateOf(false) }
    var exited by remember { mutableStateOf(false) }

    KeepScreenOn()

    LaunchedEffect(state.session) {
        if (state.session != null) {
            hadSession = true
        } else if (hadSession && !exited) {
            onFinished()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape && state.session != null) {
        LandscapeFocusLayout(
            state = state,
            onBack = {
                exited = true
                onDismissed()
            },
            onPauseResume = viewModel::togglePause,
            onDiscard = {
                exited = true
                viewModel.cancel()
                onDismissed()
            },
            onFinish = {
                exited = true
                viewModel.finishEarly()
                onOpenBreak()
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(
            title = "Focus",
            onBack = {
                exited = true
                onDismissed()
            },
        )

        if (state.session == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Timer,
                    title = "No active session",
                    message = "Start a focus session from the Focus tab.",
                )
            }
            return@Column
        }

        Spacer(Modifier.height(24.dp))
        ProgressRing(
            progress = state.progress,
            modifier = Modifier.size(260.dp),
            strokeWidth = 18f,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DurationFormat.clock(state.remainingMillis),
                    style = clockNumerals.copy(fontSize = 56.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (state.isPaused) "Paused" else "Remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = state.subjectName ?: "Focus session",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        val chapter = state.chapterName
        if (chapter != null) {
            Text(
                text = chapter,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Elapsed",
                value = DurationFormat.human(state.elapsedMillis),
                modifier = Modifier.weight(1f),
                accent = ChronoTheme.semantics.accent,
            )
            StatTile(
                label = "Today · subject",
                value = DurationFormat.human(state.todaySubjectMillis),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        StatTile(
            label = "Today · all subjects",
            value = DurationFormat.human(state.todayTotalMillis),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    exited = true
                    viewModel.cancel()
                    onDismissed()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Discard")
            }
            Button(
                onClick = {
                    exited = true
                    viewModel.finishEarly()
                    onOpenBreak()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Finish")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::togglePause,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isPaused) "Resume" else "Pause")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LandscapeFocusLayout(
    state: FocusTimerUiState,
    onBack: () -> Unit,
    onPauseResume: () -> Unit,
    onDiscard: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ScreenHeader(title = "Focus", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.subjectName ?: "Focus session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                val chapter = state.chapterName
                if (chapter != null) {
                    Text(
                        text = chapter,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressRing(
                    progress = state.progress,
                    modifier = Modifier.size(220.dp),
                    strokeWidth = 16f,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DurationFormat.clock(state.remainingMillis),
                            style = clockNumerals.copy(fontSize = 48.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (state.isPaused) "Paused" else "Remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        label = "Elapsed",
                        value = DurationFormat.human(state.elapsedMillis),
                        modifier = Modifier.weight(1f),
                        accent = ChronoTheme.semantics.accent,
                    )
                    StatTile(
                        label = "Today",
                        value = DurationFormat.human(state.todayTotalMillis),
                        modifier = Modifier.weight(1f),
                    )
                }

                Button(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    Text(
                        text = if (state.isPaused) "Resume" else "Pause",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = "Finish",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Discard")
                }
            }
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
