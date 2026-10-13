package com.chronostudy.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chronostudy.app.ChronoApplication
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.timer.SessionTimer
import kotlinx.coroutines.launch

/**
 * Recovers an interrupted session after a reboot or app update. Because the monotonic clock
 * resets on reboot, the session is reconstructed from wall-clock timestamps and then either
 * completed (if its planned time already passed) or re-armed with a fresh alarm.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as? ChronoApplication ?: return
        val pendingResult = goAsync()
        app.container.applicationScope.launch {
            try {
                val container = app.container
                val session = container.sessionRepository.getActiveSession() ?: return@launch
                val remaining = SessionTimer.remainingMillis(
                    session,
                    container.timeProvider.elapsedRealtimeMillis(),
                    container.timeProvider.wallClockMillis(),
                )
                if (session.state == SessionState.RUNNING && remaining <= 0L) {
                    container.sessionCoordinator.completeRunning(session.id)
                } else if (session.state == SessionState.RUNNING) {
                    container.sessionCoordinator.rescheduleAlarm(session.id)
                    container.sessionCoordinator.startService()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
