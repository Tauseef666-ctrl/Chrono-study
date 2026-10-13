package com.chronostudy.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.chronostudy.app.MainActivity
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

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Ongoing, low-priority notification showing the live countdown while a session runs. */
    fun buildOngoingSession(
        title: String,
        text: String,
        endWallClockMillis: Long,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_chrono_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent())
            .setWhen(endWallClockMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()

    fun showCompletion(title: String, text: String, isBreak: Boolean) {
        val notification = NotificationCompat.Builder(
            context,
            if (isBreak) CHANNEL_BREAK else CHANNEL_FOCUS,
        )
            .setSmallIcon(R.drawable.ic_chrono_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent())
            .build()

        val managerCompat = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        try {
            managerCompat.notify(
                if (isBreak) BREAK_COMPLETE_NOTIFICATION_ID else FOCUS_COMPLETE_NOTIFICATION_ID,
                notification,
            )
        } catch (ignored: SecurityException) {
        } catch (ignored: Exception) {
        }
    }

    fun cancel(id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
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
