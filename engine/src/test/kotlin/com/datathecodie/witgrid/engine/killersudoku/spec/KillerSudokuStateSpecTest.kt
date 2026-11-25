package com.datathecodie.witgrid.engine.killersudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuRules
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.puzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Killer Sudoku move, mistake and completion cases from plan 12.7. */
class KillerSudokuStateSpecTest {
    @Test
    fun setDigitEntersAValueAndClearsThatCellsNotes() {
        val noted = KillerSudokuState.initial(puzzle()).withMove(Move.SetNotes(Cell(1), 0b101))
        assertEquals(0b101, noted.notes[1])

        val entered = noted.withMove(Move.SetDigit(Cell(1), 3))
        assertEquals(3, entered.digits[1])
        assertEquals(0, entered.notes[1])

        val cleared = entered.withMove(Move.SetDigit(Cell(1), 0))
        assertNull(cleared.digits[1])
        assertEquals(0, cleared.notes[1])
    }

    @Test
    fun setNotesReplacesTheWholeMaskAndZeroClearsIt() {
        val initial = KillerSudokuState.initial(puzzle())

        // Bits 0 and 8 are digits 1 and 9.
        val first = initial.withMove(Move.SetNotes(Cell(2), 0b1_0000_0001))
        assertEquals(257, first.notes[2])

        val replaced = first.withMove(Move.SetNotes(Cell(2), 0b10))
        assertEquals(0b10, replaced.notes[2])

        val cleared = replaced.withMove(Move.SetNotes(Cell(2), 0))
        assertEquals(0, cleared.notes[2])
    }

    @Test
    fun givenCellsCannotChangeAndMovesStayInsideTheBoardAndDigitRange() {
        val givens = List(81) { index -> if (index == 0) 5 else null }
        val state = KillerSudokuState.initial(puzzle(givens = givens))

        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetDigit(Cell(0), 2)) }
        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetDigit(Cell(0), 0)) }
        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetNotes(Cell(0), 1)) }
        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetDigit(Cell(1), 10)) }
        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetNotes(Cell(1), 1 shl 9)) }
        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetDigit(Cell(81), 1)) }
        assertThrows(IllegalArgumentException::class.java) { state.withMove(Move.SetMark(Cell(1), Mark.FILLED)) }
    }

    @Test
    fun mistakesCountWrongEnteredDigitsAndIgnoreNotes() {
        val fixture = puzzle()
        val state = KillerSudokuState.initial(fixture)
            .withMove(Move.SetDigit(Cell(0), 5))
            .withMove(Move.SetDigit(Cell(1), 9))
            .withMove(Move.SetNotes(Cell(2), 0b1))

        // Cell 0 is correct (5). Cell 1 is wrong: its solution digit is 3.
        assertEquals(listOf(Cell(1)), KillerSudokuRules.mistakes(fixture, state))

        val corrected = state.withMove(Move.SetDigit(Cell(1), 0))
        assertEquals(emptyList<Cell>(), KillerSudokuRules.mistakes(fixture, corrected))
    }

    @Test
    fun solvedRequiresEveryCellToHoldItsSolutionDigit() {
        val fixture = puzzle()
        val initial = KillerSudokuState.initial(fixture)
        val solved = fill(fixture, solution)

        assertTrue(KillerSudokuRules.isSolved(fixture, solved))
        assertFalse(KillerSudokuRules.isSolved(fixture, initial))

        // Cell 80 holds 9 in the solution.
        val oneWrong = solved.withMove(Move.SetDigit(Cell(80), 1))
        assertFalse(KillerSudokuRules.isSolved(fixture, oneWrong))
        assertEquals(listOf(Cell(80)), KillerSudokuRules.mistakes(fixture, oneWrong))

        val oneEmpty = solved.withMove(Move.SetDigit(Cell(80), 0))
        assertFalse(KillerSudokuRules.isSolved(fixture, oneEmpty))
        assertTrue(KillerSudokuRules.mistakes(fixture, oneEmpty).isEmpty())
    }

    private fun fill(
        fixture: KillerSudokuPuzzle,
        values: List<Int>,
    ): KillerSudokuState = values.indices.fold(KillerSudokuState.initial(fixture)) { state, index ->
        state.withMove(Move.SetDigit(Cell(index), values[index]))
    }
}
