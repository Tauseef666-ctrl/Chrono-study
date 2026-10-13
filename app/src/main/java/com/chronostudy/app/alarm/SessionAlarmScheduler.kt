package com.chronostudy.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Schedules a single alarm for the expected completion time of a focus session so the completion
 * is delivered even if the activity is gone. Uses an exact alarm when permitted and falls back to
 * an inexact (idle-tolerant) alarm otherwise. The alarm is only a signal — the authoritative
 * timing always comes from the persisted session.
 */
class SessionAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService()

    fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager?.canScheduleExactAlarms() == true
    }

    fun scheduleCompletion(sessionId: Long, triggerAtWallClockMillis: Long) {
        val manager = alarmManager ?: return
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            action = ACTION_SESSION_COMPLETE
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(sessionId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            if (canScheduleExact()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallClockMillis, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallClockMillis, pending)
            }
        }
    }

    fun cancel(sessionId: Long) {
        val manager = alarmManager ?: return
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            action = ACTION_SESSION_COMPLETE
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(sessionId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching { manager.cancel(pending) }
    }

    private fun requestCode(sessionId: Long): Int = (sessionId % Int.MAX_VALUE).toInt()

    companion object {
        const val ACTION_SESSION_COMPLETE = "com.chronostudy.app.ACTION_SESSION_COMPLETE"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
