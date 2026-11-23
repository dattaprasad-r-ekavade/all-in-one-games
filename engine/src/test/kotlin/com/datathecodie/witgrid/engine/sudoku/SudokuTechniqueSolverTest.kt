package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SudokuTechniqueSolverTest {
    @Test
    fun solvesUniqueFixturesUsingEachPublishedTechniqueAndGradesTheirPack() {
        val fixtures = listOf(
            TechniqueFixture(Pack.EASY, SudokuTechniques.NAKED_SINGLE, nakedSingleClues),
            TechniqueFixture(Pack.MEDIUM, SudokuTechniques.HIDDEN_SINGLE, hiddenSingleClues),
            TechniqueFixture(Pack.HARD, SudokuTechniques.LOCKED_CANDIDATES, lockedCandidatesClues),
            TechniqueFixture(Pack.HARD, SudokuTechniques.NAKED_PAIR, nakedPairClues),
            TechniqueFixture(Pack.EXPERT, SudokuTechniques.HIDDEN_PAIR, hiddenPairClues),
            TechniqueFixture(Pack.EXPERT, SudokuTechniques.NAKED_TRIPLE, nakedTripleClues),
            TechniqueFixture(Pack.EXPERT, SudokuTechniques.X_WING, xWingClues),
        )

        fixtures.forEach { fixture ->
            val puzzle = puzzle(fixture.clues, fixture.pack)
            assertEquals(1, SudokuRules.countSolutions(puzzle), "${fixture.technique.id} fixture must be unique")

            val solvedByKit = solveWithHints(SudokuEngine, puzzle)
            assertTrue(SudokuEngine.isSolved(puzzle, solvedByKit), "${fixture.technique.id} must solve with the test kit")

            val (solved, steps) = collectHintSolve(puzzle)
            assertTrue(SudokuRules.isSolved(puzzle, solved), "${fixture.technique.id} fixture must solve")
            assertTrue(steps.any { it.technique == fixture.technique }) {
                "${fixture.pack} fixture must use ${fixture.technique.displayName}"
            }
            steps.forEach(::assertOneSentence)
            val techniqueStep = steps.first { it.technique == fixture.technique }
            val placement = techniqueStep.moves.single() as Move.SetDigit
            assertEquals(puzzle.solution[placement.cell.index], placement.digit)
            assertTrue(techniqueStep.cells.contains(placement.cell))
            if (fixture.technique.difficultyRank >= SudokuTechniques.LOCKED_CANDIDATES.difficultyRank) {
                assertTrue(techniqueStep.cells.size >= 3, "Advanced hints must highlight their source cells")
            }

            val expected = Grade(
                steps.maxBy { it.technique.difficultyRank }.technique,
                steps.size,
                fixture.pack,
            )
            val grade = SudokuTechniqueSolver.grade(puzzle)
            assertEquals(expected, grade)
            assertTrue(SudokuPackBands.isGradeInPackBand(fixture.pack, grade))
            val storedGrade = SudokuGradeCodec.encode(grade)
            SudokuGradeCodec.validate(storedGrade)
        }
    }

    @Test
    fun rejectsAUniquePuzzleThatIsStuckWithTheSupportedTechniques() {
        val puzzle = puzzle(stuckClues, Pack.EASY)

        assertEquals(1, SudokuRules.countSolutions(puzzle))
        assertEquals(null, SudokuTechniqueSolver.nextStep(puzzle, SudokuState.initial(puzzle)))
        val error = assertThrows(IllegalArgumentException::class.java) {
            SudokuTechniqueSolver.grade(puzzle)
        }
        assertTrue(error.message.orEmpty().contains("stuck", ignoreCase = true))
    }

    @Test
    fun gradeRequiresExactlyOneSolutionAndAMatchingTechniqueBand() {
        val multiple = puzzle(emptyList(), Pack.EASY)
        val zero = puzzle(listOf(0, 10), Pack.EASY).let { source ->
            val givens = source.givens.toMutableList()
            givens[0] = 1
            givens[10] = 1
            SudokuPuzzle(source.id, givens, source.solution, source.pack, source.storedGrade)
        }
        val easyInWrongBand = puzzle(nakedSingleClues, Pack.MEDIUM)
        val mismatchedGivens = puzzle(nakedSingleClues, Pack.EASY).let { source ->
            val givens = source.givens.toMutableList()
            givens[0] = if (solution[0] == 9) 1 else solution[0] + 1
            SudokuPuzzle(source.id, givens, source.solution, source.pack, source.storedGrade)
        }
        val invalidSolution = puzzle(nakedSingleClues, Pack.EASY).let { source ->
            val solutionCopy = source.solution.toMutableList()
            solutionCopy[0] = solutionCopy[1]
            SudokuPuzzle(source.id, source.givens, solutionCopy, source.pack, source.storedGrade)
        }

        assertEquals(2, SudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(0, SudokuRules.countSolutions(zero))
        assertThrows(IllegalArgumentException::class.java) { SudokuTechniqueSolver.grade(multiple) }
        assertThrows(IllegalArgumentException::class.java) { SudokuTechniqueSolver.grade(zero) }
        assertThrows(IllegalArgumentException::class.java) { SudokuTechniqueSolver.grade(easyInWrongBand) }
        assertThrows(IllegalArgumentException::class.java) { SudokuTechniqueSolver.grade(mismatchedGivens) }
        assertThrows(IllegalArgumentException::class.java) { SudokuTechniqueSolver.grade(invalidSolution) }
        assertFalse(
            SudokuPackBands.isGradeInPackBand(
                Pack.EXPERT,
                Grade(SudokuTechniques.NAKED_PAIR, 20, Pack.EXPERT),
            ),
        )
        assertFalse(
            SudokuPackBands.isGradeInPackBand(
                Pack.HARD,
                Grade(SudokuTechniques.HIDDEN_PAIR, 20, Pack.HARD),
            ),
        )
        assertFalse(
            SudokuPackBands.isGradeInPackBand(
                Pack.EASY,
                Grade(SudokuTechniques.NAKED_SINGLE, SudokuPackBands.MINIMUM_HINT_STEP_COUNT - 1, Pack.EASY),
            ),
        )
    }

    @Test
    fun engineDelegatesHintsAndGradeToTechniqueSolver() {
        val puzzle = puzzle(nakedSingleClues, Pack.EASY)
        val state = SudokuEngine.initialState(puzzle)

        assertEquals(SudokuTechniqueSolver.nextStep(puzzle, state), SudokuEngine.nextStep(puzzle, state))
        assertEquals(SudokuTechniqueSolver.grade(puzzle), SudokuEngine.grade(puzzle))
    }

    @Test
    fun gradeCodecRejectsUnknownTechniquesAndMalformedCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            SudokuGradeCodec.encode(Grade(Technique("outside", "Outside", 1), 1, Pack.MEDIUM))
        }
        listOf("outside:1", "none:1", "naked-single:0", "naked-single:-1", "naked-single:many")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { SudokuGradeCodec.validate(value) }
            }
        assertThrows(IllegalArgumentException::class.java) {
            SudokuGradeCodec.validate("naked-single:1:extra")
        }
        SudokuGradeCodec.validate("none:0")
        SudokuGradeCodec.validate("x-wing:12")
    }

    private fun collectHintSolve(puzzle: SudokuPuzzle): Pair<SudokuState, List<HintStep>> {
        var state = SudokuState.initial(puzzle)
        val steps = mutableListOf<HintStep>()
        while (!SudokuRules.isSolved(puzzle, state)) {
            val step = SudokuTechniqueSolver.nextStep(puzzle, state)
                ?: throw AssertionError("${puzzle.pack} fixture is stuck")
            assertOneSentence(step)
            steps += step
            state = step.moves.fold(state, SudokuState::withMove)
            assertTrue(steps.size <= SUDOKU_CELL_COUNT)
        }
        return state to steps
    }

    private fun assertOneSentence(step: HintStep) {
        assertTrue(step.explanation.isNotBlank())
        assertTrue(step.explanation.endsWith('.'))
        assertEquals(1, step.explanation.count { it == '.' })
    }

    private fun puzzle(clueIndices: List<Int>, pack: Pack): SudokuPuzzle {
        val clues = clueIndices.toSet()
        return SudokuPuzzle(
            id = "SU-TECHNIQUE-FIXTURE",
            givens = solution.mapIndexed { index, digit -> if (index in clues) digit else null },
            solution = solution,
            pack = pack,
            storedGrade = "pending",
        )
    }

    private data class TechniqueFixture(val pack: Pack, val technique: Technique, val clues: List<Int>)

    private companion object {
        val solution = listOf(
            "812753649",
            "943682175",
            "675491283",
            "154237896",
            "369845721",
            "287169534",
            "521974368",
            "438526917",
            "796318452",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val nakedSingleClues = listOf(
            0, 2, 11, 12, 15, 18, 19, 21, 22, 24, 28, 30, 32, 38, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78, 80,
        )
        val hiddenSingleClues = listOf(
            0, 11, 12, 18, 19, 21, 22, 24, 28, 30, 32, 38, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78, 80,
        )
        val lockedCandidatesClues = listOf(
            0, 11, 12, 18, 19, 21, 22, 24, 28, 32, 40, 41, 42, 46, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78,
        )
        val nakedPairClues = listOf(
            0, 11, 12, 15, 19, 22, 24, 28, 29, 31, 32, 40, 41, 42, 48, 52, 56, 60, 61, 62, 65, 66, 70, 71, 73, 78,
        )
        val hiddenPairClues = listOf(
            0, 8, 11, 12, 19, 22, 24, 28, 32, 40, 41, 42, 47, 48, 52, 56, 61, 62, 63, 65, 66, 68, 70, 73, 78,
        )
        val nakedTripleClues = listOf(
            0, 11, 12, 19, 22, 24, 28, 32, 34, 37, 40, 41, 42, 46, 48, 52, 56, 61, 62, 64, 65, 66, 70, 72, 73, 78,
        )
        val xWingClues = listOf(
            0, 1, 11, 12, 13, 19, 22, 24, 28, 32, 40, 41, 42, 44, 48, 52, 56, 61, 62, 64, 65, 66, 70, 73, 78,
        )
        val stuckClues = listOf(
            0, 11, 12, 19, 22, 24, 28, 32, 40, 41, 42, 48, 52, 56, 61, 62, 65, 66, 70, 73, 78,
        )
    }
}
