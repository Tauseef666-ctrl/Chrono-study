package com.chronostudy.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.data.backup.ImportStrategy
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.components.ChronoCard
import com.chronostudy.app.ui.components.ScreenHeader
import com.chronostudy.app.ui.components.SectionHeader
import java.time.LocalDate

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    var pendingImport by remember { mutableStateOf<String?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
        pendingExportJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImport = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ScreenHeader(title = "Backup & restore", onBack = onBack)

        SectionHeader("Export")
        ChronoCard {
            Column {
                Text(
                    "Save a JSON file containing your library, sessions, planner and settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.buildExport { json ->
                            pendingExportJson = json
                            exportLauncher.launch(defaultFileName())
                        }
                    },
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (exporting) "Preparing…" else "Export backup")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Import")
        ChronoCard {
            Column {
                Text(
                    "Restore from a backup file. Merge adds new records; replace clears existing data first.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choose backup file")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ChronoCard {
            Text(
                "Backups never leave your device unless you share the exported file yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    pendingImport?.let { raw ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import backup") },
            text = { Text("How should this backup be applied?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.import(raw, ImportStrategy.REPLACE)
                    pendingImport = null
                }) { Text("Replace all") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
                    TextButton(onClick = {
                        viewModel.import(raw, ImportStrategy.MERGE)
                        pendingImport = null
                    }) { Text("Merge") }
                }
            },
        )
    }

    result?.let { outcome ->
        AlertDialog(
            onDismissRequest = { viewModel.clearResult() },
            title = { Text(if (outcome.success) "Import complete" else "Import failed") },
            text = {
                Text(
                    if (outcome.success) {
                        "${outcome.message}\n\nCourses: ${outcome.courses}, subjects: ${outcome.subjects}, " +
                            "chapters: ${outcome.chapters}, sessions: ${outcome.sessions}, tasks: ${outcome.plannerTasks}."
                    } else {
                        outcome.message
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearResult() }) { Text("OK") }
            },
        )
    }
}

private fun defaultFileName(): String = "chrono-study-backup-${LocalDate.now()}.json"
