package com.chronostudy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class ChronoSemantics(
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val chartPalette: List<Color>,
    val isLight: Boolean,
)

val LocalChronoSemantics = staticCompositionLocalOf {
    ChronoSemantics(
        accent = ChronoColors.Amber,
        accentSoft = ChronoColors.AmberSoft,
        success = ChronoColors.Green,
        warning = ChronoColors.Amber,
        danger = ChronoColors.Red,
        chartPalette = listOf(ChronoColors.Amber, ChronoColors.Teal, ChronoColors.Blue, ChronoColors.Violet, ChronoColors.Green),
        isLight = false,
    )
}

object ChronoTheme {
    val semantics: ChronoSemantics
        @Composable @ReadOnlyComposable get() = LocalChronoSemantics.current
}

@Composable
fun ChronoTheme(
    preset: ThemePreset,
    accent: Accent?,
    content: @Composable () -> Unit,
) {
    val resolvedAccent = preset.resolveAccent(accent)
    val scheme = buildColorScheme(preset, resolvedAccent)
    val semantics = ChronoSemantics(
        accent = resolvedAccent.color,
        accentSoft = resolvedAccent.color.copy(alpha = 0.7f),
        success = ChronoColors.Green,
        warning = ChronoColors.Amber,
        danger = ChronoColors.Red,
        chartPalette = buildList {
            add(resolvedAccent.color)
            addAll(
                listOf(
                    ChronoColors.Teal,
                    ChronoColors.Blue,
                    ChronoColors.Violet,
                    ChronoColors.Green,
                    ChronoColors.Amber,
                    ChronoColors.Red,
                ).filter { it != resolvedAccent.color },
            )
        },
        isLight = preset.isLight,
    )
    CompositionLocalProvider(LocalChronoSemantics provides semantics) {
        MaterialTheme(
            colorScheme = scheme,
            typography = chronoTypography,
            content = content,
        )
    }
}

private fun buildColorScheme(preset: ThemePreset, accent: Accent) = run {
    val a = accent.color
    val onAccent = if (a.luminance() > 0.55f) Color(0xFF101214) else Color(0xFFFFFFFF)
    if (preset.isLight) {
        lightColorScheme(
            primary = a,
            onPrimary = onAccent,
            primaryContainer = a.copy(alpha = 0.18f),
            onPrimaryContainer = preset.onBackground,
            secondary = ChronoColors.MutedGray,
            onSecondary = Color.White,
            background = preset.background,
            onBackground = preset.onBackground,
            surface = preset.surface,
            onSurface = preset.onBackground,
            surfaceVariant = preset.surfaceVariant,
            onSurfaceVariant = ChronoColors.LightMuted,
            outline = preset.outline,
            outlineVariant = preset.outline.copy(alpha = 0.6f),
            error = ChronoColors.Red,
            onError = Color.White,
        )
    } else {
        darkColorScheme(
            primary = a,
            onPrimary = onAccent,
            primaryContainer = a.copy(alpha = 0.22f),
            onPrimaryContainer = preset.onBackground,
            secondary = ChronoColors.Gray,
            onSecondary = Color(0xFF101214),
            background = preset.background,
            onBackground = preset.onBackground,
            surface = preset.surface,
            onSurface = preset.onBackground,
            surfaceVariant = preset.surfaceVariant,
            onSurfaceVariant = ChronoColors.Gray,
            outline = preset.outline,
            outlineVariant = preset.outline.copy(alpha = 0.6f),
            error = ChronoColors.Red,
            onError = Color.White,
        )
    }
}

private fun Color.luminance(): Float {
    fun channel(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

val clockNumerals: TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Light,
    letterSpacing = 2.sp,
)

private val chronoTypography = Typography()
