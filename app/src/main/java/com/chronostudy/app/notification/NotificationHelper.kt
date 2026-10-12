package com.chronostudy.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.chronostudy.app.R

class NotificationHelper(private val context: Context) {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOCUS,
                context.getString(R.string.notification_channel_focus),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_focus_desc)
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BREAK,
                context.getString(R.string.notification_channel_break),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_break_desc)
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                context.getString(R.string.notification_channel_general),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_general_desc)
            },
        )
    }

    companion object {
        const val CHANNEL_FOCUS = "chrono_focus"
        const val CHANNEL_BREAK = "chrono_break"
        const val CHANNEL_GENERAL = "chrono_general"

        const val FOCUS_SERVICE_NOTIFICATION_ID = 1001
        const val FOCUS_COMPLETE_NOTIFICATION_ID = 1002
        const val BREAK_COMPLETE_NOTIFICATION_ID = 1003
    }
}
