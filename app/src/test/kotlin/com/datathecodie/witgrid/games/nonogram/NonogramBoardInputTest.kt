package com.datathecodie.witgrid.games.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramRules
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NonogramBoardInputTest {
    @Test
    fun selectedModeSetsItsMarkAndRepeatedTapIsUnchanged() {
        val state = oneFilledCellState()
        val cell = Cell(1)

        val fill = NonogramBoardInput.markMoves(state, listOf(cell), NonogramMarkMode.FILL, false)
        assertEquals(listOf(Move.SetMark(cell, Mark.FILLED)), fill)
        val filledState = fill.fold(state, NonogramState::withMove)
        assertEquals(emptyList<Move>(), NonogramBoardInput.markMoves(filledState, listOf(cell), NonogramMarkMode.FILL, false))
        assertEquals(listOf(Move.SetMark(cell, Mark.CROSSED)), NonogramBoardInput.markMoves(filledState, listOf(cell), NonogramMarkMode.CROSS, false))
    }

    @Test
    fun dragMarksEachDistinctCellForOneGesture() {
        val state = oneFilledCellState()
        val path = listOf(Cell(1), Cell(2), Cell(1))

        val moves = NonogramBoardInput.markMoves(state, path, NonogramMarkMode.CROSS, false)

        assertEquals(listOf(Move.SetMark(Cell(1), Mark.CROSSED), Move.SetMark(Cell(2), Mark.CROSSED)), moves)
    }

    @Test
    fun longPressClearsOnlyMarkedCells() {
        val state = oneFilledCellState().withMove(Move.SetMark(Cell(1), Mark.FILLED))

        assertEquals(listOf(Move.SetMark(Cell(1), Mark.EMPTY)), NonogramBoardInput.clearMoves(state, Cell(1)))
        assertEquals(emptyList<Move>(), NonogramBoardInput.clearMoves(state, Cell(2)))
    }

    @Test
    fun autoCrossMarksRemainingCellsInCompletedRowsAndColumns() {
        val state = oneFilledCellState()

        val moves = NonogramBoardInput.markMoves(
            state,
            listOf(Cell(0)),
            NonogramMarkMode.FILL,
            autoCrossCompletedLines = true,
        )
        val finalState = moves.fold(state, NonogramState::withMove)

        assertEquals(Mark.FILLED, finalState.marks[0])
        assertEquals(List(25) { index -> if (index == 0) Mark.FILLED else Mark.CROSSED }, finalState.marks)
    }

    @Test
    fun autoCrossLeavesAClueLineIncompleteUntilItsRunsMatch() {
        val state = twoCellRunState()

        val moves = NonogramBoardInput.markMoves(
            state,
            listOf(Cell(0)),
            NonogramMarkMode.FILL,
            autoCrossCompletedLines = true,
        )
        val finalState = moves.fold(state, NonogramState::withMove)

        assertEquals(Mark.EMPTY, finalState.marks[1])
        assertEquals(Mark.FILLED, finalState.marks[0])
    }

    @Test
    fun inputRejectsCellsOutsideThePuzzle() {
        val state = oneFilledCellState()

        assertThrows(IllegalArgumentException::class.java) {
            NonogramBoardInput.markMoves(state, listOf(Cell(state.marks.size)), NonogramMarkMode.FILL, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NonogramBoardInput.clearMoves(state, Cell(state.marks.size))
        }
    }

    private fun oneFilledCellState(): NonogramState = stateWithFirstRow(listOf(1))

    private fun twoCellRunState(): NonogramState = stateWithFirstRow(listOf(0, 1))

    private fun stateWithFirstRow(filledColumns: List<Int>): NonogramState {
        val solution = List(25) { index ->
            if (index / BOARD_SIZE == 0 && index % BOARD_SIZE in filledColumns) 1 else 0
        }
        val (rowClues, columnClues) = NonogramRules.cluesForSolution(BOARD_SIZE, solution)
        return NonogramState.initial(
            NonogramPuzzle(
                id = "NO-TEST-1",
                size = BOARD_SIZE,
                rowClues = rowClues,
                columnClues = columnClues,
                solution = solution,
                pack = Pack.EASY,
                storedGrade = "fixture",
            ),
        )
    }

    private companion object {
        const val BOARD_SIZE = 5
    }
}
