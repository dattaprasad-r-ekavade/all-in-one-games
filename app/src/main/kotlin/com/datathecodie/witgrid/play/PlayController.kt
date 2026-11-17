package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Cell
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
import java.security.MessageDigest

/**
 * What the play screen shows. [mistakesMade] counts new wrong entries in this attempt. It is
 * kept in memory only, so a reopened level starts at 0. M5 owns its persistence and the
 * "level failed" signal built on it.
 */
data class PlaySnapshot<S : Any>(
    val state: S,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val mistakes: List<Cell>,
    val mistakesMade: Int,
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
        // Null while the timer is stopped: paused in background or frozen on a solved board.
        val resumedAtMillis: Long?,
    )

    private val mutationMutex = Mutex()
    private var history = History(initialState)
    @Volatile
    private var timerState = TimerState(
        elapsedMillis = restoredProgress?.elapsedTimeMillis ?: 0L,
        resumedAtMillis = if (gameModule.isSolved(level, initialState)) null else clock.nowMillis(),
    )
    private var backgroundPaused = false
    private var hintsUsed = restoredProgress?.hintsUsed ?: 0
    private var mistakesMade = 0
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

    fun currentBoardFingerprint(): String {
        val encodedState = gameModule.encodeState(snapshot.value.state)
        val digest = MessageDigest.getInstance("SHA-256").digest(encodedState).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        return "${level.ref.gameId}:${level.ref.levelId}:$digest"
    }

    suspend fun applyMove(move: Move): Boolean = applyGesture(listOf(move))

    suspend fun applyGesture(moves: List<Move>): Boolean = mutationMutex.withLock {
        if (moves.isEmpty()) return@withLock false
        val mistakesBefore = gameModule.findMistakes(level, history.present).toSet()
        val nextState = gameModule.applyMoves(history.present, moves)
        val newMistakes = gameModule.findMistakes(level, nextState).count { it !in mistakesBefore }
        saveHistory(history.record(nextState), hintsUsed, newMistakes)
    }

    suspend fun applyHint(hint: HintStep): Boolean = mutationMutex.withLock {
        applyHintLocked(hint, countUsage = true)
    }

    /** Applies an already revealed hint; usage is recorded when the advice is first shown. */
    suspend fun applyRevealedHint(hint: HintStep): Boolean = mutationMutex.withLock {
        applyHintLocked(hint, countUsage = false)
    }

    private suspend fun applyHintLocked(hint: HintStep, countUsage: Boolean): Boolean {
        if (gameModule.nextHint(level, history.present) != hint) return false
        val nextState = gameModule.applyMoves(history.present, hint.moves)
        return saveHistory(history.record(nextState), hintsUsed + if (countUsage) 1 else 0)
    }

    /**
     * Counts a hint that is not a [HintStep], such as a mistake hint. The board does not change.
     */
    suspend fun recordHintUse(hintKey: String) = mutationMutex.withLock {
        require(hintKey.isNotBlank()) { "Hint key must not be blank" }
        hintsUsed += 1
        persistCurrentState()
        publishSnapshot()
    }

    suspend fun undo(): Boolean = mutationMutex.withLock { saveHistory(history.undo(), hintsUsed) }

    suspend fun redo(): Boolean = mutationMutex.withLock { saveHistory(history.redo(), hintsUsed) }

    /**
     * Starts a new attempt at the level. Completion and best time are kept. A solve in the
     * new attempt sends a new solved event. A paused timer stays paused until resume.
     */
    suspend fun restart() = mutationMutex.withLock {
        history = History(gameModule.newState(level))
        val now = clock.nowMillis()
        timerState = TimerState(elapsedMillis = 0L, resumedAtMillis = runningAt(now))
        hintsUsed = 0
        mistakesMade = 0
        solvedEventSent = false
        persistCurrentState()
        publishSnapshot()
    }

    suspend fun pauseForBackground() {
        val pauseRequestedAtMillis = clock.nowMillis()
        mutationMutex.withLock {
            backgroundPaused = true
            setTimerRunning(running = false, atMillis = pauseRequestedAtMillis)
            try {
                persistCurrentState()
            } finally {
                publishSnapshot()
            }
        }
    }

    suspend fun resumeFromBackground() = mutationMutex.withLock {
        backgroundPaused = false
        val now = clock.nowMillis()
        setTimerRunning(running = runningAt(now) != null, atMillis = now)
        publishSnapshot()
    }

    private suspend fun saveHistory(
        nextHistory: History<S>,
        nextHintsUsed: Int,
        newMistakes: Int = 0,
    ): Boolean {
        if (nextHistory === history) return false

        val now = clock.nowMillis()
        val nextState = nextHistory.present
        val nowSolved = gameModule.isSolved(level, nextState)
        val nextCompleted = completed || nowSolved
        val elapsed = elapsedAt(timerState, now)
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
        mistakesMade += newMistakes
        completed = nextCompleted
        bestTimeMillis = nextBestTime
        // A solved board stops the clock. Undo out of it restarts the clock unless the app is paused.
        setTimerRunning(running = !backgroundPaused && !nowSolved, atMillis = now)
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

    /** Returns the start of a running timer at [atMillis], or null when the timer must stay stopped. */
    private fun runningAt(atMillis: Long): Long? =
        atMillis.takeUnless { backgroundPaused || gameModule.isSolved(level, history.present) }

    private fun setTimerRunning(running: Boolean, atMillis: Long) {
        val elapsed = elapsedAt(timerState, atMillis)
        timerState = TimerState(elapsedMillis = elapsed, resumedAtMillis = if (running) atMillis else null)
    }

    private fun elapsedAt(timer: TimerState, atMillis: Long): Long {
        val resumedAt = timer.resumedAtMillis ?: return timer.elapsedMillis
        return timer.elapsedMillis + (atMillis - resumedAt).coerceAtLeast(0L)
    }

    private fun currentElapsedTimeMillis(): Long = elapsedAt(timerState, clock.nowMillis())

    private fun createSnapshot(): PlaySnapshot<S> {
        val state = history.present
        return PlaySnapshot(
            state = state,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            elapsedTimeMillis = currentElapsedTimeMillis(),
            hintsUsed = hintsUsed,
            mistakes = gameModule.findMistakes(level, state),
            mistakesMade = mistakesMade,
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
            val restoredState = savedProgress?.let { restoreStateOrNull(gameModule, level, it.stateBytes) }
            // A save that cannot be used starts a fresh attempt. Completion and best time are kept.
            val restoredProgress = when {
                savedProgress == null || restoredState != null -> savedProgress
                else -> savedProgress.copy(elapsedTimeMillis = 0L, hintsUsed = 0)
            }
            return PlayController(
                gameModule = gameModule,
                level = level,
                progressRepository = progressRepository,
                clock = clock,
                initialState = restoredState ?: gameModule.newState(level),
                restoredProgress = restoredProgress,
            )
        }

        /**
         * Returns the saved state, or null when the save is corrupt, from another version,
         * or holds a different puzzle than [level]. A bad save must not block play (plan rule 7).
         */
        private fun <P : Any, S : Any> restoreStateOrNull(
            gameModule: GameModule<P, S>,
            level: GameLevel<P>,
            stateBytes: ByteArray,
        ): S? = try {
            gameModule.decodeState(stateBytes).takeIf { state -> gameModule.stateMatchesLevel(level, state) }
        } catch (exception: Exception) {
            null
        }
    }
}
