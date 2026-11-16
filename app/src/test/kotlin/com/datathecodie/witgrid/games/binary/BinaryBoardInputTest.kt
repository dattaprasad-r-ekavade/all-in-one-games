package com.datathecodie.witgrid.games.binary

import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BinaryBoardInputTest {
    @Test
    fun tapCyclesEmptyToAThenBThenEmpty() {
        val cell = Cell(1)
        var state = testState()

        val enterA = BinaryBoardInput.tapMoves(state, cell)
        assertEquals(listOf(Move.SetMark(cell, Mark.FILLED)), enterA)
        state = state.withMove(enterA.single())

        val enterB = BinaryBoardInput.tapMoves(state, cell)
        assertEquals(listOf(Move.SetMark(cell, Mark.CROSSED)), enterB)
        state = state.withMove(enterB.single())

        val clear = BinaryBoardInput.tapMoves(state, cell)
        assertEquals(listOf(Move.SetMark(cell, Mark.EMPTY)), clear)
        assertEquals(BinarySymbol.EMPTY, state.withMove(clear.single()).symbols[cell.index])
    }

    @Test
    fun givensIgnoreTapsAndClearRequests() {
        val state = testState()

        assertEquals(BinarySymbol.B, state.symbols[0])
        assertEquals(emptyList<Move>(), BinaryBoardInput.tapMoves(state, Cell(0)))
        assertEquals(emptyList<Move>(), BinaryBoardInput.clearMoves(state, Cell(0)))
    }

    @Test
    fun longPressClearsPlayerSymbolsOnly() {
        val cell = Cell(1)
        val state = testState().withMove(Move.SetMark(cell, Mark.FILLED))

        assertEquals(listOf(Move.SetMark(cell, Mark.EMPTY)), BinaryBoardInput.clearMoves(state, cell))
        assertEquals(emptyList<Move>(), BinaryBoardInput.clearMoves(state, Cell(2)))
    }

    @Test
    fun boardActionsRejectCellsOutsideThePuzzle() {
        val state = testState()

        assertThrows(IllegalArgumentException::class.java) {
            BinaryBoardInput.tapMoves(state, Cell(state.symbols.size))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BinaryBoardInput.clearMoves(state, Cell(state.symbols.size))
        }
    }

    private fun testState(): BinaryState = BinaryState.initial(
        BinaryPuzzle(
            id = "BI-TEST-1",
            size = 6,
            givens = List(36) { index -> if (index == 0) 1 else null },
            solution = List(36) { 0 },
            pack = Pack.EASY,
            storedGrade = "none:0",
        ),
    )
}
