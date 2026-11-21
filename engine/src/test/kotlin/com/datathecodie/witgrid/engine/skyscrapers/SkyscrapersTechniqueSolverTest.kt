package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections

class SkyscrapersTechniqueSolverTest {

    @Test
    fun solvesUniqueFixturesUsingEachPublishedTechniqueAndGradesTheirPack() {
        val easyStandard = puzzle4()
        assertEquals(1, SkyscrapersRules.countSolutions(easyStandard))
        val (solvedStandard, stepsStandard) = SkyscrapersTechniqueSolver.solveWithHints(easyStandard)
        assertTrue(SkyscrapersRules.isSolved(easyStandard, solvedStandard))
        stepsStandard.forEach(::assertOneSentence)

        // Verifies CLUE_1_AND_N and NAKED_SINGLE
        val firstStep = requireNotNull(SkyscrapersTechniqueSolver.nextStep(easyStandard, SkyscrapersState.initial(easyStandard)))
        assertEquals(SkyscrapersTechniques.CLUE_1_AND_N, firstStep.technique)
        assertOneSentence(firstStep)
        assertTrue(stepsStandard.any { it.technique == SkyscrapersTechniques.CLUE_1_AND_N })
        assertTrue(stepsStandard.any { it.technique == SkyscrapersTechniques.NAKED_SINGLE })

        val gradeStandard = SkyscrapersTechniqueSolver.grade(easyStandard)
        assertEquals(Pack.EASY, gradeStandard.pack)
        assertTrue(SkyscrapersPackBands.isGradeInPackBand(Pack.EASY, gradeStandard))
        assertEquals(stepsStandard.size, gradeStandard.stepCount)
        val encodedStandard = SkyscrapersGradeCodec.encode(gradeStandard)
        SkyscrapersGradeCodec.validate(encodedStandard)

        // Verifies HEIGHT_BOUNDS and HIDDEN_SINGLE with a 4x4 fixture having no clues 1 or 4
        val hbPuzzle = heightBoundsPuzzle4()
        assertEquals(1, SkyscrapersRules.countSolutions(hbPuzzle))
        val (solvedHb, stepsHb) = SkyscrapersTechniqueSolver.solveWithHints(hbPuzzle)
        assertTrue(SkyscrapersRules.isSolved(hbPuzzle, solvedHb))
        stepsHb.forEach(::assertOneSentence)

        val firstHbStep = requireNotNull(SkyscrapersTechniqueSolver.nextStep(hbPuzzle, SkyscrapersState.initial(hbPuzzle)))
        assertEquals(SkyscrapersTechniques.HEIGHT_BOUNDS, firstHbStep.technique)
        assertOneSentence(firstHbStep)
        assertTrue(stepsHb.any { it.technique == SkyscrapersTechniques.HEIGHT_BOUNDS })
        assertTrue(stepsHb.any { it.technique == SkyscrapersTechniques.HIDDEN_SINGLE })
        assertTrue(stepsHb.any {
            val explanation = it.explanation.lowercase()
            "checking the " in explanation &&
                Regex("""clue \d on (row|column) \d""").containsMatchIn(explanation) &&
                Regex("""rules out \d(?: or \d)* at r\dc\d""").containsMatchIn(explanation)
        }, "Permutation hints should name the clues and the heights eliminated at each cell")

        val gradeHb = SkyscrapersTechniqueSolver.grade(hbPuzzle)
        assertEquals(Pack.EASY, gradeHb.pack)
        assertEquals(SkyscrapersTechniques.HEIGHT_BOUNDS, gradeHb.hardestTechnique)
        assertTrue(SkyscrapersPackBands.isGradeInPackBand(Pack.EASY, gradeHb))
        assertEquals(stepsHb.size, gradeHb.stepCount)
        val encodedHb = SkyscrapersGradeCodec.encode(gradeHb)
        SkyscrapersGradeCodec.validate(encodedHb)
    }

    @Test
    fun solvesPuzzlesAcrossAllSupportedPacksAndSizes() {
        // Easy 4x4
        val puzzle4 = puzzle4()
        assertEquals(1, SkyscrapersRules.countSolutions(puzzle4))
        val (state4, steps4) = SkyscrapersTechniqueSolver.solveWithHints(puzzle4)
        assertTrue(SkyscrapersRules.isSolved(puzzle4, state4))
        assertEquals(Pack.EASY, SkyscrapersTechniqueSolver.grade(puzzle4).pack)
        steps4.forEach(::assertOneSentence)

        // Medium 5x5
        val puzzle5 = puzzle5()
        assertEquals(1, SkyscrapersRules.countSolutions(puzzle5))
        val (state5, steps5) = SkyscrapersTechniqueSolver.solveWithHints(puzzle5)
        assertTrue(SkyscrapersRules.isSolved(puzzle5, state5))
        assertEquals(Pack.MEDIUM, SkyscrapersTechniqueSolver.grade(puzzle5).pack)
        steps5.forEach(::assertOneSentence)

        // Hard 6x6
        val puzzle6 = puzzle6()
        assertEquals(1, SkyscrapersRules.countSolutions(puzzle6))
        val (state6, steps6) = SkyscrapersTechniqueSolver.solveWithHints(puzzle6)
        assertTrue(SkyscrapersRules.isSolved(puzzle6, state6))
        assertEquals(Pack.HARD, SkyscrapersTechniqueSolver.grade(puzzle6).pack)
        steps6.forEach(::assertOneSentence)

        // Expert 7x7
        val puzzle7 = puzzle7()
        assertEquals(1, SkyscrapersRules.countSolutions(puzzle7))
        val (state7, steps7) = SkyscrapersTechniqueSolver.solveWithHints(puzzle7)
        assertTrue(SkyscrapersRules.isSolved(puzzle7, state7))
        assertEquals(Pack.EXPERT, SkyscrapersTechniqueSolver.grade(puzzle7).pack)
        steps7.forEach(::assertOneSentence)
    }

    @Test
    fun rejectsAUniquePuzzleThatIsStuckWithTheSupportedTechniques() {
        val stuck = stuckPuzzle4()

        assertTrue(SkyscrapersRules.isValidSolution(stuck))
        assertEquals(1, SkyscrapersRules.countSolutions(stuck))
        val initialState = SkyscrapersState.initial(stuck)

        // Plays until techniques are stuck
        var state = initialState
        while (true) {
            val step = SkyscrapersTechniqueSolver.nextStep(stuck, state) ?: break
            state = step.moves.fold(state, SkyscrapersState::withMove)
        }
        assertFalse(SkyscrapersRules.isSolved(stuck, state))
        assertNull(SkyscrapersTechniqueSolver.nextStep(stuck, state))

        // Grade must reject a unique puzzle that techniques cannot finish
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersTechniqueSolver.grade(stuck)
        }
        assertEquals("Skyscrapers puzzle is stuck with the supported techniques", exception.message)
    }

    @Test
    fun gradeRequiresExactlyOneSolutionAndUsesSizeBasedBands() {
        // Zero solutions: contradictory clues
        val zeroSolutions = puzzle4(
            topClues = listOf(1, null, null, null),
            bottomClues = listOf(1, null, null, null),
        )
        assertEquals(0, SkyscrapersRules.countSolutions(zeroSolutions))
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersTechniqueSolver.grade(zeroSolutions)
        }

        // Two solutions: unconstrained puzzle
        val unconstrained = puzzle4(
            topClues = Collections.nCopies(4, null),
            bottomClues = Collections.nCopies(4, null),
            leftClues = Collections.nCopies(4, null),
            rightClues = Collections.nCopies(4, null),
        )
        assertEquals(2, SkyscrapersRules.countSolutions(unconstrained, limit = 2))
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersTechniqueSolver.grade(unconstrained)
        }

        val easyGrade = SkyscrapersTechniqueSolver.grade(heightBoundsPuzzle4())
        assertTrue(SkyscrapersPackBands.isGradeInPackBand(Pack.EASY, easyGrade))
        assertFalse(SkyscrapersPackBands.isGradeInPackBand(Pack.MEDIUM, easyGrade))

        // Mismatched givens
        val badGivens = puzzle4(givens = List(16) { if (it == 0) 4 else null }) // solution4[0] is 1
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersTechniqueSolver.grade(badGivens)
        }
    }

    @Test
    fun engineDelegatesHintsAndGradeToTechniqueSolver() {
        val puzzle = puzzle4()
        val state = SkyscrapersEngine.initialState(puzzle)

        assertEquals(SkyscrapersTechniqueSolver.nextStep(puzzle, state), SkyscrapersEngine.nextStep(puzzle, state))
        assertEquals(SkyscrapersTechniqueSolver.grade(puzzle), SkyscrapersEngine.grade(puzzle))
    }

    @Test
    fun gradeCodecRoundTripsAndValidatesTechniqueAndStepCount() {
        val grade = Grade(SkyscrapersTechniques.HEIGHT_BOUNDS, 16, Pack.EASY)
        val encoded = SkyscrapersGradeCodec.encode(grade)
        assertEquals("height-bounds:16", encoded)
        SkyscrapersGradeCodec.validate(encoded)

        val noneGrade = Grade(null, 0, Pack.EASY)
        val encodedNone = SkyscrapersGradeCodec.encode(noneGrade)
        assertEquals("none:0", encodedNone)
        SkyscrapersGradeCodec.validate(encodedNone)

        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersGradeCodec.encode(Grade(Technique("foreign", "Foreign", 1), 1, Pack.EASY))
        }

        listOf("outside:1", "none:1", "height-bounds:0", "height-bounds:-1", "height-bounds:abc")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { SkyscrapersGradeCodec.validate(value) }
            }
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersGradeCodec.validate("height-bounds:1:extra")
        }
    }

    @Test
    fun hintsReturnNullOnMistakesOrSolvedState() {
        val puzzle = puzzle4()
        val initial = SkyscrapersState.initial(puzzle)

        // Board with mistake
        val mistaken = initial.withMove(Move.SetDigit(Cell(0), 4)) // solution[0] is 1
        assertNull(SkyscrapersTechniqueSolver.nextStep(puzzle, mistaken))

        // Fully solved board
        val (solved, _) = SkyscrapersTechniqueSolver.solveWithHints(puzzle)
        assertTrue(SkyscrapersRules.isSolved(puzzle, solved))
        assertNull(SkyscrapersTechniqueSolver.nextStep(puzzle, solved))
    }

    private fun assertOneSentence(step: HintStep) {
        assertTrue(step.explanation.isNotBlank(), "Explanation must not be blank")
        assertTrue(step.explanation.endsWith('.'), "Explanation must end with a period: ${step.explanation}")
        assertEquals(1, step.explanation.count { it == '.' }, "Explanation must have exactly one period: ${step.explanation}")
    }

    private fun puzzle4(
        topClues: List<Int?> = listOf(4, 3, 2, 1),
        bottomClues: List<Int?> = listOf(1, 2, 2, 2),
        leftClues: List<Int?> = listOf(4, 3, 2, 1),
        rightClues: List<Int?> = listOf(1, 2, 2, 2),
        givens: List<Int?> = Collections.nCopies(16, null),
    ): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-4-TEST",
        size = 4,
        topClues = topClues,
        bottomClues = bottomClues,
        leftClues = leftClues,
        rightClues = rightClues,
        solution = solution4,
        pack = Pack.EASY,
        storedGrade = "pending",
        givens = givens,
    )

    private fun heightBoundsPuzzle4(): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-4-HB-TEST",
        size = 4,
        topClues = listOf(3, 2, null, 2),
        bottomClues = listOf(2, 2, 3, null),
        leftClues = listOf(2, 2, null, 3),
        rightClues = listOf(2, 2, 3, null),
        solution = solutionHb4,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun stuckPuzzle4(): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-4-STUCK-TEST",
        size = 4,
        topClues = listOf(null, 2, null, null),
        bottomClues = Collections.nCopies(4, null),
        leftClues = Collections.nCopies(4, null),
        rightClues = Collections.nCopies(4, null),
        givens = List(16) { index ->
            when (index) {
                0 -> 1
                7 -> 1
                12 -> 4
                14 -> 2
                else -> null
            }
        },
        solution = listOf("1342", "2431", "3214", "4123").flatMap { row -> row.map(Char::digitToInt) },
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun puzzle5(): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-5-TEST",
        size = 5,
        topClues = listOf(5, 4, 3, 2, 1),
        bottomClues = listOf(1, 2, 2, 2, 2),
        leftClues = listOf(5, 4, 3, 2, 1),
        rightClues = listOf(1, 2, 2, 2, 2),
        solution = solution5,
        pack = Pack.MEDIUM,
        storedGrade = "pending",
    )

    private fun puzzle6(): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-6-TEST",
        size = 6,
        topClues = listOf(6, 5, 4, 3, 2, 1),
        bottomClues = listOf(1, 2, 2, 2, 2, 2),
        leftClues = listOf(6, 5, 4, 3, 2, 1),
        rightClues = listOf(1, 2, 2, 2, 2, 2),
        solution = solution6,
        pack = Pack.HARD,
        storedGrade = "pending",
    )

    private fun puzzle7(): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-7-TEST",
        size = 7,
        topClues = listOf(7, 6, 5, 4, 3, 2, 1),
        bottomClues = listOf(1, 2, 2, 2, 2, 2, 2),
        leftClues = listOf(7, 6, 5, 4, 3, 2, 1),
        rightClues = listOf(1, 2, 2, 2, 2, 2, 2),
        solution = solution7,
        pack = Pack.EXPERT,
        storedGrade = "pending",
    )

    companion object {
        val solution4 = listOf(
            "1234",
            "2341",
            "3412",
            "4123",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val solutionHb4 = listOf(
            "2143",
            "3412",
            "4231",
            "1324",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val solution5 = listOf(
            "12345",
            "23451",
            "34512",
            "45123",
            "51234",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val solution6 = listOf(
            "123456",
            "234561",
            "345612",
            "456123",
            "561234",
            "612345",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val solution7 = listOf(
            "1234567",
            "2345671",
            "3456712",
            "4567123",
            "5671234",
            "6712345",
            "7123456",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
