package com.chronostudy.app

import android.app.Application
import com.chronostudy.app.notification.NotificationHelper

class ChronoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper(this).ensureChannels()
    }
}
