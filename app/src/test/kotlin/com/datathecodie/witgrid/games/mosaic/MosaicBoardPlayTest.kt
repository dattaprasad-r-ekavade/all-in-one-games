package com.datathecodie.witgrid.games.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.mosaic.MosaicCodec
import com.datathecodie.witgrid.engine.mosaic.MosaicEngine
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicBoardPlayTest {
    // 8x8 Easy technique-solvable hand fixture from MosaicTechniqueSolverTest
    private val easy8x8Line =
        "MO-EASY-1|8|0224455313355553243444423434443234455654445566545564556544423343|0010111100101101100100001010011010011010010110111101011011010011|EASY|clue-overlap:42"

    @Test
    fun playsHandFixturePuzzleToSolvedThroughBoardInput() {
        val puzzle = MosaicCodec.decode(easy8x8Line)
        var state = MosaicState.initial(puzzle)

        assertFalse(MosaicEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), MosaicEngine.mistakes(puzzle, state))

        // Play each cell from solution using appropriate mark mode
        for (index in 0 until puzzle.grid.cellCount) {
            val cell = Cell(index)
            val mode = if (puzzle.solution[index] == 1) MosaicMarkMode.FILL else MosaicMarkMode.CROSS
            val moves = MosaicBoardInput.markMoves(state, listOf(cell), mode)
            assertEquals(1, moves.size)
            state = moves.fold(state, MosaicState::withMove)
        }

        assertTrue(MosaicEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), MosaicEngine.mistakes(puzzle, state))

        // In the solved state, every clue must be satisfied
        for (index in 0 until puzzle.grid.cellCount) {
            val clue = puzzle.clues[index] ?: continue
            val status = MosaicBoardInput.clueStatus(puzzle.grid, clue, Cell(index), state.marks)
            assertEquals(MosaicClueStatus.SATISFIED, status)
        }
    }

    @Test
    fun playsGeneratedEasyPuzzleToSolvedThroughBoardInput() {
        val rng = SplitMix64(42L)
        val puzzle = MosaicEngine.generate(rng, Pack.EASY)
        var state = MosaicState.initial(puzzle)

        assertFalse(MosaicEngine.isSolved(puzzle, state))

        for (index in 0 until puzzle.grid.cellCount) {
            val cell = Cell(index)
            val mode = if (puzzle.solution[index] == 1) MosaicMarkMode.FILL else MosaicMarkMode.CROSS
            val moves = MosaicBoardInput.markMoves(state, listOf(cell), mode)
            state = moves.fold(state, MosaicState::withMove)
        }

        assertTrue(MosaicEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), MosaicEngine.mistakes(puzzle, state))
    }

    @Test
    fun mistakesAreIdentifiedAndCleared() {
        val puzzle = MosaicCodec.decode(easy8x8Line)
        var state = MosaicState.initial(puzzle)

        // Cell 0 in easy8x8Line has solution bit 0. Mark it as FILLED (a mistake).
        val wrongMove = MosaicBoardInput.markMoves(state, listOf(Cell(0)), MosaicMarkMode.FILL)
        state = wrongMove.fold(state, MosaicState::withMove)

        assertEquals(listOf(Cell(0)), MosaicEngine.mistakes(puzzle, state))

        // Clue at cell 0 is 0. With cell 0 marked filled, clue 0 must be in ERROR state.
        val status = MosaicBoardInput.clueStatus(puzzle.grid, 0, Cell(0), state.marks)
        assertEquals(MosaicClueStatus.ERROR, status)

        // Clear the mistake using clearMoves
        val clearMoves = MosaicBoardInput.clearMoves(state, Cell(0))
        state = clearMoves.fold(state, MosaicState::withMove)

        assertEquals(emptyList<Cell>(), MosaicEngine.mistakes(puzzle, state))
        assertEquals(Mark.EMPTY, state.marks[0])
    }

    @Test
    fun gameModuleContractIsConsistent() {
        assertEquals("mosaic", MosaicGameModule.engine.id)
        assertEquals("mosaic", MosaicGameModule.id)
        val puzzle = MosaicCodec.decode(easy8x8Line)
        val levelRef = LevelRef("mosaic", puzzle.id, Pack.EASY, levelNumber = 1L)
        val gameLevel = GameLevel(levelRef, puzzle.size, puzzle)
        assertEquals(2, MosaicGameModule.levelsPerInterstitial(gameLevel))
    }
}
