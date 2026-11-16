package com.datathecodie.witgrid.engine.minisudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuPuzzle
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuRules
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Mini Sudoku plan-rule cases, using hand-built grids and clues. */
class MiniSudokuRulesSpecTest {
    @Test
    fun solutionUsesEveryDigitOnceInEachRowColumnAndTwoByThreeBox() {
        val puzzle = puzzle()
        val repeatedRowDigit = solution.toMutableList().also { it[1] = it[0] }
        val repeatedColumnDigit = solution.toMutableList().also { it[6] = it[0] }
        val repeatedBoxDigit = solution.toMutableList().also { values ->
            for (row in 0 until BOARD_SIZE) {
                val first = row * BOARD_SIZE
                val fifth = first + 4
                val value = values[first]
                values[first] = values[fifth]
                values[fifth] = value
            }
        }

        assertTrue(MiniSudokuRules.isValidSolution(puzzle))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, repeatedRowDigit))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, repeatedColumnDigit))
        assertTrue(rowsAreValid(repeatedBoxDigit))
        assertTrue(columnsAreValid(repeatedBoxDigit))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, repeatedBoxDigit))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, solution.dropLast(1)))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, solution.toMutableList().also { it[0] = 7 }))
    }

    @Test
    fun solutionCounterFindsZeroOneAndMultipleHandBuiltBoardsAndHonorsItsCap() {
        val impossible = puzzle(givens = List(CELL_COUNT) { index -> if (index in 0..1) 1 else null })
        val unique = puzzle(givens = solution)
        val multiple = puzzle()

        assertEquals(0, MiniSudokuRules.countSolutions(impossible, limit = 2))
        assertEquals(1, MiniSudokuRules.countSolutions(unique, limit = 2))
        assertEquals(2, MiniSudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(1, MiniSudokuRules.countSolutions(multiple, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            MiniSudokuRules.countSolutions(multiple, limit = 0)
        }
    }

    @Test
    fun mistakesReportWrongEnteredDigitsAndSolvedRequiresEveryStoredDigit() {
        val givens = List(CELL_COUNT) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens)
        val initial = MiniSudokuState.initial(puzzle)
        val noted = initial.withMove(Move.SetNotes(Cell(1), 1 shl 0))
        val wrong = noted.withMove(Move.SetDigit(Cell(2), solution[1]))

        assertTrue(MiniSudokuRules.mistakes(puzzle, initial).isEmpty())
        assertTrue(MiniSudokuRules.mistakes(puzzle, noted).isEmpty())
        assertEquals(listOf(Cell(2)), MiniSudokuRules.mistakes(puzzle, wrong))
        assertFalse(MiniSudokuRules.isSolved(puzzle, initial))
        assertFalse(MiniSudokuRules.isSolved(puzzle, wrong))

        val solved = solution.indices.filter { givens[it] == null }.fold(initial) { state, index ->
            state.withMove(Move.SetDigit(Cell(index), solution[index]))
        }
        val incomplete = solved.withMove(Move.SetDigit(Cell(1), 0))
        val incorrect = solved.withMove(Move.SetDigit(Cell(1), solution[0]))

        assertTrue(MiniSudokuRules.isSolved(puzzle, solved))
        assertFalse(MiniSudokuRules.isSolved(puzzle, incomplete))
        assertFalse(MiniSudokuRules.isSolved(puzzle, incorrect))
    }

    private fun puzzle(givens: List<Int?> = List(CELL_COUNT) { null }): MiniSudokuPuzzle =
        MiniSudokuPuzzle(
            id = "MS-SPEC-FIXTURE",
            givens = givens,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "pending",
        )

    private fun rowsAreValid(values: List<Int>): Boolean =
        (0 until BOARD_SIZE).all { row ->
            (0 until BOARD_SIZE).map { column -> values[row * BOARD_SIZE + column] }.toSet() == DIGITS
        }

    private fun columnsAreValid(values: List<Int>): Boolean =
        (0 until BOARD_SIZE).all { column ->
            (0 until BOARD_SIZE).map { row -> values[row * BOARD_SIZE + column] }.toSet() == DIGITS
        }

    private companion object {
        const val BOARD_SIZE = 6
        const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE
        val DIGITS = (1..BOARD_SIZE).toSet()
        val solution = listOf(
            "123456",
            "456123",
            "234561",
            "561234",
            "345612",
            "612345",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
