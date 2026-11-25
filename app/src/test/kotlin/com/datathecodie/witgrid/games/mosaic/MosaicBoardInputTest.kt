package com.datathecodie.witgrid.games.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.mosaic.MosaicPuzzle
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Collections

class MosaicBoardInputTest {
    private val boardSize = 8

    private fun sampleState(): MosaicState {
        val solution = List(64) { if (it % 2 == 0) 1 else 0 }
        val puzzle = MosaicPuzzle(
            id = "MO-INPUT-TEST",
            size = boardSize,
            clues = Collections.nCopies(64, null),
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "fixture",
        )
        return MosaicState.initial(puzzle)
    }

    @Test
    fun selectedModeSetsItsMarkAndRepeatedTapIsUnchanged() {
        val state = sampleState()
        val cell = Cell(1)

        val fillMoves = MosaicBoardInput.markMoves(state, listOf(cell), MosaicMarkMode.FILL)
        assertEquals(listOf(Move.SetMark(cell, Mark.FILLED)), fillMoves)

        val filledState = fillMoves.fold(state, MosaicState::withMove)
        assertEquals(
            emptyList<Move>(),
            MosaicBoardInput.markMoves(filledState, listOf(cell), MosaicMarkMode.FILL),
        )

        val crossMoves = MosaicBoardInput.markMoves(filledState, listOf(cell), MosaicMarkMode.CROSS)
        assertEquals(listOf(Move.SetMark(cell, Mark.CROSSED)), crossMoves)

        val crossedState = crossMoves.fold(filledState, MosaicState::withMove)
        assertEquals(
            emptyList<Move>(),
            MosaicBoardInput.markMoves(crossedState, listOf(cell), MosaicMarkMode.CROSS),
        )
    }

    @Test
    fun dragMarksEachDistinctCellForOneGesture() {
        val state = sampleState()
        val path = listOf(Cell(1), Cell(2), Cell(1), Cell(3))

        val moves = MosaicBoardInput.markMoves(state, path, MosaicMarkMode.FILL)
        assertEquals(
            listOf(
                Move.SetMark(Cell(1), Mark.FILLED),
                Move.SetMark(Cell(2), Mark.FILLED),
                Move.SetMark(Cell(3), Mark.FILLED),
            ),
            moves,
        )
    }

    @Test
    fun longPressClearsOnlyMarkedCells() {
        var state = sampleState()
        state = state.withMove(Move.SetMark(Cell(1), Mark.FILLED))
        state = state.withMove(Move.SetMark(Cell(2), Mark.CROSSED))

        assertEquals(
            listOf(Move.SetMark(Cell(1), Mark.EMPTY)),
            MosaicBoardInput.clearMoves(state, Cell(1)),
        )
        assertEquals(
            listOf(Move.SetMark(Cell(2), Mark.EMPTY)),
            MosaicBoardInput.clearMoves(state, Cell(2)),
        )
        assertEquals(
            emptyList<Move>(),
            MosaicBoardInput.clearMoves(state, Cell(3)),
        )
    }

    @Test
    fun inputRejectsCellsOutsideThePuzzle() {
        val state = sampleState()
        val invalidCell = Cell(64)

        assertThrows(IllegalArgumentException::class.java) {
            MosaicBoardInput.markMoves(state, listOf(invalidCell), MosaicMarkMode.FILL)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicBoardInput.clearMoves(state, invalidCell)
        }
    }

    @Test
    fun clueStatusReflectsSatisfiedErrorAndNormalStates() {
        val grid = Grid(3)
        // Center cell (1, 1) has index 4; its area includes all 9 cells of the 3x3 grid.
        val center = Cell(4)

        // Case 1: Clue is 0, empty board -> NORMAL
        val allEmpty = List(9) { Mark.EMPTY }
        assertEquals(
            MosaicClueStatus.NORMAL,
            MosaicBoardInput.clueStatus(grid, clue = 0, cell = center, marks = allEmpty),
        )

        // One fill in area with clue 0 -> ERROR (too many fills)
        val oneFill = List(9) { if (it == 0) Mark.FILLED else Mark.EMPTY }
        assertEquals(
            MosaicClueStatus.ERROR,
            MosaicBoardInput.clueStatus(grid, clue = 0, cell = center, marks = oneFill),
        )

        // All 9 cells crossed with clue 0 -> SATISFIED (emptyCount == 0 && filledCount == 0)
        val allCrossed = List(9) { Mark.CROSSED }
        assertEquals(
            MosaicClueStatus.SATISFIED,
            MosaicBoardInput.clueStatus(grid, clue = 0, cell = center, marks = allCrossed),
        )

        // Case 2: Clue is 4
        // 5 fills -> ERROR (too many fills: 5 > 4)
        val fiveFills = List(9) { if (it < 5) Mark.FILLED else Mark.EMPTY }
        assertEquals(
            MosaicClueStatus.ERROR,
            MosaicBoardInput.clueStatus(grid, clue = 4, cell = center, marks = fiveFills),
        )

        // 6 crosses, 0 fills, 3 empties -> remaining max fills = 3 < 4 -> ERROR (too many crosses)
        val sixCrosses = List(9) { if (it < 6) Mark.CROSSED else Mark.EMPTY }
        assertEquals(
            MosaicClueStatus.ERROR,
            MosaicBoardInput.clueStatus(grid, clue = 4, cell = center, marks = sixCrosses),
        )

        // 4 fills and 5 crosses -> SATISFIED (whole area marked and count matches)
        val satisfiedFour = List(9) { if (it < 4) Mark.FILLED else Mark.CROSSED }
        assertEquals(
            MosaicClueStatus.SATISFIED,
            MosaicBoardInput.clueStatus(grid, clue = 4, cell = center, marks = satisfiedFour),
        )

        // 4 fills and 2 crosses and 3 empties -> NORMAL (not all 3x3 area is marked yet)
        val partialFour = List(9) {
            when {
                it < 4 -> Mark.FILLED
                it < 6 -> Mark.CROSSED
                else -> Mark.EMPTY
            }
        }
        assertEquals(
            MosaicClueStatus.NORMAL,
            MosaicBoardInput.clueStatus(grid, clue = 4, cell = center, marks = partialFour),
        )

        // Case 3: Corner cell (0, 0) has area of size 4 (index 0, 1, 3, 4)
        val corner = Cell(0)
        // Corner with clue 2: 3 fills in its area -> ERROR
        val cornerThreeFills = List(9) { if (it in listOf(0, 1, 3)) Mark.FILLED else Mark.EMPTY }
        assertEquals(
            MosaicClueStatus.ERROR,
            MosaicBoardInput.clueStatus(grid, clue = 2, cell = corner, marks = cornerThreeFills),
        )

        // Corner with clue 2: cells 0, 1 filled and 3, 4 crossed -> SATISFIED
        val cornerSatisfied = List(9) {
            when (it) {
                0, 1 -> Mark.FILLED
                3, 4 -> Mark.CROSSED
                else -> Mark.EMPTY
            }
        }
        assertEquals(
            MosaicClueStatus.SATISFIED,
            MosaicBoardInput.clueStatus(grid, clue = 2, cell = corner, marks = cornerSatisfied),
        )
    }
}
