package com.datathecodie.witgrid.engine.binary.spec

import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinaryRules
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Binary rule cases, using hand-built boards rather than generator output. */
class BinaryRulesSpecTest {
    @Test
    fun validBoardAndEachLineRuleAreCheckedFromHandBuiltGrids() {
        val puzzle = puzzle()

        assertTrue(BinaryRules.isValidSolution(puzzle))

        // Swapping opposite symbols within a row changes only column counts.
        val unequalColumns = board("101001", "001101", "110010", "010011", "101100", "110100")
        assertFalse(BinaryRules.isValidSolution(puzzle, unequalColumns), "Unequal column counts")
        assertFalse(BinaryRules.isValidSolution(puzzle, transpose(unequalColumns)), "Unequal row counts")

        // Swapping the first two rows preserves counts and distinctness, but creates column triples.
        val columnTriples = board("001101", "001011", "110010", "010011", "101100", "110100")
        assertFalse(BinaryRules.isValidSolution(puzzle, columnTriples), "Three adjacent column symbols")
        assertFalse(BinaryRules.isValidSolution(puzzle, transpose(columnTriples)), "Three adjacent row symbols")

        // Rows three and six match; all counts, adjacency rules and column distinctness still hold.
        val repeatedRows = board("001011", "001101", "110010", "010101", "101100", "110010")
        assertFalse(BinaryRules.isValidSolution(puzzle, repeatedRows), "Repeated rows")
        assertFalse(BinaryRules.isValidSolution(puzzle, transpose(repeatedRows)), "Repeated columns")
    }

    @Test
    fun solutionCounterDistinguishesZeroOneAndMultipleSolutions() {
        val impossible = puzzle(givens = List(36) { index -> if (index in 0..2) 0 else null })
        val unique = puzzle(givens = solution.mapIndexed { index, value -> if (index == 2) null else value })
        val multiple = puzzle(givens = List(36) { null })

        assertEquals(0, BinaryRules.countSolutions(impossible, limit = 2))
        assertEquals(1, BinaryRules.countSolutions(unique, limit = 2))
        assertEquals(2, BinaryRules.countSolutions(multiple, limit = 2))
    }

    @Test
    fun marksMapToTwoSymbolsGivensStayLockedAndWrongEntriesAreMistakes() {
        val givens = List(36) { index -> if (index == 0 || index == 5) solution[index] else null }
        val puzzle = puzzle(givens)
        val initial = BinaryState.initial(puzzle)

        assertEquals(BinarySymbol.A, initial.symbols[0])
        assertEquals(BinarySymbol.B, initial.symbols[5])
        assertEquals(BinarySymbol.EMPTY, initial.symbols[1])
        assertTrue(BinaryRules.mistakes(puzzle, initial).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(0), Mark.CROSSED))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(5), Mark.FILLED))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(0), Mark.EMPTY))
        }

        val state = initial
            .withMove(Move.SetMark(Cell(1), Mark.CROSSED))
            .withMove(Move.SetMark(Cell(2), Mark.FILLED))

        assertEquals(BinarySymbol.B, state.symbols[1])
        assertEquals(BinarySymbol.A, state.symbols[2])
        assertEquals(BinarySymbol.EMPTY, state.symbols[3])
        assertEquals(listOf(Cell(1), Cell(2)), BinaryRules.mistakes(puzzle, state))

        val corrected = state
            .withMove(Move.SetMark(Cell(1), Mark.EMPTY))
            .withMove(Move.SetMark(Cell(2), Mark.CROSSED))
        assertEquals(BinarySymbol.EMPTY, corrected.symbols[1])
        assertTrue(BinaryRules.mistakes(puzzle, corrected).isEmpty())
        assertEquals(BinarySymbol.EMPTY, initial.symbols[2])
    }

    @Test
    fun solvedBoardMustMatchEverySymbol() {
        val puzzle = puzzle(givens = List(36) { index -> if (index % 5 == 0) solution[index] else null })
        val completed = solution.indices.fold(BinaryState.initial(puzzle)) { state, index ->
            state.withMove(
                Move.SetMark(
                    Cell(index),
                    if (solution[index] == 0) Mark.FILLED else Mark.CROSSED,
                ),
            )
        }
        val incorrect = completed.withMove(Move.SetMark(Cell(2), Mark.FILLED))

        assertTrue(BinaryRules.isSolved(puzzle, completed))
        assertTrue(BinaryRules.mistakes(puzzle, completed).isEmpty())
        assertFalse(BinaryRules.isSolved(puzzle, BinaryState.initial(puzzle)))
        assertFalse(BinaryRules.isSolved(puzzle, incorrect))
        assertFalse(BinaryRules.isSolved(puzzle, completed.withMove(Move.SetMark(Cell(2), Mark.EMPTY))))
    }

    private fun puzzle(givens: List<Int?> = List(36) { null }): BinaryPuzzle = BinaryPuzzle(
        id = "BI-SPEC-6",
        size = 6,
        givens = givens,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun board(vararg rows: String): List<Int> = rows.flatMap { row -> row.map(Char::digitToInt) }

    private fun transpose(board: List<Int>): List<Int> = (0 until 36).map { index ->
        board[index % 6 * 6 + index / 6]
    }

    private companion object {
        val solution = listOf(
            "001011",
            "001101",
            "110010",
            "010011",
            "101100",
            "110100",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
