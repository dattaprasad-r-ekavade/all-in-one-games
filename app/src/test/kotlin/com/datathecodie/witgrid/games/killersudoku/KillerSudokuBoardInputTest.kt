package com.datathecodie.witgrid.games.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillerSudokuBoardInputTest {
    @Test
    fun digitEntryUpdatesEditableCellAndLeavesGivensFixed() {
        val state = testState(givenCellIndex = 0)
        val cell = Cell(1)

        val moves = KillerSudokuBoardInput.digitMoves(state, cell, 4, autoNotesEnabled = false)
        val updatedState = moves.fold(state, KillerSudokuState::withMove)

        assertEquals(listOf(Move.SetDigit(cell, 4)), moves)
        assertEquals(4, updatedState.digits[cell.index])
        assertEquals(emptyList<Move>(), KillerSudokuBoardInput.digitMoves(state, Cell(0), 2, false))
        assertEquals(KillerSudokuBoardFixtures.solution[0], updatedState.digits[0])
    }

    @Test
    fun noteModeTogglesOneDigitBitOnAndOff() {
        val state = testState()
        val cell = Cell(7)
        val bit = 1 shl (4 - 1)

        val add = KillerSudokuBoardInput.noteMoves(state, cell, 4)
        assertEquals(listOf(Move.SetNotes(cell, bit)), add)
        val notedState = add.fold(state, KillerSudokuState::withMove)
        val remove = KillerSudokuBoardInput.noteMoves(notedState, cell, 4)

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

        val moves = KillerSudokuBoardInput.digitMoves(state, Cell(0), 3, autoNotesEnabled = true)
        val updatedState = moves.fold(state, KillerSudokuState::withMove)

        assertEquals(Move.SetDigit(Cell(0), 3), moves.first())
        assertEquals(0, updatedState.notes[1])
        assertEquals(0, updatedState.notes[9])
        assertEquals(0, updatedState.notes[10])
        assertEquals(noteBit, updatedState.notes[80])
        assertEquals(4, moves.size)
    }

    @Test
    fun autoNotesRemoveEnteredDigitFromACagePeerOutsideTheHouse() {
        val givens = List<Int?>(KillerSudokuBoardFixtures.solution.size) { null }
        val puzzle = KillerSudokuBoardFixtures.puzzle(
            id = "KS-CAGE-PEER",
            givens = givens,
            cages = KillerSudokuBoardFixtures.cagesLinking(0, 80),
        )
        val noteBit = 1 shl (5 - 1)
        val state = KillerSudokuState.initial(puzzle).withMove(Move.SetNotes(Cell(80), noteBit))

        val moves = KillerSudokuBoardInput.digitMoves(state, Cell(0), 5, autoNotesEnabled = true)
        val updatedState = moves.fold(state, KillerSudokuState::withMove)

        assertEquals(Move.SetDigit(Cell(0), 5), moves.first())
        assertEquals(listOf(Move.SetNotes(Cell(80), 0)), moves.drop(1))
        assertEquals(0, updatedState.notes[80])
        assertTrue(moves.size == 2)
    }

    @Test
    fun autoNotesSettingOffPreservesPeerNotesWhileEntryClearsItsOwnNotes() {
        val state = testState()
            .withMove(Move.SetNotes(Cell(0), 7))
            .withMove(Move.SetNotes(Cell(1), 7))

        val moves = KillerSudokuBoardInput.digitMoves(state, Cell(0), 3, autoNotesEnabled = false)
        val updatedState = moves.fold(state, KillerSudokuState::withMove)

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

        val clearDigit = KillerSudokuBoardInput.clearMoves(state, cellWithDigit)
        state = state.withMove(clearDigit.single())
        val clearNotes = KillerSudokuBoardInput.clearMoves(state, cellWithNotes)

        assertEquals(listOf(Move.SetDigit(cellWithDigit, 0)), clearDigit)
        assertEquals(null, state.digits[cellWithDigit.index])
        assertEquals(listOf(Move.SetNotes(cellWithNotes, 0)), clearNotes)
        assertEquals(emptyList<Move>(), KillerSudokuBoardInput.clearMoves(state, Cell(12)))
    }

    @Test
    fun notesCannotBeChangedInGivensOrFilledCells() {
        val state = testState(givenCellIndex = 0).withMove(Move.SetDigit(Cell(1), 2))

        assertEquals(emptyList<Move>(), KillerSudokuBoardInput.noteMoves(state, Cell(0), 3))
        assertEquals(emptyList<Move>(), KillerSudokuBoardInput.noteMoves(state, Cell(1), 3))
        assertEquals(emptyList<Move>(), KillerSudokuBoardInput.clearMoves(state, Cell(0)))
    }

    @Test
    fun inputRejectsInvalidCellsAndDigits() {
        val state = testState()

        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuBoardInput.digitMoves(state, Cell(state.digits.size), 1, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuBoardInput.digitMoves(state, Cell(1), 10, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuBoardInput.noteMoves(state, Cell(1), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuBoardInput.noteMoves(state, Cell(1), 10)
        }
    }

    private fun testState(givenCellIndex: Int? = null): KillerSudokuState {
        val givens = List<Int?>(KillerSudokuBoardFixtures.solution.size) { index ->
            if (index == givenCellIndex) KillerSudokuBoardFixtures.solution[index] else null
        }
        return KillerSudokuBoardFixtures.state(givens)
    }
}
