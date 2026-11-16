package com.datathecodie.witgrid.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.datathecodie.witgrid.data.progress.RoomProgressRepository
import com.datathecodie.witgrid.data.progress.WitgridDatabase
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.screens.CalendarScreen
import com.datathecodie.witgrid.ui.screens.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.PlayScreen
import com.datathecodie.witgrid.ui.screens.ResultScreen
import com.datathecodie.witgrid.ui.screens.SettingsScreen
import com.datathecodie.witgrid.ui.screens.StatisticsScreen
import com.datathecodie.witgrid.ui.screens.game.GameScreenRoute
import com.datathecodie.witgrid.ui.screens.game.dailyPuzzleIndex
import com.datathecodie.witgrid.ui.screens.home.HomeScreenRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WitgridNavigation(navigator: AppNavigator) {
    val destination by navigator.destination.collectAsState()
    val applicationContext = LocalContext.current.applicationContext
    val bankSource = remember(applicationContext) { AssetBankSource(applicationContext.assets) }
    val database = remember(applicationContext) { WitgridDatabase.create(applicationContext) }
    DisposableEffect(database) {
        onDispose { database.close() }
    }
    val progressRepository = remember(database) { RoomProgressRepository(database.levelProgressDao()) }
    val scope = rememberCoroutineScope()
    val catalog by produceState<Map<String, List<LevelRef>>?>(null, bankSource) {
        value = withContext(Dispatchers.IO) {
            GameRegistry.games.associate { module ->
                module.id to Pack.entries.flatMap { pack -> module.levelRefs(pack, bankSource) }
            }
        }
    }

    BackHandler(enabled = navigator.canGoBack) {
        navigator.goBack()
    }

    val levelRefsByGame = catalog
    if (levelRefsByGame == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    when (val currentDestination = destination) {
        AppDestination.Home -> HomeScreenRoute(
            gameIds = GameRegistry.games.map { it.id },
            levelRefsByGame = levelRefsByGame,
            progressRepository = progressRepository,
            onSelectGame = { navigator.navigate(AppDestination.Game(it)) },
            onOpenCalendar = { navigator.navigate(AppDestination.Calendar) },
            onOpenStatistics = { navigator.navigate(AppDestination.Statistics) },
            onOpenSettings = { navigator.navigate(AppDestination.Settings) },
        )

        is AppDestination.Game -> {
            val module = requireNotNull(GameRegistry.find(currentDestination.gameId))
            val levelRefsByPack = remember(currentDestination.gameId, levelRefsByGame) {
                Pack.entries.associateWith { pack ->
                    levelRefsByGame[currentDestination.gameId].orEmpty().filter { it.pack == pack }
                }
            }
            GameScreenRoute(
                gameId = currentDestination.gameId,
                levelRefsByPack = levelRefsByPack,
                progressRepository = progressRepository,
                onContinue = { navigator.navigate(AppDestination.Play(it)) },
                onSelectPack = { navigator.navigate(AppDestination.Play(it)) },
                onOpenDaily = {
                    scope.launch {
                        val dailyRef = withContext(Dispatchers.IO) {
                            module.dailyRef(dailyPuzzleIndex(System.currentTimeMillis()), bankSource)
                        }
                        if (navigator.destination.value == currentDestination) {
                            navigator.navigate(AppDestination.Play(dailyRef))
                        }
                    }
                },
                onHome = navigator::returnHome,
            )
        }

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
