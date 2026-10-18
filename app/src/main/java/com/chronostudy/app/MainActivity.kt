package com.chronostudy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.main.MainViewModel
import com.chronostudy.app.ui.navigation.ChronoApp
import com.chronostudy.app.ui.theme.ChronoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("chrono_prefs", MODE_PRIVATE)
        val needsPermissionPrompt =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED &&
                !prefs.getBoolean(PREF_NOTIFICATION_PROMPT_DONE, false)

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = AppViewModelFactory)
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            var showPermissionPrompt by remember { mutableStateOf(needsPermissionPrompt) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* App remains fully usable if denied; alert delivery degrades gracefully. */ }

            fun markPromptDone() {
                prefs.edit().putBoolean(PREF_NOTIFICATION_PROMPT_DONE, true).apply()
                showPermissionPrompt = false
            }

            ChronoTheme(preset = settings.themePreset, accent = settings.accent) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChronoApp(settings = settings)

                    if (showPermissionPrompt) {
                        AlertDialog(
                            onDismissRequest = ::markPromptDone,
                            title = { Text("Stay on schedule") },
                            text = {
                                Text(
                                    "Allow notifications so Chrono Study can remind you about your " +
                                        "study schedule, alert you when a focus timer finishes, and tell " +
                                        "you when a break starts. Notifications only appear while you use " +
                                        "the app or when a timer you started completes. You can change this " +
                                        "anytime in Android settings.",
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        markPromptDone()
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                ) { Text("Allow") }
                            },
                            dismissButton = {
                                TextButton(onClick = ::markPromptDone) { Text("Not now") }
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val PREF_NOTIFICATION_PROMPT_DONE = "notification_prompt_done"
    }
}
