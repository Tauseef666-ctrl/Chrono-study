package com.chronostudy.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that keeps an active, user-initiated focus session alive while the
 * screen is off or the app is backgrounded. The authoritative timing state lives in the
 * database (reconstructed from persisted timestamps); this service only surfaces the
 * ongoing notification and requests the completion check. It is started only while a
 * session is active and is stopped when the session ends.
 */
class FocusTimerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
