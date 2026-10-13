package com.chronostudy.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.chronostudy.app.ChronoApplication
import com.chronostudy.app.domain.timer.SessionTimer
import com.chronostudy.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service for a user-initiated focus session. It owns no timing state — the session is
 * reconstructed from persisted timestamps — and simply mirrors the live countdown into an ongoing
 * notification while the screen may be off or the app backgrounded. It stops itself as soon as
 * there is no active session.
 */
class FocusTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collector: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundSafely(buildPlaceholder())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        observeActiveSession()
        return START_NOT_STICKY
    }

    private fun observeActiveSession() {
        collector?.cancel()
        collector = scope.launch {
            val container = (application as ChronoApplication).container
            container.sessionRepository.activeSession.collectLatest { session ->
                if (session == null) {
                    stopForegroundAndSelf()
                    return@collectLatest
                }
                val subjectName = container.libraryRepository.getSubject(session.subjectId)?.name ?: "Focus"
                val chapterName = session.chapterId
                    ?.let { container.libraryRepository.getChapter(it)?.name }
                val text = buildString {
                    append(subjectName)
                    if (!chapterName.isNullOrBlank()) append(" • ").append(chapterName)
                    if (session.state.name == "PAUSED") append(" • paused")
                }
                val remaining = SessionTimer.remainingMillis(
                    session,
                    container.timeProvider.elapsedRealtimeMillis(),
                    container.timeProvider.wallClockMillis(),
                )
                val endWall = container.timeProvider.wallClockMillis() + remaining
                startForegroundSafely(
                    container.notificationHelper.buildOngoingSession(
                        title = "Focus session",
                        text = text,
                        endWallClockMillis = endWall,
                    ),
                )
            }
        }
    }

    private fun buildPlaceholder() =
        (application as ChronoApplication).container.notificationHelper.buildOngoingSession(
            title = "Focus session",
            text = "Starting…",
            endWallClockMillis = System.currentTimeMillis() + 60_000L,
        )

    private fun startForegroundSafely(notification: android.app.Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.FOCUS_SERVICE_NOTIFICATION_ID,
                notification,
                type,
            )
        }
    }

    private fun stopForegroundAndSelf() {
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
