package com.chronostudy.app.ui.util

import androidx.compose.ui.graphics.Color

fun parseColorHex(hex: String?, fallback: Color = Color(0xFF8A9099)): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val cleaned = hex.removePrefix("#")
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> fallback
        }
    }.getOrDefault(fallback)
}
