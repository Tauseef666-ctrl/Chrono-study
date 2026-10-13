package com.chronostudy.app

import android.app.Application
import com.chronostudy.app.di.AppContainer

class ChronoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannels()
    }
}
