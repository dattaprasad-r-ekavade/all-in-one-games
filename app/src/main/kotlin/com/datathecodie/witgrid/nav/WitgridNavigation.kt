package com.datathecodie.witgrid.nav

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.datathecodie.witgrid.data.progress.RoomProgressRepository
import com.datathecodie.witgrid.data.progress.WitgridDatabase
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.screens.CalendarScreen
import com.datathecodie.witgrid.ui.screens.SettingsScreen
import com.datathecodie.witgrid.ui.screens.StatisticsScreen
import com.datathecodie.witgrid.ui.screens.failed.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.game.GameScreenRoute
import com.datathecodie.witgrid.ui.screens.game.dailyPuzzleIndex
import com.datathecodie.witgrid.ui.screens.home.HomeScreenRoute
import com.datathecodie.witgrid.ui.screens.play.PlayScreenRoute
import com.datathecodie.witgrid.ui.screens.play.PlayPersistenceScope
import com.datathecodie.witgrid.ui.screens.result.ResultScreenRoute
import com.datathecodie.witgrid.ui.screens.result.nextResultLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WitgridNavigation(navigator: AppNavigator) {
    val destination by navigator.destination.collectAsState()
    val applicationContext = LocalContext.current.applicationContext
    val bankSource = remember(applicationContext) { AssetBankSource(applicationContext.assets) }
    val database = remember(applicationContext) { WitgridDatabase.create(applicationContext) }
    val persistence = remember(database) { PlayPersistenceScope(Dispatchers.Main.immediate) }
    DisposableEffect(database, persistence) {
        onDispose { persistence.close(database::close) }
    }
    val progressRepository = remember(database) { RoomProgressRepository(database.levelProgressDao()) }
    val scope = rememberCoroutineScope()
    var restartRequest by remember { mutableStateOf<PlayRestartRequest?>(null) }
    var nextRestartRequestId by remember { mutableStateOf(0L) }
    var activeBackAction by remember(destination) {
        mutableStateOf<suspend () -> Unit>({ navigator.goBack() })
    }
    val catalog by produceState<Map<String, List<LevelRef>>?>(null, bankSource) {
        value = withContext(Dispatchers.IO) {
            GameRegistry.games.associate { module ->
                module.id to Pack.entries.flatMap { pack -> module.levelRefs(pack, bankSource) }
            }
        }
    }

    var backProgress by remember { mutableFloatStateOf(0f) }
    var backGestureFromLeft by remember { mutableStateOf(true) }
    PredictiveBackHandler(enabled = navigator.canGoBack) { progressFlow ->
        try {
            progressFlow.collect { event ->
                backProgress = event.progress
                backGestureFromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
            }
            backProgress = 1f
            activeBackAction()
        } catch (exception: CancellationException) {
            backProgress = 0f
            throw exception
        } finally {
            backProgress = 0f
        }
    }

    val levelRefsByGame = catalog
    if (levelRefsByGame == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val progress = backProgress.coerceIn(0f, 1f)
                val direction = if (backGestureFromLeft) 1f else -1f
                translationX = size.width * progress * BACK_TRANSLATION_FRACTION * direction
                scaleX = 1f - progress * BACK_SCALE_FRACTION
                scaleY = 1f - progress * BACK_SCALE_FRACTION
                alpha = 1f - progress * BACK_ALPHA_FRACTION
            },
    ) {
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

            is AppDestination.Play -> {
                val levelRef = currentDestination.levelRef
                val module = requireNotNull(GameRegistry.find(levelRef.gameId))
                val matchingRestartRequest = restartRequest?.takeIf { it.levelRef == levelRef }
                PlayScreenRoute(
                    levelRef = levelRef,
                    gameModule = module,
                    bankSource = bankSource,
                    progressRepository = progressRepository,
                    persistence = persistence,
                    restartRequestId = matchingRestartRequest?.id,
                    onRestartRequestConsumed = { requestId ->
                        if (restartRequest?.id == requestId) restartRequest = null
                    },
                    onRegisterBackAction = { action -> activeBackAction = action },
                    onSolved = { solved ->
                        if (navigator.destination.value == currentDestination) {
                            navigator.goBack()
                            navigator.navigate(AppDestination.Result(solved.levelRef))
                        }
                    },
                    onBack = { navigator.goBack() },
                )
            }

            is AppDestination.Result -> ResultScreenRoute(
                levelRef = currentDestination.levelRef,
                progressRepository = progressRepository,
                onNext = {
                    val next = nextResultLevel(
                        currentDestination.levelRef,
                        levelRefsByGame[currentDestination.levelRef.gameId].orEmpty(),
                    )
                    navigator.goBack()
                    if (next != null) navigator.navigate(AppDestination.Play(next))
                },
                onHome = navigator::returnHome,
            )

            is AppDestination.LevelFailed -> LevelFailedScreen(
                onContinue = { navigator.navigate(AppDestination.Play(currentDestination.levelRef)) },
                onRestart = {
                    nextRestartRequestId += 1
                    restartRequest = PlayRestartRequest(currentDestination.levelRef, nextRestartRequestId)
                    navigator.navigate(AppDestination.Play(currentDestination.levelRef))
                },
                onHome = navigator::returnHome,
            )

            AppDestination.Calendar -> CalendarScreen(onHome = navigator::returnHome)
            AppDestination.Statistics -> StatisticsScreen(onHome = navigator::returnHome)
            AppDestination.Settings -> SettingsScreen(onHome = navigator::returnHome)
        }
    }
}

private data class PlayRestartRequest(val levelRef: LevelRef, val id: Long)

private const val BACK_TRANSLATION_FRACTION = 0.08f
private const val BACK_SCALE_FRACTION = 0.04f
private const val BACK_ALPHA_FRACTION = 0.16f
