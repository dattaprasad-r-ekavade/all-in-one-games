package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.Collections

class SkyscrapersRulesTest {

    @Test
    fun validatesLatinSquareAndAllCluesAcrossSupportedSizes() {
        // 4x4
        val puzzle4 = puzzle4()
        assertTrue(SkyscrapersRules.isValidSolution(puzzle4))
        // 5x5
        val puzzle5 = puzzle5()
        assertTrue(SkyscrapersRules.isValidSolution(puzzle5))
        // 6x6
        val puzzle6 = puzzle6()
        assertTrue(SkyscrapersRules.isValidSolution(puzzle6))
        // 7x7
        val puzzle7 = puzzle7()
        assertTrue(SkyscrapersRules.isValidSolution(puzzle7))
    }

    @Test
    fun rejectsInvalidLatinSquareOrClueViolations() {
        val puzzle = puzzle4()

        // Row collision
        assertFalse(
            SkyscrapersRules.isValidSolution(
                puzzle,
                solution4.toMutableList().also { it[0] = it[1] },
            ),
        )

        // Col collision
        assertFalse(
            SkyscrapersRules.isValidSolution(
                puzzle,
                solution4.toMutableList().also { it[0] = it[4] },
            ),
        )

        // Digit out of range
        assertFalse(
            SkyscrapersRules.isValidSolution(
                puzzle,
                solution4.toMutableList().also { it[0] = 5 },
            ),
        )

        // Wrong size
        assertFalse(SkyscrapersRules.isValidSolution(puzzle, solution4.dropLast(1)))

        // Violated top clue
        val badTopClue = puzzle4(topClues = listOf(1, 3, 2, 1))
        assertFalse(SkyscrapersRules.isValidSolution(badTopClue))

        // Violated bottom clue
        val badBottomClue = puzzle4(bottomClues = listOf(4, 2, 2, 2))
        assertFalse(SkyscrapersRules.isValidSolution(badBottomClue))

        // Violated left clue
        val badLeftClue = puzzle4(leftClues = listOf(1, 3, 2, 1))
        assertFalse(SkyscrapersRules.isValidSolution(badLeftClue))

        // Violated right clue
        val badRightClue = puzzle4(rightClues = listOf(4, 2, 2, 2))
        assertFalse(SkyscrapersRules.isValidSolution(badRightClue))

        // Mismatched given
        val badGiven = puzzle4(givens = List(16) { if (it == 0) 2 else null })
        assertFalse(SkyscrapersRules.isValidSolution(badGiven))
    }

    @Test
    fun countSolutionsReturnsZeroOneAndTwoAndHonorsCap() {
        // 0 solutions: contradictory clues (top clue 1 and bottom clue 1 on same column)
        val impossible = puzzle4(
            topClues = listOf(1, null, null, null),
            bottomClues = listOf(1, null, null, null),
        )
        assertEquals(0, SkyscrapersRules.countSolutions(impossible))

        // 0 solutions: impossible row clues
        val impossibleRow = puzzle4(
            leftClues = listOf(4, null, null, null),
            rightClues = listOf(4, null, null, null),
        )
        assertEquals(0, SkyscrapersRules.countSolutions(impossibleRow))

        // 0 solutions: given collision in column
        val impossibleGivens = puzzle4(
            givens = List(16) { if (it == 0 || it == 4) 1 else null },
        )
        assertEquals(0, SkyscrapersRules.countSolutions(impossibleGivens))

        // 1 solution: unique hand fixture
        val unique = puzzle4()
        assertEquals(1, SkyscrapersRules.countSolutions(unique))

        // 2 solutions: unconstrained puzzle with limit = 2
        val unconstrained = puzzle4(
            topClues = Collections.nCopies(4, null),
            bottomClues = Collections.nCopies(4, null),
            leftClues = Collections.nCopies(4, null),
            rightClues = Collections.nCopies(4, null),
        )
        assertEquals(2, SkyscrapersRules.countSolutions(unconstrained, limit = 2))
        assertEquals(1, SkyscrapersRules.countSolutions(unconstrained, limit = 1))

        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersRules.countSolutions(unique, limit = 0)
        }
    }

    @Test
    fun countSolutionsOnLargerSizesIsFastAndAccurate() {
        val unique5 = puzzle5()
        assertEquals(1, SkyscrapersRules.countSolutions(unique5, limit = 2))

        val unique6 = puzzle6()
        assertEquals(1, SkyscrapersRules.countSolutions(unique6, limit = 2))

        val unique7 = puzzle7()
        assertEquals(1, SkyscrapersRules.countSolutions(unique7, limit = 2))
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun countSolutionsPrunesPartialColumnsWithOnlyABottomClue() {
        val puzzle = SkyscrapersPuzzle(
            id = "SK-BOTTOM-ONLY",
            size = 7,
            topClues = List(7) { null },
            bottomClues = listOf(7, null, null, null, null, null, null),
            leftClues = List(7) { null },
            rightClues = List(7) { null },
            solution = solution7.chunked(7).asReversed().flatten(),
            pack = Pack.EXPERT,
            storedGrade = "pending",
        )

        assertTrue(SkyscrapersRules.isValidSolution(puzzle))
        assertEquals(2, SkyscrapersRules.countSolutions(puzzle, limit = 2))
    }

    @Test
    fun initialStateLocksGivensAndDigitMovesClearCellNotes() {
        val givens = List(16) { index -> if (index == 0) solution4[index] else null }
        val puzzle = puzzle4(givens = givens)
        val initial = SkyscrapersState.initial(puzzle)

        assertEquals(solution4[0], initial.digits[0])
        assertEquals(null, initial.digits[1])
        assertEquals(0, initial.notes[1])

        val noted = initial.withMove(Move.SetNotes(Cell(1), (1 shl 0) or (1 shl 2)))
        assertEquals((1 shl 0) or (1 shl 2), noted.notes[1])

        val entered = noted.withMove(Move.SetDigit(Cell(1), 3))
        assertEquals(3, entered.digits[1])
        assertEquals(0, entered.notes[1]) // SetDigit cleared notes

        val cleared = entered.withMove(Move.SetDigit(Cell(1), 0))
        assertEquals(null, cleared.digits[1])
        assertEquals(0, cleared.notes[1])

        // Givens cannot be changed
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(0), 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(0), 1))
        }

        // Cannot set notes on filled cell
        assertThrows(IllegalArgumentException::class.java) {
            entered.withMove(Move.SetNotes(Cell(1), 1))
        }

        // Invalid digit out of range
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(1), 5))
        }

        // Invalid note mask out of range
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(1), 1 shl 4))
        }

        // SetMark not supported
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(1), Mark.FILLED))
        }
    }

    @Test
    fun mistakesAndSolvedStateUseEveryEnteredDigitAndIgnoreNotes() {
        val givens = List(16) { index -> if (index == 0) solution4[index] else null }
        val puzzle = puzzle4(givens = givens)
        val initial = SkyscrapersState.initial(puzzle)

        val wrong = initial
            .withMove(Move.SetDigit(Cell(1), 4)) // wrong digit (should be 2)
            .withMove(Move.SetNotes(Cell(2), 1 shl 0))

        assertEquals(listOf(Cell(1)), SkyscrapersRules.mistakes(puzzle, wrong))
        assertFalse(SkyscrapersRules.isSolved(puzzle, wrong))

        // Fully solve board
        val solved = solution4.indices.filter { givens[it] == null }.fold(initial) { state, index ->
            state.withMove(Move.SetDigit(Cell(index), solution4[index]))
        }

        assertTrue(SkyscrapersRules.mistakes(puzzle, solved).isEmpty())
        assertTrue(SkyscrapersRules.isSolved(puzzle, solved))

        val incomplete = solved.withMove(Move.SetDigit(Cell(1), 0))
        assertFalse(SkyscrapersRules.isSolved(puzzle, incomplete))
    }

    @Test
    fun codecRoundTripsAndRejectsMalformedPayloadAndSolutions() {
        val puzzle = puzzle4()
        val line = SkyscrapersCodec.encode(puzzle)

        val decoded = SkyscrapersCodec.decode(line)
        assertEquals(puzzle, decoded)

        // With givens
        val withGivens = puzzle4(givens = List(16) { if (it == 0) solution4[0] else null })
        val lineWithGivens = SkyscrapersCodec.encode(withGivens)
        val decodedWithGivens = SkyscrapersCodec.decode(lineWithGivens)
        assertEquals(withGivens, decodedWithGivens)

        // Payload with 0s as empty clues
        val lineWithZeros = line.replace('.', '0')
        val decodedWithZeros = SkyscrapersCodec.decode(lineWithZeros)
        assertEquals(puzzle, decodedWithZeros)

        // Invalid payload
        val fields = line.split('|')
        val invalidPayload = fields.toMutableList().also { it[2] = "1/2/3" }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersCodec.decode(invalidPayload)
        }

        // Invalid digit in clue
        val invalidClueChar = fields.toMutableList().also {
            it[2] = "9.../..../..../...."
        }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersCodec.decode(invalidClueChar)
        }

        // Invalid board size
        val badSize = fields.toMutableList().also { it[1] = "8" }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersCodec.decode(badSize)
        }

        // Mismatched pack
        val badPack = fields.toMutableList().also { it[4] = Pack.EXPERT.name }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersCodec.decode(badPack)
        }
    }

    @Test
    fun engineExposesContract() {
        val puzzle = puzzle4()
        val line = SkyscrapersEngine.encodePuzzle(puzzle)
        val decoded = SkyscrapersEngine.decodePuzzle(line)
        val state = SkyscrapersEngine.applyMove(
            SkyscrapersEngine.initialState(decoded),
            Move.SetDigit(Cell(0), 4),
        )

        assertEquals("skyscrapers", SkyscrapersEngine.id)
        assertEquals(line, SkyscrapersEngine.encodePuzzle(decoded))
        assertEquals(1, SkyscrapersEngine.countSolutions(decoded, limit = 2))
        assertEquals(listOf(Cell(0)), SkyscrapersEngine.mistakes(decoded, state))
        assertFalse(SkyscrapersEngine.isSolved(decoded, state))

        assertEquals(SkyscrapersTechniqueSolver.nextStep(decoded, state), SkyscrapersEngine.nextStep(decoded, state))
        assertEquals(SkyscrapersTechniqueSolver.grade(decoded), SkyscrapersEngine.grade(decoded))
        assertEquals(Pack.EASY, SkyscrapersEngine.generate(SplitMix64(1L), Pack.EASY).pack)
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
