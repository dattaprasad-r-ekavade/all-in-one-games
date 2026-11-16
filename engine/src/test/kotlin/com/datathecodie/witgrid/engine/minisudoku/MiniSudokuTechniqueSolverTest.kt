package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiniSudokuTechniqueSolverTest {
    @Test
    fun solvesUniqueFixturesUsingEachPublishedTechniqueAndGradesTheirPack() {
        val fixtures = listOf(
            TechniqueFixture(Pack.EASY, MiniSudokuTechniques.NAKED_SINGLE, nakedSingleClues),
            TechniqueFixture(Pack.MEDIUM, MiniSudokuTechniques.HIDDEN_SINGLE, hiddenSingleClues),
            TechniqueFixture(Pack.HARD, MiniSudokuTechniques.LOCKED_CANDIDATES, lockedCandidatesClues),
            TechniqueFixture(Pack.EXPERT, MiniSudokuTechniques.NAKED_PAIR, nakedPairClues),
        )

        fixtures.forEach { fixture ->
            val puzzle = puzzle(fixture.clues, fixture.pack)
            assertEquals(1, MiniSudokuRules.countSolutions(puzzle), "${fixture.pack} fixture must be unique")

            val (solved, steps) = solveWithHints(puzzle)
            assertTrue(MiniSudokuRules.isSolved(puzzle, solved), "${fixture.pack} fixture must solve")
            assertTrue(steps.any { it.technique == fixture.technique }) {
                "${fixture.pack} fixture must use ${fixture.technique.displayName}"
            }
            steps.forEach(::assertOneSentence)
            val techniqueStep = steps.first { it.technique == fixture.technique }
            val placement = techniqueStep.moves.single() as Move.SetDigit
            assertEquals(puzzle.solution[placement.cell.index], placement.digit)
            assertTrue(techniqueStep.cells.contains(placement.cell))
            if (fixture.technique == MiniSudokuTechniques.LOCKED_CANDIDATES ||
                fixture.technique == MiniSudokuTechniques.NAKED_PAIR
            ) {
                assertTrue(techniqueStep.cells.size >= 3, "Advanced hints must highlight their source cells")
            }

            val expected = Grade(fixture.technique, steps.size, fixture.pack)
            val grade = MiniSudokuTechniqueSolver.grade(puzzle)
            assertEquals(expected, grade)
            assertTrue(MiniSudokuPackBands.isGradeInPackBand(fixture.pack, grade))
            val storedGrade = MiniSudokuGradeCodec.encode(grade)
            MiniSudokuGradeCodec.validate(storedGrade)
        }
    }

    @Test
    fun rejectsAUniquePuzzleThatIsStuckWithTheSupportedTechniques() {
        val puzzle = puzzle(stuckClues, Pack.EASY)

        assertEquals(1, MiniSudokuRules.countSolutions(puzzle))
        assertEquals(null, MiniSudokuTechniqueSolver.nextStep(puzzle, MiniSudokuState.initial(puzzle)))
        assertThrows(IllegalArgumentException::class.java) {
            MiniSudokuTechniqueSolver.grade(puzzle)
        }
    }

    @Test
    fun gradeRequiresExactlyOneSolutionAndAMatchingTechniqueBand() {
        val multiple = puzzle(emptyList(), Pack.EASY)
        val easyInWrongBand = puzzle(nakedSingleClues, Pack.MEDIUM)
        val mismatchedGivens = puzzle(nakedSingleClues, Pack.EASY).let { source ->
            val givens = source.givens.toMutableList()
            givens[0] = if (solution[0] == 6) 1 else solution[0] + 1
            MiniSudokuPuzzle(source.id, givens, source.solution, source.pack, source.storedGrade)
        }
        val invalidSolution = puzzle(nakedSingleClues, Pack.EASY).let { source ->
            val solutionCopy = source.solution.toMutableList()
            solutionCopy[0] = solutionCopy[1]
            MiniSudokuPuzzle(source.id, source.givens, solutionCopy, source.pack, source.storedGrade)
        }

        assertEquals(2, MiniSudokuRules.countSolutions(multiple, limit = 2))
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuTechniqueSolver.grade(multiple) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuTechniqueSolver.grade(easyInWrongBand) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuTechniqueSolver.grade(mismatchedGivens) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuTechniqueSolver.grade(invalidSolution) }
        assertFalse(
            MiniSudokuPackBands.isGradeInPackBand(
                Pack.EXPERT,
                Grade(MiniSudokuTechniques.LOCKED_CANDIDATES, 1, Pack.EXPERT),
            ),
        )
    }

    @Test
    fun engineDelegatesHintsAndGradeToTechniqueSolver() {
        val puzzle = puzzle(nakedSingleClues, Pack.EASY)
        val state = MiniSudokuEngine.initialState(puzzle)

        assertEquals(MiniSudokuTechniqueSolver.nextStep(puzzle, state), MiniSudokuEngine.nextStep(puzzle, state))
        assertEquals(MiniSudokuTechniqueSolver.grade(puzzle), MiniSudokuEngine.grade(puzzle))
    }

    @Test
    fun gradeCodecRejectsUnknownTechniquesAndMalformedCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            MiniSudokuGradeCodec.encode(Grade(Technique("outside", "Outside", 1), 1, Pack.MEDIUM))
        }
        listOf("outside:1", "none:1", "naked-single:0", "naked-single:-1", "naked-single:many")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { MiniSudokuGradeCodec.validate(value) }
            }
        assertThrows(IllegalArgumentException::class.java) {
            MiniSudokuGradeCodec.validate("naked-single:1:extra")
        }
    }

    private fun solveWithHints(puzzle: MiniSudokuPuzzle): Pair<MiniSudokuState, List<HintStep>> {
        var state = MiniSudokuState.initial(puzzle)
        val steps = mutableListOf<HintStep>()
        while (!MiniSudokuRules.isSolved(puzzle, state)) {
            val step = MiniSudokuTechniqueSolver.nextStep(puzzle, state)
                ?: throw AssertionError("${puzzle.pack} fixture is stuck")
            assertOneSentence(step)
            steps += step
            state = step.moves.fold(state, MiniSudokuState::withMove)
            assertTrue(steps.size <= MINI_SUDOKU_CELL_COUNT)
        }
        return state to steps
    }

    private fun assertOneSentence(step: HintStep) {
        assertTrue(step.explanation.isNotBlank())
        assertTrue(step.explanation.endsWith('.'))
        assertEquals(1, step.explanation.count { it == '.' })
    }

    private fun puzzle(clueIndices: List<Int>, pack: Pack): MiniSudokuPuzzle {
        val clues = clueIndices.toSet()
        return MiniSudokuPuzzle(
            id = "MS-TECHNIQUE-FIXTURE",
            givens = solution.mapIndexed { index, digit -> if (index in clues) digit else null },
            solution = solution,
            pack = pack,
            storedGrade = "pending",
        )
    }

    private data class TechniqueFixture(val pack: Pack, val technique: Technique, val clues: List<Int>)

    private companion object {
        val solution = listOf("123456", "456123", "234561", "561234", "345612", "612345")
            .flatMap { row -> row.map(Char::digitToInt) }

        val nakedSingleClues = listOf(0, 1, 2, 4, 9, 10, 11, 12, 13, 14, 15, 16, 18, 19, 20, 21, 22, 23, 24, 25, 27, 30, 31, 34, 35)
        val hiddenSingleClues = listOf(0, 1, 11, 12, 16, 17, 21, 26, 30, 34)
        val lockedCandidatesClues = listOf(3, 5, 10, 11, 14, 18, 19, 24, 26, 31, 33, 35)
        val nakedPairClues = listOf(1, 2, 6, 16, 17, 21, 24, 26, 32, 34)
        val stuckClues = listOf(0, 4, 5, 8, 9, 11, 15, 17, 22, 25, 30, 32)
    }
}
