package com.datathecodie.witgrid.games.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuEngine
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuRules
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillerSudokuBoardPlayTest {

    @Test
    fun playsHandFixturePuzzleToSolvedThroughBoardInput() {
        val puzzle = KillerSudokuBoardFixtures.puzzle(
            id = "KS-9-HAND-FIXTURE",
            givens = KillerSudokuBoardFixtures.handGivens,
            storedGrade = "naked-single:1",
        )
        var state = KillerSudokuState.initial(puzzle)

        assertTrue(KillerSudokuRules.isValidSolution(puzzle))
        assertEquals(81, puzzle.cages.sumOf { it.cells.size })
        assertTrue(puzzle.cages.all { cage -> cage.cells.first().index == cage.cells.minOf { it.index } })
        assertFalse(KillerSudokuEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), KillerSudokuEngine.mistakes(puzzle, state))

        for (index in 0 until puzzle.grid.cellCount) {
            if (puzzle.givens[index] == null) {
                val cell = Cell(index)
                val digit = puzzle.solution[index]
                val moves = KillerSudokuBoardInput.digitMoves(state, cell, digit, autoNotesEnabled = true)
                assertEquals(1, moves.size)
                state = moves.fold(state, KillerSudokuState::withMove)
            }
        }

        assertTrue(KillerSudokuEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), KillerSudokuEngine.mistakes(puzzle, state))
    }

    @Test
    fun mistakesAreIdentifiedAndCleared() {
        val puzzle = KillerSudokuBoardFixtures.puzzle(
            id = "KS-9-HAND-FIXTURE",
            givens = KillerSudokuBoardFixtures.handGivens,
            storedGrade = "naked-single:1",
        )
        var state = KillerSudokuState.initial(puzzle)

        val cell = Cell(2)
        assertEquals(null, puzzle.givens[cell.index])
        assertEquals(4, puzzle.solution[cell.index])

        val moves = KillerSudokuBoardInput.digitMoves(state, cell, 9, autoNotesEnabled = false)
        state = moves.fold(state, KillerSudokuState::withMove)

        assertEquals(listOf(cell), KillerSudokuEngine.mistakes(puzzle, state))

        val clearMoves = KillerSudokuBoardInput.clearMoves(state, cell)
        state = clearMoves.fold(state, KillerSudokuState::withMove)

        assertEquals(emptyList<Cell>(), KillerSudokuEngine.mistakes(puzzle, state))
        assertEquals(null, state.digits[cell.index])
    }
}
