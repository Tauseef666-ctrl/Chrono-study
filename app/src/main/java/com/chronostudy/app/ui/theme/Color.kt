package com.chronostudy.app.ui.theme

import androidx.compose.ui.graphics.Color

object ChronoColors {
    val OledBlack = Color(0xFF000000)
    val Graphite = Color(0xFF0E1013)
    val Charcoal = Color(0xFF16181C)
    val CharcoalSoft = Color(0xFF1E2127)
    val Slate = Color(0xFF2A2E36)
    val Outline = Color(0xFF3A3F49)

    val White = Color(0xFFFFFFFF)
    val SoftWhite = Color(0xFFE8EAED)
    val Gray = Color(0xFFB7BCC4)
    val MutedGray = Color(0xFF8A9099)

    val Amber = Color(0xFFFFB020)
    val AmberSoft = Color(0xFFFFC766)
    val Teal = Color(0xFF2Fbf9F)
    val Green = Color(0xFF4CC38A)
    val Violet = Color(0xFF9B7BFF)
    val Blue = Color(0xFF4C8DFF)
    val Red = Color(0xFFE5675B)

    val LightBackground = Color(0xFFF6F7F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFECEEF2)
    val LightOutline = Color(0xFFD3D7DE)
    val LightOnBackground = Color(0xFF14161A)
    val LightMuted = Color(0xFF5C626B)
}

enum class ThemePreset(
    val displayName: String,
    val isLight: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val onBackground: Color,
    val defaultAccent: Accent,
) {
    OLED_BLACK(
        displayName = "OLED Black",
        isLight = false,
        background = ChronoColors.OledBlack,
        surface = ChronoColors.Graphite,
        surfaceVariant = ChronoColors.Charcoal,
        outline = ChronoColors.Outline,
        onBackground = ChronoColors.White,
        defaultAccent = Accent.Amber,
    ),
    CHARCOAL(
        displayName = "Charcoal",
        isLight = false,
        background = ChronoColors.Charcoal,
        surface = ChronoColors.CharcoalSoft,
        surfaceVariant = ChronoColors.Slate,
        outline = ChronoColors.Outline,
        onBackground = ChronoColors.SoftWhite,
        defaultAccent = Accent.Amber,
    ),
    PURE_WHITE(
        displayName = "Pure White",
        isLight = true,
        background = ChronoColors.LightBackground,
        surface = ChronoColors.LightSurface,
        surfaceVariant = ChronoColors.LightSurfaceVariant,
        outline = ChronoColors.LightOutline,
        onBackground = ChronoColors.LightOnBackground,
        defaultAccent = Accent.Amber,
    ),
    OCEAN(
        displayName = "Ocean",
        isLight = false,
        background = Color(0xFF04080F),
        surface = Color(0xFF0A1220),
        surfaceVariant = Color(0xFF10203A),
        outline = Color(0xFF23395B),
        onBackground = Color(0xFFE6EEF8),
        defaultAccent = Accent.Blue,
    ),
    AMBER_FOCUS(
        displayName = "Amber Focus",
        isLight = false,
        background = Color(0xFF0C0903),
        surface = Color(0xFF171006),
        surfaceVariant = Color(0xFF241708),
        outline = Color(0xFF4A3413),
        onBackground = Color(0xFFF6ECDA),
        defaultAccent = Accent.Amber,
    ),
    FOREST(
        displayName = "Forest",
        isLight = false,
        background = Color(0xFF030A06),
        surface = Color(0xFF0A140E),
        surfaceVariant = Color(0xFF102318),
        outline = Color(0xFF264A34),
        onBackground = Color(0xFFE4F0E8),
        defaultAccent = Accent.Green,
    ),
    VIOLET(
        displayName = "Violet",
        isLight = false,
        background = Color(0xFF07050D),
        surface = Color(0xFF100C1A),
        surfaceVariant = Color(0xFF1A1329),
        outline = Color(0xFF382A57),
        onBackground = Color(0xFFEDE8F8),
        defaultAccent = Accent.Violet,
    ),
}

enum class Accent(
    val displayName: String,
    val color: Color,
) {
    Amber("Amber", ChronoColors.Amber),
    Orange("Orange", Color(0xFFFF8A3D)),
    Teal("Teal", ChronoColors.Teal),
    Green("Green", ChronoColors.Green),
    Blue("Blue", ChronoColors.Blue),
    Violet("Violet", ChronoColors.Violet),
    Rose("Rose", Color(0xFFFF7A9A)),
}

fun ThemePreset.resolveAccent(accent: Accent?): Accent = accent ?: defaultAccent
