package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiniSudokuRulesTest {
    @Test
    fun validatesHandBuiltRowsColumnsAndTwoByThreeBoxes() {
        val puzzle = puzzle()

        assertTrue(MiniSudokuRules.isValidSolution(puzzle))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = it[1]
        }))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, solution.dropLast(1)))
        assertFalse(MiniSudokuRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = 4
        }))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoAndHonorsItsCap() {
        val conflictingGivens = List(36) { index -> if (index == 0 || index == 1) 1 else null }
        val impossible = puzzle(givens = conflictingGivens)
        val unique = puzzle(givens = solution)
        val multiple = puzzle()

        assertEquals(0, MiniSudokuRules.countSolutions(impossible))
        assertEquals(1, MiniSudokuRules.countSolutions(unique))
        assertEquals(2, MiniSudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(1, MiniSudokuRules.countSolutions(multiple, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            MiniSudokuRules.countSolutions(multiple, limit = 0)
        }
    }

    @Test
    fun initialStateLocksGivensAndDigitMovesClearCellNotes() {
        val givens = List(36) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens)
        val initial = MiniSudokuState.initial(puzzle)
        val noted = initial.withMove(Move.SetNotes(Cell(1), (1 shl 0) or (1 shl 5)))
        val entered = noted.withMove(Move.SetDigit(Cell(1), 4))
        val cleared = entered.withMove(Move.SetDigit(Cell(1), 0))

        assertEquals(solution[0], initial.digits[0])
        assertEquals(null, initial.digits[1])
        assertEquals(0, initial.notes[1])
        assertEquals((1 shl 0) or (1 shl 5), noted.notes[1])
        assertEquals(4, entered.digits[1])
        assertEquals(0, entered.notes[1])
        assertEquals(null, cleared.digits[1])
        assertEquals(0, cleared.notes[1])
        assertEquals(null, initial.digits[1])

        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(0), 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(0), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            entered.withMove(Move.SetNotes(Cell(1), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(1), 1 shl 6))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(1), 7))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(1), Mark.FILLED))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (entered.digits as MutableList<Int?>)[1] = null
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (noted.notes as MutableList<Int>)[1] = 0
        }
    }

    @Test
    fun mistakesAndSolvedStateUseEveryEnteredDigitAndIgnoreNotes() {
        val givens = List(36) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens)
        val initial = MiniSudokuState.initial(puzzle)
        val wrong = initial
            .withMove(Move.SetDigit(Cell(1), 3))
            .withMove(Move.SetNotes(Cell(2), 1 shl 0))

        assertEquals(listOf(Cell(1)), MiniSudokuRules.mistakes(puzzle, wrong))
        assertFalse(MiniSudokuRules.isSolved(puzzle, wrong))

        val solved = solution.indices.filter { givens[it] == null }.fold(initial) { state, index ->
            state.withMove(Move.SetDigit(Cell(index), solution[index]))
        }
        val incorrect = solved.withMove(Move.SetDigit(Cell(1), 1))
        val incomplete = solved.withMove(Move.SetDigit(Cell(2), 0))

        assertTrue(MiniSudokuRules.mistakes(puzzle, solved).isEmpty())
        assertTrue(MiniSudokuRules.isSolved(puzzle, solved))
        assertFalse(MiniSudokuRules.isSolved(puzzle, incorrect))
        assertFalse(MiniSudokuRules.isSolved(puzzle, incomplete))
    }

    @Test
    fun codecRoundTripsGivensAndRejectsMalformedPayloadAndSolutions() {
        val givens = solution.mapIndexed { index, digit -> if (index % 5 == 0) digit else null }
        val puzzle = puzzle(givens)
        val line = MiniSudokuCodec.encode(puzzle)
        val fields = line.split('|')
        val invalidPayload = fields.toMutableList().also { it[2] = "7" + it[2].drop(1) }.joinToString("|")
        val invalidSolution = fields.toMutableList().also { it[3] = "0" + it[3].drop(1) }.joinToString("|")
        val invalidGrid = fields.toMutableList().also { it[3] = "2" + it[3].drop(1) }.joinToString("|")
        val badSize = fields.toMutableList().also { it[1] = "5" }.joinToString("|")
        val mismatchedGiven = fields.toMutableList().also {
            it[2] = "6" + it[2].drop(1)
        }.joinToString("|")

        assertEquals(puzzle, MiniSudokuCodec.decode(line))
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuCodec.decode(invalidPayload) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuCodec.decode(invalidSolution) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuCodec.decode(invalidGrid) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuCodec.decode(badSize) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuCodec.decode(mismatchedGiven) }
    }

    @Test
    fun engineExposesMiniSudokuRulesAndCounter() {
        val puzzle = puzzle()
        val line = MiniSudokuEngine.encodePuzzle(puzzle)
        val decoded = MiniSudokuEngine.decodePuzzle(line)
        val state = MiniSudokuEngine.applyMove(
            MiniSudokuEngine.initialState(decoded),
            Move.SetDigit(Cell(0), 2),
        )

        assertEquals("minisudoku", MiniSudokuEngine.id)
        assertEquals(line, MiniSudokuEngine.encodePuzzle(decoded))
        assertEquals(2, MiniSudokuEngine.countSolutions(decoded, limit = 2))
        assertEquals(listOf(Cell(0)), MiniSudokuEngine.mistakes(decoded, state))
        assertFalse(MiniSudokuEngine.isSolved(decoded, state))
    }

    private fun puzzle(givens: List<Int?> = List(36) { null }): MiniSudokuPuzzle = MiniSudokuPuzzle(
        id = "MS-FIXTURE-6",
        givens = givens,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private companion object {
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
