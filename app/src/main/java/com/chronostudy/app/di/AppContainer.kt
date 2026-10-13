package com.chronostudy.app.di

import android.content.Context
import com.chronostudy.app.alert.AlertPlayer
import com.chronostudy.app.alarm.SessionAlarmScheduler
import com.chronostudy.app.data.backup.BackupRepository
import com.chronostudy.app.data.local.ChronoDatabase
import com.chronostudy.app.data.local.DefaultData
import com.chronostudy.app.data.repository.GoalRepository
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.data.settings.SettingsRepository
import com.chronostudy.app.domain.time.SystemTimeProvider
import com.chronostudy.app.domain.time.TimeProvider
import com.chronostudy.app.notification.NotificationHelper
import com.chronostudy.app.session.SessionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container — small enough that a DI framework is unnecessary. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ChronoDatabase = ChronoDatabase.build(appContext)
    val timeProvider: TimeProvider = SystemTimeProvider()

    val libraryRepository = LibraryRepository(
        courseDao = database.courseDao(),
        subjectDao = database.subjectDao(),
        chapterDao = database.chapterDao(),
        timeProvider = timeProvider,
    )
    val sessionRepository = SessionRepository(
        dao = database.studySessionDao(),
        timeProvider = timeProvider,
    )
    val plannerRepository = PlannerRepository(
        dao = database.plannerTaskDao(),
        timeProvider = timeProvider,
    )
    val goalRepository = GoalRepository(database.dailyGoalDao())
    val settingsRepository = SettingsRepository(appContext)

    val alertPlayer = AlertPlayer(appContext)
    val notificationHelper = NotificationHelper(appContext)
    val alarmScheduler = SessionAlarmScheduler(appContext)

    val sessionCoordinator = SessionCoordinator(
        context = appContext,
        sessions = sessionRepository,
        library = libraryRepository,
        settings = settingsRepository,
        alerts = alertPlayer,
        notifications = notificationHelper,
        alarms = alarmScheduler,
        timeProvider = timeProvider,
    )

    val backupRepository = BackupRepository(
        database = database,
        library = libraryRepository,
        sessions = sessionRepository,
        planner = plannerRepository,
        goals = goalRepository,
        settings = settingsRepository,
    )

    init {
        applicationScope.launch { DefaultData.seedIfEmpty(database) }
    }
}
