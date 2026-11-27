package com.datathecodie.witgrid.games.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrownsBoardInputTest {
    @Test
    fun tapCyclesFromXToCrownToClear() {
        val state = testState()
        val cell = Cell(5)

        assertEquals(Mark.CROSSED, CrownsBoardInput.nextMark(Mark.EMPTY))
        assertEquals(Mark.FILLED, CrownsBoardInput.nextMark(Mark.CROSSED))
        assertEquals(Mark.EMPTY, CrownsBoardInput.nextMark(Mark.FILLED))
        assertEquals(listOf(Move.SetMark(cell, Mark.CROSSED)), CrownsBoardInput.tapMoves(state, cell, false))
    }

    @Test
    fun autoXAddsEmptyRowColumnRegionAndNeighbourCellsToTheSameGesture() {
        val state = testState()
        val crownCell = Cell(16)
        val moves = CrownsBoardInput.tapMoves(state, crownCell, autoXEnabled = true)

        assertEquals(Move.SetMark(crownCell, Mark.CROSSED), moves.first())
        val crownState = state.withMove(moves.first())
        val crownMoves = CrownsBoardInput.tapMoves(crownState, crownCell, autoXEnabled = true)
        assertEquals(Move.SetMark(crownCell, Mark.FILLED), crownMoves.first())
        val grid = crownState.puzzle.grid
        val row = grid.row(crownCell)
        val column = grid.column(crownCell)
        val region = crownState.puzzle.regions[crownCell.index]
        val expected = crownState.marks.indices.mapNotNull { index ->
            val candidate = Cell(index)
            val sameLine = grid.row(candidate) == row || grid.column(candidate) == column
            val sameRegion = crownState.puzzle.regions[index] == region
            if (crownState.marks[index] == Mark.EMPTY &&
                (sameLine || sameRegion || candidate in grid.neighbours(crownCell))
            ) {
                Move.SetMark(candidate, Mark.CROSSED)
            } else {
                null
            }
        }
        assertEquals(expected, crownMoves.drop(1))
        assertTrue(crownMoves.any { it == Move.SetMark(Cell(12), Mark.CROSSED) }) // Same row, not adjacent.
        assertTrue(crownMoves.any { it == Move.SetMark(Cell(4), Mark.CROSSED) }) // Same column, not adjacent.
        assertTrue(crownMoves.any { it == Move.SetMark(Cell(6), Mark.CROSSED) }) // Same region, not adjacent.
    }

    @Test
    fun autoXKeepsExistingMarksAndCrowns() {
        val cell = Cell(0)
        val state = testState()
            .withMove(Move.SetMark(cell, Mark.CROSSED))
            .withMove(Move.SetMark(Cell(1), Mark.FILLED))
            .withMove(Move.SetMark(Cell(6), Mark.CROSSED))

        val moves = CrownsBoardInput.tapMoves(state, cell, autoXEnabled = true)
        assertEquals(Move.SetMark(cell, Mark.FILLED), moves.first())
        assertTrue(moves.none { it == Move.SetMark(Cell(1), Mark.CROSSED) })
        assertTrue(moves.none { it == Move.SetMark(Cell(6), Mark.CROSSED) })
        assertTrue(moves.contains(Move.SetMark(Cell(7), Mark.CROSSED)))
    }

    @Test
    fun dragSetsOnlyEmptyCellsAndRemovesRepeatedCells() {
        val state = testState()
            .withMove(Move.SetMark(Cell(3), Mark.FILLED))
            .withMove(Move.SetMark(Cell(4), Mark.CROSSED))

        assertEquals(
            listOf(Move.SetMark(Cell(2), Mark.CROSSED)),
            CrownsBoardInput.dragMoves(state, listOf(Cell(2), Cell(3), Cell(4), Cell(2))),
        )
    }

    @Test
    fun longPressClearsOnlyMarkedCells() {
        val state = testState().withMove(Move.SetMark(Cell(3), Mark.CROSSED))

        assertEquals(listOf(Move.SetMark(Cell(3), Mark.EMPTY)), CrownsBoardInput.clearMoves(state, Cell(3)))
        assertEquals(emptyList<Move>(), CrownsBoardInput.clearMoves(state, Cell(4)))
    }

    private fun testState(): CrownsState = CrownsState.initial(testPuzzle())

    private fun testPuzzle() = CrownsPuzzle(
        id = "CR-TEST-1",
        size = 6,
        regions = (0 until 6).flatMap { row -> (0 until 6).map { column ->
            if (row == 1 && column == 0) 2 else row
        } },
        solutionColumns = listOf(0, 2, 4, 1, 3, 5),
        pack = Pack.EASY,
        storedGrade = "none:0",
    )
}
