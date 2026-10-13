package com.chronostudy.app.domain.model

/** Lifecycle of a focus session. Transitions are validated by the timer state machine. */
enum class SessionState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED;

    val isActive: Boolean get() = this == RUNNING || this == PAUSED
    val isFinal: Boolean get() = this == COMPLETED || this == CANCELLED
}

/** Academic progress of a chapter. Independent from tracked study duration. */
enum class ChapterProgress {
    NOT_STARTED,
    IN_PROGRESS,
    STUDIED,
    REVISION_NEEDED,
    COMPLETED,
}

/** Planner task lifecycle. Completing a task never awards study time by itself. */
enum class TaskStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
}

/** How a session-completion alert is delivered. */
enum class AlertMode {
    SOUND_AND_VIBRATION,
    SOUND_ONLY,
    VIBRATION_ONLY,
    SILENT,
}

/** What happens to partial time when the user finishes a focus session early. */
enum class EarlyFinishBehavior {
    /** Keep the elapsed eligible focus time. */
    RECORD_ELAPSED,

    /** Ask the user whether to keep or discard the partial time. */
    ASK,
}

/** How a student entered a diploma programme. */
enum class EntryType {
    NORMAL,
    LATERAL,
    CUSTOM,
}
