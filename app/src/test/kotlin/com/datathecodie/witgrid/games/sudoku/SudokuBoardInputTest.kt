package com.datathecodie.witgrid.games.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SudokuBoardInputTest {
    @Test
    fun digitEntryUpdatesEditableCellAndLeavesGivensFixed() {
        val state = testState(givenCellIndex = 0)
        val cell = Cell(1)

        val moves = SudokuBoardInput.digitMoves(state, cell, 4, autoNotesEnabled = false)
        val updatedState = moves.fold(state, SudokuState::withMove)

        assertEquals(listOf(Move.SetDigit(cell, 4)), moves)
        assertEquals(4, updatedState.digits[cell.index])
        assertEquals(emptyList<Move>(), SudokuBoardInput.digitMoves(state, Cell(0), 2, false))
        assertEquals(solution[0], updatedState.digits[0])
    }

    @Test
    fun noteModeTogglesOneDigitBitOnAndOff() {
        val state = testState()
        val cell = Cell(7)
        val bit = 1 shl (4 - 1)

        val add = SudokuBoardInput.noteMoves(state, cell, 4)
        assertEquals(listOf(Move.SetNotes(cell, bit)), add)
        val notedState = add.fold(state, SudokuState::withMove)
        val remove = SudokuBoardInput.noteMoves(notedState, cell, 4)

        assertEquals(listOf(Move.SetNotes(cell, 0)), remove)
        assertEquals(0, notedState.withMove(remove.single()).notes[cell.index])
    }

    @Test
    fun autoNotesRemoveEnteredDigitFromRowColumnAndBoxPeersAsOneGesture() {
        val noteBit = 1 shl (3 - 1)
        var state = testState()
        // Cells 1 (same row), 9 (same column), 10 (same 3x3 box, diff row/col), 80 (different row, col, box)
        listOf(1, 9, 10, 80).forEach { index ->
            state = state.withMove(Move.SetNotes(Cell(index), noteBit))
        }

        val moves = SudokuBoardInput.digitMoves(state, Cell(0), 3, autoNotesEnabled = true)
        val updatedState = moves.fold(state, SudokuState::withMove)

        assertEquals(Move.SetDigit(Cell(0), 3), moves.first())
        assertEquals(0, updatedState.notes[1])
        assertEquals(0, updatedState.notes[9])
        assertEquals(0, updatedState.notes[10])
        assertEquals(noteBit, updatedState.notes[80])
        assertEquals(4, moves.size)
    }

    @Test
    fun autoNotesSettingOffPreservesPeerNotesWhileEntryClearsItsOwnNotes() {
        val state = testState()
            .withMove(Move.SetNotes(Cell(0), 7))
            .withMove(Move.SetNotes(Cell(1), 7))

        val moves = SudokuBoardInput.digitMoves(state, Cell(0), 3, autoNotesEnabled = false)
        val updatedState = moves.fold(state, SudokuState::withMove)

        assertEquals(listOf(Move.SetDigit(Cell(0), 3)), moves)
        assertEquals(0, updatedState.notes[0])
        assertEquals(7, updatedState.notes[1])
    }

    @Test
    fun clearMovesRemoveDigitOrNotesFromEditableCells() {
        val cellWithDigit = Cell(10)
        val cellWithNotes = Cell(11)
        var state = testState()
            .withMove(Move.SetDigit(cellWithDigit, 2))
            .withMove(Move.SetNotes(cellWithNotes, 5))

        val clearDigit = SudokuBoardInput.clearMoves(state, cellWithDigit)
        state = state.withMove(clearDigit.single())
        val clearNotes = SudokuBoardInput.clearMoves(state, cellWithNotes)

        assertEquals(listOf(Move.SetDigit(cellWithDigit, 0)), clearDigit)
        assertEquals(null, state.digits[cellWithDigit.index])
        assertEquals(listOf(Move.SetNotes(cellWithNotes, 0)), clearNotes)
        assertEquals(emptyList<Move>(), SudokuBoardInput.clearMoves(state, Cell(12)))
    }

    @Test
    fun notesCannotBeChangedInGivensOrFilledCells() {
        val state = testState(givenCellIndex = 0).withMove(Move.SetDigit(Cell(1), 2))

        assertEquals(emptyList<Move>(), SudokuBoardInput.noteMoves(state, Cell(0), 3))
        assertEquals(emptyList<Move>(), SudokuBoardInput.noteMoves(state, Cell(1), 3))
        assertEquals(emptyList<Move>(), SudokuBoardInput.clearMoves(state, Cell(0)))
    }

    @Test
    fun inputRejectsInvalidCellsAndDigits() {
        val state = testState()

        assertThrows(IllegalArgumentException::class.java) {
            SudokuBoardInput.digitMoves(state, Cell(state.digits.size), 1, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SudokuBoardInput.digitMoves(state, Cell(1), 10, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SudokuBoardInput.noteMoves(state, Cell(1), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SudokuBoardInput.noteMoves(state, Cell(1), 10)
        }
    }

    private fun testState(givenCellIndex: Int? = null): SudokuState {
        val givens = List<Int?>(solution.size) { index ->
            if (index == givenCellIndex) solution[index] else null
        }
        val puzzle = SudokuPuzzle(
            id = "SU-TEST-1",
            givens = givens,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "fixture",
        )
        return SudokuState.initial(puzzle)
    }

    private companion object {
        const val BOARD_SIZE = 9
        val solution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
