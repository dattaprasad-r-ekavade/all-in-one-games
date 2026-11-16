package com.datathecodie.witgrid.play

import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class PlayControllerTest {
    @Test
    fun groupedGestureCreatesOneHistoryEntryAndSavesAfterTheGesture() = runBlocking {
        val repository = FakeProgressRepository()
        val clock = FakeClock(10_000)
        val controller = PlayController.open(testModule, testLevel, repository, clock)
        clock.advance(250)

        val changed = controller.applyGesture(
            listOf(
                Move.SetMark(Cell(0), Mark.FILLED),
                Move.SetMark(Cell(1), Mark.FILLED),
            ),
        )

        assertTrue(changed)
        assertTrue(controller.snapshot.value.solved)
        assertTrue(controller.snapshot.value.canUndo)
        assertEquals(1, repository.writes.size)
        assertEquals(250, repository.lastSaved().elapsedTimeMillis)
        assertEquals(2, TestEngine.decodeState(repository.lastSaved().stateBytes).marks.count { it == Mark.FILLED })
        assertEquals(250, controller.solvedEvents.first().elapsedTimeMillis)

        assertTrue(controller.undo())
        assertFalse(controller.snapshot.value.solved)
        assertTrue(controller.snapshot.value.canRedo)
        assertTrue(controller.redo())
        assertTrue(controller.snapshot.value.solved)
        assertEquals(3, repository.writes.size)
    }

    @Test
    fun overlappingGesturesAreAppliedInOrder() = runBlocking {
        val repository = BlockingFirstSaveRepository()
        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(10_000))
        val firstMove = launch { controller.applyMove(Move.SetMark(Cell(0), Mark.FILLED)) }
        repository.firstSaveStarted.await()
        val secondMove = launch(start = CoroutineStart.UNDISPATCHED) {
            controller.applyMove(Move.SetMark(Cell(1), Mark.FILLED))
        }

        repository.allowFirstSave.complete(Unit)
        firstMove.join()
        secondMove.join()

        assertEquals(List(2) { Mark.FILLED }, controller.snapshot.value.state.marks)
        assertEquals(2, repository.writes.size)
        assertEquals(List(2) { Mark.FILLED }, TestEngine.decodeState(repository.writes.last().stateBytes).marks)
    }

    @Test
    fun backgroundSaveWaitsForAnInFlightMoveSave() = runBlocking {
        val repository = BlockingFirstSaveRepository()
        val clock = FakeClock(20_000)
        val controller = PlayController.open(testModule, testLevel, repository, clock)
        val move = launch { controller.applyMove(Move.SetMark(Cell(0), Mark.FILLED)) }
        repository.firstSaveStarted.await()
        clock.advance(400)
        val pause = launch(start = CoroutineStart.UNDISPATCHED) { controller.pauseForBackground() }
        clock.advance(1_500)

        repository.allowFirstSave.complete(Unit)
        move.join()
        pause.join()

        assertEquals(2, repository.writes.size)
        assertTrue(repository.writes.all { saved ->
            TestEngine.decodeState(saved.stateBytes).marks[0] == Mark.FILLED
        })
        assertEquals(400, repository.writes.last().elapsedTimeMillis)
    }

    @Test
    fun applyingAHintUsesOneHistoryEntryAndIncrementsHintCount() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(50))
        val hint = controller.nextHint()

        assertNotNull(hint)
        assertTrue(controller.applyHint(requireNotNull(hint)))

        assertEquals(1, controller.snapshot.value.hintsUsed)
        assertTrue(controller.snapshot.value.canUndo)
        assertEquals(1, repository.writes.size)
        assertTrue(controller.undo())
        assertEquals(1, controller.snapshot.value.hintsUsed)
    }

    @Test
    fun reopeningRestoresProgressButStartsWithEmptyUndoHistory() = runBlocking {
        val repository = FakeProgressRepository()
        val savedState = testModule.newState(testLevel)
            .let { testModule.applyMove(it, Move.SetMark(Cell(0), Mark.FILLED)) }
        repository.saveLevelProgress(
            LevelProgress(
                gameId = testModule.id,
                levelId = testLevel.ref.levelId,
                stateBytes = testModule.encodeState(savedState),
                elapsedTimeMillis = 4_500,
                hintsUsed = 2,
                completed = false,
                bestTimeMillis = null,
            ),
        )

        val restored = PlayController.open(testModule, testLevel, repository, FakeClock(30_000))

        assertEquals(savedState, restored.snapshot.value.state)
        assertEquals(4_500, restored.elapsedTimeMillis)
        assertEquals(2, restored.snapshot.value.hintsUsed)
        assertFalse(restored.snapshot.value.canUndo)
    }

    @Test
    fun elapsedTimerPausesInBackgroundAndContinuesOnReturn() = runBlocking {
        val repository = FakeProgressRepository()
        val clock = FakeClock(100)
        val controller = PlayController.open(testModule, testLevel, repository, clock)
        clock.advance(500)

        controller.pauseForBackground()
        clock.advance(5_000)
        controller.refreshTimer()
        assertEquals(500, controller.snapshot.value.elapsedTimeMillis)
        assertEquals(500, repository.lastSaved().elapsedTimeMillis)

        controller.resumeFromBackground()
        clock.advance(250)
        controller.refreshTimer()
        assertEquals(750, controller.snapshot.value.elapsedTimeMillis)
    }

    @Test
    fun restartResetsTheAttemptButKeepsCompletionAndBestTime() = runBlocking {
        val repository = FakeProgressRepository()
        val clock = FakeClock(40_000)
        val controller = PlayController.open(testModule, testLevel, repository, clock)
        clock.advance(250)
        assertTrue(controller.applyHint(requireNotNull(controller.nextHint())))
        controller.applyMove(Move.SetMark(Cell(1), Mark.FILLED))
        assertEquals(250, controller.solvedEvents.first().elapsedTimeMillis)

        clock.advance(5_000)
        controller.restart()

        val restarted = controller.snapshot.value
        assertEquals(List(2) { Mark.EMPTY }, restarted.state.marks)
        assertFalse(restarted.solved)
        assertFalse(restarted.canUndo)
        assertFalse(restarted.canRedo)
        assertEquals(0, restarted.hintsUsed)
        assertTrue(restarted.completed)
        assertEquals(250, restarted.bestTimeMillis)
        assertEquals(0, restarted.elapsedTimeMillis)

        clock.advance(100)
        controller.refreshTimer()
        assertEquals(100, controller.snapshot.value.elapsedTimeMillis)
        val saved = repository.lastSaved()
        assertEquals(0, saved.elapsedTimeMillis)
        assertTrue(saved.completed)
        assertEquals(250, saved.bestTimeMillis)
        assertEquals(List(2) { Mark.EMPTY }, TestEngine.decodeState(saved.stateBytes).marks)

        controller.applyGesture(listOf(Move.SetMark(Cell(0), Mark.FILLED), Move.SetMark(Cell(1), Mark.FILLED)))
        val secondSolve = controller.solvedEvents.first()
        assertEquals(100, secondSolve.elapsedTimeMillis)
        assertEquals(0, secondSolve.hintsUsed)
        assertEquals(100, secondSolve.bestTimeMillis)
    }

    @Test
    fun restartWhilePausedStaysPausedUntilResume() = runBlocking {
        val repository = FakeProgressRepository()
        val clock = FakeClock(60_000)
        val controller = PlayController.open(testModule, testLevel, repository, clock)
        clock.advance(500)
        controller.pauseForBackground()

        controller.restart()
        clock.advance(5_000)
        controller.refreshTimer()
        assertEquals(0, controller.snapshot.value.elapsedTimeMillis)
        assertEquals(0, repository.lastSaved().elapsedTimeMillis)

        clock.advance(3_000)
        controller.refreshTimer()
        assertEquals(0, controller.snapshot.value.elapsedTimeMillis)

        controller.resumeFromBackground()
        clock.advance(250)
        controller.refreshTimer()
        assertEquals(250, controller.snapshot.value.elapsedTimeMillis)
    }

    @Test
    fun undoAndRedoEachSaveTheRestoredState() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(10_000))
        controller.applyGesture(solvingMoves())
        assertEquals(1, repository.writes.size)

        assertTrue(controller.undo())
        assertEquals(2, repository.writes.size)
        assertEquals(List(2) { Mark.EMPTY }, savedMarks(repository))

        assertTrue(controller.redo())
        assertEquals(3, repository.writes.size)
        assertEquals(List(2) { Mark.FILLED }, savedMarks(repository))
    }

    @Test
    fun solvedEventFiresOncePerAttempt() = runBlocking {
        val controller = PlayController.open(testModule, testLevel, FakeProgressRepository(), FakeClock(10_000))
        controller.applyGesture(solvingMoves())
        assertNotNull(withTimeoutOrNull(NO_EVENT_WAIT_MILLIS) { controller.solvedEvents.first() })

        controller.undo()
        controller.applyGesture(solvingMoves())
        assertNull(withTimeoutOrNull(NO_EVENT_WAIT_MILLIS) { controller.solvedEvents.first() })

        controller.restart()
        controller.applyGesture(solvingMoves())
        assertNotNull(withTimeoutOrNull(NO_EVENT_WAIT_MILLIS) { controller.solvedEvents.first() })
    }

    @Test
    fun mistakesMadeCountsNewWrongEntriesOnly() = runBlocking {
        val controller = PlayController.open(testModule, testLevel, FakeProgressRepository(), FakeClock(10_000))
        controller.applyMove(Move.SetMark(Cell(0), Mark.CROSSED))
        assertEquals(1, controller.snapshot.value.mistakesMade)
        controller.applyMove(Move.SetMark(Cell(1), Mark.CROSSED))
        assertEquals(2, controller.snapshot.value.mistakesMade)

        controller.applyMove(Move.SetMark(Cell(0), Mark.FILLED))
        assertEquals(2, controller.snapshot.value.mistakesMade)
        controller.undo()
        controller.redo()
        assertEquals(2, controller.snapshot.value.mistakesMade)

        controller.restart()
        assertEquals(0, controller.snapshot.value.mistakesMade)
    }

    @Test
    fun solvedBoardStopsTheTimer() = runBlocking {
        val clock = FakeClock(10_000)
        val controller = PlayController.open(testModule, testLevel, FakeProgressRepository(), clock)
        clock.advance(250)
        controller.applyGesture(solvingMoves())

        clock.advance(5_000)
        controller.refreshTimer()
        assertEquals(250, controller.snapshot.value.elapsedTimeMillis)

        controller.undo()
        clock.advance(100)
        controller.refreshTimer()
        assertEquals(350, controller.snapshot.value.elapsedTimeMillis)
    }

    @Test
    fun undoOutOfSolvedBoardStaysStoppedWhileInBackground() = runBlocking {
        val clock = FakeClock(10_000)
        val controller = PlayController.open(testModule, testLevel, FakeProgressRepository(), clock)
        clock.advance(250)
        controller.applyGesture(solvingMoves())
        controller.pauseForBackground()
        controller.undo()

        clock.advance(1_000)
        controller.refreshTimer()
        assertEquals(250, controller.snapshot.value.elapsedTimeMillis)

        controller.resumeFromBackground()
        clock.advance(100)
        controller.refreshTimer()
        assertEquals(350, controller.snapshot.value.elapsedTimeMillis)
    }

    @Test
    fun recordHintUseCountsAHintWithoutChangingTheBoard() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(50))

        controller.recordHintUse()

        assertEquals(1, controller.snapshot.value.hintsUsed)
        assertEquals(1, repository.lastSaved().hintsUsed)
        assertEquals(List(2) { Mark.EMPTY }, controller.snapshot.value.state.marks)
        assertFalse(controller.snapshot.value.canUndo)
    }

    @Test
    fun appliedHintIsSavedWithItsStateAndCount() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(50))
        val hint = requireNotNull(controller.nextHint())

        assertTrue(controller.applyHint(hint))

        assertEquals(1, controller.snapshot.value.hintsUsed)
        assertEquals(1, repository.lastSaved().hintsUsed)
        assertEquals(listOf(Mark.FILLED, Mark.EMPTY), savedMarks(repository))
    }

    @Test
    fun unreadableSaveStartsAFreshAttemptAndKeepsCompletion() = runBlocking {
        val repository = FakeProgressRepository()
        repository.saveLevelProgress(
            savedProgress(stateBytes = byteArrayOf(9), completed = true, bestTimeMillis = 3_000),
        )

        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(30_000))

        val snapshot = controller.snapshot.value
        assertEquals(List(2) { Mark.EMPTY }, snapshot.state.marks)
        assertEquals(0, snapshot.elapsedTimeMillis)
        assertEquals(0, snapshot.hintsUsed)
        assertTrue(snapshot.completed)
        assertEquals(3_000, snapshot.bestTimeMillis)
    }

    @Test
    fun saveForAnotherPuzzleStartsAFreshAttempt() = runBlocking {
        val repository = FakeProgressRepository()
        val otherPuzzleState = TestState(
            TestPuzzle(listOf(Mark.FILLED, Mark.CROSSED)),
            listOf(Mark.FILLED, Mark.EMPTY),
        )
        repository.saveLevelProgress(savedProgress(stateBytes = TestEngine.encodeState(otherPuzzleState)))

        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(30_000))

        assertEquals(List(2) { Mark.EMPTY }, controller.snapshot.value.state.marks)
        assertFalse(controller.snapshot.value.completed)
    }

    private fun solvingMoves() = listOf(
        Move.SetMark(Cell(0), Mark.FILLED),
        Move.SetMark(Cell(1), Mark.FILLED),
    )

    private fun savedMarks(repository: FakeProgressRepository): List<Mark> =
        TestEngine.decodeState(repository.lastSaved().stateBytes).marks

    private fun savedProgress(
        stateBytes: ByteArray,
        completed: Boolean = false,
        bestTimeMillis: Long? = null,
    ) = LevelProgress(
        gameId = testModule.id,
        levelId = testLevel.ref.levelId,
        stateBytes = stateBytes,
        elapsedTimeMillis = 4_500,
        hintsUsed = 2,
        completed = completed,
        bestTimeMillis = bestTimeMillis,
    )

    private fun FakeProgressRepository.lastSaved(): LevelProgress = requireNotNull(writes.lastOrNull())

    private data class TestPuzzle(val solution: List<Mark>)

    private data class TestState(val puzzle: TestPuzzle, val marks: List<Mark>)

    private object TestEngine : GameEngine<TestPuzzle, TestState> {
        override val id = "test"

        override fun decodePuzzle(line: String) = TestPuzzle(List(2) { Mark.FILLED })

        override fun encodePuzzle(puzzle: TestPuzzle) =
            puzzle.solution.joinToString(separator = ",") { it.name }

        override fun initialState(puzzle: TestPuzzle) = TestState(puzzle, List(puzzle.solution.size) { Mark.EMPTY })

        override fun applyMove(state: TestState, move: Move): TestState {
            val setMark = move as? Move.SetMark
                ?: throw IllegalArgumentException("Test engine accepts mark moves only")
            require(setMark.cell.index < state.marks.size)
            val updated = state.marks.toMutableList()
            updated[setMark.cell.index] = setMark.mark
            return state.copy(marks = updated)
        }

        override fun isSolved(puzzle: TestPuzzle, state: TestState): Boolean =
            state.marks == puzzle.solution

        override fun mistakes(puzzle: TestPuzzle, state: TestState): List<Cell> =
            state.marks.indices.filter { index ->
                state.marks[index] != Mark.EMPTY && state.marks[index] != puzzle.solution[index]
            }.map(::Cell)

        override fun countSolutions(puzzle: TestPuzzle, limit: Int): Int {
            require(limit > 0)
            return minOf(1, limit)
        }

        override fun nextStep(puzzle: TestPuzzle, state: TestState): HintStep? {
            if (mistakes(puzzle, state).isNotEmpty()) return null
            val index = state.marks.indices.firstOrNull { state.marks[it] == Mark.EMPTY } ?: return null
            return HintStep(
                technique = Technique("fill", "Fill", 0),
                cells = listOf(Cell(index)),
                explanation = "Fill the next cell.",
                moves = listOf(Move.SetMark(Cell(index), puzzle.solution[index])),
            )
        }

        override fun grade(puzzle: TestPuzzle) = Grade(Technique("fill", "Fill", 0), 1, Pack.EASY)

        override fun generate(rng: SplitMix64, pack: Pack) = TestPuzzle(List(2) { Mark.FILLED })

        override fun encodeState(state: TestState): ByteArray {
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).use { output ->
                output.writeByte(1)
                output.writeInt(state.marks.size)
                state.puzzle.solution.forEach { output.writeByte(it.ordinal) }
                state.marks.forEach { output.writeByte(it.ordinal) }
            }
            return bytes.toByteArray()
        }

        override fun decodeState(bytes: ByteArray): TestState =
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                require(input.readUnsignedByte() == 1)
                val size = input.readInt()
                val solution = List(size) { Mark.entries[input.readUnsignedByte()] }
                val marks = List(size) { Mark.entries[input.readUnsignedByte()] }
                require(input.available() == 0)
                TestState(TestPuzzle(solution), marks)
            }
    }

    private class FakeProgressRepository : ProgressRepository {
        private val saved = mutableMapOf<Pair<String, String>, LevelProgress>()
        val writes = mutableListOf<LevelProgress>()

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> =
            flowOf(saved[gameId to levelId])

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? =
            saved[gameId to levelId]

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            val copy = progress.copy(stateBytes = progress.stateBytes.copyOf())
            saved[progress.gameId to progress.levelId] = copy
            writes += copy
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            saved.remove(gameId to levelId)
        }
    }

    private class BlockingFirstSaveRepository : ProgressRepository {
        private val saved = mutableMapOf<Pair<String, String>, LevelProgress>()
        private var shouldBlockFirstSave = true
        val firstSaveStarted = CompletableDeferred<Unit>()
        val allowFirstSave = CompletableDeferred<Unit>()
        val writes = mutableListOf<LevelProgress>()

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> =
            flowOf(saved[gameId to levelId])

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? =
            saved[gameId to levelId]

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            if (shouldBlockFirstSave) {
                shouldBlockFirstSave = false
                firstSaveStarted.complete(Unit)
                allowFirstSave.await()
            }
            val copy = progress.copy(stateBytes = progress.stateBytes.copyOf())
            saved[progress.gameId to progress.levelId] = copy
            writes += copy
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            saved.remove(gameId to levelId)
        }
    }

    private class FakeClock(var currentMillis: Long) : MonotonicClock {
        override fun nowMillis(): Long = currentMillis

        fun advance(milliseconds: Long) {
            currentMillis += milliseconds
        }
    }

    private companion object {
        const val NO_EVENT_WAIT_MILLIS = 100L
        val testModule = EngineGameModule(TestEngine, TestState::puzzle) { 1 }
        val testLevel = GameLevel(
            ref = LevelRef(TestEngine.id, "test-easy-001", Pack.EASY),
            size = 2,
            puzzle = TestPuzzle(List(2) { Mark.FILLED }),
        )
    }
}
