package com.chronostudy.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronostudy.app.domain.model.AlertMode
import com.chronostudy.app.domain.model.EarlyFinishBehavior
import com.chronostudy.app.ui.theme.Accent
import com.chronostudy.app.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ClockAlignment { CENTER, START }

@JvmInline
value class ClockDateFormat(val key: String) {
    companion object {
        val DayMonth = ClockDateFormat("day_month")
        val MonthDay = ClockDateFormat("month_day")
        val WeekdayFirst = ClockDateFormat("weekday_first")
        val all = listOf(DayMonth, MonthDay, WeekdayFirst)
    }
}

data class ChronoSettings(
    // Appearance
    val themePreset: ThemePreset = ThemePreset.OLED_BLACK,
    val accent: Accent? = null,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val showDate: Boolean = true,
    val dateFormat: ClockDateFormat = ClockDateFormat.WeekdayFirst,
    val showBattery: Boolean = true,
    val showStudyTimer: Boolean = true,
    val showSubjectChapter: Boolean = true,
    val showGoalProgress: Boolean = true,
    val showNextTask: Boolean = true,
    val clockAlignment: ClockAlignment = ClockAlignment.CENTER,
    val clockScale: Float = 1.0f,
    val lowBrightness: Boolean = false,
    val autoDimEnabled: Boolean = false,
    val autoDimAfterMinutes: Int = 5,
    val dimLevel: Float = 0.15f,
    // Focus & breaks
    val defaultFocusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val autoStartBreak: Boolean = false,
    val autoStartNextFocus: Boolean = false,
    val longBreakEveryN: Int = 4,
    val earlyFinishBehavior: EarlyFinishBehavior = EarlyFinishBehavior.RECORD_ELAPSED,
    // Alerts
    val alertMode: AlertMode = AlertMode.SOUND_AND_VIBRATION,
    val focusSoundKey: String = "chime",
    val breakSoundKey: String = "soft",
    val vibrationPatternKey: String = "default",
    val repeatAlert: Boolean = false,
    // Goals
    val dailyGoalMinutes: Int = 120,
    val weeklyGoalMinutes: Int = 600,
    val reminderEnabled: Boolean = false,
    // Misc
    val lastSubjectId: Long? = null,
    val lastChapterId: Long? = null,
    val onboardingDone: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chrono_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<ChronoSettings> = store.data.map { prefs -> prefs.toSettings() }

    suspend fun setTheme(preset: ThemePreset) = edit { it[Keys.THEME] = preset.name }
    suspend fun setAccent(accent: Accent?) = edit { it[Keys.ACCENT] = accent?.name ?: "" }
    suspend fun set24Hour(value: Boolean) = edit { it[Keys.USE_24H] = value }
    suspend fun setShowSeconds(value: Boolean) = edit { it[Keys.SHOW_SECONDS] = value }
    suspend fun setShowDate(value: Boolean) = edit { it[Keys.SHOW_DATE] = value }
    suspend fun setDateFormat(value: ClockDateFormat) = edit { it[Keys.DATE_FORMAT] = value.key }
    suspend fun setShowBattery(value: Boolean) = edit { it[Keys.SHOW_BATTERY] = value }
    suspend fun setShowStudyTimer(value: Boolean) = edit { it[Keys.SHOW_STUDY_TIMER] = value }
    suspend fun setShowSubjectChapter(value: Boolean) = edit { it[Keys.SHOW_SUBJECT_CHAPTER] = value }
    suspend fun setShowGoalProgress(value: Boolean) = edit { it[Keys.SHOW_GOAL_PROGRESS] = value }
    suspend fun setShowNextTask(value: Boolean) = edit { it[Keys.SHOW_NEXT_TASK] = value }
    suspend fun setClockAlignment(value: ClockAlignment) = edit { it[Keys.CLOCK_ALIGNMENT] = value.name }
    suspend fun setClockScale(value: Float) = edit { it[Keys.CLOCK_SCALE] = value.coerceIn(0.8f, 1.4f) }
    suspend fun setLowBrightness(value: Boolean) = edit { it[Keys.LOW_BRIGHTNESS] = value }
    suspend fun setAutoDimEnabled(value: Boolean) = edit { it[Keys.AUTO_DIM_ENABLED] = value }
    suspend fun setAutoDimAfterMinutes(value: Int) = edit { it[Keys.AUTO_DIM_MINUTES] = value.coerceIn(1, 60) }
    suspend fun setDimLevel(value: Float) = edit { it[Keys.DIM_LEVEL] = value.coerceIn(0.05f, 0.8f) }

    suspend fun setDefaultFocusMinutes(value: Int) = edit { it[Keys.DEFAULT_FOCUS] = value.coerceIn(1, 240) }
    suspend fun setShortBreakMinutes(value: Int) = edit { it[Keys.SHORT_BREAK] = value.coerceIn(1, 60) }
    suspend fun setLongBreakMinutes(value: Int) = edit { it[Keys.LONG_BREAK] = value.coerceIn(1, 120) }
    suspend fun setAutoStartBreak(value: Boolean) = edit { it[Keys.AUTO_START_BREAK] = value }
    suspend fun setAutoStartNextFocus(value: Boolean) = edit { it[Keys.AUTO_START_NEXT] = value }
    suspend fun setLongBreakEveryN(value: Int) = edit { it[Keys.LONG_BREAK_EVERY] = value.coerceIn(2, 10) }
    suspend fun setEarlyFinishBehavior(value: EarlyFinishBehavior) = edit { it[Keys.EARLY_FINISH] = value.name }

    suspend fun setAlertMode(value: AlertMode) = edit { it[Keys.ALERT_MODE] = value.name }
    suspend fun setFocusSoundKey(value: String) = edit { it[Keys.FOCUS_SOUND] = value }
    suspend fun setBreakSoundKey(value: String) = edit { it[Keys.BREAK_SOUND] = value }
    suspend fun setVibrationPatternKey(value: String) = edit { it[Keys.VIBRATION_PATTERN] = value }
    suspend fun setRepeatAlert(value: Boolean) = edit { it[Keys.REPEAT_ALERT] = value }

    suspend fun setDailyGoalMinutes(value: Int) = edit { it[Keys.DAILY_GOAL] = value.coerceIn(0, 1440) }
    suspend fun setWeeklyGoalMinutes(value: Int) = edit { it[Keys.WEEKLY_GOAL] = value.coerceIn(0, 10080) }
    suspend fun setReminderEnabled(value: Boolean) = edit { it[Keys.REMINDER_ENABLED] = value }

    suspend fun setLastSelection(subjectId: Long?, chapterId: Long?) = edit {
        it[Keys.LAST_SUBJECT] = subjectId ?: -1L
        it[Keys.LAST_CHAPTER] = chapterId ?: -1L
    }

    suspend fun setOnboardingDone(value: Boolean) = edit { it[Keys.ONBOARDING_DONE] = value }

    suspend fun resetPreferences() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private fun Preferences.toSettings(): ChronoSettings {
        fun theme(): ThemePreset =
            this[Keys.THEME]?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() }
                ?: ThemePreset.OLED_BLACK
        fun accent(): Accent? =
            this[Keys.ACCENT]?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Accent.valueOf(it) }.getOrNull() }
        fun alertMode(): AlertMode =
            this[Keys.ALERT_MODE]?.let { runCatching { AlertMode.valueOf(it) }.getOrNull() }
                ?: AlertMode.SOUND_AND_VIBRATION
        fun earlyFinish(): EarlyFinishBehavior =
            this[Keys.EARLY_FINISH]?.let { runCatching { EarlyFinishBehavior.valueOf(it) }.getOrNull() }
                ?: EarlyFinishBehavior.RECORD_ELAPSED

        return ChronoSettings(
            themePreset = theme(),
            accent = accent(),
            use24Hour = this[Keys.USE_24H] ?: true,
            showSeconds = this[Keys.SHOW_SECONDS] ?: true,
            showDate = this[Keys.SHOW_DATE] ?: true,
            dateFormat = ClockDateFormat(this[Keys.DATE_FORMAT] ?: ClockDateFormat.WeekdayFirst.key),
            showBattery = this[Keys.SHOW_BATTERY] ?: true,
            showStudyTimer = this[Keys.SHOW_STUDY_TIMER] ?: true,
            showSubjectChapter = this[Keys.SHOW_SUBJECT_CHAPTER] ?: true,
            showGoalProgress = this[Keys.SHOW_GOAL_PROGRESS] ?: true,
            showNextTask = this[Keys.SHOW_NEXT_TASK] ?: true,
            clockAlignment = this[Keys.CLOCK_ALIGNMENT]?.let { runCatching { ClockAlignment.valueOf(it) }.getOrNull() }
                ?: ClockAlignment.CENTER,
            clockScale = this[Keys.CLOCK_SCALE] ?: 1.0f,
            lowBrightness = this[Keys.LOW_BRIGHTNESS] ?: false,
            autoDimEnabled = this[Keys.AUTO_DIM_ENABLED] ?: false,
            autoDimAfterMinutes = this[Keys.AUTO_DIM_MINUTES] ?: 5,
            dimLevel = this[Keys.DIM_LEVEL] ?: 0.15f,
            defaultFocusMinutes = this[Keys.DEFAULT_FOCUS] ?: 25,
            shortBreakMinutes = this[Keys.SHORT_BREAK] ?: 5,
            longBreakMinutes = this[Keys.LONG_BREAK] ?: 15,
            autoStartBreak = this[Keys.AUTO_START_BREAK] ?: false,
            autoStartNextFocus = this[Keys.AUTO_START_NEXT] ?: false,
            longBreakEveryN = this[Keys.LONG_BREAK_EVERY] ?: 4,
            earlyFinishBehavior = earlyFinish(),
            alertMode = alertMode(),
            focusSoundKey = this[Keys.FOCUS_SOUND] ?: "chime",
            breakSoundKey = this[Keys.BREAK_SOUND] ?: "soft",
            vibrationPatternKey = this[Keys.VIBRATION_PATTERN] ?: "default",
            repeatAlert = this[Keys.REPEAT_ALERT] ?: false,
            dailyGoalMinutes = this[Keys.DAILY_GOAL] ?: 120,
            weeklyGoalMinutes = this[Keys.WEEKLY_GOAL] ?: 600,
            reminderEnabled = this[Keys.REMINDER_ENABLED] ?: false,
            lastSubjectId = (this[Keys.LAST_SUBJECT] ?: -1L).takeIf { it >= 0 },
            lastChapterId = (this[Keys.LAST_CHAPTER] ?: -1L).takeIf { it >= 0 },
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
        )
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val USE_24H = booleanPreferencesKey("use_24h")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        val SHOW_STUDY_TIMER = booleanPreferencesKey("show_study_timer")
        val SHOW_SUBJECT_CHAPTER = booleanPreferencesKey("show_subject_chapter")
        val SHOW_GOAL_PROGRESS = booleanPreferencesKey("show_goal_progress")
        val SHOW_NEXT_TASK = booleanPreferencesKey("show_next_task")
        val CLOCK_ALIGNMENT = stringPreferencesKey("clock_alignment")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val LOW_BRIGHTNESS = booleanPreferencesKey("low_brightness")
        val AUTO_DIM_ENABLED = booleanPreferencesKey("auto_dim_enabled")
        val AUTO_DIM_MINUTES = intPreferencesKey("auto_dim_minutes")
        val DIM_LEVEL = floatPreferencesKey("dim_level")

        val DEFAULT_FOCUS = intPreferencesKey("default_focus")
        val SHORT_BREAK = intPreferencesKey("short_break")
        val LONG_BREAK = intPreferencesKey("long_break")
        val AUTO_START_BREAK = booleanPreferencesKey("auto_start_break")
        val AUTO_START_NEXT = booleanPreferencesKey("auto_start_next")
        val LONG_BREAK_EVERY = intPreferencesKey("long_break_every")
        val EARLY_FINISH = stringPreferencesKey("early_finish")

        val ALERT_MODE = stringPreferencesKey("alert_mode")
        val FOCUS_SOUND = stringPreferencesKey("focus_sound")
        val BREAK_SOUND = stringPreferencesKey("break_sound")
        val VIBRATION_PATTERN = stringPreferencesKey("vibration_pattern")
        val REPEAT_ALERT = booleanPreferencesKey("repeat_alert")

        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")

        val LAST_SUBJECT = longPreferencesKey("last_subject")
        val LAST_CHAPTER = longPreferencesKey("last_chapter")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
