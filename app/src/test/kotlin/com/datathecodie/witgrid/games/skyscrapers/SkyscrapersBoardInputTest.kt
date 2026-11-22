package com.datathecodie.witgrid.games.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections

class SkyscrapersBoardInputTest {

    private fun testState(
        size: Int = 4,
        givenCellIndex: Int? = null,
        givenDigit: Int = 1,
    ): SkyscrapersState {
        val givens = MutableList<Int?>(size * size) { null }
        if (givenCellIndex != null) {
            givens[givenCellIndex] = givenDigit
        }
        val solution = (0 until size * size).map { (it % size) + 1 }
        val puzzle = SkyscrapersPuzzle(
            id = "SK-TEST-$size",
            size = size,
            topClues = Collections.nCopies(size, null),
            bottomClues = Collections.nCopies(size, null),
            leftClues = Collections.nCopies(size, null),
            rightClues = Collections.nCopies(size, null),
            solution = solution,
            pack = when (size) {
                4 -> Pack.EASY
                5 -> Pack.MEDIUM
                6 -> Pack.HARD
                else -> Pack.EXPERT
            },
            storedGrade = "none:0",
            givens = givens,
        )
        return SkyscrapersState.initial(puzzle)
    }

    @Test
    fun digitEntryUpdatesEditableCellAndLeavesGivensFixed() {
        val state = testState(givenCellIndex = 0, givenDigit = 1)
        val editableCell = Cell(1)

        val moves = SkyscrapersBoardInput.digitMoves(state, editableCell, 4, autoNotesEnabled = false)
        val updatedState = moves.fold(state, SkyscrapersState::withMove)

        assertEquals(listOf(Move.SetDigit(editableCell, 4)), moves)
        assertEquals(4, updatedState.digits[editableCell.index])
        assertEquals(emptyList<Move>(), SkyscrapersBoardInput.digitMoves(state, Cell(0), 2, false))
        assertEquals(1, updatedState.digits[0])
    }

    @Test
    fun settingSameDigitReturnsEmptyList() {
        val state = testState().withMove(Move.SetDigit(Cell(2), 3))
        assertEquals(emptyList<Move>(), SkyscrapersBoardInput.digitMoves(state, Cell(2), 3, false))
    }

    @Test
    fun digitEntryRejectsOutOfRangeOrCellOutsideBoard() {
        val state = testState(size = 4)
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersBoardInput.digitMoves(state, Cell(0), 5, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersBoardInput.digitMoves(state, Cell(0), -1, false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersBoardInput.digitMoves(state, Cell(16), 1, false)
        }
    }

    @Test
    fun settingDigitClearsNotesInCell() {
        val state = testState().withMove(Move.SetNotes(Cell(3), 7))
        assertEquals(7, state.notes[3])

        val moves = SkyscrapersBoardInput.digitMoves(state, Cell(3), 2, autoNotesEnabled = false)
        val updatedState = moves.fold(state, SkyscrapersState::withMove)

        assertEquals(listOf(Move.SetDigit(Cell(3), 2)), moves)
        assertEquals(2, updatedState.digits[3])
        assertEquals(0, updatedState.notes[3])
    }

    @Test
    fun noteModeTogglesOneDigitBitOnAndOff() {
        val state = testState(size = 4)
        val cell = Cell(5)
        val bit = 1 shl (3 - 1)

        val add = SkyscrapersBoardInput.noteMoves(state, cell, 3)
        assertEquals(listOf(Move.SetNotes(cell, bit)), add)
        val notedState = add.fold(state, SkyscrapersState::withMove)
        assertEquals(bit, notedState.notes[cell.index])

        val remove = SkyscrapersBoardInput.noteMoves(notedState, cell, 3)
        assertEquals(listOf(Move.SetNotes(cell, 0)), remove)
        assertEquals(0, notedState.withMove(remove.single()).notes[cell.index])
    }

    @Test
    fun noteMovesRejectsOutOfRangeOrGivenOrFilledCell() {
        val state = testState(size = 4, givenCellIndex = 0, givenDigit = 2)
            .withMove(Move.SetDigit(Cell(1), 3))

        assertEquals(emptyList<Move>(), SkyscrapersBoardInput.noteMoves(state, Cell(0), 1))
        assertEquals(emptyList<Move>(), SkyscrapersBoardInput.noteMoves(state, Cell(1), 1))

        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersBoardInput.noteMoves(state, Cell(2), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersBoardInput.noteMoves(state, Cell(2), 5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersBoardInput.noteMoves(state, Cell(16), 1)
        }
    }

    @Test
    fun autoNotesRemoveEnteredDigitFromRowAndColumnPeers() {
        val noteBit = 1 shl (2 - 1)
        var state = testState(size = 4)
        // Set note '2' in same row (1, 2, 3), same column (4, 8, 12), and diagonal (5)
        listOf(1, 2, 4, 5, 8).forEach { index ->
            state = state.withMove(Move.SetNotes(Cell(index), noteBit))
        }

        val moves = SkyscrapersBoardInput.digitMoves(state, Cell(0), 2, autoNotesEnabled = true)
        val updatedState = moves.fold(state, SkyscrapersState::withMove)

        assertEquals(Move.SetDigit(Cell(0), 2), moves.first())
        assertEquals(0, updatedState.notes[1])
        assertEquals(0, updatedState.notes[2])
        assertEquals(0, updatedState.notes[4])
        assertEquals(0, updatedState.notes[8])
        // Diagonal cell (5) is not a peer, so its note bit must remain
        assertEquals(noteBit, updatedState.notes[5])
        assertEquals(5, moves.size)
    }

    @Test
    fun autoNotesSettingOffPreservesPeerNotes() {
        val state = testState(size = 4)
            .withMove(Move.SetNotes(Cell(0), 7))
            .withMove(Move.SetNotes(Cell(1), 7))

        val moves = SkyscrapersBoardInput.digitMoves(state, Cell(0), 2, autoNotesEnabled = false)
        val updatedState = moves.fold(state, SkyscrapersState::withMove)

        assertEquals(listOf(Move.SetDigit(Cell(0), 2)), moves)
        assertEquals(0, updatedState.notes[0])
        assertEquals(7, updatedState.notes[1])
    }

    @Test
    fun clearMovesRemoveDigitOrNotesFromEditableCells() {
        val cellWithDigit = Cell(6)
        val cellWithNotes = Cell(7)
        val givenCell = Cell(0)
        var state = testState(size = 4, givenCellIndex = 0, givenDigit = 4)
            .withMove(Move.SetDigit(cellWithDigit, 2))
            .withMove(Move.SetNotes(cellWithNotes, 5))

        val clearDigit = SkyscrapersBoardInput.clearMoves(state, cellWithDigit)
        state = state.withMove(clearDigit.single())
        val clearNotes = SkyscrapersBoardInput.clearMoves(state, cellWithNotes)

        assertEquals(listOf(Move.SetDigit(cellWithDigit, 0)), clearDigit)
        assertEquals(null, state.digits[cellWithDigit.index])
        assertEquals(listOf(Move.SetNotes(cellWithNotes, 0)), clearNotes)
        assertEquals(emptyList<Move>(), SkyscrapersBoardInput.clearMoves(state, Cell(10)))
        assertEquals(emptyList<Move>(), SkyscrapersBoardInput.clearMoves(state, givenCell))
    }

    @Test
    fun isPeerIdentifiesRowAndColumnPeersOnly() {
        val size = 4
        // Cell 0 is at (0, 0)
        assertTrue(SkyscrapersBoardInput.isPeer(0, 1, size)) // Same row
        assertTrue(SkyscrapersBoardInput.isPeer(0, 3, size)) // Same row
        assertTrue(SkyscrapersBoardInput.isPeer(0, 4, size)) // Same col
        assertTrue(SkyscrapersBoardInput.isPeer(0, 12, size)) // Same col
        assertFalse(SkyscrapersBoardInput.isPeer(0, 5, size)) // Diagonal (1, 1)
        assertFalse(SkyscrapersBoardInput.isPeer(0, 10, size)) // Other row and col
    }

    @Test
    fun clueSatisfactionIdentifiesSatisfiedAndUnsatisfiedClues() {
        // Line with nulls and max building 4 at index 0 satisfies clue 1
        assertTrue(isClueSatisfied(listOf(4, null, null, null), clue = 1, size = 4))
        // Max building 4 at index 1 with prefix [2, 4] satisfies clue 2
        assertTrue(isClueSatisfied(listOf(2, 4, null, null), clue = 2, size = 4))
        // Clue 3 not satisfied by [2, 4]
        assertFalse(isClueSatisfied(listOf(2, 4, null, null), clue = 3, size = 4))
        // Null clue returns false
        assertFalse(isClueSatisfied(listOf(4, 3, 2, 1), clue = null, size = 4))
        // Completely filled line [1, 2, 3, 4] satisfies clue 4
        assertTrue(isClueSatisfied(listOf(1, 2, 3, 4), clue = 4, size = 4))
        // Completely filled line [4, 3, 2, 1] satisfies clue 1
        assertTrue(isClueSatisfied(listOf(4, 3, 2, 1), clue = 1, size = 4))
        // Line with duplicates [4, 4, null, null] does not satisfy clue 1
        assertFalse(isClueSatisfied(listOf(4, 4, null, null), clue = 1, size = 4))
    }
}
