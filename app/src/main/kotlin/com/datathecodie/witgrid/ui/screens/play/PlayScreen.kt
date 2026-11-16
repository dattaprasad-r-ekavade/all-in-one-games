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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameModule
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuBoard
import com.datathecodie.witgrid.games.nonogram.NonogramBoard
import com.datathecodie.witgrid.play.LevelSolvedEvent
import com.datathecodie.witgrid.play.PlayController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val controllerState = remember(levelRef) { mutableStateOf<PlayController<P, S>?>(null) }
    var loadFailed by remember(levelRef) { mutableStateOf(false) }
    var saveFailed by remember(levelRef) { mutableStateOf(false) }
    val latestOnSolved by rememberUpdatedState(onSolved)
    val scope = persistence.scope

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
                action()
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
        val onMoves: (List<Move>) -> Unit = { moves ->
            perform { controller.applyGesture(moves) }
        }
        val onUndo: () -> Unit = { perform { controller.undo() } }
        val layout: @Composable (@Composable () -> Unit, @Composable () -> Unit) -> Unit = { board, controls ->
            PlayScreenLayout(
                levelRef = levelRef,
                elapsedTimeMillis = snapshot.elapsedTimeMillis,
                canUndo = snapshot.canUndo,
                canRedo = snapshot.canRedo,
                saveFailed = saveFailed,
                board = board,
                gameControls = controls,
                onUndo = onUndo,
                onRedo = { perform { controller.redo() } },
                onBack = leave,
            )
        }
        when (val state = snapshot.state) {
            is MiniSudokuState -> MiniSudokuBoard(state, onMoves, onUndo, layout = layout)
            is NonogramState -> NonogramBoard(state, onMoves, onUndo, layout = layout)
            else -> layout({ gameModule.Board(snapshot.state, onMoves, onUndo) }, {})
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
    board: @Composable () -> Unit,
    gameControls: @Composable () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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
                // Keep the board square inside its available height, including on short screens.
                Box(modifier = Modifier.width(minOf(maxWidth, maxHeight))) { board() }
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
                    TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.button_hint)) }
                    TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.button_check)) }
                    TextButton(onClick = onBack) { Text(stringResource(R.string.button_back)) }
                }
            }
        }
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
