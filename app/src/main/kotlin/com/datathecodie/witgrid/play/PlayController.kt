package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.History
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.GameModule
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class PlaySnapshot<S : Any>(
    val state: S,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val mistakes: List<com.datathecodie.witgrid.engine.core.Cell>,
    val solved: Boolean,
    val completed: Boolean,
    val bestTimeMillis: Long?,
)

data class LevelSolvedEvent(
    val levelRef: LevelRef,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val bestTimeMillis: Long,
)

fun interface MonotonicClock {
    fun nowMillis(): Long
}

object SystemMonotonicClock : MonotonicClock {
    override fun nowMillis(): Long = System.nanoTime() / NANOS_PER_MILLISECOND

    private const val NANOS_PER_MILLISECOND = 1_000_000L
}

class PlayController<P : Any, S : Any> private constructor(
    private val gameModule: GameModule<P, S>,
    private val level: GameLevel<P>,
    private val progressRepository: ProgressRepository,
    private val clock: MonotonicClock,
    initialState: S,
    restoredProgress: LevelProgress?,
) {
    private data class TimerState(
        val elapsedMillis: Long,
        val resumedAtMillis: Long?,
    )

    private val mutationMutex = Mutex()
    private var history = History(initialState)
    @Volatile
    private var timerState = TimerState(
        elapsedMillis = restoredProgress?.elapsedTimeMillis ?: 0L,
        resumedAtMillis = clock.nowMillis(),
    )
    private var hintsUsed = restoredProgress?.hintsUsed ?: 0
    private var completed = restoredProgress?.completed ?: false
    private var bestTimeMillis = restoredProgress?.bestTimeMillis
    private var solvedEventSent = completed

    private val mutableSnapshot = MutableStateFlow(createSnapshot())
    private val solvedEventChannel = Channel<LevelSolvedEvent>(Channel.BUFFERED)

    val snapshot: StateFlow<PlaySnapshot<S>> = mutableSnapshot.asStateFlow()
    val solvedEvents: Flow<LevelSolvedEvent> = solvedEventChannel.receiveAsFlow()

    val levelRef: LevelRef
        get() = level.ref

    val elapsedTimeMillis: Long
        get() = currentElapsedTimeMillis()

    fun refreshTimer() {
        mutableSnapshot.update { it.copy(elapsedTimeMillis = currentElapsedTimeMillis()) }
    }

    fun nextHint(): HintStep? = gameModule.nextHint(level, snapshot.value.state)

    suspend fun applyMove(move: Move): Boolean = applyGesture(listOf(move))

    suspend fun applyGesture(moves: List<Move>): Boolean = mutationMutex.withLock {
        if (moves.isEmpty()) return@withLock false
        val nextState = gameModule.applyMoves(history.present, moves)
        saveHistory(history.record(nextState), hintsUsed)
    }

    suspend fun applyHint(hint: HintStep): Boolean = mutationMutex.withLock {
        if (gameModule.nextHint(level, history.present) != hint) return@withLock false
        val nextState = gameModule.applyMoves(history.present, hint.moves)
        saveHistory(history.record(nextState), hintsUsed + 1)
    }

    suspend fun undo(): Boolean = mutationMutex.withLock { saveHistory(history.undo(), hintsUsed) }

    suspend fun redo(): Boolean = mutationMutex.withLock { saveHistory(history.redo(), hintsUsed) }

    suspend fun pauseForBackground() {
        val pauseRequestedAtMillis = clock.nowMillis()
        mutationMutex.withLock {
            val currentTimerState = timerState
            val resumedAtMillis = currentTimerState.resumedAtMillis ?: return@withLock
            val elapsedAtPause = currentTimerState.elapsedMillis +
                (pauseRequestedAtMillis - resumedAtMillis).coerceAtLeast(0L)
            timerState = TimerState(elapsedAtPause, null)
            try {
                persistCurrentState()
            } finally {
                publishSnapshot()
            }
        }
    }

    suspend fun resumeFromBackground() = mutationMutex.withLock {
        val currentTimerState = timerState
        if (currentTimerState.resumedAtMillis != null) return@withLock
        timerState = currentTimerState.copy(resumedAtMillis = clock.nowMillis())
        publishSnapshot()
    }

    private suspend fun saveHistory(nextHistory: History<S>, nextHintsUsed: Int): Boolean {
        if (nextHistory === history) return false

        val nextState = nextHistory.present
        val nowSolved = gameModule.isSolved(level, nextState)
        val nextCompleted = completed || nowSolved
        val elapsed = currentElapsedTimeMillis()
        val nextBestTime = if (nowSolved) {
            bestTimeMillis?.let { minOf(it, elapsed) } ?: elapsed
        } else {
            bestTimeMillis
        }
        val save = LevelProgress(
            gameId = gameModule.id,
            levelId = level.ref.levelId,
            stateBytes = gameModule.encodeState(nextState),
            elapsedTimeMillis = elapsed,
            hintsUsed = nextHintsUsed,
            completed = nextCompleted,
            bestTimeMillis = nextBestTime,
        )

        progressRepository.saveLevelProgress(save)
        history = nextHistory
        hintsUsed = nextHintsUsed
        completed = nextCompleted
        bestTimeMillis = nextBestTime
        publishSnapshot()

        if (nowSolved && !solvedEventSent) {
            solvedEventSent = true
            solvedEventChannel.send(
                LevelSolvedEvent(
                    levelRef = level.ref,
                    elapsedTimeMillis = elapsed,
                    hintsUsed = hintsUsed,
                    bestTimeMillis = requireNotNull(bestTimeMillis),
                ),
            )
        }
        return true
    }

    private suspend fun persistCurrentState() {
        val state = history.present
        progressRepository.saveLevelProgress(
            LevelProgress(
                gameId = gameModule.id,
                levelId = level.ref.levelId,
                stateBytes = gameModule.encodeState(state),
                elapsedTimeMillis = currentElapsedTimeMillis(),
                hintsUsed = hintsUsed,
                completed = completed,
                bestTimeMillis = bestTimeMillis,
            ),
        )
    }

    private fun currentElapsedTimeMillis(): Long {
        val currentTimerState = timerState
        val resumedAt = currentTimerState.resumedAtMillis ?: return currentTimerState.elapsedMillis
        return currentTimerState.elapsedMillis + (clock.nowMillis() - resumedAt).coerceAtLeast(0L)
    }

    private fun createSnapshot(): PlaySnapshot<S> {
        val state = history.present
        return PlaySnapshot(
            state = state,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            elapsedTimeMillis = currentElapsedTimeMillis(),
            hintsUsed = hintsUsed,
            mistakes = gameModule.findMistakes(level, state),
            solved = gameModule.isSolved(level, state),
            completed = completed,
            bestTimeMillis = bestTimeMillis,
        )
    }

    private fun publishSnapshot() {
        mutableSnapshot.value = createSnapshot()
    }

    companion object {
        suspend fun <P : Any, S : Any> open(
            gameModule: GameModule<P, S>,
            level: GameLevel<P>,
            progressRepository: ProgressRepository,
            clock: MonotonicClock = SystemMonotonicClock,
        ): PlayController<P, S> {
            require(level.ref.gameId == gameModule.id) { "Level belongs to another game" }
            val savedProgress = progressRepository.getLevelProgress(gameModule.id, level.ref.levelId)
            val initialState = savedProgress?.let { gameModule.decodeState(it.stateBytes) }
                ?: gameModule.newState(level)
            return PlayController(
                gameModule = gameModule,
                level = level,
                progressRepository = progressRepository,
                clock = clock,
                initialState = initialState,
                restoredProgress = savedProgress,
            )
        }
    }
}
