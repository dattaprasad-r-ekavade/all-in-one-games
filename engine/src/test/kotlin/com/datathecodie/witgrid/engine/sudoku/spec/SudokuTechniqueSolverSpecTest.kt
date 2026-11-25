package com.datathecodie.witgrid.engine.sudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.sudoku.SudokuEngine
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Sudoku 9x9 solver checks: guess-free hints, mistakes and grading of the hand-made board. */
class SudokuTechniqueSolverSpecTest {
    @Test
    fun hintsFromTheGivensPlaceOnlySolutionDigitsUntilTheBoardIsSolved() {
        val puzzle = SudokuSpecFixtures.puzzle()
        var state = SudokuState.initial(puzzle)
        var steps = 0

        while (true) {
            val step = SudokuEngine.nextStep(puzzle, state) ?: break
            assertTrue(step.explanation.isNotBlank())
            assertEquals(1, step.moves.size)
            val move = step.moves.single() as Move.SetDigit
            assertEquals(SudokuSpecFixtures.solution[move.cell.index], move.digit)
            assertTrue(step.cells.contains(move.cell))
            state = SudokuEngine.applyMove(state, move)
            steps++
            assertTrue(steps <= SudokuSpecFixtures.CELL_COUNT) { "Hints did not stop" }
        }

        assertEquals(SudokuSpecFixtures.GIVEN_EMPTY_CELL_COUNT, steps)
        assertTrue(SudokuEngine.isSolved(puzzle, state))
    }

    @Test
    fun noHintIsGivenForASolvedBoard() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val solved = SudokuSpecFixtures.solution.indices
            .filter { index -> puzzle.givens[index] == null }
            .fold(SudokuState.initial(puzzle)) { state, index ->
                SudokuEngine.applyMove(state, Move.SetDigit(Cell(index), SudokuSpecFixtures.solution[index]))
            }

        assertNull(SudokuEngine.nextStep(puzzle, solved))
    }

    @Test
    fun noHintIsGivenWhileAWrongDigitIsOnTheBoard() {
        val puzzle = SudokuSpecFixtures.puzzle()
        // Index 2 is row 1 column 3, whose solution digit is 4. Entering 1 is a mistake.
        val mistaken = SudokuEngine.applyMove(SudokuState.initial(puzzle), Move.SetDigit(Cell(2), 1))

        assertNull(SudokuEngine.nextStep(puzzle, mistaken))
    }

    @Test
    fun gradeOfTheEasyBoardIsNakedSinglesOnlyAndCountsEveryPlacement() {
        val grade = SudokuEngine.grade(SudokuSpecFixtures.puzzle(pack = Pack.EASY))

        assertEquals(SudokuSpecFixtures.GIVEN_EMPTY_CELL_COUNT, grade.stepCount)
        assertEquals("Naked single", grade.hardestTechnique?.displayName)
        assertEquals(Pack.EASY, grade.pack)
    }

    @Test
    fun gradeRejectsAPackWhoseTechniqueBandDoesNotMatch() {
        val expert = SudokuSpecFixtures.puzzle(pack = Pack.EXPERT)

        assertThrows(IllegalArgumentException::class.java) { SudokuEngine.grade(expert) }
    }

    @Test
    fun gradeRejectsAPuzzleWithMoreThanOneSolution() {
        val empty = SudokuSpecFixtures.puzzle(cellGivens = List(SudokuSpecFixtures.CELL_COUNT) { null })

        assertThrows(IllegalArgumentException::class.java) { SudokuEngine.grade(empty) }
    }
}
