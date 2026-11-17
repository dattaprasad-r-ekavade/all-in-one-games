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
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuPuzzle
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
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
    fun applyingARevealedHintDoesNotChargeUsageTwice() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(testModule, testLevel, repository, FakeClock(50))
        val hint = requireNotNull(controller.nextHint())
        val boardKeyBeforeHint = controller.currentBoardFingerprint()

        controller.recordHintUse("revealed-hint")
        assertTrue(controller.applyRevealedHint(hint))

        assertEquals(1, controller.snapshot.value.hintsUsed)
        assertTrue(boardKeyBeforeHint != controller.currentBoardFingerprint())
        assertTrue(controller.snapshot.value.canUndo)
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
    fun changingWrongDigitCountsAgainButNoOpAndNotesDoNot() = runBlocking {
        val puzzle = MiniSudokuPuzzle(
            id = "mistake-test",
            givens = List(36) { null },
            solution = List(36) { index ->
                val row = index / 6
                val column = index % 6
                (row * 3 + row / 2 + column) % 6 + 1
            },
            pack = Pack.EASY,
            storedGrade = "easy",
        )
        val level = GameLevel(
            ref = LevelRef(MiniSudokuEngine.id, "mistake-test-easy", Pack.EASY),
            size = 6,
            puzzle = puzzle,
        )
        val repository = FakeProgressRepository()
        val controller = PlayController.open(
            gameModule = MiniSudokuGameModule,
            level = level,
            progressRepository = repository,
            clock = FakeClock(10_000),
            mistakeLimitEnabled = true,
        )

        controller.applyMove(Move.SetDigit(Cell(0), 2))
        controller.applyMove(Move.SetNotes(Cell(1), 1))
        controller.applyMove(Move.SetDigit(Cell(0), 3))
        controller.applyMove(Move.SetDigit(Cell(0), 3))

        assertEquals(2, controller.snapshot.value.mistakesMade)
        assertEquals(2, repository.lastSaved().mistakesMade)
        assertEquals(setOf(Cell(0)), controller.snapshot.value.mistakes.toSet())
    }

    @Test
    fun gestureStopsAtFailureThresholdSoContinueAddsAUsableMistake() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = repository,
            clock = FakeClock(10_000),
            mistakeLimitEnabled = true,
        )

        controller.applyGesture(List(4) { index -> Move.SetMark(Cell(index), Mark.CROSSED) })
        assertEquals(3, controller.snapshot.value.mistakesMade)
        assertEquals(Mark.EMPTY, controller.snapshot.value.state.marks[3])

        assertTrue(controller.continueAfterFailure("continue-transaction-1"))
        assertFalse(controller.applyMove(Move.SetMark(Cell(3), Mark.CROSSED)))
        assertTrue(controller.completeContinueTransaction("continue-transaction-1"))
        assertTrue(controller.applyMove(Move.SetMark(Cell(3), Mark.CROSSED)))
        assertEquals(4, controller.snapshot.value.mistakesMade)
        assertTrue(controller.snapshot.value.failed)
    }

    @Test
    fun enabledMistakeLimitFailsAtThreeAndLocksTheBoard() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = repository,
            clock = FakeClock(10_000),
            mistakeLimitEnabled = true,
        )

        assertTrue(
            controller.applyGesture(
                List(4) { index -> Move.SetMark(Cell(index), Mark.CROSSED) },
            ),
        )

        assertEquals(3, controller.snapshot.value.mistakesMade)
        assertEquals(3, controller.snapshot.value.mistakesAllowed)
        assertTrue(controller.snapshot.value.failed)
        assertEquals(3, controller.snapshot.value.mistakes.size)
        assertEquals(List(3) { Mark.CROSSED } + Mark.EMPTY, controller.snapshot.value.state.marks)
        assertFalse(controller.applyMove(Move.SetMark(Cell(3), Mark.CROSSED)))
        assertEquals(3, controller.snapshot.value.mistakesMade)
        assertEquals(3, repository.lastSaved().mistakesMade)
    }

    @Test
    fun continuingPreservesBoardAndAllowanceAcrossReopen() = runBlocking {
        val repository = FakeProgressRepository()
        val clock = FakeClock(10_000)
        val controller = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = repository,
            clock = clock,
            mistakeLimitEnabled = true,
        )
        repeat(3) { index -> controller.applyMove(Move.SetMark(Cell(index), Mark.CROSSED)) }
        val failedBoard = controller.snapshot.value.state
        clock.advance(1_000)
        controller.refreshTimer()
        assertEquals(0, controller.snapshot.value.elapsedTimeMillis)

        assertTrue(controller.continueAfterFailure("continue-transaction-1"))
        assertEquals("continue-transaction-1", repository.lastSaved().pendingContinueTransactionId)
        assertEquals(failedBoard, controller.snapshot.value.state)
        assertEquals(3, controller.snapshot.value.mistakesMade)
        assertEquals(4, controller.snapshot.value.mistakesAllowed)
        assertFalse(controller.snapshot.value.failed)
        assertEquals(1, repository.lastSaved().extraMistakesGranted)
        clock.advance(200)
        controller.refreshTimer()
        assertEquals(200, controller.snapshot.value.elapsedTimeMillis)

        val reopened = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = repository,
            clock = FakeClock(20_000),
            mistakeLimitEnabled = true,
        )
        assertEquals(failedBoard, reopened.snapshot.value.state)
        assertEquals(3, reopened.snapshot.value.mistakesMade)
        assertFalse(reopened.snapshot.value.failed)
        assertEquals("continue-transaction-1", reopened.snapshot.value.pendingContinueTransactionId)
        assertTrue(reopened.continueAfterFailure("continue-transaction-1"))
        assertEquals(4, reopened.snapshot.value.mistakesAllowed)
        assertTrue(reopened.completeContinueTransaction("continue-transaction-1"))
        assertNull(reopened.snapshot.value.pendingContinueTransactionId)

        assertTrue(reopened.applyMove(Move.SetMark(Cell(3), Mark.CROSSED)))
        assertTrue(reopened.snapshot.value.failed)
        assertEquals(4, reopened.snapshot.value.mistakesMade)
        assertTrue(reopened.continueAfterFailure("continue-transaction-2"))
        assertEquals(5, reopened.snapshot.value.mistakesAllowed)
        assertFalse(reopened.snapshot.value.failed)
    }

    @Test
    fun disabledMistakeLimitDoesNotFailAndRestartClearsMistakesAndContinues() = runBlocking {
        val repository = FakeProgressRepository()
        val controller = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = repository,
            clock = FakeClock(10_000),
        )
        repeat(3) { index -> controller.applyMove(Move.SetMark(Cell(index), Mark.CROSSED)) }

        assertFalse(controller.snapshot.value.failed)
        assertEquals(3, controller.snapshot.value.mistakesMade)

        val enabledController = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = repository,
            clock = FakeClock(20_000),
            mistakeLimitEnabled = true,
        )
        assertTrue(enabledController.snapshot.value.failed)
        assertTrue(enabledController.continueAfterFailure("continue-transaction-1"))
        enabledController.restart()

        assertEquals(0, enabledController.snapshot.value.mistakesMade)
        assertEquals(3, enabledController.snapshot.value.mistakesAllowed)
        assertFalse(enabledController.snapshot.value.failed)
        assertEquals(0, repository.lastSaved().mistakesMade)
        assertEquals(0, repository.lastSaved().extraMistakesGranted)
        assertNull(repository.lastSaved().pendingContinueTransactionId)
    }

    @Test
    fun restartingFailedAttemptRestartsTheTimerAfterResettingTheMistakeLimit() = runBlocking {
        val clock = FakeClock(10_000)
        val controller = PlayController.open(
            gameModule = testModule,
            level = fourCellTestLevel,
            progressRepository = FakeProgressRepository(),
            clock = clock,
            mistakeLimitEnabled = true,
        )
        repeat(3) { index -> controller.applyMove(Move.SetMark(Cell(index), Mark.CROSSED)) }
        clock.advance(1_000)
        assertTrue(controller.snapshot.value.failed)

        controller.restart()
        clock.advance(150)
        controller.refreshTimer()

        assertFalse(controller.snapshot.value.failed)
        assertEquals(0, controller.snapshot.value.mistakesMade)
        assertEquals(150, controller.snapshot.value.elapsedTimeMillis)
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

        controller.recordHintUse("test-hint")

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

    @Test
    fun repeatedBankPuzzleStartsFreshKeepsBestAndResumesOnlyItsOwnAttempt() = runBlocking {
        val repository = FakeProgressRepository()
        val firstLevel = testLevel.copy(ref = testLevel.ref.copy(levelNumber = 1, bankIndex = 0))
        val clock = FakeClock(10_000)
        val first = PlayController.open(testModule, firstLevel, repository, clock)
        val firstEmptyFingerprint = first.currentBoardFingerprint()
        clock.advance(300)
        first.applyHint(requireNotNull(first.nextHint()))
        first.applyHint(requireNotNull(first.nextHint()))
        assertTrue(first.snapshot.value.completed)

        val loopLevel = firstLevel.copy(ref = firstLevel.ref.copy(levelNumber = 3))
        val repeated = PlayController.open(testModule, loopLevel, repository, clock)
        assertEquals(List(2) { Mark.EMPTY }, repeated.snapshot.value.state.marks)
        assertEquals(0L, repeated.snapshot.value.elapsedTimeMillis)
        assertEquals(0, repeated.snapshot.value.hintsUsed)
        assertEquals(0, repeated.snapshot.value.mistakesMade)
        assertEquals(null, repeated.snapshot.value.pendingContinueTransactionId)
        assertEquals(300L, repeated.snapshot.value.bestTimeMillis)
        assertFalse(repeated.snapshot.value.completed)
        assertFalse(repeated.snapshot.value.solved)
        assertFalse(repeated.snapshot.value.canUndo)
        assertTrue(firstEmptyFingerprint != repeated.currentBoardFingerprint())
        repeated.applyMove(solvingMoves().first())
        clock.advance(500)
        repeated.pauseForBackground()

        val resumed = PlayController.open(testModule, loopLevel, repository, clock)
        assertEquals(listOf(Mark.FILLED, Mark.EMPTY), resumed.snapshot.value.state.marks)
        assertEquals(500L, resumed.snapshot.value.elapsedTimeMillis)
        resumed.applyMove(solvingMoves().last())
        assertEquals(300L, resumed.solvedEvents.first().bestTimeMillis)
        assertEquals(3L, repository.lastSaved().attemptNumber)
        assertTrue(TestEngine.isSolved(firstLevel.puzzle, TestEngine.decodeState(
            requireNotNull(repository.getAttemptProgress(testModule.id, firstLevel.ref.levelId, 1)).stateBytes,
        )))
    }

    @Test
    fun legacyBankSaveResumesAtItsOneBasedIndexButCannotLeakIntoLaterLoop() = runBlocking {
        val repository = FakeProgressRepository()
        val legacyState = TestEngine.initialState(testLevel.puzzle).copy(marks = listOf(Mark.FILLED, Mark.EMPTY))
        repository.saveLevelProgress(savedProgress(TestEngine.encodeState(legacyState)))
        val numbered = testLevel.copy(ref = testLevel.ref.copy(levelNumber = 2, bankIndex = 1))
        val controller = PlayController.open(testModule, numbered, repository, FakeClock(10_000))
        assertEquals(legacyState.marks, controller.snapshot.value.state.marks)
        assertEquals(4_500L, controller.snapshot.value.elapsedTimeMillis)
        assertEquals(2, controller.snapshot.value.hintsUsed)
        controller.pauseForBackground()
        assertEquals(2L, repository.lastSaved().attemptNumber)

        val later = numbered.copy(ref = numbered.ref.copy(levelNumber = 5))
        val loop = PlayController.open(testModule, later, repository, FakeClock(20_000))
        assertEquals(List(2) { Mark.EMPTY }, loop.snapshot.value.state.marks)
        assertEquals(0, loop.snapshot.value.hintsUsed)
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
        private val saved = mutableMapOf<Triple<String, String, Long>, LevelProgress>()
        val writes = mutableListOf<LevelProgress>()

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> =
            flowOf(latest(gameId, levelId))

        private fun latest(gameId: String, levelId: String): LevelProgress? =
            saved.values.filter { it.gameId == gameId && it.levelId == levelId }.maxByOrNull { it.attemptNumber }

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? =
            latest(gameId, levelId)

        override suspend fun getAttemptProgress(gameId: String, levelId: String, attemptNumber: Long): LevelProgress? =
            saved[Triple(gameId, levelId, attemptNumber)]

        override suspend fun getBestTimeMillis(gameId: String, levelId: String): Long? =
            saved.values.filter { it.gameId == gameId && it.levelId == levelId }.mapNotNull { it.bestTimeMillis }.minOrNull()

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            val copy = progress.copy(stateBytes = progress.stateBytes.copyOf())
            saved[Triple(progress.gameId, progress.levelId, progress.attemptNumber)] = copy
            writes += copy
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            saved.keys.removeAll { it.first == gameId && it.second == levelId }
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
        val fourCellTestLevel = GameLevel(
            ref = LevelRef(TestEngine.id, "test-easy-004", Pack.EASY),
            size = 4,
            puzzle = TestPuzzle(List(4) { Mark.FILLED }),
        )
    }
}
