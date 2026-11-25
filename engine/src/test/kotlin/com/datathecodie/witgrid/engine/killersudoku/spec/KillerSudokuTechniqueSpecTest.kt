package com.datathecodie.witgrid.engine.killersudoku.spec

import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuRules
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuTechniqueSolver
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.cageCombinationPuzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.isSudokuGrid
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.rule45Puzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.stuckSolution
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.uniqueStuckPuzzle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Independent Killer Sudoku technique cases from plan 12.7:
 * cage combinations, the 45 rule, guess-free solves, and stuck-unique grading.
 */
class KillerSudokuTechniqueSpecTest {
    @Test
    fun cageCombinationStepForcesThePartnerDigitInATwoCellCage() {
        val puzzle = cageCombinationPuzzle()
        assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2))

        val step = requireNotNull(
            KillerSudokuTechniqueSolver.nextStep(puzzle, KillerSudokuState.initial(puzzle)),
        )
        val move = step.moves.single() as Move.SetDigit

        assertEquals("cage-combinations", step.technique.id)
        assertEquals(1, move.cell.index)
        assertEquals(3, move.digit)
        assertTrue(step.cells.contains(move.cell))
        assertOneSentence(step.explanation)
        assertLegalPlacement(puzzle, KillerSudokuState.initial(puzzle), step)
    }

    @Test
    fun ruleOf45ForcesTheInnieDigitInRowOne() {
        val puzzle = rule45Puzzle()
        assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2))

        var state = KillerSudokuState.initial(puzzle)
        var sawIntendedInnie = false
        var steps = 0
        while (!KillerSudokuRules.isSolved(puzzle, state) && steps < puzzle.grid.cellCount) {
            val step = requireNotNull(
                KillerSudokuTechniqueSolver.nextStep(puzzle, state),
            ) { "Stuck after $steps steps" }
            assertLegalPlacement(puzzle, state, step)
            assertOneSentence(step.explanation)
            if (step.technique.id == "rule-45") {
                val move = step.moves.single() as Move.SetDigit
                if (move.cell.index == 0) {
                    assertEquals(5, move.digit)
                    assertTrue(step.explanation.contains("45"), step.explanation)
                    sawIntendedInnie = true
                }
            }
            state = step.moves.fold(state, KillerSudokuState::withMove)
            steps++
        }

        assertTrue(sawIntendedInnie, "Expected a 45-rule step that places r1c1 = 5")
        assertTrue(KillerSudokuRules.isSolved(puzzle, state))
    }

    @Test
    fun followingHintsFromTheCageCombinationFixtureReachesTheSolution() {
        val puzzle = cageCombinationPuzzle()
        val (solved, steps) = followHints(puzzle)

        assertTrue(KillerSudokuRules.isSolved(puzzle, solved))
        assertEquals(80, steps.size)
        assertTrue(steps.any { it.technique.id == "cage-combinations" })
    }

    @Test
    fun nextStepReturnsALegalPlacementWithAOneSentenceExplanation() {
        val puzzle = cageCombinationPuzzle()
        val state = KillerSudokuState.initial(puzzle)
        val step = requireNotNull(KillerSudokuTechniqueSolver.nextStep(puzzle, state))

        assertLegalPlacement(puzzle, state, step)
        assertOneSentence(step.explanation)
        assertFalse(step.explanation.isBlank())
        assertTrue(step.moves.isNotEmpty())
        assertTrue(step.cells.isNotEmpty())
    }

    @Test
    fun gradeRejectsAUniquePuzzleThatTechniquesCannotFinish() {
        val puzzle = uniqueStuckPuzzle()

        assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2))
        val error = assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuTechniqueSolver.grade(puzzle)
        }
        assertTrue(error.message.orEmpty().contains("stuck", ignoreCase = true), error.message)
    }

    @Test
    fun techniqueFixturesHaveCheckedCagesDistinctDigitsAndFullPartition() {
        assertHandChecked(cageCombinationPuzzle(), KillerSudokuSpecFixtures.solution)
        assertHandChecked(rule45Puzzle(), KillerSudokuSpecFixtures.solution)
        assertHandChecked(uniqueStuckPuzzle(), stuckSolution)
    }

    private fun followHints(puzzle: KillerSudokuPuzzle): Pair<KillerSudokuState, List<HintStep>> {
        var state = KillerSudokuState.initial(puzzle)
        val steps = ArrayList<HintStep>()
        while (!KillerSudokuRules.isSolved(puzzle, state)) {
            val step = requireNotNull(
                KillerSudokuTechniqueSolver.nextStep(puzzle, state),
            ) { "${puzzle.id} stuck after ${steps.size} steps" }
            assertLegalPlacement(puzzle, state, step)
            assertOneSentence(step.explanation)
            steps += step
            state = step.moves.fold(state, KillerSudokuState::withMove)
            assertTrue(steps.size <= puzzle.grid.cellCount) { "${puzzle.id} hints did not stop" }
        }
        return state to steps
    }

    private fun assertLegalPlacement(
        puzzle: KillerSudokuPuzzle,
        state: KillerSudokuState,
        step: HintStep,
    ) {
        val move = step.moves.single() as Move.SetDigit
        assertEquals(null, state.digits[move.cell.index], "Hint must fill an empty cell")
        assertEquals(puzzle.solution[move.cell.index], move.digit)
        assertTrue(move.digit in 1..9)
        assertTrue(move.cell.index in 0 until puzzle.grid.cellCount)
        assertTrue(KillerSudokuRules.mistakes(puzzle, state.withMove(move)).isEmpty())
    }

    private fun assertOneSentence(explanation: String) {
        assertTrue(explanation.isNotBlank())
        assertTrue(explanation.endsWith('.'), explanation)
        assertEquals(1, explanation.count { it == '.' }, explanation)
    }

    private fun assertHandChecked(puzzle: KillerSudokuPuzzle, values: List<Int>) {
        assertTrue(isSudokuGrid(values), "${puzzle.id} solution is not valid Sudoku")
        assertTrue(KillerSudokuRules.isValidSolution(puzzle, values), "${puzzle.id} breaks a cage or given")
        val covered = puzzle.cages.flatMap { cage -> cage.cells.map { it.index } }
        assertEquals((0 until 81).toList(), covered.sorted(), "${puzzle.id} cages do not partition the board")
        for (cage in puzzle.cages) {
            val digits = cage.cells.map { values[it.index] }
            assertEquals(digits.size, digits.toSet().size, "${puzzle.id} cage $cage repeats a digit")
            assertEquals(digits.sum(), cage.sum, "${puzzle.id} cage $cage has the wrong sum")
        }
        puzzle.givens.forEachIndexed { index, given ->
            if (given != null) {
                assertEquals(values[index], given, "${puzzle.id} given at $index does not match the solution")
            }
        }
    }
}
