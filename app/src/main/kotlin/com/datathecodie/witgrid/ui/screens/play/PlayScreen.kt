package com.datathecodie.witgrid.ui.screens.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameModule
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.play.AlwaysAllow
import com.datathecodie.witgrid.play.CheckUi
import com.datathecodie.witgrid.play.HintController
import com.datathecodie.witgrid.play.HintRequest
import com.datathecodie.witgrid.play.HintUi
import com.datathecodie.witgrid.play.LevelSolvedEvent
import com.datathecodie.witgrid.play.PlayController
import com.datathecodie.witgrid.ui.board.LocalBoardHighlightCells
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
    restartRequestId: Long?,
    onRestartRequestConsumed: (Long) -> Unit,
    onSolved: (LevelSolvedEvent) -> Unit,
    onBack: () -> Unit,
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
    val scope = persistence.scope
    val interactionMutex = remember(levelRef) { Mutex() }

    LaunchedEffect(levelRef, gameModule, bankSource, progressRepository) {
        controllerState.value = null
        loadFailed = false
        try {
            val controller = withContext(Dispatchers.IO) {
                val level = gameModule.loadLevel(levelRef, bankSource)
                PlayController.open(gameModule, level, progressRepository).also {
                    if (restartRequestId != null || it.snapshot.value.solved) it.restart()
                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        it.pauseForBackground()
                    }
                }
            }
            if (restartRequestId != null) onRestartRequestConsumed(restartRequestId)
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
        BackHandler(onBack = leave)
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
        val hintController = remember(controller) {
            HintController(
                currentMistakes = { controller.snapshot.value.mistakes },
                nextStep = controller::nextHint,
                applyStep = controller::applyHint,
                recordMistakeHintUse = controller::recordHintUse,
                allowance = AlwaysAllow,
            )
        }
        var hintRequest by remember(controller) { mutableStateOf<HintRequest?>(null) }
        var checkResult by remember(controller) { mutableStateOf<CheckUi?>(null) }
        var highlightedCells by remember(controller) { mutableStateOf(emptySet<Cell>()) }
        var feedbackGeneration by remember(controller) { mutableStateOf(0L) }
        var hintOperationPending by remember(controller) { mutableStateOf(false) }

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
        val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit) -> Unit = { board, controls ->
            PlayScreenLayout(
                levelRef = levelRef,
                elapsedTimeMillis = snapshot.elapsedTimeMillis,
                canUndo = snapshot.canUndo,
                canRedo = snapshot.canRedo,
                saveFailed = saveFailed,
                introCoachLine = introCoachLine,
                statusMessage = statusMessage,
                hintOperationPending = hintOperationPending,
                board = board,
                gameControls = controls,
                onUndo = onUndo,
                onRedo = onRedo,
                onRequestHint = onRequestHint,
                onCheck = onCheck,
                onBack = leave,
            )
        }
        CompositionLocalProvider(LocalBoardHighlightCells provides highlightedCells) {
            when (val state = snapshot.state) {
                is MiniSudokuState -> MiniSudokuBoard(state, onMoves, onUndo, layout = layout)
                is NonogramState -> NonogramBoard(state, onMoves, onUndo, layout = layout)
                else -> layout({ gameModule.Board(snapshot.state, onMoves, onUndo) }, {})
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
internal fun PlayScreenLayout(
    levelRef: LevelRef,
    elapsedTimeMillis: Long,
    canUndo: Boolean,
    canRedo: Boolean,
    saveFailed: Boolean,
    introCoachLine: String?,
    statusMessage: String?,
    hintOperationPending: Boolean,
    board: @Composable () -> Unit,
    gameControls: @Composable () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRequestHint: () -> Unit,
    onCheck: () -> Unit,
    onBack: () -> Unit,
) {
    var timerVisible by remember(levelRef) { mutableStateOf(true) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val headerHeight = maxHeight * PLAY_HEADER_FRACTION
        val toolbarHeight = maxHeight * TOOLBAR_FRACTION
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().height(headerHeight).padding(16.dp),
            ) {
                Text(stringResource(gameNameResource(levelRef.gameId)), style = MaterialTheme.typography.titleLarge)
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val boardSize = minOf(maxWidth, maxHeight)
                if (introCoachLine == null) {
                    // Keep the board square inside its available height, including on short screens.
                    Box(modifier = Modifier.width(boardSize)) { board() }
                } else {
                    // Let larger text scroll above the board instead of shrinking its touch targets.
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            introCoachLine,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Box(modifier = Modifier.size(boardSize)) { board() }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().height(toolbarHeight)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                gameControls()
                if (saveFailed) {
                    Text(stringResource(R.string.play_save_failed), color = MaterialTheme.colorScheme.error)
                }
                statusMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (timerVisible) {
                    Text(
                        stringResource(R.string.play_elapsed_time, formatDuration(elapsedTimeMillis)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onUndo, enabled = canUndo) {
                        Text(stringResource(R.string.button_undo))
                    }
                    TextButton(onClick = onRedo, enabled = canRedo) {
                        Text(stringResource(R.string.button_redo))
                    }
                    TextButton(onClick = { timerVisible = !timerVisible }) {
                        Text(stringResource(if (timerVisible) R.string.button_hide_timer else R.string.button_show_timer))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onRequestHint, enabled = !hintOperationPending) {
                        Text(stringResource(R.string.button_hint))
                    }
                    TextButton(onClick = onCheck, enabled = !hintOperationPending) {
                        Text(stringResource(R.string.button_check))
                    }
                    TextButton(onClick = onBack) { Text(stringResource(R.string.button_back)) }
                }
            }
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

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / MILLIS_PER_SECOND
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

private fun gameNameResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.game_crowns
    "binary" -> R.string.game_binary
    "nonogram" -> R.string.game_nonogram
    "minisudoku" -> R.string.game_minisudoku
    else -> R.string.game_unknown
}

private const val PLAY_HEADER_FRACTION = 0.15f
private const val TOOLBAR_FRACTION = 1f / 3f
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val TIMER_REFRESH_MILLIS = 1_000L
