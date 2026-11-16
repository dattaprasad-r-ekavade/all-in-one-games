package com.datathecodie.witgrid.games.crowns

import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.ProgressRepository
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.play.MonotonicClock
import com.datathecodie.witgrid.play.PlayController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrownsGestureHistoryTest {
    @Test
    fun oneUndoRevertsCrownAndAllAutoXMarksAndRedoRestoresThem() = runBlocking {
        val puzzle = CrownsPuzzle(
            id = "CR-TEST-1",
            size = 6,
            regions = (0 until 6).flatMap { region -> List(6) { region } },
            solutionColumns = listOf(0, 2, 4, 1, 3, 5),
            pack = Pack.EASY,
            storedGrade = "none:0",
        )
        val repository = InMemoryProgressRepository()
        val level = GameLevel(LevelRef("crowns", puzzle.id, Pack.EASY), puzzle.size, puzzle)
        val controller = PlayController.open(
            CrownsGameModule,
            level,
            repository,
            MonotonicClock { 0L },
        )
        val crownCell = Cell(8)
        controller.applyGesture(CrownsBoardInput.tapMoves(controller.snapshot.value.state, crownCell, true))
        val beforeCrown = controller.snapshot.value.state
        assertEquals(Mark.CROSSED, beforeCrown.marks[crownCell.index])

        controller.applyGesture(CrownsBoardInput.tapMoves(beforeCrown, crownCell, true))
        val afterCrown = controller.snapshot.value.state
        assertEquals(Mark.FILLED, afterCrown.marks[crownCell.index])
        assertTrue(puzzle.grid.neighbours(crownCell).all { afterCrown.marks[it.index] == Mark.CROSSED })

        assertTrue(controller.undo())
        assertEquals(beforeCrown, controller.snapshot.value.state)
        assertEquals(beforeCrown, CrownsGameModule.decodeState(requireNotNull(repository.saved).stateBytes))
        assertTrue(controller.redo())
        assertEquals(afterCrown, controller.snapshot.value.state)

        assertTrue(controller.undo())
        assertTrue(controller.undo())
        assertTrue(controller.snapshot.value.state.marks.all { it == Mark.EMPTY })
        assertFalse(controller.snapshot.value.canUndo)
    }

    private class InMemoryProgressRepository : ProgressRepository {
        var saved: LevelProgress? = null

        override fun observeLevelProgress(gameId: String, levelId: String): Flow<LevelProgress?> = flowOf(null)

        override suspend fun getLevelProgress(gameId: String, levelId: String): LevelProgress? = null

        override suspend fun saveLevelProgress(progress: LevelProgress) {
            saved = progress
        }

        override suspend fun deleteLevelProgress(gameId: String, levelId: String) {
            saved = null
        }
    }
}
