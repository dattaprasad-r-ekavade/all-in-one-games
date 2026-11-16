package com.datathecodie.witgrid.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.ui.screens.CalendarScreen
import com.datathecodie.witgrid.ui.screens.GameScreen
import com.datathecodie.witgrid.ui.screens.HomeScreen
import com.datathecodie.witgrid.ui.screens.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.PlayScreen
import com.datathecodie.witgrid.ui.screens.ResultScreen
import com.datathecodie.witgrid.ui.screens.SettingsScreen
import com.datathecodie.witgrid.ui.screens.StatisticsScreen

@Composable
fun WitgridNavigation(navigator: AppNavigator) {
    val destination by navigator.destination.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = navigator.canGoBack) {
        navigator.goBack()
    }

    when (val currentDestination = destination) {
        AppDestination.Home -> HomeScreen(
            gameIds = GameRegistry.games.map { it.id },
            onSelectGame = { navigator.navigate(AppDestination.Game(it)) },
            onOpenCalendar = { navigator.navigate(AppDestination.Calendar) },
            onOpenStatistics = { navigator.navigate(AppDestination.Statistics) },
            onOpenSettings = { navigator.navigate(AppDestination.Settings) },
        )

        is AppDestination.Game -> GameScreen(
            gameId = currentDestination.gameId,
            onStart = {
                val module = requireNotNull(GameRegistry.find(currentDestination.gameId))
                val levelRef = module.firstLevelRef(Pack.EASY, AssetBankSource(context.assets))
                navigator.navigate(AppDestination.Play(levelRef))
            },
            onHome = navigator::returnHome,
        )

        is AppDestination.Play -> PlayScreen(
            levelRef = currentDestination.levelRef,
            onComplete = { navigator.navigate(AppDestination.Result(currentDestination.levelRef)) },
            onFail = { navigator.navigate(AppDestination.LevelFailed(currentDestination.levelRef)) },
            onBack = { navigator.goBack() },
        )

        is AppDestination.Result -> ResultScreen(
            onNext = { navigator.navigate(AppDestination.Game(currentDestination.levelRef.gameId)) },
            onHome = navigator::returnHome,
        )

        is AppDestination.LevelFailed -> LevelFailedScreen(
            onContinue = { navigator.navigate(AppDestination.Play(currentDestination.levelRef)) },
            onRestart = { navigator.navigate(AppDestination.Play(currentDestination.levelRef)) },
            onHome = navigator::returnHome,
        )

        AppDestination.Calendar -> CalendarScreen(onHome = navigator::returnHome)
        AppDestination.Statistics -> StatisticsScreen(onHome = navigator::returnHome)
        AppDestination.Settings -> SettingsScreen(onHome = navigator::returnHome)
    }
}
