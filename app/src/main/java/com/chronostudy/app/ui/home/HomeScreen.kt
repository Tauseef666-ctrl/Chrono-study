package com.chronostudy.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.EmptyState
import com.chronostudy.app.ui.components.MeterRow
import com.chronostudy.app.ui.components.ProgressRing
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.components.StatTile
import com.chronostudy.app.ui.components.SubjectDot
import com.chronostudy.app.ui.theme.ChronoTheme
import com.chronostudy.app.ui.util.parseColorHex

@Composable
fun HomeScreen(
    onOpenClock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onStartFocus: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = "Chrono Study",
                actions = {
                    IconButton(onClick = onOpenClock) { Icon(Icons.Filled.Bedtime, "Clock mode") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings") }
                },
            )
        }

        item { TodayCard(state) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Current streak",
                    value = "${state.currentStreak}",
                    caption = if (state.currentStreak == 1) "day" else "days",
                    modifier = Modifier.weight(1f),
                    accent = ChronoTheme.semantics.warning,
                )
                StatTile(
                    label = "This week",
                    value = DurationFormat.human(state.weekFocusMillis),
                    caption = "goal ${DurationFormat.minutesLabel(state.weeklyGoalMinutes)}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "All-time focus",
                    value = DurationFormat.human(state.totalFocusMillis),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Sessions",
                    value = "${state.completedCount}",
                    caption = "longest streak ${state.longestStreak}d",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onStartFocus, modifier = Modifier.weight(1f)) {
                    Text("Start focus")
                }
                FilledTonalButton(onClick = onOpenClock, modifier = Modifier.weight(1f)) {
                    Text("Clock mode")
                }
            }
        }

        if (state.nextTaskId != null) {
            item {
                SectionHeader("Next up")
            }
            item {
                ChronoCard {
                    Column {
                        Text(
                            state.nextTaskSubject ?: "Study task",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val chapter = state.nextTaskChapter
                        if (chapter != null) {
                            Text(
                                chapter,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.nextTaskMinutes > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Planned ${DurationFormat.minutesLabel(state.nextTaskMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Where your time goes") }

        if (state.subjectFocus.isEmpty()) {
            item {
                ChronoCard {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = "No sessions yet",
                        message = "Complete a focus session to see how your study time is distributed.",
                    )
                }
            }
        } else {
            val max = state.subjectFocus.maxOf { it.focusMillis }.coerceAtLeast(1L)
            items(state.subjectFocus, key = { it.subjectId }) { row ->
                ChronoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SubjectDot(parseColorHex(row.colorHex))
                        Spacer(Modifier.size(12.dp))
                        MeterRow(
                            label = row.name,
                            valueText = DurationFormat.human(row.focusMillis),
                            fraction = row.focusMillis.toFloat() / max.toFloat(),
                            color = parseColorHex(row.colorHex),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            FilledTonalButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.History, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("View full history")
            }
        }
    }
}

@Composable
private fun TodayCard(state: HomeUiState) {
    val goalMillis = state.dailyGoalMinutes * 60_000L
    val fraction = if (goalMillis <= 0L) 0f else (state.todayFocusMillis.toFloat() / goalMillis).coerceIn(0f, 1f)
    ChronoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = fraction,
                modifier = Modifier.size(112.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DurationFormat.human(state.todayFocusMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    DurationFormat.minutesLabel(state.dailyGoalMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                val remaining = (goalMillis - state.todayFocusMillis).coerceAtLeast(0L)
                Text(
                    text = if (remaining == 0L) "Goal reached — nice work." else "${DurationFormat.human(remaining)} to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.mostStudiedName != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Most studied: ${state.mostStudiedName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
