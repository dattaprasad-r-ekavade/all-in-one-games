package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.testkit.assertUnique
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicTechniqueSolverTest {
    // 8x8 Easy technique-solvable hand fixture
    private val easy8x8Line =
        "MO-EASY-1|8|0224455313355553243444423434443234455654445566545564556544423343|0010111100101101100100001010011010011010010110111101011011010011|EASY|clue-overlap:42"

    // 10x10 Medium technique-solvable hand fixture
    private val medium10x10Line =
        "MO-MEDIUM-0|10|3333454432455667675324465655421346556652133555655223234565422324555553333355543233446554323332444200|1101111110010010100110111011100010110100000100110110101101101000011000000110011111010110001001011000|MEDIUM|clue-overlap:60"

    // 12x12 Hard technique-solvable hand fixture
    private val hard12x12Line =
        "MO-HARD-0|12|233444454333355765465555244532244566355533344565343224543343354336765442354225654342354213444353343101122232342101243222243324453211132324442111|110111111001001010011011101110001011010000010011011010110110100001100000011001111101011000100101100000000001010000001000101000111001001011100000|HARD|clue-overlap:94"

    // 15x15 Expert technique-solvable hand fixture
    private val expert15x15Line =
        "MO-EXPERT-0|15|333355555333432344577766333543244566646444653243456633222454244567643444443233467641222222134666431233200243434442122211255522233344321477643344545331589764433445320698776543435431588766542235642355544343336752123322232124531|110111111001001010011011101110001011010000010011011010110110100001100000011001111101011000100101100000000001010000001000101000111001001011100000110100111111001010100111101110001000111011001010110011110100001110000000011001100|EXPERT|clue-overlap:169"

    @Test
    fun emptyAreaTechniqueCrossesAllCellsInArea() {
        // 3x3 with clue 0 at center cell 4 (row 1, col 1); entire 3x3 must be crossed
        val clues = List(9) { if (it == 4) 0 else null }
        val puzzle = MosaicPuzzle("MO-ZERO", 3, clues, List(9) { 0 }, Pack.EASY, "none:1")
        val state = MosaicState.initial(puzzle)

        val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
        assertEquals("empty-area", step.technique.id)
        assertEquals(9, step.moves.size)
        assertTrue(step.moves.all { it.mark == Mark.CROSSED })
        assertEquals((0 until 9).map(::Cell), step.cells)
        assertValidOneSentenceExplanation(step.explanation)
    }

    @Test
    fun fullAreaTechniqueFillsAllCellsInArea() {
        // 3x3 corner cell 0 has area size 4 (cells 0, 1, 3, 4) with clue 4
        val clues = List(9) { if (it == 0) 4 else null }
        val solution = listOf(1, 1, 0, 1, 1, 0, 0, 0, 0)
        val puzzle = MosaicPuzzle("MO-FULL", 3, clues, solution, Pack.EASY, "none:1")
        val state = MosaicState.initial(puzzle)

        val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
        assertEquals("full-area", step.technique.id)
        assertEquals(4, step.moves.size)
        assertTrue(step.moves.all { it.mark == Mark.FILLED })
        assertEquals(listOf(0, 1, 3, 4).map(::Cell), step.cells)
        assertEquals(listOf(0, 1, 3, 4).map(::Cell), step.moves.map { it.cell })
        assertValidOneSentenceExplanation(step.explanation)
    }

    @Test
    fun remainingCountTechniqueCrossesWhenNeededFillsIsZero() {
        // 3x3 corner cell 0 has area size 4 with clue 2. Cells 0 and 1 are already filled.
        val clues = List(9) { if (it == 0) 2 else null }
        val solution = listOf(1, 1, 0, 0, 0, 0, 0, 0, 0)
        val puzzle = MosaicPuzzle("MO-REM-CROSS", 3, clues, solution, Pack.EASY, "none:1")
        val state = MosaicState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.FILLED))
            .withMove(Move.SetMark(Cell(1), Mark.FILLED))

        val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
        assertEquals("remaining-count", step.technique.id)
        assertEquals(2, step.moves.size)
        assertTrue(step.moves.all { it.mark == Mark.CROSSED })
        assertEquals(listOf(Cell(3), Cell(4)), step.moves.map { it.cell })
        assertValidOneSentenceExplanation(step.explanation)
    }

    @Test
    fun remainingCountTechniqueFillsWhenNeededFillsMatchesUnknownCount() {
        // 3x3 corner cell 0 has area size 4 with clue 2. Cells 0 and 1 are already crossed.
        val clues = List(9) { if (it == 0) 2 else null }
        val solution = listOf(0, 0, 0, 1, 1, 0, 0, 0, 0)
        val puzzle = MosaicPuzzle("MO-REM-FILL", 3, clues, solution, Pack.EASY, "none:1")
        val state = MosaicState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.CROSSED))
            .withMove(Move.SetMark(Cell(1), Mark.CROSSED))

        val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
        assertEquals("remaining-count", step.technique.id)
        assertEquals(2, step.moves.size)
        assertTrue(step.moves.all { it.mark == Mark.FILLED })
        assertEquals(listOf(Cell(3), Cell(4)), step.moves.map { it.cell })
        assertValidOneSentenceExplanation(step.explanation)
    }

    @Test
    fun clueOverlapSubsetCrossDeducesOutsideCellsMustBeCrossed() {
        // Corner 0 (area {0, 1, 3, 4}) has clue 2.
        // Edge 1 (area {0, 1, 2, 3, 4, 5}) has clue 2.
        // Area 0 is subset of Area 1. Both need 2 fills.
        // Outside cells {2, 5} must both be crossed!
        val clues = listOf(
            2, 2, null,
            null, null, null,
            null, null, null,
        )
        val solution = listOf(
            1, 1, 0,
            0, 0, 0,
            0, 0, 0,
        )
        val puzzle = MosaicPuzzle("MO-SUB-CROSS", 3, clues, solution, Pack.EASY, "none:1")
        val state = MosaicState.initial(puzzle)

        val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
        assertEquals("clue-overlap", step.technique.id)
        assertEquals(listOf(Cell(2), Cell(5)), step.moves.map { it.cell })
        assertTrue(step.moves.all { it.mark == Mark.CROSSED })
        assertValidOneSentenceExplanation(step.explanation)
    }

    @Test
    fun clueOverlapSubsetFillDeducesOutsideCellsMustBeFilled() {
        // Corner 0 (area {0, 1, 3, 4}) has clue 2.
        // Edge 1 (area {0, 1, 2, 3, 4, 5}) has clue 4.
        // Difference 4 - 2 = 2 == outside count {2, 5}.
        // Outside cells {2, 5} must both be filled!
        val clues = listOf(
            2, 4, null,
            null, null, null,
            null, null, null,
        )
        val solution = listOf(
            1, 1, 1,
            0, 0, 1,
            0, 0, 0,
        )
        val puzzle = MosaicPuzzle("MO-SUB-FILL", 3, clues, solution, Pack.EASY, "none:1")
        val state = MosaicState.initial(puzzle)

        val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
        assertEquals("clue-overlap", step.technique.id)
        assertEquals(listOf(Cell(2), Cell(5)), step.moves.map { it.cell })
        assertTrue(step.moves.all { it.mark == Mark.FILLED })
        assertValidOneSentenceExplanation(step.explanation)
    }

    @Test
    fun solveWithHintsSolvesEasyPuzzleWithOneSentenceExplanations() {
        val puzzle = MosaicEngine.decodePuzzle(easy8x8Line)
        assertUnique(MosaicEngine, puzzle)

        var state = MosaicEngine.initialState(puzzle)
        val techniquesSeen = mutableSetOf<String>()
        var stepCount = 0

        while (!MosaicEngine.isSolved(puzzle, state)) {
            val step = requireNotNull(MosaicEngine.nextStep(puzzle, state))
            assertEquals(step, MosaicEngine.nextStep(puzzle, state), "nextStep must be deterministic")
            assertValidOneSentenceExplanation(step.explanation)

            techniquesSeen += step.technique.id
            state = step.moves.fold(state, MosaicEngine::applyMove)
            assertTrue(MosaicEngine.mistakes(puzzle, state).isEmpty())
            stepCount++
        }

        assertTrue(MosaicEngine.isSolved(puzzle, state))
        val solvedState = solveWithHints(MosaicEngine, puzzle)
        assertTrue(MosaicEngine.isSolved(puzzle, solvedState))

        // Easy fixture exercises empty-area, remaining-count, and clue-overlap
        assertTrue(techniquesSeen.contains("empty-area"))
        assertTrue(techniquesSeen.contains("remaining-count"))
        assertTrue(techniquesSeen.contains("clue-overlap"))

        val grade = MosaicEngine.grade(puzzle)
        assertEquals(stepCount, grade.stepCount)
        assertEquals(Pack.EASY, grade.pack)
        assertEquals("clue-overlap", grade.hardestTechnique?.id)
    }

    @Test
    fun gradeAndSolveWithHintsAcrossAllLaunchPacks() {
        val fixtures = listOf(
            MosaicEngine.decodePuzzle(easy8x8Line),
            MosaicEngine.decodePuzzle(medium10x10Line),
            MosaicEngine.decodePuzzle(hard12x12Line),
            MosaicEngine.decodePuzzle(expert15x15Line),
        )

        for (puzzle in fixtures) {
            val solvedState = solveWithHints(MosaicEngine, puzzle)
            assertTrue(MosaicEngine.isSolved(puzzle, solvedState))

            val grade = MosaicEngine.grade(puzzle)
            assertEquals(puzzle.pack, grade.pack)
            assertTrue(grade.stepCount > 0)
            assertNotNull(grade.hardestTechnique)
            assertTrue(MosaicPackBands.isGradeInPackBand(puzzle.pack, grade))
        }
    }

    @Test
    fun gradeRejectsUniquePuzzleWhenTechniquesGetStuck() {
        // 8x8 checkerboard pattern with full clues has 1 unique solution,
        // but no 0 or full-area clue and no subset overlap, so techniques get stuck.
        val solution8x8 = listOf(
            1, 0, 0, 1, 1, 0, 0, 1,
            0, 1, 1, 0, 0, 1, 1, 0,
            0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1, 1, 0, 0, 1,
            1, 0, 0, 1, 1, 0, 0, 1,
            0, 1, 1, 0, 0, 1, 1, 0,
            0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1, 1, 0, 0, 1,
        )
        val grid8 = Grid(8)
        val clues8x8 = (0 until 64).map {
            MosaicRules.clueAt(grid8, solution8x8, Cell(it))
        }
        val stuckPuzzle = MosaicPuzzle("MO-STUCK", 8, clues8x8, solution8x8, Pack.EASY, "pending")

        assertEquals(1, MosaicRules.countSolutions(stuckPuzzle, limit = 2), "Must be unique")
        val state = MosaicState.initial(stuckPuzzle)
        assertNull(MosaicEngine.nextStep(stuckPuzzle, state), "Techniques cannot make progress")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            MosaicEngine.grade(stuckPuzzle)
        }
        assertTrue(exception.message?.contains("stuck") == true)
    }

    @Test
    fun gradeRejectsNonUniqueAndInvalidPuzzles() {
        // Multi-solution puzzle (empty clues)
        val multiSolution = MosaicPuzzle(
            id = "MO-MULTI",
            size = 8,
            clues = List(64) { null },
            solution = List(64) { 0 },
            pack = Pack.EASY,
            storedGrade = "none:1",
        )
        assertThrows(IllegalArgumentException::class.java) {
            MosaicEngine.grade(multiSolution)
        }
    }

    @Test
    fun nextStepReturnsNullOnMistakesOrSolvedBoard() {
        val puzzle = MosaicEngine.decodePuzzle(easy8x8Line)
        val initial = MosaicState.initial(puzzle)

        // Solved board
        val solved = solveWithHints(MosaicEngine, puzzle)
        assertNull(MosaicEngine.nextStep(puzzle, solved))

        // Mistaken board
        val mistakeState = initial.withMove(Move.SetMark(Cell(0), Mark.FILLED))
        assertTrue(MosaicEngine.mistakes(puzzle, mistakeState).isNotEmpty())
        assertNull(MosaicEngine.nextStep(puzzle, mistakeState))
    }

    @Test
    fun gradeCodecEncodesAndValidatesCorrectly() {
        val grade = Grade(MosaicTechniques.CLUE_OVERLAP, 42, Pack.EASY)
        val encoded = MosaicGradeCodec.encode(grade)
        assertEquals("clue-overlap:42", encoded)
        MosaicGradeCodec.validate(encoded)

        val zeroGrade = Grade(null, 0, Pack.EASY)
        assertEquals("none:0", MosaicGradeCodec.encode(zeroGrade))
        MosaicGradeCodec.validate("none:0")

        assertThrows(IllegalArgumentException::class.java) {
            MosaicGradeCodec.validate("unknown-tech:5")
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicGradeCodec.validate("clue-overlap:-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicGradeCodec.validate("none:5")
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicGradeCodec.validate("invalid_format")
        }
    }

    private fun assertValidOneSentenceExplanation(explanation: String) {
        assertTrue(explanation.isNotBlank(), "Explanation must not be blank")
        assertTrue(explanation.trim().endsWith('.'), "Explanation must end with a period: $explanation")
        assertEquals(
            1,
            explanation.count { it == '.' || it == '!' || it == '?' },
            "Explanation must be a single sentence with one punctuation mark: $explanation",
        )
    }
}
