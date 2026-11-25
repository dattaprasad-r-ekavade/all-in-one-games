package com.datathecodie.witgrid.engine.killersudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.killersudoku.KillerCage
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuRules
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuTechniqueSolver
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.cageCombinationPuzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.isSudokuGrid
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
        val puzzle = rule45InnieFirstPuzzle()
        assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2))

        val initialState = KillerSudokuState.initial(puzzle)
        val firstStep = requireNotNull(KillerSudokuTechniqueSolver.nextStep(puzzle, initialState)) {
            "Expected a 45-rule step on the initial board"
        }
        val firstMove = firstStep.moves.single() as Move.SetDigit

        assertEquals("rule-45", firstStep.technique.id)
        assertEquals(0, firstMove.cell.index)
        assertEquals(5, firstMove.digit)
        assertTrue(firstStep.cells.contains(firstMove.cell))
        assertTrue(firstStep.explanation.contains("45"), firstStep.explanation)
        assertOneSentence(firstStep.explanation)
        assertLegalPlacement(puzzle, initialState, firstStep)

        var state = firstStep.moves.fold(initialState, KillerSudokuState::withMove)
        var steps = 1
        while (!KillerSudokuRules.isSolved(puzzle, state) && steps < puzzle.grid.cellCount) {
            val step = requireNotNull(
                KillerSudokuTechniqueSolver.nextStep(puzzle, state),
            ) { "Stuck after $steps steps" }
            assertLegalPlacement(puzzle, state, step)
            assertOneSentence(step.explanation)
            state = step.moves.fold(state, KillerSudokuState::withMove)
            steps++
        }

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
        val puzzle = uniqueBoxStuckPuzzle()

        assertEquals(1, KillerSudokuRules.countSolutions(puzzle, limit = 2))
        val error = assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuTechniqueSolver.grade(puzzle)
        }
        assertTrue(error.message.orEmpty().contains("stuck", ignoreCase = true), error.message)
    }

    @Test
    fun techniqueFixturesHaveCheckedCagesDistinctDigitsAndFullPartition() {
        assertHandChecked(cageCombinationPuzzle(), KillerSudokuSpecFixtures.solution)
        assertHandChecked(rule45InnieFirstPuzzle(), KillerSudokuSpecFixtures.solution)
        assertHandChecked(uniqueBoxStuckPuzzle(), escargotSolution)
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

    /**
     * Unique Medium board where rows 1-6 are empty, rows 7-9 are given, and no single or
     * cage-combination step can place a digit before the 45 rule places r1c1 = 5.
     * Row 1 holds cages {1,2,3,4}=20 and {5,6,7,8}=20, leaving r1c1 in {0,9}=11 as the sole innie.
     */
    private fun rule45InnieFirstPuzzle(): KillerSudokuPuzzle {
        val values = KillerSudokuSpecFixtures.solution
        val givens = List(81) { index -> if (index >= 54) values[index] else null }
        val emptyGroups = listOf(
            listOf(0, 9),
            listOf(1, 2, 3, 4),
            listOf(5, 6, 7, 8),
            listOf(10, 11, 12, 19),
            listOf(13, 20, 21, 22),
            listOf(14, 15, 23),
            listOf(16, 17),
            listOf(18, 27, 28, 36, 37),
            listOf(24, 25, 26, 33, 35),
            listOf(29, 30, 39),
            listOf(31, 32),
            listOf(34, 41, 42, 43),
            listOf(38, 47, 48),
            listOf(40, 49, 50),
            listOf(44, 51, 52, 53),
            listOf(45, 46),
        )
        return KillerSudokuPuzzle(
            id = "KS-SPEC-RULE-45-FIRST",
            givens = givens,
            cages = cagesFromGroups(emptyGroups, values),
            solution = values,
            pack = Pack.MEDIUM,
            storedGrade = "hand",
        )
    }

    /**
     * Unique 9x9 board (AI Escargot) with 1-cell cages for givens and one box cage for the
     * remaining empty cells in each 3x3 box, so no supported technique can finish it.
     */
    private fun uniqueBoxStuckPuzzle(): KillerSudokuPuzzle {
        val givens = ESCARGOT_GIVENS_TEXT.map { symbol ->
            if (symbol == '.') null else symbol.digitToInt()
        }
        val boxGroups = (0 until 9).map { box ->
            boxCells(box).filter { index -> givens[index] == null }
        }
        return KillerSudokuPuzzle(
            id = "KS-SPEC-STUCK-ESCARGOT",
            givens = givens,
            cages = cagesFromGroups(boxGroups, escargotSolution),
            solution = escargotSolution,
            pack = Pack.EXPERT,
            storedGrade = "hand",
        )
    }

    private fun cagesFromGroups(
        groups: List<List<Int>>,
        values: List<Int>,
    ): List<KillerCage> {
        val covered = groups.flatten().toSet()
        val grouped = groups.map { indices ->
            KillerCage(indices.map(::Cell), indices.sumOf { values[it] })
        }
        val singletons = values.indices
            .filter { index -> index !in covered }
            .map { index -> KillerCage(listOf(Cell(index)), values[index]) }
        return grouped + singletons
    }

    private fun boxCells(box: Int): List<Int> {
        val boxRow = box / 3
        val boxColumn = box % 3
        return (0 until 9).map { offset ->
            (boxRow * 3 + offset / 3) * 9 + (boxColumn * 3 + offset % 3)
        }
    }

    companion object {
        private const val ESCARGOT_GIVENS_TEXT =
            "1....7.9." +
                ".3..2...8" +
                "..96..5.." +
                "..53..9.." +
                ".1..8...2" +
                "6....4..." +
                "3......1." +
                ".4......7" +
                "..7...3.."

        private const val ESCARGOT_SOLUTION_TEXT =
            "162857493" +
                "534129678" +
                "789643521" +
                "475312986" +
                "913586742" +
                "628794135" +
                "356478219" +
                "241935867" +
                "897261354"

        private val escargotSolution: List<Int> = ESCARGOT_SOLUTION_TEXT.map { it.digitToInt() }
    }
}
