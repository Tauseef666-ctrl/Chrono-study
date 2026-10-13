package com.chronostudy.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.stats.StatsCalculator
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.EmptyState
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SubjectDot
import com.chronostudy.app.ui.theme.ChronoTheme
import com.chronostudy.app.ui.util.parseColorHex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    var editing by remember { mutableStateOf<StudySessionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    val finished = state.sessions.filter { it.state.isFinal }
    val grouped = finished.groupBy { StatsCalculator.toLocalDate(it.startWallClockMillis, zone) }
        .toSortedMap(compareByDescending { it })

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(title = "History", onBack = onBack) }

        if (finished.isEmpty()) {
            item {
                ChronoCard {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = "No finished sessions",
                        message = "Completed and cancelled sessions will appear here.",
                    )
                }
            }
        }

        grouped.forEach { (date, sessions) ->
            item(key = "header-$date") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dateLabel(date, zone),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        DurationFormat.human(sessions.filter { it.state == SessionState.COMPLETED }.sumOf { it.actualFocusMillis }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(sessions, key = { it.id }) { session ->
                val subject = state.subjectNames[session.subjectId]
                ChronoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SubjectDot(parseColorHex(subject?.colorHex))
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                subject?.name ?: "Removed subject",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            val chapter = session.chapterId?.let { state.chapterNames[it] }
                            if (chapter != null) {
                                Text(chapter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                timeRange(session, zone) + " · " + session.state.label(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (session.note.isNotBlank()) {
                                Text(session.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            DurationFormat.human(session.actualFocusMillis),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (session.state == SessionState.COMPLETED) ChronoTheme.semantics.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { editing = session }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                        }
                        IconButton(onClick = { pendingDelete = session.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.padding(bottom = 12.dp)) }
    }

    editing?.let { session ->
        var note by remember(session.id) { mutableStateOf(session.note) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Session note") },
            text = {
                OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNote(session, note)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete session?") },
            text = { Text("This removes the recorded study time permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun SessionState.label(): String = when (this) {
    SessionState.COMPLETED -> "Completed"
    SessionState.CANCELLED -> "Cancelled"
    else -> name
}

private fun dateLabel(date: LocalDate, zone: ZoneId): String {
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
    }
}

private fun timeRange(session: StudySessionEntity, zone: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val start = java.time.Instant.ofEpochMilli(session.startWallClockMillis).atZone(zone).format(formatter)
    val end = session.endWallClockMillis
        ?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).format(formatter) }
        ?: "—"
    return "$start–$end"
}
