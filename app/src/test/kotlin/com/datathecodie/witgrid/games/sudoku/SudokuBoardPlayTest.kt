package com.datathecodie.witgrid.games.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.sudoku.SudokuEngine
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SudokuBoardPlayTest {

    private fun handFixture9x9(): SudokuPuzzle {
        val solution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val givens = listOf(
            "53..7....",
            "6..195...",
            ".98....6.",
            "8...6...3",
            "4..8.3..1",
            "7...2...6",
            ".6....28.",
            "...419..5",
            "....8..79",
        ).flatMap { row -> row.map { if (it == '.') null else it.digitToInt() } }

        return SudokuPuzzle(
            id = "SU-9-HAND-FIXTURE",
            givens = givens,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "naked-single:1",
        )
    }

    @Test
    fun playsHandFixturePuzzleToSolvedThroughBoardInput() {
        val puzzle = handFixture9x9()
        var state = SudokuState.initial(puzzle)

        assertFalse(SudokuEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), SudokuEngine.mistakes(puzzle, state))

        // Play each cell from solution
        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.givens[index] == null) {
                val cell = Cell(index)
                val digit = puzzle.solution[index]
                val moves = SudokuBoardInput.digitMoves(state, cell, digit, autoNotesEnabled = true)
                assertEquals(1, moves.size)
                state = moves.fold(state, SudokuState::withMove)
            }
        }

        assertTrue(SudokuEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), SudokuEngine.mistakes(puzzle, state))
    }

    @Test
    fun mistakesAreIdentifiedAndCleared() {
        val puzzle = handFixture9x9()
        var state = SudokuState.initial(puzzle)

        // Find an empty cell (cell 2, solution is 4)
        val cell = Cell(2)
        assertEquals(null, puzzle.givens[cell.index])
        assertEquals(4, puzzle.solution[cell.index])

        // Enter wrong digit 9
        val moves = SudokuBoardInput.digitMoves(state, cell, 9, autoNotesEnabled = false)
        state = moves.fold(state, SudokuState::withMove)

        assertEquals(listOf(cell), SudokuEngine.mistakes(puzzle, state))

        // Clear the mistake
        val clearMoves = SudokuBoardInput.clearMoves(state, cell)
        state = clearMoves.fold(state, SudokuState::withMove)

        assertEquals(emptyList<Cell>(), SudokuEngine.mistakes(puzzle, state))
        assertEquals(null, state.digits[cell.index])
    }
}
