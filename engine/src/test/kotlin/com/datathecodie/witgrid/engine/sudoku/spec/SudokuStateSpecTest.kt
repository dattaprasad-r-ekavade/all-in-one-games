package com.datathecodie.witgrid.engine.sudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Independent Sudoku 9x9 input rules: digits, notes and fixed givens. */
class SudokuStateSpecTest {
    @Test
    fun settingADigitClearsThatCellsNotesAndZeroClearsTheDigit() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val noted = SudokuState.initial(puzzle).withMove(Move.SetNotes(Cell(2), 0b10101))

        val entered = noted.withMove(Move.SetDigit(Cell(2), 4))
        assertEquals(4, entered.digits[2])
        assertEquals(0, entered.notes[2])

        val cleared = entered.withMove(Move.SetDigit(Cell(2), 0))
        assertNull(cleared.digits[2])
    }

    @Test
    fun setNotesReplacesAllNotesAndZeroClearsThem() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val initial = SudokuState.initial(puzzle)

        val first = initial.withMove(Move.SetNotes(Cell(2), 0b000000111))
        val replaced = first.withMove(Move.SetNotes(Cell(2), 0b100000000))
        val cleared = replaced.withMove(Move.SetNotes(Cell(2), 0))

        assertEquals(0b100000000, replaced.notes[2])
        assertEquals(0, cleared.notes[2])
    }

    @Test
    fun givensCannotBeChangedOrNoted() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val initial = SudokuState.initial(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(0), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(0), 1))
        }
    }

    @Test
    fun notesCannotBeSetInAFilledCell() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val filled = SudokuState.initial(puzzle).withMove(Move.SetDigit(Cell(2), 4))

        assertThrows(IllegalArgumentException::class.java) {
            filled.withMove(Move.SetNotes(Cell(2), 1))
        }
    }

    @Test
    fun digitsAndNoteBitsStayInsideOneThroughNine() {
        val puzzle = SudokuSpecFixtures.puzzle()
        val initial = SudokuState.initial(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(2), 10))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(2), 1 shl 9))
        }
        assertEquals(0b100000000, initial.withMove(Move.SetNotes(Cell(2), 1 shl 8)).notes[2])
    }
}
