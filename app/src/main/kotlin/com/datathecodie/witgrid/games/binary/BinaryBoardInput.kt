package com.datathecodie.witgrid.games.binary

import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.binary.BinarySymbol
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move

/** Converts Binary taps and clear actions into absolute moves. */
internal object BinaryBoardInput {
    fun nextSymbol(symbol: BinarySymbol): BinarySymbol = when (symbol) {
        BinarySymbol.EMPTY -> BinarySymbol.A
        BinarySymbol.A -> BinarySymbol.B
        BinarySymbol.B -> BinarySymbol.EMPTY
    }

    fun tapMoves(state: BinaryState, cell: Cell): List<Move> {
        require(cell.index < state.symbols.size) { "Tap cell outside Binary board" }
        if (state.puzzle.givens[cell.index] != null) return emptyList()
        val mark = when (nextSymbol(state.symbols[cell.index])) {
            BinarySymbol.EMPTY -> Mark.EMPTY
            BinarySymbol.A -> Mark.FILLED
            BinarySymbol.B -> Mark.CROSSED
        }
        return listOf(Move.SetMark(cell, mark))
    }

    fun clearMoves(state: BinaryState, cell: Cell): List<Move> {
        require(cell.index < state.symbols.size) { "Clear cell outside Binary board" }
        if (state.puzzle.givens[cell.index] != null || state.symbols[cell.index] == BinarySymbol.EMPTY) {
            return emptyList()
        }
        return listOf(Move.SetMark(cell, Mark.EMPTY))
    }
}
