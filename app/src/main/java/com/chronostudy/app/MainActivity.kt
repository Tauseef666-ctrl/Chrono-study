package com.chronostudy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chronostudy.app.ui.theme.ChronoTheme
import com.chronostudy.app.ui.theme.ThemePreset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChronoTheme(preset = ThemePreset.OLED_BLACK, accent = null) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("Chrono Study")
                }
            }
        }
    }
}
