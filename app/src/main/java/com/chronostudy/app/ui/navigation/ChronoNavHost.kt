package com.chronostudy.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chronostudy.app.data.settings.ChronoSettings
import com.chronostudy.app.ui.AppViewModelFactory
import com.chronostudy.app.ui.analytics.AnalyticsScreen
import com.chronostudy.app.ui.clock.ClockScreen
import com.chronostudy.app.ui.clock.ClockSettingsScreen
import com.chronostudy.app.ui.focus.BreakScreen
import com.chronostudy.app.ui.focus.FocusSetupScreen
import com.chronostudy.app.ui.focus.FocusTimerScreen
import com.chronostudy.app.ui.history.HistoryScreen
import com.chronostudy.app.ui.home.HomeScreen
import com.chronostudy.app.ui.library.LibraryScreen
import com.chronostudy.app.ui.main.MainViewModel
import com.chronostudy.app.ui.planner.PlannerScreen
import com.chronostudy.app.ui.settings.BackupScreen
import com.chronostudy.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val FOCUS = "focus"
    const val LIBRARY = "library"
    const val PLANNER = "planner"
    const val STATS = "stats"
    const val CLOCK = "clock"
    const val CLOCK_SETTINGS = "clock_settings"
    const val FOCUS_TIMER = "focus_timer"
    const val BREAK = "break"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.FOCUS, "Focus", Icons.Filled.Timer),
    BottomItem(Routes.LIBRARY, "Library", Icons.Filled.MenuBook),
    BottomItem(Routes.PLANNER, "Planner", Icons.Filled.Today),
    BottomItem(Routes.STATS, "Stats", Icons.Filled.BarChart),
)

@Composable
fun ChronoApp(settings: ChronoSettings) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(220)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenClock = { navController.navigate(Routes.CLOCK) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onStartFocus = { navController.navigateToTab(Routes.FOCUS) },
                )
            }
            composable(Routes.FOCUS) {
                FocusSetupScreen(
                    onStarted = {
                        navController.navigate(Routes.FOCUS_TIMER) {
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.PLANNER) {
                PlannerScreen(onStartTask = { navController.navigateToTab(Routes.FOCUS) })
            }
            composable(Routes.STATS) {
                AnalyticsScreen(onOpenHistory = { navController.navigate(Routes.HISTORY) })
            }
            composable(Routes.CLOCK) {
                ClockScreen(
                    onOpenSettings = { navController.navigate(Routes.CLOCK_SETTINGS) },
                    onExit = { navController.popBackStack() },
                    onFocus = { navController.navigate(Routes.FOCUS_TIMER) },
                    onBreak = { navController.navigate(Routes.BREAK) },
                )
            }
            composable(Routes.CLOCK_SETTINGS) {
                ClockSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FOCUS_TIMER) {
                FocusTimerScreen(
                    onFinished = { navController.navigate(Routes.BREAK) },
                    onDismissed = { navController.popBackStack() },
                    onOpenBreak = { navController.navigate(Routes.BREAK) },
                )
            }
            composable(Routes.BREAK) {
                BreakScreen(
                    onDone = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    onStartFocus = { navController.navigateToTab(Routes.FOCUS) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenClockSettings = { navController.navigate(Routes.CLOCK_SETTINGS) },
                )
            }
            composable(Routes.BACKUP) { BackupScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
