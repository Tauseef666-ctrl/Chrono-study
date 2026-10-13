package com.chronostudy.app.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.domain.util.DurationFormat
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.DurationStepper
import com.chronostudy.app.ui.components.EmptyState
import com.chronostudy.app.ui.components.Pill
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.util.parseColorHex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusSetupScreen(
    onStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: FocusSetupViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader(title = "Start a focus session", onBack = onBack)

        if (state.subjects.isEmpty()) {
            ChronoCard {
                EmptyState(
                    icon = Icons.Filled.MenuBook,
                    title = "No subjects yet",
                    message = "Add a subject in the Library before starting a focus session.",
                )
            }
            return@Column
        }

        SectionHeader("Subject")
        ChronoCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.subjects.forEach { subject ->
                    Pill(
                        text = subject.name,
                        selected = subject.id == state.selectedSubjectId,
                        color = parseColorHex(subject.colorHex),
                        onClick = { viewModel.selectSubject(subject.id) },
                    )
                }
            }
        }

        if (state.chapters.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Chapter (optional)")
            ChronoCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.chapters.forEach { chapter ->
                        Pill(
                            text = if (chapter.unitLabel.isBlank()) chapter.name else "${chapter.unitLabel} · ${chapter.name}",
                            selected = chapter.id == state.selectedChapterId,
                            onClick = { viewModel.selectChapter(chapter.id) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Duration")
        ChronoCard {
            Column {
                DurationStepper(
                    value = state.durationMinutes,
                    onChange = viewModel::setDuration,
                    range = 1..240,
                    label = "Focus length",
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60, 90).forEach { preset ->
                        Pill(
                            text = DurationFormat.minutesLabel(preset),
                            selected = state.durationMinutes == preset,
                            onClick = { viewModel.setDuration(preset) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Note (optional)")
        OutlinedTextField(
            value = note,
            onValueChange = {
                note = it
                viewModel.setNote(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What are you covering?") },
            maxLines = 3,
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.start()
                onStarted()
            },
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start ${DurationFormat.minutesLabel(state.durationMinutes)} focus")
        }
        Spacer(Modifier.height(24.dp))
    }
}
