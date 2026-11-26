package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameModule
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.PlayGameModule
import com.datathecodie.witgrid.games.arrows.ArrowsBoard
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.games.skyscrapers.SkyscrapersBoard
import com.datathecodie.witgrid.play.Allowance
import com.datathecodie.witgrid.play.CheckUi
import com.datathecodie.witgrid.play.HintAllowanceDecision
import com.datathecodie.witgrid.play.HintController
import com.datathecodie.witgrid.play.HintRequest
import com.datathecodie.witgrid.play.HintUi
import com.datathecodie.witgrid.play.LevelSolvedEvent
import com.datathecodie.witgrid.play.PlayActionController
import com.datathecodie.witgrid.play.PlayController
import java.security.MessageDigest
import com.datathecodie.witgrid.ui.board.LocalBoardHighlightCells
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
internal fun <P : Any, S : Any> PlayScreenRoute(
    levelRef: LevelRef,
    gameModule: GameModule<P, S>,
    bankSource: BankSource,
    progressRepository: ProgressRepository,
    persistence: PlayPersistenceScope,
    allowance: Allowance,
    restartRequestId: Long?,
    onRestartRequestConsumed: (Long) -> Unit,
    onSolved: (LevelSolvedEvent) -> Unit,
    onBack: () -> Unit,
    onRegisterBackAction: ((suspend () -> Unit) -> Unit) = {},
    mistakeLimitEnabled: Boolean = false,
    continueRequestId: String? = null,
    onContinueRequestConsumed: (String) -> Unit = {},
    onConsumeContinueTransaction: suspend (String) -> Boolean = { false },
    onFailed: () -> Unit = {},
    hintCredits: String? = null,
) {
    val introCoachLineResourceId = levelRef.introCoachLineResourceId
    val introCoachLine = if (introCoachLineResourceId == null) {
        null
    } else {
        stringResource(introCoachLineResourceId)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val controllerState = remember(levelRef) { mutableStateOf<PlayController<P, S>?>(null) }
    var loadFailed by remember(levelRef) { mutableStateOf(false) }
    var saveFailed by remember(levelRef) { mutableStateOf(false) }
    val latestOnSolved by rememberUpdatedState(onSolved)
    val latestOnFailed by rememberUpdatedState(onFailed)
    val latestConsumeContinueTransaction by rememberUpdatedState(onConsumeContinueTransaction)
    val scope = persistence.scope
    val interactionMutex = remember(levelRef) { Mutex() }

    LaunchedEffect(levelRef, gameModule, bankSource, progressRepository, restartRequestId, continueRequestId) {
        try {
            val existingController = controllerState.value
            if (existingController != null) {
                when {
                    restartRequestId != null -> {
                        existingController.restart()
                        existingController.resumeFromBackground()
                        onRestartRequestConsumed(restartRequestId)
                    }

                    continueRequestId != null -> {
                        check(existingController.continueAfterFailure(continueRequestId)) {
                            "Level is no longer waiting for Continue"
                        }
                        finalizePendingContinue(existingController, latestConsumeContinueTransaction)
                        existingController.resumeFromBackground()
                        onContinueRequestConsumed(continueRequestId)
                    }

                    else -> finalizePendingContinue(existingController, latestConsumeContinueTransaction)
                }
                return@LaunchedEffect
            }

            loadFailed = false
            val controller = withContext(Dispatchers.IO) {
                val level = gameModule.loadLevel(levelRef, bankSource)
                PlayController.open(
                    gameModule = gameModule,
                    level = level,
                    progressRepository = progressRepository,
                    mistakeLimitEnabled = mistakeLimitEnabled,
                ).also { openedController ->
                    when {
                        restartRequestId != null || openedController.snapshot.value.solved -> openedController.restart()
                        continueRequestId != null -> check(openedController.continueAfterFailure(continueRequestId)) {
                            "Level is no longer waiting for Continue"
                        }
                    }
                    finalizePendingContinue(openedController, latestConsumeContinueTransaction)
                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        openedController.pauseForBackground()
                    }
                }
            }
            if (restartRequestId != null) onRestartRequestConsumed(restartRequestId)
            if (continueRequestId != null) onContinueRequestConsumed(continueRequestId)
            persistence.registerController(controller::pauseForBackground)
            controllerState.value = controller
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    val controller = controllerState.value
    LaunchedEffect(controller) {
        controller?.solvedEvents?.collect { latestOnSolved(it) }
    }
    LaunchedEffect(controller) {
        if (controller != null) {
            while (isActive) {
                delay(TIMER_REFRESH_MILLIS)
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    controller.refreshTimer()
                }
            }
        }
    }

    fun perform(action: suspend () -> Unit) {
        scope.launch {
            try {
                interactionMutex.withLock { action() }
                saveFailed = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                saveFailed = true
            }
        }
    }

    if (controller == null) {
        PlayLoadingScreen(loadFailed, onBack)
    } else {
        val leave: () -> Unit = {
            perform {
                controller.pauseForBackground()
                onBack()
            }
        }
        DisposableEffect(controller) {
            onRegisterBackAction {
                try {
                    interactionMutex.withLock {
                        controller.pauseForBackground()
                        onBack()
                    }
                    saveFailed = false
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    saveFailed = true
                }
            }
            onDispose { onRegisterBackAction { onBack() } }
        }
        DisposableEffect(lifecycleOwner, controller) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> perform { controller.pauseForBackground() }
                    Lifecycle.Event.ON_RESUME -> perform { controller.resumeFromBackground() }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                perform { controller.pauseForBackground() }
            }
        }
        val snapshot by controller.snapshot.collectAsState()
        LaunchedEffect(controller, snapshot.failed) {
            if (snapshot.failed) {
                try {
                    interactionMutex.withLock { controller.pauseForBackground() }
                    latestOnFailed()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    saveFailed = true
                }
            }
        }
        val hintController = remember(controller) {
            HintController(
                currentMistakes = { controller.snapshot.value.mistakes },
                nextStep = controller::nextHint,
                applyStep = controller::applyRevealedHint,
                recordHintUse = controller::recordHintUse,
                allowance = allowance,
                boardKey = controller::currentBoardFingerprint,
            )
        }
        var hintRequest by remember(controller) { mutableStateOf<HintRequest?>(null) }
        var checkResult by remember(controller) { mutableStateOf<CheckUi?>(null) }
        var highlightedCells by remember(controller) { mutableStateOf(emptySet<Cell>()) }
        var feedbackGeneration by remember(controller) { mutableStateOf(0L) }
        var hintOperationPending by remember(controller) { mutableStateOf(false) }
        var enlargedBoardMode by remember(controller) { mutableStateOf(false) }

        fun clearFeedback() {
            feedbackGeneration += 1
            hintRequest = null
            checkResult = null
            highlightedCells = emptySet()
        }

        val onMoves: (List<Move>) -> Unit = { moves ->
            clearFeedback()
            perform { controller.applyGesture(moves) }
        }
        val onUndo: () -> Unit = {
            clearFeedback()
            perform { controller.undo() }
        }
        val onRedo: () -> Unit = {
            clearFeedback()
            perform { controller.redo() }
        }
        val onRequestHint: () -> Unit = {
            if (!hintOperationPending) {
                hintOperationPending = true
                clearFeedback()
                val generation = feedbackGeneration
                scope.launch {
                    try {
                        val request = interactionMutex.withLock { hintController.requestHint() }
                        if (feedbackGeneration == generation) {
                            hintRequest = request
                            highlightedCells = (request as? HintRequest.Available)?.hint?.cells?.toSet().orEmpty()
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        saveFailed = true
                    } finally {
                        hintOperationPending = false
                    }
                }
            }
        }
        val onCheck: () -> Unit = {
            if (!hintOperationPending) {
                hintOperationPending = true
                clearFeedback()
                val generation = feedbackGeneration
                scope.launch {
                    try {
                        val result = interactionMutex.withLock { hintController.check() }
                        if (feedbackGeneration == generation) {
                            checkResult = result
                            highlightedCells = when (result) {
                                is CheckUi.Mistakes -> result.cells.toSet()
                                else -> emptySet()
                            }
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        saveFailed = true
                    } finally {
                        hintOperationPending = false
                    }
                }
            }
        }
        val statusMessage = when (val request = hintRequest) {
            HintRequest.NoStep -> stringResource(R.string.hint_unavailable)
            HintRequest.NotAllowed -> stringResource(R.string.hint_allowance_unavailable)
            else -> when (val result = checkResult) {
                is CheckUi.Mistakes -> stringResource(R.string.check_mistakes_found, result.cells.size)
                CheckUi.Clear -> stringResource(R.string.check_no_mistakes)
                CheckUi.NotAllowed -> stringResource(R.string.check_allowance_unavailable)
                null -> null
            }
        }
        val puzzleStatus = playPuzzleStatus(snapshot.state)
        val visibleStatusMessage = statusMessage ?: if (snapshot.mistakeLimitEnabled) {
            stringResource(
                R.string.play_status_with_mistakes,
                puzzleStatus.orEmpty(),
                stringResource(R.string.play_mistake_count, snapshot.mistakesMade, snapshot.mistakesAllowed),
            )
        } else {
            puzzleStatus
        }
        val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit, (@Composable () -> Unit)?) -> Unit = { board, controls, modeTools ->
            PlayScreenLayout(
                levelRef = levelRef,
                elapsedTimeMillis = snapshot.elapsedTimeMillis,
                canUndo = snapshot.canUndo,
                canRedo = snapshot.canRedo,
                hintCredits = hintCredits,
                onRestart = { clearFeedback(); perform { controller.restart() } },
                saveFailed = saveFailed,
                introCoachLine = introCoachLine,
                statusMessage = visibleStatusMessage,
                hintOperationPending = hintOperationPending,
                enlargedBoardMode = enlargedBoardMode,
                onToggleEnlargedBoard = { enlargedBoardMode = !enlargedBoardMode },
                board = board,
                gameControls = controls,
                modeTools = modeTools,
                onUndo = onUndo,
                onRedo = onRedo,
                onRequestHint = onRequestHint,
                onCheck = onCheck,
                onBack = leave,
            )
        }
        val flaggedCells = if (snapshot.mistakeLimitEnabled) snapshot.mistakes.toSet() else emptySet()
        CompositionLocalProvider(
            LocalBoardHighlightCells provides (highlightedCells + flaggedCells),
            LocalBoardViewportMode provides BoardViewportMode(enlargedBoardMode, embedded = true),
        ) {
            when (val state = snapshot.state) {
                is MiniSudokuState -> MiniSudokuBoard(state, onMoves, onUndo, playLayout = { board, actions ->
                    layout(board, { PlaySudokuDigits(state, actions) }, { PlaySudokuTools(actions) })
                })
                is NonogramState -> NonogramBoard(state, onMoves, onUndo, layout = { board, controls -> layout(board, controls, null) })
                is SkyscrapersState -> SkyscrapersBoard(state, onMoves, onUndo, layout = { board, controls -> layout(board, controls, null) })
                else -> layout({ gameModule.Board(snapshot.state, onMoves, onUndo) }, {}, null)
            }
        }
        when (val request = hintRequest) {
            is HintRequest.Available -> HintDialog(
                hint = request.hint,
                applyPending = hintOperationPending,
                onDismiss = { hintRequest = null },
                onApply = {
                    val step = request.hint as? HintUi.Step
                    if (step != null && !hintOperationPending) {
                        hintOperationPending = true
                        perform {
                            try {
                                if (hintController.apply(step)) {
                                    clearFeedback()
                                } else {
                                    hintRequest = HintRequest.NoStep
                                    highlightedCells = emptySet()
                                }
                            } finally {
                                hintOperationPending = false
                            }
                        }
                    }
                },
            )
            else -> Unit
        }
    }
}

private suspend fun <P : Any, S : Any> finalizePendingContinue(
    controller: PlayController<P, S>,
    consumeContinueTransaction: suspend (String) -> Boolean,
) {
    val transactionId = controller.snapshot.value.pendingContinueTransactionId ?: return
    if (!consumeContinueTransaction(transactionId)) {
        controller.rollbackContinueTransaction(transactionId)
        error("Continue credit is no longer available")
    }
    check(controller.completeContinueTransaction(transactionId)) {
        "Continue transaction could not be completed"
    }
}

@Composable
internal fun <P : Any, S : Any, A : Any> PlayScreenRoute(
    levelRef: LevelRef,
    playModule: PlayGameModule<P, S, A>,
    bankSource: BankSource,
    progressRepository: ProgressRepository,
    persistence: PlayPersistenceScope,
    allowance: Allowance,
    restartRequestId: Long?,
    onRestartRequestConsumed: (Long) -> Unit,
    onSolved: (LevelSolvedEvent) -> Unit,
    onBack: () -> Unit,
    onRegisterBackAction: ((suspend () -> Unit) -> Unit) = {},
    continueRequestId: String? = null,
    onContinueRequestConsumed: (String) -> Unit = {},
    onConsumeContinueTransaction: suspend (String) -> Boolean = { false },
    onFailed: () -> Unit = {},
    hintCredits: String? = null,
) {
    val introCoachLineResourceId = levelRef.introCoachLineResourceId
    val introCoachLine = introCoachLineResourceId?.let { stringResource(it) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val controllerState = remember(levelRef) { mutableStateOf<PlayActionController<P, S, A>?>(null) }
    var loadFailed by remember(levelRef) { mutableStateOf(false) }
    var saveFailed by remember(levelRef) { mutableStateOf(false) }
    val latestOnSolved by rememberUpdatedState(onSolved)
    val latestOnFailed by rememberUpdatedState(onFailed)
    val latestConsumeContinueTransaction by rememberUpdatedState(onConsumeContinueTransaction)
    val scope = persistence.scope
    val interactionMutex = remember(levelRef) { Mutex() }

    LaunchedEffect(levelRef, playModule, bankSource, progressRepository, restartRequestId, continueRequestId) {
        try {
            val existingController = controllerState.value
            if (existingController != null) {
                when {
                    restartRequestId != null -> {
                        existingController.restart()
                        existingController.resumeFromBackground()
                        onRestartRequestConsumed(restartRequestId)
                    }

                    continueRequestId != null -> {
                        check(existingController.continueAfterFailure(continueRequestId)) {
                            "Level is no longer waiting for Continue"
                        }
                        finalizePendingActionContinue(existingController, latestConsumeContinueTransaction)
                        existingController.resumeFromBackground()
                        onContinueRequestConsumed(continueRequestId)
                    }

                    else -> finalizePendingActionContinue(existingController, latestConsumeContinueTransaction)
                }
                return@LaunchedEffect
            }

            loadFailed = false
            val controller = withContext(Dispatchers.IO) {
                val level = playModule.loadLevel(levelRef, bankSource)
                PlayActionController.open(
                    playModule = playModule,
                    level = level,
                    progressRepository = progressRepository,
                ).also { openedController ->
                    when {
                        restartRequestId != null || openedController.snapshot.value.solved -> openedController.restart()
                        continueRequestId != null -> check(openedController.continueAfterFailure(continueRequestId)) {
                            "Level is no longer waiting for Continue"
                        }
                    }
                    finalizePendingActionContinue(openedController, latestConsumeContinueTransaction)
                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        openedController.pauseForBackground()
                    }
                }
            }
            if (restartRequestId != null) onRestartRequestConsumed(restartRequestId)
            if (continueRequestId != null) onContinueRequestConsumed(continueRequestId)
            persistence.registerController(controller::pauseForBackground)
            controllerState.value = controller
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    val controller = controllerState.value
    LaunchedEffect(controller) {
        controller?.solvedEvents?.collect { latestOnSolved(it) }
    }
    LaunchedEffect(controller) {
        if (controller != null) {
            while (isActive) {
                delay(TIMER_REFRESH_MILLIS)
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    controller.refreshTimer()
                }
            }
        }
    }

    fun perform(action: suspend () -> Unit) {
        scope.launch {
            try {
                interactionMutex.withLock { action() }
                saveFailed = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                saveFailed = true
            }
        }
    }

    if (controller == null) {
        PlayLoadingScreen(loadFailed, onBack)
    } else {
        val leave: () -> Unit = {
            perform {
                controller.pauseForBackground()
                onBack()
            }
        }
        DisposableEffect(controller) {
            onRegisterBackAction {
                try {
                    interactionMutex.withLock {
                        controller.pauseForBackground()
                        onBack()
                    }
                    saveFailed = false
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    saveFailed = true
                }
            }
            onDispose { onRegisterBackAction { onBack() } }
        }
        DisposableEffect(lifecycleOwner, controller) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> perform { controller.pauseForBackground() }
                    Lifecycle.Event.ON_RESUME -> perform { controller.resumeFromBackground() }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                perform { controller.pauseForBackground() }
            }
        }
        val snapshot by controller.snapshot.collectAsState()
        LaunchedEffect(controller, snapshot.failed) {
            if (snapshot.failed) {
                try {
                    interactionMutex.withLock { controller.pauseForBackground() }
                    latestOnFailed()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    saveFailed = true
                }
            }
        }

        var actionHintRequest by remember(controller) { mutableStateOf<ActionHintRequest<A>?>(null) }
        var checkResult by remember(controller) { mutableStateOf<CheckUi?>(null) }
        var highlightedCells by remember(controller) { mutableStateOf(emptySet<Cell>()) }
        var feedbackGeneration by remember(controller) { mutableStateOf(0L) }
        var hintOperationPending by remember(controller) { mutableStateOf(false) }
        var enlargedBoardMode by remember(controller) { mutableStateOf(false) }

        fun clearFeedback() {
            feedbackGeneration += 1
            actionHintRequest = null
            checkResult = null
            highlightedCells = emptySet()
        }

        val onAction: (A) -> Unit = { action ->
            clearFeedback()
            perform { controller.applyAction(action) }
        }
        val onUndo: () -> Unit = {
            clearFeedback()
            perform { controller.undo() }
        }
        val onRedo: () -> Unit = {
            clearFeedback()
            perform { controller.redo() }
        }
        val onRequestHint: () -> Unit = {
            if (!hintOperationPending) {
                hintOperationPending = true
                clearFeedback()
                val generation = feedbackGeneration
                scope.launch {
                    try {
                        val request = interactionMutex.withLock {
                            val action = controller.hintAction()
                            if (action == null) {
                                ActionHintRequest.NoStep
                            } else {
                                val hintKey = stableActionHintKey(controller.currentBoardFingerprint(), action)
                                when (allowance.allowHint(hintKey)) {
                                    HintAllowanceDecision.Denied -> ActionHintRequest.NotAllowed
                                    HintAllowanceDecision.AlreadyCharged -> ActionHintRequest.Available(action, hintCellsForAction(action))
                                    HintAllowanceDecision.Allowed -> {
                                        controller.recordHintUse(hintKey)
                                        ActionHintRequest.Available(action, hintCellsForAction(action))
                                    }
                                }
                            }
                        }
                        if (feedbackGeneration == generation) {
                            actionHintRequest = request
                            highlightedCells = (request as? ActionHintRequest.Available)?.highlightedCells.orEmpty()
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        saveFailed = true
                    } finally {
                        hintOperationPending = false
                    }
                }
            }
        }
        val onCheck: () -> Unit = {
            if (!hintOperationPending) {
                clearFeedback()
                checkResult = CheckUi.Clear
            }
        }

        val statusMessage = when (actionHintRequest) {
            ActionHintRequest.NoStep -> stringResource(R.string.hint_unavailable)
            ActionHintRequest.NotAllowed -> stringResource(R.string.hint_allowance_unavailable)
            else -> when (val result = checkResult) {
                is CheckUi.Mistakes -> stringResource(R.string.check_mistakes_found, result.cells.size)
                CheckUi.Clear -> stringResource(R.string.check_no_mistakes)
                CheckUi.NotAllowed -> stringResource(R.string.check_allowance_unavailable)
                null -> null
            }
        }
        val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit, (@Composable () -> Unit)?) -> Unit = { board, controls, modeTools ->
            PlayScreenLayout(
                levelRef = levelRef,
                elapsedTimeMillis = snapshot.elapsedTimeMillis,
                canUndo = snapshot.canUndo,
                canRedo = snapshot.canRedo,
                hintCredits = hintCredits,
                onRestart = { clearFeedback(); perform { controller.restart() } },
                saveFailed = saveFailed,
                introCoachLine = introCoachLine,
                statusMessage = statusMessage,
                hintOperationPending = hintOperationPending,
                enlargedBoardMode = enlargedBoardMode,
                onToggleEnlargedBoard = { enlargedBoardMode = !enlargedBoardMode },
                board = board,
                gameControls = controls,
                modeTools = modeTools,
                onUndo = onUndo,
                onRedo = onRedo,
                onRequestHint = onRequestHint,
                onCheck = onCheck,
                onBack = leave,
            )
        }
        CompositionLocalProvider(
            LocalBoardHighlightCells provides highlightedCells,
            LocalBoardViewportMode provides BoardViewportMode(enlargedBoardMode, embedded = true),
        ) {
            when (val state = snapshot.state) {
                is ArrowsState -> {
                    @Suppress("UNCHECKED_CAST")
                    val onArrowAction = onAction as (ArrowsAction) -> Unit
                    ArrowsBoard(
                        state = state,
                        onAction = onArrowAction,
                        onUndo = onUndo,
                        playLayout = { board, controls -> layout(board, controls, null) },
                    )
                }
                else -> layout({ playModule.Board(snapshot.state, onAction, onUndo) }, {}, null)
            }
        }
        when (val request = actionHintRequest) {
            is ActionHintRequest.Available -> ActionHintDialog(
                action = request.action,
                state = snapshot.state,
                applyPending = hintOperationPending,
                onDismiss = { actionHintRequest = null },
                onApply = {
                    if (!hintOperationPending) {
                        hintOperationPending = true
                        perform {
                            try {
                                if (controller.applyRevealedHintAction(request.action)) {
                                    clearFeedback()
                                } else {
                                    actionHintRequest = ActionHintRequest.NoStep
                                    highlightedCells = emptySet()
                                }
                            } finally {
                                hintOperationPending = false
                            }
                        }
                    }
                },
            )
            else -> Unit
        }
    }
}

private sealed interface ActionHintRequest<out A : Any> {
    data class Available<A : Any>(val action: A, val highlightedCells: Set<Cell>) : ActionHintRequest<A>
    data object NoStep : ActionHintRequest<Nothing>
    data object NotAllowed : ActionHintRequest<Nothing>
}

private fun hintCellsForAction(action: Any): Set<Cell> = when (action) {
    is ArrowsAction -> setOf(action.cell)
    else -> emptySet()
}

private fun stableActionHintKey(boardFingerprint: String, action: Any): String {
    val fingerprint = "$boardFingerprint|action:$action".toByteArray(Charsets.UTF_8)
    return MessageDigest.getInstance("SHA-256").digest(fingerprint).joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

private suspend fun <P : Any, S : Any, A : Any> finalizePendingActionContinue(
    controller: PlayActionController<P, S, A>,
    consumeContinueTransaction: suspend (String) -> Boolean,
) {
    val transactionId = controller.snapshot.value.pendingContinueTransactionId ?: return
    if (!consumeContinueTransaction(transactionId)) {
        controller.rollbackContinueTransaction(transactionId)
        error("Continue credit is no longer available")
    }
    check(controller.completeContinueTransaction(transactionId)) {
        "Continue transaction could not be completed"
    }
}

@Composable
private fun ActionHintDialog(
    action: Any,
    state: Any,
    applyPending: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val body = when (action) {
        is ArrowsAction -> {
            val size = (state as? ArrowsState)?.puzzle?.size ?: 1
            stringResource(R.string.hint_arrows_body, action.cell.index / size + 1, action.cell.index % size + 1)
        }
        else -> stringResource(R.string.hint_play_action_body)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hint_action_title)) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = onApply, enabled = !applyPending) { Text(stringResource(R.string.button_apply_hint)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_dismiss_hint)) }
        },
    )
}

@Composable
private fun PlayLoadingScreen(loadFailed: Boolean, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (loadFailed) {
            Text(stringResource(R.string.play_load_failed), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onBack) { Text(stringResource(R.string.button_back)) }
        } else {
            CircularProgressIndicator()
            Text(stringResource(R.string.play_loading), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HintDialog(
    hint: HintUi,
    applyPending: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    when (hint) {
        is HintUi.Mistake -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.hint_mistake_title)) },
            text = { Text(stringResource(R.string.hint_mistake_body)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_dismiss_hint)) }
            },
        )

        is HintUi.Step -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(hint.hintStep.technique.displayName) },
            text = { Text(hint.hintStep.explanation) },
            confirmButton = {
                Button(onClick = onApply, enabled = !applyPending) { Text(stringResource(R.string.button_apply_hint)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_dismiss_hint)) }
            },
        )
    }
}

internal fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

internal fun gameNameResource(gameId: String): Int = when (gameId) {
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

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val TIMER_REFRESH_MILLIS = 1_000L
