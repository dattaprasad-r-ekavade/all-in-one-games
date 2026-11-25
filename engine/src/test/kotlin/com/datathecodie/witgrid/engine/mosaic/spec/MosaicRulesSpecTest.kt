package com.datathecodie.witgrid.engine.mosaic.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicRules
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicRulesSpecTest {
    @Test
    fun clueCountsTheCellAndItsNeighboursAtCornersEdgesAndInterior() {
        val grid = Grid(4)
        val solution = listOf(
            1, 0, 1, 0,
            0, 1, 0, 1,
            1, 0, 1, 0,
            0, 1, 0, 1,
        )

        assertEquals(2, MosaicRules.clueAt(grid, solution, Cell(0))) // corner: 4 cells
        assertEquals(3, MosaicRules.clueAt(grid, solution, Cell(1))) // top edge: 6 cells
        assertEquals(5, MosaicRules.clueAt(grid, solution, Cell(5))) // interior: 9 cells
        assertEquals(listOf(Cell(0), Cell(1), Cell(4), Cell(5)), MosaicRules.areaCells(grid, Cell(0)))
    }

    @Test
    fun solvedRequiresEveryCellToBeMarkedAsItsSolutionValue() {
        val puzzle = MosaicSpecFixtures.allEmptySmallPuzzle()
        val state = MosaicState.initial(puzzle)
        assertFalse(MosaicRules.isSolved(puzzle, state))

        val solved = puzzle.solution.indices.fold(state) { current, index ->
            current.withMove(Move.SetMark(Cell(index), Mark.CROSSED))
        }
        assertTrue(MosaicRules.isSolved(puzzle, solved))
        assertFalse(MosaicRules.isSolved(puzzle, solved.withMove(Move.SetMark(Cell(0), Mark.FILLED))))
    }

    @Test
    fun conflictingFillAndCrossMarksAreReportedAsMistakes() {
        val puzzle = MosaicSpecFixtures.allEmptySmallPuzzle()
        val state = MosaicState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.FILLED))
            .withMove(Move.SetMark(Cell(2), Mark.CROSSED))

        assertEquals(listOf(Cell(0)), MosaicRules.mistakes(puzzle, state))
    }
}
