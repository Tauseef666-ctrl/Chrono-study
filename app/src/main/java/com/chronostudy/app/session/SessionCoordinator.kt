package com.chronostudy.app.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.chronostudy.app.alert.AlertPlayer
import com.chronostudy.app.alarm.SessionAlarmScheduler
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.time.TimeProvider
import com.chronostudy.app.domain.timer.SessionTimer
import com.chronostudy.app.notification.NotificationHelper
import com.chronostudy.app.service.FocusTimerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Application service that coordinates a focus session across the timer engine, persistence,
 * background alarms and alerts. All completion paths funnel through [completeIfDue], which is
 * idempotent, so the UI, the alarm and the foreground service can all call it safely.
 */
class SessionCoordinator(
    private val context: Context,
    private val sessions: SessionRepository,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
    private val alerts: AlertPlayer,
    private val notifications: NotificationHelper,
    private val alarms: SessionAlarmScheduler,
    private val timeProvider: TimeProvider,
) {

    suspend fun activeSession(): StudySessionEntity? = sessions.getActiveSession()

    val activeSessionFlow: Flow<StudySessionEntity?> = sessions.activeSession

    suspend fun startSession(
        subjectId: Long,
        chapterId: Long?,
        plannedDurationMillis: Long,
        plannerTaskId: Long? = null,
        note: String = "",
    ): Long {
        sessions.getActiveSession()?.let { return it.id }
        val id = sessions.startSession(
            subjectId = subjectId,
            chapterId = chapterId,
            plannedDurationMillis = plannedDurationMillis,
            plannerTaskId = plannerTaskId,
            note = note,
        )
        scheduleAlarm(id)
        startService()
        settings.setLastSelection(subjectId, chapterId)
        return id
    }

    suspend fun pause(sessionId: Long) {
        if (sessions.pause(sessionId)) alarms.cancel(sessionId)
    }

    suspend fun resume(sessionId: Long) {
        if (sessions.resume(sessionId)) scheduleAlarm(sessionId)
    }

    suspend fun rescheduleAlarm(sessionId: Long) = scheduleAlarm(sessionId)

    suspend fun finishEarly(sessionId: Long): StudySessionEntity? {
        val result = sessions.finishEarly(sessionId)
        alarms.cancel(sessionId)
        stopService()
        return result
    }

    suspend fun cancel(sessionId: Long): Boolean {
        val ok = sessions.cancel(sessionId)
        alarms.cancel(sessionId)
        stopService()
        return ok
    }

    /**
     * Completes a session if its planned duration has elapsed and it is still running. Returns
     * true when this call actually finalised the session (and therefore delivered the alert).
     */
    suspend fun completeIfDue(sessionId: Long): Boolean {
        val session = sessions.getSession(sessionId) ?: return false
        if (session.state != SessionState.RUNNING) return false
        val remaining = SessionTimer.remainingMillis(
            session,
            timeProvider.elapsedRealtimeMillis(),
            timeProvider.wallClockMillis(),
        )
        if (remaining > DUE_TOLERANCE_MILLIS) return false
        return completeNow(session)
    }

    /** Force-completes a running session (used by the foreground service countdown). */
    suspend fun completeRunning(sessionId: Long): Boolean {
        val session = sessions.getSession(sessionId) ?: return false
        if (session.state != SessionState.RUNNING) return false
        return completeNow(session)
    }

    private suspend fun completeNow(session: StudySessionEntity): Boolean {
        val result = sessions.completeNaturally(session.id) ?: return false
        alarms.cancel(session.id)
        stopService()
        if (result.state == SessionState.COMPLETED) {
            deliverFocusCompletion(result)
        }
        return true
    }

    private suspend fun deliverFocusCompletion(session: StudySessionEntity) {
        val current = settings.settings.first()
        val subjectName = library.getSubject(session.subjectId)?.name ?: "Focus"
        notifications.showCompletion(
            title = "Focus session complete",
            text = "$subjectName • ${com.chronostudy.app.domain.util.DurationFormat.human(session.actualFocusMillis)} recorded",
            isBreak = false,
        )
        alerts.play(
            mode = current.alertMode,
            soundKey = current.focusSoundKey,
            vibrationKey = current.vibrationPatternKey,
            repeat = current.repeatAlert,
        )
    }

    fun signalBreakCompletion(minutes: Int) {
        // Break alerts are handled by the break view model; kept here for symmetry/idempotency.
    }

    private suspend fun scheduleAlarm(sessionId: Long) {
        val session = sessions.getSession(sessionId) ?: return
        if (session.state != SessionState.RUNNING) return
        val remaining = SessionTimer.remainingMillis(
            session,
            timeProvider.elapsedRealtimeMillis(),
            timeProvider.wallClockMillis(),
        )
        alarms.scheduleCompletion(sessionId, timeProvider.wallClockMillis() + remaining)
    }

    fun startService() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FocusTimerService::class.java),
            )
        }
    }

    fun stopService() {
        runCatching { context.stopService(Intent(context, FocusTimerService::class.java)) }
    }

    companion object {
        /** Allow a couple of seconds of scheduling slack when deciding "due". */
        const val DUE_TOLERANCE_MILLIS = 2_000L
    }
}
