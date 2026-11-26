package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsCodec
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.play.MonotonicClock
import com.datathecodie.witgrid.play.PlayActionController
import com.datathecodie.witgrid.ui.screens.game.GameViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class ArrowsPlayHostTest {
    @Test
    fun slideUndoRedoMistakeAndReviveContinueWorkThroughActionController() = runBlocking {
        val bank = fakeBank()
        val ref = ArrowsGameModule.levelRefs(Pack.EASY, bank)[1]
        val level = ArrowsGameModule.loadLevel(ref, bank)
        val repository = MemoryProgressRepository()
        val clock = FakeClock(1_000L)
        val controller = PlayActionController.open(ArrowsGameModule, level, repository, clock)

        // Slide free arrow at cell 2, then undo and redo it.
        assertTrue(controller.applyAction(ArrowsAction(Cell(2))))
        assertEquals(1, controller.snapshot.value.state.slideCount)
        assertTrue(controller.snapshot.value.canUndo)
        assertTrue(controller.undo())
        assertEquals(0, controller.snapshot.value.state.slideCount)
        assertTrue(controller.snapshot.value.canRedo)
        assertTrue(controller.redo())
        assertEquals(1, controller.snapshot.value.state.slideCount)

        // Three blocked taps on cell 12 (blocked by cell 14) fail the level, and mistakes cannot be undone.
        assertTrue(controller.applyAction(ArrowsAction(Cell(12))))
        assertFalse(controller.snapshot.value.canUndo)
        assertEquals(1, controller.snapshot.value.state.mistakes)
        assertTrue(controller.applyAction(ArrowsAction(Cell(12))))
        assertTrue(controller.applyAction(ArrowsAction(Cell(12))))
        assertTrue(controller.snapshot.value.failed)
        assertEquals(3, controller.snapshot.value.state.mistakes)

        // Rollback restores lost state; Continue revives with one extra life (mistakes 3 -> 2).
        assertTrue(controller.continueAfterFailure("tx-rollback"))
        assertFalse(controller.snapshot.value.failed)
        assertEquals(2, controller.snapshot.value.state.mistakes)
        assertTrue(controller.rollbackContinueTransaction("tx-rollback"))
        assertTrue(controller.snapshot.value.failed)
        assertEquals(3, controller.snapshot.value.state.mistakes)

        assertTrue(controller.continueAfterFailure("tx-revive"))
        assertTrue(controller.completeContinueTransaction("tx-revive"))
        assertFalse(controller.snapshot.value.failed)
        assertEquals(2, controller.snapshot.value.state.mistakes)
        assertEquals(1, ArrowsBoardInput.livesLeft(controller.snapshot.value.state))

        // Finish remaining arrows via hints.
        while (!controller.snapshot.value.solved) {
            val hint = controller.hintAction()
            assertNotNull(hint)
            assertTrue(controller.applyHintAction(hint!!))
        }
        assertTrue(controller.snapshot.value.solved)
        assertEquals(level.ref, controller.solvedEvents.first().levelRef)
    }

    @Test
    fun gameViewModelLoadsFamilyBPackSizesAndContinueState() = runBlocking {
        val bank = fakeBank()
        val easyRefs = ArrowsGameModule.levelRefs(Pack.EASY, bank)
        val regularRef = easyRefs[1]
        val level = ArrowsGameModule.loadLevel(regularRef, bank)
        val inProgressState = ArrowsGameModule.applyAction(
            ArrowsGameModule.newState(level),
            ArrowsAction(Cell(2)),
        )
        val repository = MemoryProgressRepository().apply {
            saveLevelProgress(
                LevelProgress(
                    gameId = "arrows",
                    levelId = regularRef.levelId,
                    stateBytes = ArrowsGameModule.encodeState(inProgressState),
                    elapsedTimeMillis = 12_000L,
                    hintsUsed = 0,
                    completed = false,
                    bestTimeMillis = null,
                    attemptNumber = requireNotNull(regularRef.levelNumber),
                ),
            )
            saveLevelProgress(
                LevelProgress(
                    gameId = "arrows",
                    levelId = easyRefs.first().levelId,
                    stateBytes = byteArrayOf(),
                    elapsedTimeMillis = 5_000L,
                    hintsUsed = 0,
                    completed = true,
                    bestTimeMillis = 5_000L,
                    attemptNumber = 0L,
                ),
            )
        }
        val viewModel = GameViewModel(
            gameId = "arrows",
            levelRefsByPack = mapOf(Pack.EASY to easyRefs),
            progressRepository = repository,
            bankSource = bank,
        )

        viewModel.refresh()

        val uiState = viewModel.state.value
        assertEquals(5..5, uiState.packs.first().boardSizes)
        assertEquals(regularRef, uiState.continueLevelRef)
        assertEquals(inProgressState, uiState.continueState)
    }

    private fun fakeBank(): BankSource {
        val line = ArrowsCodec.encodePuzzle(ArrowsBoardFixtures.easyPuzzle)
        val introLine = ArrowsCodec.encodePuzzle(
            ArrowsBoardFixtures.easyPuzzle.copy(
                bankId = "AR-INTRO-01",
                initialArrows = ArrowsCodec.decodeLayout(".^....^.>><.>^>....v....v", 5),
            ),
        )
        val files = mapOf(
            "bank/arrows-intro.txt" to "$introLine\n",
            "bank/arrows-easy.txt.gz" to "$line\n",
        )
        return object : BankSource {
            override fun open(path: String): InputStream {
                val text = files[path] ?: throw FileNotFoundException(path)
                return ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private class FakeClock(var currentMillis: Long) : MonotonicClock {
        override fun nowMillis(): Long = currentMillis
    }

    private class MemoryProgressRepository : ProgressRepository, GameProgressSummaryRepository {
        private val records = mutableListOf<LevelProgress>()

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> =
            flowOf(records.lastOrNull { it.gameId == gameId && it.levelId == levelId })

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? =
            records.lastOrNull { it.gameId == gameId && it.levelId == levelId }

        override suspend fun getAttemptProgress(gameId: String, levelId: String, attemptNumber: Long): LevelProgress? =
            records.lastOrNull {
                it.gameId == gameId && it.levelId == levelId && it.attemptNumber == attemptNumber
            }

        override suspend fun getProgressForGame(gameId: String): List<LevelProgress> =
            records.filter { it.gameId == gameId }

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            records.removeAll {
                it.gameId == progress.gameId &&
                    it.levelId == progress.levelId &&
                    it.attemptNumber == progress.attemptNumber
            }
            records.add(progress)
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            records.removeAll { it.gameId == gameId && it.levelId == levelId }
        }
    }
}
