package com.chronostudy.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.domain.stats.SessionDailyBucket
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.MeterRow
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.components.StatTile
import com.chronostudy.app.ui.theme.ChronoTheme
import com.chronostudy.app.ui.util.parseColorHex
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(
    onOpenHistory: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Analytics",
                actions = {
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Filled.History, "History") }
                },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsRange.entries.forEach { range ->
                    com.chronostudy.app.ui.components.Pill(
                        text = range.label,
                        selected = state.range == range,
                        onClick = { viewModel.selectRange(range) },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Total focus",
                    value = DurationFormat.human(state.totalMillis),
                    modifier = Modifier.weight(1f),
                    accent = ChronoTheme.semantics.accent,
                )
                StatTile(
                    label = "Sessions",
                    value = "${state.sessionCount}",
                    caption = "avg ${DurationFormat.human(state.averageSessionMillis)}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Active days",
                    value = "${state.activeDays}",
                    caption = "avg ${DurationFormat.human(state.averagePerActiveDayMillis)}",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Best day",
                    value = state.bestDay?.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) ?: "—",
                    caption = DurationFormat.human(state.bestDayMillis),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Current streak",
                    value = "${state.currentStreak}d",
                    modifier = Modifier.weight(1f),
                    accent = ChronoTheme.semantics.warning,
                )
                StatTile(
                    label = "Longest streak",
                    value = "${state.longestStreak}d",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.buckets.isNotEmpty()) {
            item { SectionHeader("Daily focus") }
            item {
                ChronoCard {
                    DailyBarChart(state.buckets)
                }
            }
        }

        if (state.plannedMillis > 0L) {
            item { SectionHeader("Planned vs actual") }
            item {
                ChronoCard {
                    val max = maxOf(state.plannedMillis, state.actualMillis).coerceAtLeast(1L)
                    Column {
                        MeterRow(
                            label = "Planned",
                            valueText = DurationFormat.human(state.plannedMillis),
                            fraction = state.plannedMillis.toFloat() / max,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        MeterRow(
                            label = "Actual focus",
                            valueText = DurationFormat.human(state.actualMillis),
                            fraction = state.actualMillis.toFloat() / max,
                        )
                    }
                }
            }
        }

        if (state.subjects.isNotEmpty()) {
            item { SectionHeader("By subject") }
            val max = state.subjects.maxOf { it.millis }.coerceAtLeast(1L)
            item {
                ChronoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.subjects.forEach { slice ->
                            MeterRow(
                                label = slice.name,
                                valueText = DurationFormat.human(slice.millis),
                                fraction = slice.millis.toFloat() / max,
                                color = parseColorHex(slice.colorHex),
                            )
                        }
                    }
                }
            }
        }

        if (state.chapters.isNotEmpty()) {
            item { SectionHeader("By chapter") }
            val max = state.chapters.maxOf { it.millis }.coerceAtLeast(1L)
            item {
                ChronoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.chapters.forEach { slice ->
                            MeterRow(
                                label = slice.name,
                                valueText = DurationFormat.human(slice.millis),
                                fraction = slice.millis.toFloat() / max,
                                color = ChronoTheme.semantics.chartPalette.first(),
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DailyBarChart(buckets: List<SessionDailyBucket>) {
    val display = buckets.takeLast(30)
    val max = display.maxOfOrNull { it.focusMillis }?.coerceAtLeast(1L) ?: 1L
    val barColor = ChronoTheme.semantics.accent
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(vertical = 8.dp),
        ) {
            if (display.isEmpty()) return@Canvas
            val gap = 4f
            val barWidth = (size.width - gap * (display.size - 1)) / display.size
            display.forEachIndexed { index, bucket ->
                val height = (bucket.focusMillis.toFloat() / max) * size.height
                val left = index * (barWidth + gap)
                drawRoundRect(
                    color = if (bucket.focusMillis > 0) barColor else emptyColor,
                    topLeft = Offset(left, size.height - height),
                    size = Size(barWidth, height.coerceAtLeast(2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f, barWidth / 3f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            Text(
                display.firstOrNull()?.date?.format(fmt) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                display.lastOrNull()?.date?.format(fmt) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
