package com.chronostudy.app.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.chronostudy.app.domain.model.TaskStatus
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.Pill
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.components.SubjectDot
import com.chronostudy.app.ui.util.parseColorHex
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    onStartTask: () -> Unit,
    viewModel: PlannerViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(state.epochDay)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Planner",
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add task")
                    }
                },
            )
        }

        item {
            ChronoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::previousDay) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Planned ${DurationFormat.minutesLabel(state.plannedMinutes)} · studied ${DurationFormat.human(state.actualMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::nextDay) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::jumpToToday) { Text("Today") }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (state.goalMinutes > 0) "Goal ${DurationFormat.minutesLabel(state.goalMinutes)}" else "No goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showGoal = true }) { Text("Set goal") }
                }
            }
        }

        if (state.tasks.isEmpty()) {
            item {
                ChronoCard {
                    Text(
                        "No tasks planned for this day. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.tasks, key = { it.id }) { task ->
                ChronoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SubjectDot(parseColorHex(null))
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.nextSubjectNameById[task.subjectId] ?: "Subject",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            val chapter = task.chapterId?.let { state.chapterNameById[it] }
                            if (chapter != null) {
                                Text(
                                    chapter,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (task.plannedDurationMinutes > 0) {
                                Text(
                                    DurationFormat.minutesLabel(task.plannedDurationMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Pill(
                            text = task.status.label(),
                            selected = task.status == TaskStatus.COMPLETED,
                            onClick = { viewModel.setStatus(task.id, task.status.next()) },
                        )
                        IconButton(onClick = onStartTask) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                        }
                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showAdd) {
        AddTaskDialog(
            state = state,
            onDismiss = { showAdd = false },
            onConfirm = { subjectId, chapterId, minutes, note ->
                viewModel.addTask(subjectId, chapterId, null, null, minutes, note)
                showAdd = false
            },
        )
    }

    if (showGoal) {
        var goalText by remember { mutableStateOf(state.goalMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showGoal = false },
            title = { Text("Daily goal (minutes)") },
            text = {
                OutlinedTextField(
                    goalText,
                    { goalText = it.filter(Char::isDigit) },
                    label = { Text("Minutes") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    goalText.toIntOrNull()?.let(viewModel::setGoal)
                    showGoal = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showGoal = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTaskDialog(
    state: PlannerUiState,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?, Int, String) -> Unit,
) {
    var subjectId by remember { mutableStateOf(state.subjects.firstOrNull()?.id) }
    var chapterId by remember { mutableStateOf<Long?>(null) }
    var minutes by remember { mutableStateOf(45) }
    var note by remember { mutableStateOf("") }

    val chapters = state.chapters.filter { it.subjectId == subjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add task") },
        text = {
            Column {
                Text("Subject", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.subjects.forEach { subject ->
                        Pill(
                            text = subject.name,
                            selected = subjectId == subject.id,
                            onClick = {
                                subjectId = subject.id
                                chapterId = null
                            },
                        )
                    }
                }
                if (chapters.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Chapter (optional)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chapters.forEach { chapter ->
                            Pill(
                                text = chapter.name,
                                selected = chapterId == chapter.id,
                                onClick = { chapterId = if (chapterId == chapter.id) null else chapter.id },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    minutes.toString(),
                    { minutes = it.filter(Char::isDigit).toIntOrNull() ?: 0 },
                    label = { Text("Planned minutes") },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { subjectId?.let { onConfirm(it, chapterId, minutes, note) } },
                enabled = subjectId != null,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun TaskStatus.label(): String = when (this) {
    TaskStatus.PLANNED -> "Planned"
    TaskStatus.IN_PROGRESS -> "Doing"
    TaskStatus.COMPLETED -> "Done"
    TaskStatus.SKIPPED -> "Skipped"
}

private fun TaskStatus.next(): TaskStatus = when (this) {
    TaskStatus.PLANNED -> TaskStatus.IN_PROGRESS
    TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
    TaskStatus.COMPLETED -> TaskStatus.SKIPPED
    TaskStatus.SKIPPED -> TaskStatus.PLANNED
}
