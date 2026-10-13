package com.chronostudy.app.domain.util

import java.util.Locale

object DurationFormat {

    /** "12:34" for under an hour, "1:02:34" otherwise. */
    fun clock(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0) / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** "1h 25m", "45m", "30s". Rounds down to the most useful unit. */
    fun human(millis: Long): String {
        val totalMinutes = millis.coerceAtLeast(0) / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            totalMinutes > 0 -> "${minutes}m"
            else -> "${millis.coerceAtLeast(0) / 1000}s"
        }
    }

    /** "1.5h" style compact value for charts and tight spaces. */
    fun hoursDecimal(millis: Long): String {
        val hours = millis.coerceAtLeast(0) / 3_600_000.0
        return String.format(Locale.US, "%.1fh", hours)
    }

    fun minutesLabel(minutes: Int): String = human(minutes * 60_000L)
}
