package com.chronostudy.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chronostudy.app.ChronoApplication
import kotlinx.coroutines.launch

class SessionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SessionAlarmScheduler.ACTION_SESSION_COMPLETE) return
        val sessionId = intent.getLongExtra(SessionAlarmScheduler.EXTRA_SESSION_ID, -1L)
        if (sessionId < 0) return
        val app = context.applicationContext as? ChronoApplication ?: return
        val pendingResult = goAsync()
        app.container.applicationScope.launch {
            try {
                app.container.sessionCoordinator.completeIfDue(sessionId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
