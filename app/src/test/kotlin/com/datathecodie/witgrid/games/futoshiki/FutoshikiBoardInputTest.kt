package com.datathecodie.witgrid.games.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiPuzzle
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections

class FutoshikiBoardInputTest {

    private fun testState(
        size: Int = 5,
        givenCellIndex: Int? = null,
        givenDigit: Int = 1,
    ): FutoshikiState {
        val givens = MutableList<Int?>(size * size) { null }
        if (givenCellIndex != null) {
            givens[givenCellIndex] = givenDigit
        }
        val horizontalSigns = Collections.nCopies(size * (size - 1), FutoshikiSign.NONE)
        val verticalSigns = Collections.nCopies((size - 1) * size, FutoshikiSign.NONE)
        val solution = (0 until size * size).map { (it % size) + 1 }
        val puzzle = FutoshikiPuzzle(
            id = "FU-TEST-$size",
            size = size,
            givens = givens,
            horizontalSigns = horizontalSigns,
            verticalSigns = verticalSigns,
            solution = solution,
            pack = when (size) {
                5 -> Pack.EASY
                6 -> Pack.MEDIUM
                else -> Pack.HARD
            },
            storedGrade = "none:0",
        )
        return FutoshikiState.initial(puzzle)
    }

    @Test
    fun digitEntryUpdatesEditableCellAndLeavesGivensFixed() {
        val state = testState(givenCellIndex = 0, givenDigit = 1)
        val editableCell = Cell(1)

        val moves = FutoshikiBoardInput.digitMoves(state, editableCell, 4, autoNotesEnabled = false)
        val updatedState = moves.fold(state, FutoshikiState::withMove)

        assertEquals(listOf(Move.SetDigit(editableCell, 4)), moves)
        assertEquals(4, updatedState.digits[editableCell.index])
        assertEquals(emptyList<Move>(), FutoshikiBoardInput.digitMoves(state, Cell(0), 2, false))
        assertEquals(1, updatedState.digits[0])
    }

    @Test
    fun settingSameDigitReturnsEmptyList() {
        val state = testState().withMove(Move.SetDigit(Cell(2), 3))
        assertEquals(emptyList<Move>(), FutoshikiBoardInput.digitMoves(state, Cell(2), 3, false))
    }

    @Test
    fun digitEntryRejectsOutOfRangeOrCellOutsideBoard() {
        val state = testState(size = 5)
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiBoardInput.digitMoves(state, Cell(0), 6, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiBoardInput.digitMoves(state, Cell(0), -1, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiBoardInput.digitMoves(state, Cell(25), 1, false)
        }
    }

    @Test
    fun settingDigitClearsNotesInCell() {
        val state = testState().withMove(Move.SetNotes(Cell(3), 7))
        assertEquals(7, state.notes[3])

        val moves = FutoshikiBoardInput.digitMoves(state, Cell(3), 2, autoNotesEnabled = false)
        val updatedState = moves.fold(state, FutoshikiState::withMove)

        assertEquals(listOf(Move.SetDigit(Cell(3), 2)), moves)
        assertEquals(2, updatedState.digits[3])
        assertEquals(0, updatedState.notes[3])
    }

    @Test
    fun noteModeTogglesOneDigitBitOnAndOff() {
        val state = testState(size = 5)
        val cell = Cell(5)
        val bit = 1 shl (3 - 1)

        val add = FutoshikiBoardInput.noteMoves(state, cell, 3)
        assertEquals(listOf(Move.SetNotes(cell, bit)), add)
        val notedState = add.fold(state, FutoshikiState::withMove)
        assertEquals(bit, notedState.notes[cell.index])

        val remove = FutoshikiBoardInput.noteMoves(notedState, cell, 3)
        assertEquals(listOf(Move.SetNotes(cell, 0)), remove)
        assertEquals(0, notedState.withMove(remove.single()).notes[cell.index])
    }

    @Test
    fun noteMovesRejectsOutOfRangeOrGivenOrFilledCell() {
        val state = testState(size = 5, givenCellIndex = 0, givenDigit = 2)
            .withMove(Move.SetDigit(Cell(1), 3))

        assertEquals(emptyList<Move>(), FutoshikiBoardInput.noteMoves(state, Cell(0), 1))
        assertEquals(emptyList<Move>(), FutoshikiBoardInput.noteMoves(state, Cell(1), 1))

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiBoardInput.noteMoves(state, Cell(2), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiBoardInput.noteMoves(state, Cell(2), 6)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiBoardInput.noteMoves(state, Cell(25), 1)
        }
    }

    @Test
    fun autoNotesRemoveEnteredDigitFromRowAndColumnPeers() {
        val noteBit = 1 shl (2 - 1)
        var state = testState(size = 5)
        // Set note '2' in same row (1, 2, 3), same column (5, 10, 15), and diagonal (6)
        listOf(1, 2, 3, 5, 6, 10, 15).forEach { index ->
            state = state.withMove(Move.SetNotes(Cell(index), noteBit))
        }

        val moves = FutoshikiBoardInput.digitMoves(state, Cell(0), 2, autoNotesEnabled = true)
        val updatedState = moves.fold(state, FutoshikiState::withMove)

        assertEquals(Move.SetDigit(Cell(0), 2), moves.first())
        assertEquals(0, updatedState.notes[1])
        assertEquals(0, updatedState.notes[2])
        assertEquals(0, updatedState.notes[3])
        assertEquals(0, updatedState.notes[5])
        assertEquals(0, updatedState.notes[10])
        assertEquals(0, updatedState.notes[15])
        // Diagonal cell (6) is not a peer, so its note bit must remain
        assertEquals(noteBit, updatedState.notes[6])
        assertEquals(7, moves.size)
    }

    @Test
    fun autoNotesSettingOffPreservesPeerNotes() {
        val state = testState(size = 5)
            .withMove(Move.SetNotes(Cell(0), 7))
            .withMove(Move.SetNotes(Cell(1), 7))

        val moves = FutoshikiBoardInput.digitMoves(state, Cell(0), 2, autoNotesEnabled = false)
        val updatedState = moves.fold(state, FutoshikiState::withMove)

        assertEquals(listOf(Move.SetDigit(Cell(0), 2)), moves)
        assertEquals(0, updatedState.notes[0])
        assertEquals(7, updatedState.notes[1])
    }

    @Test
    fun clearMovesRemoveDigitOrNotesFromEditableCells() {
        val cellWithDigit = Cell(6)
        val cellWithNotes = Cell(7)
        val givenCell = Cell(0)
        var state = testState(size = 5, givenCellIndex = 0, givenDigit = 4)
            .withMove(Move.SetDigit(cellWithDigit, 2))
            .withMove(Move.SetNotes(cellWithNotes, 5))

        val clearDigit = FutoshikiBoardInput.clearMoves(state, cellWithDigit)
        state = state.withMove(clearDigit.single())
        val clearNotes = FutoshikiBoardInput.clearMoves(state, cellWithNotes)

        assertEquals(listOf(Move.SetDigit(cellWithDigit, 0)), clearDigit)
        assertEquals(null, state.digits[cellWithDigit.index])
        assertEquals(listOf(Move.SetNotes(cellWithNotes, 0)), clearNotes)
        assertEquals(emptyList<Move>(), FutoshikiBoardInput.clearMoves(state, Cell(10)))
        assertEquals(emptyList<Move>(), FutoshikiBoardInput.clearMoves(state, givenCell))
    }

    @Test
    fun isPeerIdentifiesRowAndColumnPeersOnly() {
        val size = 5
        // Cell 0 is at (0, 0)
        assertTrue(FutoshikiBoardInput.isPeer(0, 1, size)) // Same row
        assertTrue(FutoshikiBoardInput.isPeer(0, 4, size)) // Same row
        assertTrue(FutoshikiBoardInput.isPeer(0, 5, size)) // Same col
        assertTrue(FutoshikiBoardInput.isPeer(0, 20, size)) // Same col
        assertFalse(FutoshikiBoardInput.isPeer(0, 6, size)) // Diagonal (1, 1)
        assertFalse(FutoshikiBoardInput.isPeer(0, 12, size)) // Other row and col
    }

    @Test
    fun horizontalInequalityIdentifiesBrokenAndValidStates() {
        // LESS: left < right
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.LESS, 2, 4))
        assertTrue(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.LESS, 4, 2))
        assertTrue(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.LESS, 3, 3))
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.LESS, null, 4))
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.LESS, 2, null))
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.LESS, null, null))

        // GREATER: left > right
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.GREATER, 4, 2))
        assertTrue(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.GREATER, 2, 4))
        assertTrue(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.GREATER, 3, 3))
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.GREATER, null, 2))
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.GREATER, 4, null))

        // NONE
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.NONE, 4, 2))
        assertFalse(FutoshikiBoardInput.isHorizontalBroken(FutoshikiSign.NONE, 2, 4))
    }

    @Test
    fun verticalInequalityIdentifiesBrokenAndValidStates() {
        // LESS: top < bottom
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.LESS, 1, 5))
        assertTrue(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.LESS, 5, 1))
        assertTrue(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.LESS, 3, 3))
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.LESS, null, 5))
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.LESS, 1, null))

        // GREATER: top > bottom
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.GREATER, 5, 1))
        assertTrue(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.GREATER, 1, 5))
        assertTrue(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.GREATER, 3, 3))
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.GREATER, null, 1))
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.GREATER, 5, null))

        // NONE
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.NONE, 5, 1))
        assertFalse(FutoshikiBoardInput.isVerticalBroken(FutoshikiSign.NONE, 1, 5))
    }
}
