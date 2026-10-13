package com.chronostudy.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.domain.model.ChapterProgress
import com.chronostudy.app.domain.model.EntryType
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.Divider
import com.chronostudy.app.ui.components.Pill
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import com.chronostudy.app.ui.util.parseColorHex

private val subjectPalette = listOf(
    "#FFB020", "#4CC38A", "#4C8DFF", "#9B7BFF", "#E5675B", "#2FBF9F", "#F26B9B", "#C0CA33",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(factory = AppViewModelFactory),
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    val expandedCourses = remember { mutableStateMapOf<Long, Boolean>() }
    val expandedSubjects = remember { mutableStateMapOf<Long, Boolean>() }

    var showCourseDialog by remember { mutableStateOf(false) }
    var subjectForDialog by remember { mutableStateOf<Long?>(null) }
    var chapterForDialog by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }

    val activeCourses = courses.filter { !it.isArchived }
    val unassigned = subjects.filter { it.courseId == null && !it.isArchived }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Library",
                actions = {
                    IconButton(onClick = { showCourseDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add course")
                    }
                },
            )
        }

        if (activeCourses.isEmpty() && unassigned.isEmpty()) {
            item {
                ChronoCard {
                    Text("Your library is empty. Add a course to begin.")
                }
            }
        }

        items(activeCourses, key = { it.id }) { course ->
            val isExpanded = expandedCourses[course.id] == true
            val courseSubjects = subjects.filter { it.courseId == course.id && !it.isArchived }
            ChronoCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { expandedCourses[course.id] = !isExpanded },
                        ) {
                            Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    course.branch.takeIf { it.isNotBlank() },
                                    course.semester?.let { "Semester $it" },
                                    course.entryType.label(),
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { expandedCourses[course.id] = !isExpanded }) {
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                            )
                        }
                        IconButton(onClick = { subjectForDialog = course.id }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add subject")
                        }
                        IconButton(onClick = { pendingDelete = { viewModel.deleteCourse(course.id) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete course")
                        }
                    }

                    if (isExpanded) {
                        if (courseSubjects.isEmpty()) {
                            Text(
                                "No subjects yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        courseSubjects.forEach { subject ->
                            SubjectBlock(
                                subject = subject,
                                chapters = chapters.filter { it.subjectId == subject.id && !it.isArchived },
                                expanded = expandedSubjects[subject.id] == true,
                                onToggle = { expandedSubjects[subject.id] = expandedSubjects[subject.id] != true },
                                onAddChapter = { chapterForDialog = subject.id },
                                onDelete = { pendingDelete = { viewModel.deleteSubject(subject.id) } },
                                onCycleProgress = { chapter ->
                                    viewModel.setChapterProgress(chapter.id, chapter.progress.next())
                                },
                                onDeleteChapter = { chapter -> pendingDelete = { viewModel.deleteChapter(chapter.id) } },
                            )
                        }
                    }
                }
            }
        }

        if (unassigned.isNotEmpty()) {
            item { SectionHeader("Subjects without a course") }
            items(unassigned, key = { it.id }) { subject ->
                ChronoCard {
                    SubjectBlock(
                        subject = subject,
                        chapters = chapters.filter { it.subjectId == subject.id && !it.isArchived },
                        expanded = expandedSubjects[subject.id] == true,
                        onToggle = { expandedSubjects[subject.id] = expandedSubjects[subject.id] != true },
                        onAddChapter = { chapterForDialog = subject.id },
                        onDelete = { pendingDelete = { viewModel.deleteSubject(subject.id) } },
                        onCycleProgress = { chapter ->
                            viewModel.setChapterProgress(chapter.id, chapter.progress.next())
                        },
                        onDeleteChapter = { chapter -> pendingDelete = { viewModel.deleteChapter(chapter.id) } },
                    )
                }
            }
        }
    }

    if (showCourseDialog) {
        AddCourseDialog(
            onDismiss = { showCourseDialog = false },
            onConfirm = { name, branch, semester, entry ->
                viewModel.addCourse(name, branch, semester, entry)
                showCourseDialog = false
            },
        )
    }

    subjectForDialog?.let { courseId ->
        AddSubjectDialog(
            courses = courses,
            initialCourseId = courseId,
            onDismiss = { subjectForDialog = null },
            onConfirm = { name, selectedCourse, semester, color, target ->
                viewModel.addSubject(name, selectedCourse, semester, color, target)
                subjectForDialog = null
            },
        )
    }

    chapterForDialog?.let { subjectId ->
        AddChapterDialog(
            onDismiss = { chapterForDialog = null },
            onConfirm = { name, unit ->
                viewModel.addChapter(subjectId, name, unit)
                chapterForDialog = null
            },
        )
    }

    pendingDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete?") },
            text = { Text("This permanently removes the item and its study links. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    action()
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SubjectBlock(
    subject: SubjectEntity,
    chapters: List<ChapterEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddChapter: () -> Unit,
    onDelete: () -> Unit,
    onCycleProgress: (ChapterEntity) -> Unit,
    onDeleteChapter: (ChapterEntity) -> Unit,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(parseColorHex(subject.colorHex)),
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f).clickable(onClick = onToggle)) {
                Text(subject.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val meta = buildList {
                    if (subject.targetMinutesPerWeek != null) add("${subject.targetMinutesPerWeek} min/week")
                    add("${chapters.size} chapters")
                }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAddChapter) { Icon(Icons.Filled.Add, "Add chapter") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete subject") }
        }

        if (expanded) {
            Column(Modifier.padding(start = 22.dp, top = 4.dp)) {
                chapters.forEach { chapter ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(chapter.name, style = MaterialTheme.typography.bodyMedium)
                            if (chapter.unitLabel.isNotBlank()) {
                                Text(
                                    chapter.unitLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Pill(
                            text = chapter.progress.label(),
                            selected = chapter.progress != ChapterProgress.NOT_STARTED,
                            onClick = { onCycleProgress(chapter) },
                        )
                        IconButton(onClick = { onDeleteChapter(chapter) }) {
                            Icon(Icons.Filled.Delete, "Delete chapter")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int?, EntryType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var entry by remember { mutableStateOf(EntryType.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add course") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Course name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(branch, { branch = it }, label = { Text("Branch (optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    semester,
                    { semester = it.filter(Char::isDigit) },
                    label = { Text("Semester (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Entry type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryType.entries.forEach { type ->
                        Pill(
                            text = type.label(),
                            selected = entry == type,
                            onClick = { entry = type },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, branch, semester.toIntOrNull(), entry) },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSubjectDialog(
    courses: List<CourseEntity>,
    initialCourseId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Int?, String, Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var courseId by remember { mutableStateOf(initialCourseId) }
    var color by remember { mutableStateOf(subjectPalette.first()) }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add subject") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Subject name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    target,
                    { target = it.filter(Char::isDigit) },
                    label = { Text("Weekly target minutes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Colour", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectPalette.take(4).forEach { hex ->
                        Pill(
                            text = " ",
                            selected = color == hex,
                            color = parseColorHex(hex),
                            onClick = { color = hex },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectPalette.drop(4).forEach { hex ->
                        Pill(
                            text = " ",
                            selected = color == hex,
                            color = parseColorHex(hex),
                            onClick = { color = hex },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Course", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(text = "None", selected = courseId == null, onClick = { courseId = null })
                    courses.filter { !it.isArchived }.forEach { course ->
                        Pill(
                            text = course.name,
                            selected = courseId == course.id,
                            onClick = { courseId = course.id },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, courseId, null, color, target.toIntOrNull()) },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddChapterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add chapter") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Chapter name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit label (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, unit) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun EntryType.label(): String = when (this) {
    EntryType.NORMAL -> "Normal entry"
    EntryType.LATERAL -> "Lateral entry"
    EntryType.CUSTOM -> "Custom"
}

private fun ChapterProgress.label(): String = when (this) {
    ChapterProgress.NOT_STARTED -> "Not started"
    ChapterProgress.IN_PROGRESS -> "In progress"
    ChapterProgress.STUDIED -> "Studied"
    ChapterProgress.REVISION_NEEDED -> "Revise"
    ChapterProgress.COMPLETED -> "Done"
}

private fun ChapterProgress.next(): ChapterProgress = when (this) {
    ChapterProgress.NOT_STARTED -> ChapterProgress.IN_PROGRESS
    ChapterProgress.IN_PROGRESS -> ChapterProgress.STUDIED
    ChapterProgress.STUDIED -> ChapterProgress.REVISION_NEEDED
    ChapterProgress.REVISION_NEEDED -> ChapterProgress.COMPLETED
    ChapterProgress.COMPLETED -> ChapterProgress.NOT_STARTED
}
