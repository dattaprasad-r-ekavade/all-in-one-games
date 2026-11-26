package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.core.History
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.PlayGameModule
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

/**
 * Play state snapshot for Family B [PlayGameModule] levels.
 */
data class PlayActionSnapshot<S : Any>(
    val state: S,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val elapsedTimeMillis: Long,
    val hintsUsed: Int,
    val failed: Boolean,
    val pendingContinueTransactionId: String?,
    val solved: Boolean,
    val completed: Boolean,
    val bestTimeMillis: Long?,
)

/**
 * Coordinates Family B action play, timer, persistence, and Continue revives for [PlayGameModule].
 */
class PlayActionController<P : Any, S : Any, A : Any> private constructor(
    private val playModule: PlayGameModule<P, S, A>,
    private val level: GameLevel<P>,
    private val progressRepository: ProgressRepository,
    private val clock: MonotonicClock,
    initialState: S,
    restoredProgress: LevelProgress?,
    bankBestTimeMillis: Long?,
) {
    private data class TimerState(
        val elapsedMillis: Long,
        val resumedAtMillis: Long?,
    )

    private val mutationMutex = Mutex()
    private var history = History(initialState)
    private var backgroundPaused = false
    private var hintsUsed = restoredProgress?.hintsUsed ?: 0
    private var pendingContinueTransactionId = restoredProgress?.pendingContinueTransactionId
    private var preContinueState: S? = null
    private var completed = restoredProgress?.completed ?: false
    private var bestTimeMillis = bankBestTimeMillis
    private var solvedEventSent = completed

    @Volatile
    private var timerState = TimerState(
        elapsedMillis = restoredProgress?.elapsedTimeMillis ?: 0L,
        resumedAtMillis = if (isFrozen(initialState)) null else clock.nowMillis(),
    )

    private val mutableSnapshot = MutableStateFlow(createSnapshot())
    private val solvedEventChannel = Channel<LevelSolvedEvent>(Channel.BUFFERED)

    val snapshot: StateFlow<PlayActionSnapshot<S>> = mutableSnapshot.asStateFlow()
    val solvedEvents: Flow<LevelSolvedEvent> = solvedEventChannel.receiveAsFlow()

    val levelRef: LevelRef
        get() = level.ref

    val elapsedTimeMillis: Long
        get() = currentElapsedTimeMillis()

    fun refreshTimer() {
        mutableSnapshot.update { it.copy(elapsedTimeMillis = currentElapsedTimeMillis()) }
    }

    fun hintAction(): A? = playModule.hintAction(level, snapshot.value.state)

    fun currentBoardFingerprint(): String {
        val encodedState = playModule.encodeState(snapshot.value.state)
        val digest = MessageDigest.getInstance("SHA-256").digest(encodedState).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        return "${level.ref.gameId}:${level.ref.levelId}:${level.ref.levelNumber ?: 0}:$digest"
    }

    suspend fun applyAction(action: A): Boolean = mutationMutex.withLock {
        if (isFrozen(history.present) || hasPendingContinueTransaction()) return@withLock false
        val currentState = history.present
        val nextState = playModule.applyAction(currentState, action)
        if (playModule.encodeState(currentState).contentEquals(playModule.encodeState(nextState))) {
            return@withLock false
        }
        // In Arrows, a mistake costs a life and must not be undoable.
        val isArrowsMistake = currentState is ArrowsState &&
            nextState is ArrowsState &&
            nextState.mistakes > currentState.mistakes
        if (isArrowsMistake) {
            saveHistory(History(nextState), hintsUsed, forceSave = true)
        } else {
            saveHistory(history.record(nextState), hintsUsed)
        }
    }

    suspend fun applyHintAction(action: A): Boolean = mutationMutex.withLock {
        if (isFrozen(history.present) || hasPendingContinueTransaction()) return@withLock false
        applyHintLocked(action, countUsage = true)
    }

    /** Applies an already revealed hint action; usage is recorded when the advice is first shown. */
    suspend fun applyRevealedHintAction(action: A): Boolean = mutationMutex.withLock {
        if (isFrozen(history.present) || hasPendingContinueTransaction()) return@withLock false
        applyHintLocked(action, countUsage = false)
    }

    private suspend fun applyHintLocked(action: A, countUsage: Boolean): Boolean {
        if (playModule.hintAction(level, history.present) != action) return false
        val nextState = playModule.applyAction(history.present, action)
        return saveHistory(history.record(nextState), hintsUsed + if (countUsage) 1 else 0)
    }

    suspend fun recordHintUse(hintKey: String) = mutationMutex.withLock {
        if (isFrozen(history.present) || hasPendingContinueTransaction()) return@withLock
        require(hintKey.isNotBlank()) { "Hint key must not be blank" }
        hintsUsed += 1
        persistCurrentState()
        publishSnapshot()
    }

    suspend fun undo(): Boolean = mutationMutex.withLock {
        if (isFailed() || hasPendingContinueTransaction()) return@withLock false
        saveHistory(history.undo(), hintsUsed)
    }

    suspend fun redo(): Boolean = mutationMutex.withLock {
        if (isFailed() || hasPendingContinueTransaction()) return@withLock false
        saveHistory(history.redo(), hintsUsed)
    }

    /** Applies the game's Continue revive (for Arrows, one extra life) and preserves the board. */
    suspend fun continueAfterFailure(transactionId: String): Boolean = mutationMutex.withLock {
        require(transactionId.isNotBlank()) { "Continue transaction ID must not be blank" }
        if (pendingContinueTransactionId == transactionId) return@withLock true
        if (pendingContinueTransactionId != null || !isFailed()) return@withLock false
        val lostState = history.present
        val revivedState = playModule.applyContinue(lostState)
        preContinueState = lostState
        history = History(revivedState)
        pendingContinueTransactionId = transactionId
        val now = clock.nowMillis()
        setTimerRunning(running = runningAt(now) != null, atMillis = now)
        persistCurrentState()
        publishSnapshot()
        true
    }

    suspend fun completeContinueTransaction(transactionId: String): Boolean = mutationMutex.withLock {
        if (pendingContinueTransactionId != transactionId) return@withLock false
        pendingContinueTransactionId = null
        preContinueState = null
        persistCurrentState()
        publishSnapshot()
        true
    }

    suspend fun rollbackContinueTransaction(transactionId: String): Boolean = mutationMutex.withLock {
        if (pendingContinueTransactionId != transactionId) return@withLock false
        pendingContinueTransactionId = null
        preContinueState?.let { history = History(it) }
        preContinueState = null
        val now = clock.nowMillis()
        setTimerRunning(running = runningAt(now) != null, atMillis = now)
        persistCurrentState()
        publishSnapshot()
        true
    }

    suspend fun restart() = mutationMutex.withLock {
        history = History(playModule.newState(level))
        hintsUsed = 0
        pendingContinueTransactionId = null
        preContinueState = null
        val now = clock.nowMillis()
        timerState = TimerState(elapsedMillis = 0L, resumedAtMillis = runningAt(now))
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
        forceSave: Boolean = false,
    ): Boolean {
        if (isFailed() || hasPendingContinueTransaction()) return false
        if (!forceSave && nextHistory === history) return false

        val now = clock.nowMillis()
        val nextState = nextHistory.present
        val nowWon = playModule.isWon(level, nextState)
        val nowLost = playModule.isLost(level, nextState)
        val nextCompleted = completed || nowWon
        val elapsed = elapsedAt(timerState, now)
        val nextBestTime = if (nowWon) {
            bestTimeMillis?.let { minOf(it, elapsed) } ?: elapsed
        } else {
            bestTimeMillis
        }
        val save = LevelProgress(
            gameId = playModule.id,
            levelId = level.ref.levelId,
            stateBytes = playModule.encodeState(nextState),
            elapsedTimeMillis = elapsed,
            hintsUsed = nextHintsUsed,
            completed = nextCompleted,
            bestTimeMillis = nextBestTime,
            mistakesMade = (nextState as? ArrowsState)?.mistakes ?: 0,
            extraMistakesGranted = 0,
            pendingContinueTransactionId = pendingContinueTransactionId,
            attemptNumber = level.ref.levelNumber ?: 0,
        )

        progressRepository.saveLevelProgress(save)
        history = nextHistory
        hintsUsed = nextHintsUsed
        completed = nextCompleted
        bestTimeMillis = nextBestTime
        setTimerRunning(running = !backgroundPaused && !nowWon && !nowLost, atMillis = now)
        publishSnapshot()

        if (nowWon && !solvedEventSent) {
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
                gameId = playModule.id,
                levelId = level.ref.levelId,
                stateBytes = playModule.encodeState(state),
                elapsedTimeMillis = currentElapsedTimeMillis(),
                hintsUsed = hintsUsed,
                completed = completed,
                bestTimeMillis = bestTimeMillis,
                mistakesMade = (state as? ArrowsState)?.mistakes ?: 0,
                extraMistakesGranted = 0,
                pendingContinueTransactionId = pendingContinueTransactionId,
                attemptNumber = level.ref.levelNumber ?: 0,
            ),
        )
    }

    private fun runningAt(atMillis: Long): Long? =
        atMillis.takeUnless { backgroundPaused || isFrozen(history.present) }

    private fun isFrozen(state: S): Boolean =
        playModule.isWon(level, state) || playModule.isLost(level, state)

    private fun isFailed(): Boolean = playModule.isLost(level, history.present)

    private fun isSolved(): Boolean = playModule.isWon(level, history.present)

    private fun hasPendingContinueTransaction(): Boolean = pendingContinueTransactionId != null

    private fun setTimerRunning(running: Boolean, atMillis: Long) {
        val elapsed = elapsedAt(timerState, atMillis)
        timerState = TimerState(elapsedMillis = elapsed, resumedAtMillis = if (running) atMillis else null)
    }

    private fun elapsedAt(timer: TimerState, atMillis: Long): Long {
        val resumedAt = timer.resumedAtMillis ?: return timer.elapsedMillis
        return timer.elapsedMillis + (atMillis - resumedAt).coerceAtLeast(0L)
    }

    private fun currentElapsedTimeMillis(): Long = elapsedAt(timerState, clock.nowMillis())

    private fun createSnapshot(): PlayActionSnapshot<S> {
        val state = history.present
        return PlayActionSnapshot(
            state = state,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            elapsedTimeMillis = currentElapsedTimeMillis(),
            hintsUsed = hintsUsed,
            failed = playModule.isLost(level, state),
            pendingContinueTransactionId = pendingContinueTransactionId,
            solved = playModule.isWon(level, state),
            completed = completed,
            bestTimeMillis = bestTimeMillis,
        )
    }

    private fun publishSnapshot() {
        mutableSnapshot.value = createSnapshot()
    }

    companion object {
        suspend fun <P : Any, S : Any, A : Any> open(
            playModule: PlayGameModule<P, S, A>,
            level: GameLevel<P>,
            progressRepository: ProgressRepository,
            clock: MonotonicClock = SystemMonotonicClock,
        ): PlayActionController<P, S, A> {
            require(level.ref.gameId == playModule.id) { "Level belongs to another game" }
            val attemptNumber = level.ref.levelNumber ?: 0L
            val savedAttempt = progressRepository.getAttemptProgress(playModule.id, level.ref.levelId, attemptNumber)
            val canRestoreLegacy = attemptNumber == 0L ||
                attemptNumber == level.ref.bankIndex?.plus(1L)
            val savedProgress = savedAttempt ?: if (canRestoreLegacy) {
                progressRepository.getAttemptProgress(playModule.id, level.ref.levelId, 0L)
            } else {
                null
            }
            val bankBestTimeMillis = progressRepository.getBestTimeMillis(playModule.id, level.ref.levelId)
            val restoredState = savedProgress?.let { restoreStateOrNull(playModule, level, it.stateBytes) }
            val restoredProgress = when {
                savedProgress == null || restoredState != null -> savedProgress
                else -> savedProgress.copy(
                    elapsedTimeMillis = 0L,
                    hintsUsed = 0,
                    mistakesMade = 0,
                    extraMistakesGranted = 0,
                    pendingContinueTransactionId = null,
                )
            }
            return PlayActionController(
                playModule = playModule,
                level = level,
                progressRepository = progressRepository,
                clock = clock,
                initialState = restoredState ?: playModule.newState(level),
                restoredProgress = restoredProgress,
                bankBestTimeMillis = bankBestTimeMillis,
            )
        }

        private fun <P : Any, S : Any, A : Any> restoreStateOrNull(
            playModule: PlayGameModule<P, S, A>,
            level: GameLevel<P>,
            stateBytes: ByteArray,
        ): S? = try {
            playModule.decodeState(stateBytes).takeIf { state -> playModule.stateMatchesLevel(level, state) }
        } catch (_: Exception) {
            null
        }
    }
}
