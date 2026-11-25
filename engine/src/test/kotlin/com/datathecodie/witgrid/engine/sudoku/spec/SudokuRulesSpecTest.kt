package com.datathecodie.witgrid.engine.sudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuRules
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Sudoku 9x9 plan-rule cases, using the hand-made classic board. */
class SudokuRulesSpecTest {
    @Test
    fun solutionUsesEveryDigitOnceInEachRowColumnAndThreeByThreeBox() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val solution = SudokuSpecFixtures.solution
        // Index 1 is row 1 column 2. Copying the first digit into it repeats a digit in row 1.
        val repeatedInRow = solution.toMutableList().also { it[1] = it[0] }
        // Index 9 is row 2 column 1. Copying the first digit into it repeats a digit in column 1.
        val repeatedInColumn = solution.toMutableList().also { it[9] = it[0] }
        // Index 20 is row 3 column 3, inside the top-left box. Copying the first digit repeats it there.
        val repeatedInBox = solution.toMutableList().also { it[20] = it[0] }

        assertTrue(SudokuRules.isValidSolution(puzzle))
        assertFalse(SudokuRules.isValidSolution(puzzle, repeatedInRow))
        assertFalse(SudokuRules.isValidSolution(puzzle, repeatedInColumn))
        assertFalse(SudokuRules.isValidSolution(puzzle, repeatedInBox))
        assertFalse(SudokuRules.isValidSolution(puzzle, solution.dropLast(1)))
        assertFalse(SudokuRules.isValidSolution(puzzle, solution.toMutableList().also { it[0] = 0 }))
        assertFalse(SudokuRules.isValidSolution(puzzle, solution.toMutableList().also { it[0] = 10 }))
    }

    @Test
    fun solutionCounterFindsZeroOneAndMultipleBoardsAndHonorsItsCap() {
        val impossible = SudokuSpecFixtures.puzzle(
            cellGivens = List(SudokuSpecFixtures.CELL_COUNT) { index -> if (index in 0..1) 5 else null },
        )
        val unique = SudokuSpecFixtures.puzzle(cellGivens = SudokuSpecFixtures.solution)
        val multiple = SudokuSpecFixtures.puzzle(cellGivens = List(SudokuSpecFixtures.CELL_COUNT) { null })

        assertEquals(0, SudokuRules.countSolutions(impossible, limit = 2))
        assertEquals(1, SudokuRules.countSolutions(unique, limit = 2))
        assertEquals(2, SudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(1, SudokuRules.countSolutions(multiple, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            SudokuRules.countSolutions(multiple, limit = 0)
        }
    }

    @Test
    fun mistakesReportWrongEnteredDigitsOnlyAndSolvedRequiresEveryStoredDigit() {
        val givens = List(SudokuSpecFixtures.CELL_COUNT) { index ->
            if (index == 0) SudokuSpecFixtures.solution[0] else null
        }
        val puzzle = SudokuSpecFixtures.puzzle(cellGivens = givens)
        val initial = SudokuState.initial(puzzle)
        val noted = initial.withMove(Move.SetNotes(Cell(1), 1 shl 2))
        val wrong = noted.withMove(Move.SetDigit(Cell(1), 1))

        assertTrue(SudokuRules.mistakes(puzzle, initial).isEmpty())
        assertTrue(SudokuRules.mistakes(puzzle, noted).isEmpty())
        assertEquals(listOf(Cell(1)), SudokuRules.mistakes(puzzle, wrong))
        assertFalse(SudokuRules.isSolved(puzzle, initial))
        assertFalse(SudokuRules.isSolved(puzzle, wrong))

        val solved = fillWithSolution(initial, puzzle)
        val incomplete = solved.withMove(Move.SetDigit(Cell(2), 0))
        val incorrect = solved.withMove(Move.SetDigit(Cell(2), 1))

        assertTrue(SudokuRules.mistakes(puzzle, solved).isEmpty())
        assertTrue(SudokuRules.isSolved(puzzle, solved))
        assertFalse(SudokuRules.isSolved(puzzle, incomplete))
        assertFalse(SudokuRules.isSolved(puzzle, incorrect))
    }

    @Test
    fun solvedAndMistakeChecksRejectAStateFromAnotherPuzzle() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val otherPuzzle = SudokuSpecFixtures.puzzle(id = "SU-SPEC-OTHER")
        val state = SudokuState.initial(otherPuzzle)

        assertThrows(IllegalArgumentException::class.java) { SudokuRules.isSolved(puzzle, state) }
        assertThrows(IllegalArgumentException::class.java) { SudokuRules.mistakes(puzzle, state) }
    }

    private fun fillWithSolution(start: SudokuState, puzzle: SudokuPuzzle): SudokuState =
        puzzle.solution.indices
            .filter { index -> puzzle.givens[index] == null }
            .fold(start) { state, index ->
                state.withMove(Move.SetDigit(Cell(index), puzzle.solution[index]))
            }
}
