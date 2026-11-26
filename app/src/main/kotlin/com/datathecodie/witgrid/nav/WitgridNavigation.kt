package com.datathecodie.witgrid.nav

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.datathecodie.witgrid.data.progress.RoomDailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.progress.RoomProgressRepository
import com.datathecodie.witgrid.data.progress.WitgridDatabase
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.ads.GoogleAdService
import com.datathecodie.witgrid.ads.flow.InterstitialAdFlow
import com.datathecodie.witgrid.ads.flow.RewardedAdFlow
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.billing.PlayBillingService
import com.datathecodie.witgrid.data.settings.DataStorePlayerDataRepository
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.domain.daily.DailyClock
import com.datathecodie.witgrid.domain.daily.DailyProgressCoordinator
import com.datathecodie.witgrid.domain.daily.DailyProgressSnapshot
import com.datathecodie.witgrid.domain.daily.DailyStreakPolicy
import com.datathecodie.witgrid.domain.config.DefaultConfig
import com.datathecodie.witgrid.domain.monetisation.AllowanceLedger
import com.datathecodie.witgrid.domain.monetisation.InterstitialTracker
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.AssetBankSource
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.PlayRegistry
import com.datathecodie.witgrid.games.arrows.ArrowsBoardOptions
import com.datathecodie.witgrid.games.arrows.LocalArrowsBoardOptions
import com.datathecodie.witgrid.games.binary.BinaryBoardOptions
import com.datathecodie.witgrid.games.binary.LocalBinaryBoardOptions
import com.datathecodie.witgrid.games.crowns.CrownsBoardOptions
import com.datathecodie.witgrid.games.crowns.LocalCrownsBoardOptions
import com.datathecodie.witgrid.games.minisudoku.LocalMiniSudokuBoardOptions
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoardOptions
import com.datathecodie.witgrid.games.nonogram.LocalNonogramBoardOptions
import com.datathecodie.witgrid.games.nonogram.NonogramBoardOptions
import com.datathecodie.witgrid.reminder.AndroidReminderAlarmScheduler
import com.datathecodie.witgrid.reminder.AndroidReminderPermissionStatus
import com.datathecodie.witgrid.reminder.ReminderController
import com.datathecodie.witgrid.reminder.ReminderEnableResult
import com.datathecodie.witgrid.ui.screens.settings.SettingsScreen
import com.datathecodie.witgrid.ui.screens.calendar.CalendarScreenRoute
import com.datathecodie.witgrid.ui.screens.failed.LevelFailedScreen
import com.datathecodie.witgrid.ui.screens.game.GameScreenRoute
import com.datathecodie.witgrid.ui.screens.game.GameRulesSheet
import com.datathecodie.witgrid.ui.screens.home.HomeScreenRoute
import com.datathecodie.witgrid.ui.screens.play.PlayScreenRoute
import com.datathecodie.witgrid.ui.screens.play.PlayPersistenceScope
import com.datathecodie.witgrid.ui.screens.result.ResultScreenRoute
import com.datathecodie.witgrid.ui.screens.result.nextResultLevel
import com.datathecodie.witgrid.ui.screens.stats.StatisticsScreenRoute
import com.datathecodie.witgrid.ui.screens.welcome.WelcomeScreen
import com.datathecodie.witgrid.ui.layout.TopBarAction
import com.datathecodie.witgrid.ui.layout.WitgridScreenFrame
import com.datathecodie.witgrid.ui.layout.WitgridTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone
import java.util.UUID

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
    val billingService = remember(applicationContext, playerDataRepository) {
        PlayBillingService(applicationContext, playerDataRepository)
    }
    val billingState by billingService.state.collectAsState()
    DisposableEffect(billingService) {
        onDispose { billingService.close() }
    }
    val reminderController = remember(applicationContext, playerDataRepository) {
        ReminderController(
            playerDataRepository = playerDataRepository,
            permissionStatus = AndroidReminderPermissionStatus(applicationContext),
            alarmScheduler = AndroidReminderAlarmScheduler(applicationContext),
        )
    }
    val adService = remember(applicationContext) { GoogleAdService(applicationContext) }
    val consentState by adService.consentState.collectAsState()
    DisposableEffect(adService) {
        onDispose { adService.close() }
    }
    val activity = LocalActivity.current
    LaunchedEffect(adService, activity) {
        if (activity != null) {
            try {
                adService.requestConsent(activity)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Consent failures must not block offline puzzle play.
            }
        }
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
    val rewardLedger = remember(playerDataRepository, config, applicationContext, dailyProgressCoordinator) {
        AllowanceLedger(
            playerDataRepository = playerDataRepository,
            config = config,
            localEpochDay = { dailyProgressCoordinator.today().epochDay },
            isOnline = { hasValidatedInternet(applicationContext) },
            freeChecksInEasyPack = false,
        )
    }
    val interstitialAdFlow = remember(adService, interstitialTracker) {
        InterstitialAdFlow(adService, interstitialTracker)
    }
    val rewardedAdFlow = remember(adService, rewardLedger) {
        RewardedAdFlow(adService, rewardLedger)
    }
    var firstSessionAfterInstall by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(interstitialTracker) {
        if (firstSessionAfterInstall == null) {
            try {
                firstSessionAfterInstall = interstitialTracker.beginSession()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Skip interstitials when session history could not be read safely.
                firstSessionAfterInstall = null
            }
        }
    }
    var dailyProgress by remember { mutableStateOf<DailyProgressSnapshot?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(billingService, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            billingService.refresh()
        }
    }
    var reminderPermissionPending by rememberSaveable { mutableStateOf(false) }
    var reminderActionMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    val reminderPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (reminderPermissionPending) {
            reminderPermissionPending = false
            if (!granted) {
                reminderActionMessage = R.string.settings_reminder_permission_denied
            } else {
                reminderActionMessage = null
                scope.launch {
                    try {
                        val settings = playerDataRepository.playerData.first().reminder.copy(enabled = true)
                        when (reminderController.enable(settings)) {
                            ReminderEnableResult.Enabled -> reminderActionMessage = null
                            ReminderEnableResult.NotificationPermissionRequired -> {
                                reminderActionMessage = R.string.settings_reminder_permission_denied
                            }
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        reminderActionMessage = R.string.settings_action_failed
                    }
                }
            }
        }
    }
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
    var continueRequest by remember { mutableStateOf<PlayContinueRequest?>(null) }
    var activeBackAction by remember(destination) {
        mutableStateOf<suspend () -> Unit>({ navigator.goBack() })
    }
    var resultNextInProgress by remember { mutableStateOf(false) }
    val allGameIds = remember { GameRegistry.games.map { it.id } + PlayRegistry.games.map { it.id } }
    val catalog by produceState<Map<String, List<LevelRef>>?>(null, bankSource) {
        value = withContext(Dispatchers.IO) {
            val familyA = GameRegistry.games.associate { module ->
                module.id to Pack.entries.flatMap { pack -> module.levelRefs(pack, bankSource) }
            }
            val familyB = PlayRegistry.games.associate { module ->
                module.id to Pack.entries.flatMap { pack -> module.levelRefs(pack, bankSource) }
            }
            familyA + familyB
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
            if (!resultNextInProgress) activeBackAction()
        } catch (exception: CancellationException) {
            backProgress = 0f
            throw exception
        } finally {
            backProgress = 0f
        }
    }

    val storedWelcomeSeen by produceState<Boolean?>(null, playerDataRepository) {
        value = try {
            playerDataRepository.playerData.first().settings.welcomeSeen
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // A storage failure must not block play; skip the welcome instead.
            true
        }
    }
    var welcomeDismissed by rememberSaveable { mutableStateOf(false) }
    when (storedWelcomeSeen) {
        // The splash and the paper window background cover this short read.
        null -> return
        false -> if (!welcomeDismissed) {
            WelcomeScreen(
                onFinish = {
                    welcomeDismissed = true
                    persistence.scope.launch {
                        val settings = playerDataRepository.playerData.first().settings
                        playerDataRepository.updateSettings(settings.copy(welcomeSeen = true))
                    }
                },
            )
            return
        }
        true -> Unit
    }

    val levelRefsByGame = catalog
    if (levelRefsByGame == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val goBack: () -> Unit = { navigator.goBack() }

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
            AppDestination.Home -> WitgridScreenFrame(
                topBar = {
                    WitgridTopBar(
                        title = stringResource(R.string.app_name),
                        showLogo = true,
                        actions = listOf(
                            TopBarAction(R.drawable.ic_calendar, stringResource(R.string.action_calendar)) {
                                navigator.navigate(AppDestination.Calendar)
                            },
                            TopBarAction(R.drawable.ic_statistics, stringResource(R.string.action_statistics)) {
                                navigator.navigate(AppDestination.Statistics)
                            },
                            TopBarAction(R.drawable.ic_settings, stringResource(R.string.action_settings)) {
                                navigator.navigate(AppDestination.Settings)
                            },
                        ),
                    )
                },
            ) { HomeScreenRoute(
                gameIds = allGameIds,
                levelRefsByGame = levelRefsByGame,
                progressRepository = progressRepository,
                streak = dailyProgress?.streak ?: playerData.streak,
                completedDailyGameIds = todayCompletions.map { it.gameId }.toSet(),
                premiumEnabled = playerData.premiumEnabled,
                billingState = billingState,
                onPurchasePremium = { activity?.let(billingService::launchPurchase) },
                onSelectGame = { navigator.navigate(AppDestination.Game(it)) },
                onPlayDaily = { gameId ->
                    scope.launch {
                        val dailyRef = withContext(Dispatchers.IO) {
                            dailyRefFor(gameId, dailyProgressCoordinator.today().bankIndex, bankSource)
                        }
                        if (navigator.destination.value == currentDestination) {
                            navigator.navigate(AppDestination.Play(dailyRef))
                        }
                    }
                },
            ) }

            is AppDestination.Game -> {
                var showGameRules by rememberSaveable(currentDestination.gameId) { mutableStateOf(false) }
                val levelRefsByPack = remember(currentDestination.gameId, levelRefsByGame) {
                    Pack.entries.associateWith { pack ->
                        levelRefsByGame[currentDestination.gameId].orEmpty().filter { it.pack == pack }
                    }
                }
                WitgridScreenFrame(
                    topBar = {
                        WitgridTopBar(
                            title = stringResource(gameTitleResource(currentDestination.gameId)),
                            onBack = goBack,
                            actions = listOf(
                                TopBarAction(R.drawable.ic_info, stringResource(R.string.play_rules)) {
                                    showGameRules = true
                                },
                            ),
                        )
                    },
                ) { GameScreenRoute(
                    gameId = currentDestination.gameId,
                    levelRefsByPack = levelRefsByPack,
                    progressRepository = progressRepository,
                    dailyCompletedToday = todayCompletions.any { it.gameId == currentDestination.gameId },
                    premiumEnabled = playerData.premiumEnabled,
                    billingState = billingState,
                    onPurchasePremium = { activity?.let(billingService::launchPurchase) },
                    onEarnHintCredit = {
                        activity?.let { rewardedAdFlow.grantReward(it, RewardType.HINT) }
                            ?: AdShowResult.Unavailable
                    },
                    onEarnCheckCredit = {
                        activity?.let { rewardedAdFlow.grantReward(it, RewardType.CHECK) }
                            ?: AdShowResult.Unavailable
                    },
                    onContinue = { navigator.navigate(AppDestination.Play(it)) },
                    onSelectPack = { navigator.navigate(AppDestination.Play(it)) },
                    onOpenDaily = {
                        scope.launch {
                            val dailyRef = withContext(Dispatchers.IO) {
                                dailyRefFor(
                                    currentDestination.gameId,
                                    dailyProgressCoordinator.today().bankIndex,
                                    bankSource,
                                )
                            }
                            if (navigator.destination.value == currentDestination) {
                                navigator.navigate(AppDestination.Play(dailyRef))
                            }
                        }
                    },
                    hintCredits = playerData.rewardBalances[RewardType.HINT] ?: 0,
                    checkCredits = playerData.rewardBalances[RewardType.CHECK] ?: 0,
                ) }
                if (showGameRules) {
                    GameRulesSheet(currentDestination.gameId) { showGameRules = false }
                }
            }

            is AppDestination.Play -> {
                val levelRef = currentDestination.levelRef
                val module = GameRegistry.find(levelRef.gameId)
                val playModule = PlayRegistry.find(levelRef.gameId)
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
                val matchingContinueRequest = continueRequest?.takeIf { it.levelRef == levelRef }
                val onPlaySolved: (com.datathecodie.witgrid.play.LevelSolvedEvent) -> Unit = { solved ->
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
                }
                CompositionLocalProvider(
                    LocalCrownsBoardOptions provides CrownsBoardOptions(
                        showRegionAids = playerData.settings.crownsShowRegionAids,
                        autoXEnabled = playerData.settings.crownsAutoXEnabled,
                        hapticsEnabled = playerData.settings.hapticsEnabled,
                        reduceMotion = playerData.settings.reduceMotion,
                    ),
                    LocalBinaryBoardOptions provides BinaryBoardOptions(
                        hapticsEnabled = playerData.settings.hapticsEnabled,
                        reduceMotion = playerData.settings.reduceMotion,
                    ),
                    LocalNonogramBoardOptions provides NonogramBoardOptions(
                        autoCrossCompletedLines = playerData.settings.nonogramAutoCrossEnabled,
                        hapticsEnabled = playerData.settings.hapticsEnabled,
                        reduceMotion = playerData.settings.reduceMotion,
                    ),
                    LocalMiniSudokuBoardOptions provides MiniSudokuBoardOptions(
                        hapticsEnabled = playerData.settings.hapticsEnabled,
                        reduceMotion = playerData.settings.reduceMotion,
                    ),
                    LocalArrowsBoardOptions provides ArrowsBoardOptions(
                        hapticsEnabled = playerData.settings.hapticsEnabled,
                        reduceMotion = playerData.settings.reduceMotion,
                    ),
                ) {
                    if (module != null) {
                        PlayScreenRoute(
                            levelRef = levelRef,
                            gameModule = module,
                            bankSource = bankSource,
                            progressRepository = progressRepository,
                            persistence = persistence,
                            allowance = allowance,
                            hintCredits = (playerData.rewardBalances[RewardType.HINT] ?: 0).toString(),
                            mistakeLimitEnabled = playerData.settings.mistakeLimitEnabled,
                            restartRequestId = matchingRestartRequest?.id,
                            onRestartRequestConsumed = { requestId ->
                                if (restartRequest?.id == requestId) restartRequest = null
                            },
                            continueRequestId = matchingContinueRequest?.transactionId,
                            onContinueRequestConsumed = { transactionId ->
                                if (continueRequest?.transactionId == transactionId) continueRequest = null
                            },
                            onConsumeContinueTransaction = { transactionId ->
                                rewardLedger.allowContinue(transactionId)
                            },
                            onRegisterBackAction = { action -> activeBackAction = action },
                            onSolved = onPlaySolved,
                            onFailed = {
                                if (navigator.destination.value == currentDestination) {
                                    navigator.navigate(AppDestination.LevelFailed(levelRef))
                                }
                            },
                            onBack = { navigator.goBack() },
                        )
                    } else {
                        PlayScreenRoute(
                            levelRef = levelRef,
                            playModule = requireNotNull(playModule) { "Unknown game: ${levelRef.gameId}" },
                            bankSource = bankSource,
                            progressRepository = progressRepository,
                            persistence = persistence,
                            allowance = allowance,
                            hintCredits = (playerData.rewardBalances[RewardType.HINT] ?: 0).toString(),
                            restartRequestId = matchingRestartRequest?.id,
                            onRestartRequestConsumed = { requestId ->
                                if (restartRequest?.id == requestId) restartRequest = null
                            },
                            continueRequestId = matchingContinueRequest?.transactionId,
                            onContinueRequestConsumed = { transactionId ->
                                if (continueRequest?.transactionId == transactionId) continueRequest = null
                            },
                            onConsumeContinueTransaction = { transactionId ->
                                rewardLedger.allowContinue(transactionId)
                            },
                            onRegisterBackAction = { action -> activeBackAction = action },
                            onSolved = onPlaySolved,
                            onFailed = {
                                if (navigator.destination.value == currentDestination) {
                                    navigator.navigate(AppDestination.LevelFailed(levelRef))
                                }
                            },
                            onBack = { navigator.goBack() },
                        )
                    }
                }
            }

            is AppDestination.Result -> ResultScreenRoute(
                levelRef = currentDestination.levelRef,
                progressRepository = progressRepository,
                onNext = {
                    if (!resultNextInProgress) {
                        resultNextInProgress = true
                        scope.launch {
                            try {
                                val levelRef = currentDestination.levelRef
                                val next = nextResultLevel(
                                    levelRef,
                                    levelRefsByGame[levelRef.gameId].orEmpty(),
                                )
                                val currentActivity = activity
                                val firstSession = firstSessionAfterInstall
                                try {
                                    if (next != null && currentActivity != null && firstSession != null) {
                                        val boardSize = withContext(Dispatchers.IO) {
                                            loadLevelSize(levelRef, bankSource)
                                        }
                                        interstitialAdFlow.afterResultNext(
                                            activity = currentActivity,
                                            gameId = levelRef.gameId,
                                            boardSize = boardSize,
                                            firstSessionAfterInstall = firstSession,
                                        )
                                    }
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (_: Exception) {
                                    // Ad or tracking failures must never prevent the player from continuing.
                                }
                                if (navigator.destination.value == currentDestination) {
                                    if (next == null) {
                                        navigator.returnHome()
                                        navigator.navigate(AppDestination.Calendar)
                                    } else {
                                        navigator.goBack()
                                        navigator.navigate(AppDestination.Play(next))
                                    }
                                }
                            } finally {
                                resultNextInProgress = false
                            }
                        }
                    }
                },
                onBackToPacks = {
                    navigator.returnHome()
                    navigator.navigate(AppDestination.Game(currentDestination.levelRef.gameId))
                },
            )

            is AppDestination.LevelFailed -> {
                val failedBoardState by produceState<Any?>(null, currentDestination.levelRef) {
                    value = withContext(Dispatchers.IO) {
                        val ref = currentDestination.levelRef
                        val saved = progressRepository.getAttemptProgress(ref.gameId, ref.levelId, ref.levelNumber ?: 0)
                        saved?.let {
                            runCatching {
                                GameRegistry.find(ref.gameId)?.decodeState(it.stateBytes)
                                    ?: PlayRegistry.find(ref.gameId)?.decodeState(it.stateBytes)
                            }.getOrNull()
                        }
                    }
                }
                LevelFailedScreen(
                    onContinue = {
                        continueRequest = PlayContinueRequest(
                            currentDestination.levelRef,
                            UUID.randomUUID().toString(),
                        )
                        navigator.goBack()
                    },
                    onRestart = {
                        nextRestartRequestId += 1
                        restartRequest = PlayRestartRequest(currentDestination.levelRef, nextRestartRequestId)
                        navigator.goBack()
                    },
                    onClose = {
                        navigator.returnHome()
                        navigator.navigate(AppDestination.Game(currentDestination.levelRef.gameId))
                    },
                    levelRef = currentDestination.levelRef,
                    boardState = failedBoardState,
                    premiumEnabled = playerData.premiumEnabled,
                    continueCreditAvailable =
                        (playerData.rewardBalances[RewardType.EXTRA_LIFE] ?: 0) > 0,
                    billingState = billingState,
                    onPurchasePremium = { activity?.let(billingService::launchPurchase) },
                    onRewardedContinue = {
                        activity?.let { rewardedAdFlow.grantReward(it, RewardType.EXTRA_LIFE) }
                            ?: AdShowResult.Unavailable
                    },
                )

            }

            AppDestination.Calendar -> WitgridScreenFrame(
                topBar = { WitgridTopBar(title = stringResource(R.string.screen_calendar), onBack = goBack) },
            ) { CalendarScreenRoute(
                dailyProgress = dailyProgress,
                completionRepository = dailyCompletionRepository,
                gameIds = allGameIds,
                premiumEnabled = playerData.premiumEnabled,
                streakRepairCreditAvailable =
                    (playerData.rewardBalances[RewardType.STREAK_REPAIR] ?: 0) > 0,
                billingState = billingState,
                onPurchasePremium = { activity?.let(billingService::launchPurchase) },
                onRepairStreak = {
                    var freshProgress = dailyProgressCoordinator.reconcile()
                    dailyProgress = freshProgress
                    if (freshProgress.repairableMiss == null) {
                        AdShowResult.Unavailable
                    } else {
                        val freshPlayerData = playerDataRepository.playerData.first()
                        val hasRepairCredit =
                            (freshPlayerData.rewardBalances[RewardType.STREAK_REPAIR] ?: 0) > 0
                        val rewardResult = if (freshPlayerData.premiumEnabled || hasRepairCredit) {
                            AdShowResult.RewardGranted
                        } else {
                            activity?.let {
                                rewardedAdFlow.grantReward(it, RewardType.STREAK_REPAIR)
                            } ?: AdShowResult.Unavailable
                        }
                        if (rewardResult != AdShowResult.RewardGranted) {
                            rewardResult
                        } else {
                            freshProgress = dailyProgressCoordinator.reconcile()
                            dailyProgress = freshProgress
                            val repairableMiss = freshProgress.repairableMiss
                            if (repairableMiss == null) {
                                AdShowResult.Unavailable
                            } else {
                                val repairTime = System.currentTimeMillis()
                                val repairedStreak = DailyStreakPolicy(
                                    clock = DailyClock { repairTime },
                                    timeZone = TimeZone.getTimeZone(freshProgress.today.timeZoneId),
                                ).repairLatestMiss(freshProgress.streak)?.streak
                                if (repairedStreak == null) {
                                    AdShowResult.Unavailable
                                } else {
                                    val committed = playerDataRepository.commitStreakRepair(
                                        expectedStreak = freshProgress.streak,
                                        repairedStreak = repairedStreak,
                                        expectedMissedEpochDay = repairableMiss.dateEpochDay,
                                        expectedRepairDeadlineEpochMillis = repairableMiss.deadlineEpochMillis,
                                        expectedLocalEpochDay = freshProgress.today.epochDay,
                                        expectedTimeZoneId = freshProgress.today.timeZoneId,
                                        currentLocalEpochDay = { dailyProgressCoordinator.today().epochDay },
                                        currentTimeZoneId = { dailyProgressCoordinator.today().timeZoneId },
                                        nowEpochMillis = System::currentTimeMillis,
                                    )
                                    dailyProgress = dailyProgressCoordinator.reconcile()
                                    if (committed) {
                                        AdShowResult.RewardGranted
                                    } else {
                                        AdShowResult.Unavailable
                                    }
                                }
                            }
                        }
                    }
                },
                onOpenDailyPuzzle = { gameId ->
                    scope.launch {
                        val dailyRef = withContext(Dispatchers.IO) {
                            dailyRefFor(gameId, dailyProgressCoordinator.today().bankIndex, bankSource)
                        }
                        if (navigator.destination.value == AppDestination.Calendar) {
                            navigator.navigate(AppDestination.Play(dailyRef))
                        }
                    }
                },
            ) }
            AppDestination.Statistics -> WitgridScreenFrame(
                topBar = { WitgridTopBar(title = stringResource(R.string.screen_statistics), onBack = goBack) },
            ) { StatisticsScreenRoute(
                gameIds = allGameIds,
                levelRefsByGame = levelRefsByGame,
                progressRepository = progressRepository,
                dailyPuzzleCompletionRepository = dailyCompletionRepository,
            ) }
            AppDestination.Settings -> WitgridScreenFrame(
                topBar = { WitgridTopBar(title = stringResource(R.string.screen_settings), onBack = goBack) },
            ) { SettingsScreen(
                settings = playerData.settings,
                reminder = playerData.reminder,
                reminderActionMessage = reminderActionMessage,
                privacyOptionsAvailable = consentState.privacyOptionsRequired,
                onUpdateSettings = playerDataRepository::updateSettings,
                onEnableReminder = {
                    reminderActionMessage = null
                    val settings = playerDataRepository.playerData.first().reminder.copy(enabled = true)
                    val result = reminderController.enable(settings)
                    if (result == ReminderEnableResult.NotificationPermissionRequired &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !reminderPermissionPending
                    ) {
                        reminderPermissionPending = true
                        reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    result
                },
                onDisableReminder = {
                    reminderController.disable()
                    reminderActionMessage = null
                },
                onUpdateReminderTime = reminderController::updateTime,
                onShowPrivacyOptions = {
                    val currentActivity = activity
                    if (currentActivity != null) adService.showPrivacyOptions(currentActivity)
                },
                premiumEnabled = playerData.premiumEnabled,
                billingState = billingState,
                onPurchasePremium = { activity?.let(billingService::launchPurchase) },
                onRestorePurchases = {
                    scope.launch { billingService.restorePurchases() }
                },
            ) }
        }
    }
}

private fun dailyRefFor(gameId: String, dayIndex: Int, bankSource: AssetBankSource): LevelRef =
    GameRegistry.find(gameId)?.dailyRef(dayIndex, bankSource)
        ?: requireNotNull(PlayRegistry.find(gameId)) { "Unknown game: $gameId" }
            .dailyRef(dayIndex, bankSource)

private fun loadLevelSize(levelRef: LevelRef, bankSource: AssetBankSource): Int =
    GameRegistry.find(levelRef.gameId)?.loadLevel(levelRef, bankSource)?.size
        ?: requireNotNull(PlayRegistry.find(levelRef.gameId)) { "Unknown game: ${levelRef.gameId}" }
            .loadLevel(levelRef, bankSource).size

private fun gameTitleResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    "sudoku" -> R.string.game_sudoku
    "skyscrapers" -> R.string.game_skyscrapers
    "futoshiki" -> R.string.game_futoshiki
    "path" -> R.string.game_path
    "arrows" -> R.string.game_arrows
    else -> R.string.game_unknown
}

private data class PlayRestartRequest(val levelRef: LevelRef, val id: Long)

private data class PlayContinueRequest(val levelRef: LevelRef, val transactionId: String)

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
