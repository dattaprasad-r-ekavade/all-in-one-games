package com.datathecodie.witgrid.nav

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.datathecodie.witgrid.data.progress.RoomDailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.progress.RoomProgressRepository
import com.datathecodie.witgrid.data.progress.WitgridDatabase
import com.datathecodie.witgrid.data.settings.DataStorePlayerDataRepository
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.domain.daily.DailyClock
import com.datathecodie.witgrid.domain.daily.DailyProgressCoordinator
import com.datathecodie.witgrid.domain.daily.DailyProgressSnapshot
import com.datathecodie.witgrid.domain.config.DefaultConfig
import com.datathecodie.witgrid.domain.monetisation.AllowanceLedger
import com.datathecodie.witgrid.domain.monetisation.InterstitialTracker
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.screens.SettingsScreen
import com.datathecodie.witgrid.ui.screens.calendar.CalendarScreenRoute
import com.datathecodie.witgrid.ui.screens.failed.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.game.GameScreenRoute
import com.datathecodie.witgrid.ui.screens.home.HomeScreenRoute
import com.datathecodie.witgrid.ui.screens.play.PlayScreenRoute
import com.datathecodie.witgrid.ui.screens.play.PlayPersistenceScope
import com.datathecodie.witgrid.ui.screens.result.ResultScreenRoute
import com.datathecodie.witgrid.ui.screens.result.nextResultLevel
import com.datathecodie.witgrid.ui.screens.stats.StatisticsScreenRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

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
    val dailyCompletionRepository = remember(database) {
        RoomDailyPuzzleCompletionRepository(database.dailyPuzzleCompletionDao())
    }
    val playerDataRepository = remember(applicationContext) {
        DataStorePlayerDataRepository(applicationContext)
    }
    val config = remember { DefaultConfig }
    val interstitialTracker = remember(playerDataRepository, config) {
        InterstitialTracker(playerDataRepository, config, System::currentTimeMillis)
    }
    val playerData by playerDataRepository.playerData.collectAsState(initial = PlayerData())
    val dailyClock = remember { DailyClock { System.currentTimeMillis() } }
    val dailyProgressCoordinator = remember(playerDataRepository, dailyCompletionRepository, dailyClock) {
        DailyProgressCoordinator(
            playerDataRepository = playerDataRepository,
            dailyPuzzleCompletionRepository = dailyCompletionRepository,
            clock = dailyClock,
            timeZoneProvider = { TimeZone.getDefault() },
        )
    }
    var dailyProgress by remember { mutableStateOf<DailyProgressSnapshot?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentDailyDay = dailyProgress?.today ?: dailyProgressCoordinator.today()
    val todayCompletionsFlow = remember(dailyCompletionRepository, currentDailyDay.epochDay) {
        dailyCompletionRepository.observeBetween(currentDailyDay.epochDay, currentDailyDay.epochDay)
    }
    val todayCompletions by todayCompletionsFlow.collectAsState(initial = emptyList())
    LaunchedEffect(destination, lifecycleOwner, dailyProgressCoordinator) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                dailyProgress = dailyProgressCoordinator.reconcile()
                delay(DAILY_STATE_REFRESH_MILLIS)
            }
        }
    }
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
                streak = dailyProgress?.streak ?: playerData.streak,
                completedDailyGamesToday = todayCompletions.size,
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
                    dailyCompletedToday = todayCompletions.any { it.gameId == currentDestination.gameId },
                    onContinue = { navigator.navigate(AppDestination.Play(it)) },
                    onSelectPack = { navigator.navigate(AppDestination.Play(it)) },
                    onOpenDaily = {
                        scope.launch {
                            val dailyRef = withContext(Dispatchers.IO) {
                                module.dailyRef(dailyProgressCoordinator.today().bankIndex, bankSource)
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
                val allowance = remember(
                    levelRef,
                    playerDataRepository,
                    dailyProgressCoordinator,
                    config,
                    applicationContext,
                ) {
                    AllowanceLedger(
                        playerDataRepository = playerDataRepository,
                        config = config,
                        localEpochDay = { dailyProgressCoordinator.today().epochDay },
                        isOnline = { hasValidatedInternet(applicationContext) },
                        freeChecksInEasyPack = levelRef.pack == Pack.EASY,
                    )
                }
                val matchingRestartRequest = restartRequest?.takeIf { it.levelRef == levelRef }
                PlayScreenRoute(
                    levelRef = levelRef,
                    gameModule = module,
                    bankSource = bankSource,
                    progressRepository = progressRepository,
                    persistence = persistence,
                    allowance = allowance,
                    restartRequestId = matchingRestartRequest?.id,
                    onRestartRequestConsumed = { requestId ->
                        if (restartRequest?.id == requestId) restartRequest = null
                    },
                    onRegisterBackAction = { action -> activeBackAction = action },
                    onSolved = { solved ->
                        if (solved.levelRef.daily) {
                            persistence.scope.launch {
                                dailyProgress = dailyProgressCoordinator.completeDailyPuzzle(
                                    gameId = solved.levelRef.gameId,
                                    levelId = solved.levelRef.levelId,
                                )
                                interstitialTracker.recordLevelCompleted(solved.levelRef.gameId)
                                if (navigator.destination.value == currentDestination) {
                                    navigator.goBack()
                                    navigator.navigate(AppDestination.Result(solved.levelRef))
                                }
                            }
                        } else {
                            persistence.scope.launch {
                                interstitialTracker.recordLevelCompleted(solved.levelRef.gameId)
                                if (navigator.destination.value == currentDestination) {
                                    navigator.goBack()
                                    navigator.navigate(AppDestination.Result(solved.levelRef))
                                }
                            }
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

            AppDestination.Calendar -> CalendarScreenRoute(
                dailyProgress = dailyProgress,
                completionRepository = dailyCompletionRepository,
                gameIds = GameRegistry.games.map { it.id },
                onOpenDailyPuzzle = { gameId ->
                    val module = requireNotNull(GameRegistry.find(gameId))
                    scope.launch {
                        val dailyRef = withContext(Dispatchers.IO) {
                            module.dailyRef(dailyProgressCoordinator.today().bankIndex, bankSource)
                        }
                        if (navigator.destination.value == AppDestination.Calendar) {
                            navigator.navigate(AppDestination.Play(dailyRef))
                        }
                    }
                },
                onHome = navigator::returnHome,
            )
            AppDestination.Statistics -> StatisticsScreenRoute(
                gameIds = GameRegistry.games.map { it.id },
                levelRefsByGame = levelRefsByGame,
                progressRepository = progressRepository,
                dailyPuzzleCompletionRepository = dailyCompletionRepository,
                onHome = navigator::returnHome,
            )
            AppDestination.Settings -> SettingsScreen(onHome = navigator::returnHome)
        }
    }
}

private data class PlayRestartRequest(val levelRef: LevelRef, val id: Long)

private fun hasValidatedInternet(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private const val BACK_TRANSLATION_FRACTION = 0.08f
private const val BACK_SCALE_FRACTION = 0.04f
private const val BACK_ALPHA_FRACTION = 0.16f
private const val DAILY_STATE_REFRESH_MILLIS = 60_000L
