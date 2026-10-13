package com.chronostudy.app.ui.focus

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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.ProgressRing
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.theme.clockNumerals

@Composable
fun BreakScreen(
    onDone: () -> Unit,
    onStartFocus: () -> Unit,
    viewModel: BreakViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nextUp by viewModel.nextUp.collectAsStateWithLifecycle()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(title = "Break", onBack = onDone)

        if (!state.running) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Take a rest",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = nextUp?.let { "Up next: $it" } ?: "Choose a break length",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { viewModel.start(5, isLong = false) }) {
                    Text("5 min")
                }
                FilledTonalButton(onClick = { viewModel.start(15, isLong = true) }) {
                    Text("15 min")
                }
                OutlinedButton(onClick = { viewModel.start(10, isLong = false) }) {
                    Text("10 min")
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Skip break")
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(24.dp))
            ProgressRing(
                progress = state.progress,
                modifier = Modifier.size(240.dp),
                strokeWidth = 18f,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DurationFormat.clock(state.remainingMillis),
                        style = clockNumerals.copy(fontSize = 52.sp),
                    )
                    Text(
                        text = if (state.isLongBreak) "Long break" else "Short break",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (nextUp != null) {
                ChronoCard {
                    Text(
                        text = "Up next: $nextUp",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.extend(5) }) { Text("+5 min") }
                OutlinedButton(onClick = { viewModel.skip() }) { Text("Skip") }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onStartFocus, modifier = Modifier.fillMaxWidth()) {
                Text("Start next focus")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
