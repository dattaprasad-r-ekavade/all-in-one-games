package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.tactics.TacticsState

/** Converts Tactics tap-tap picks into engine moves. */
internal object TacticsBoardInput {
    fun tapMoves(state: TacticsState, cell: Cell): List<Move> {
        require(cell.index in 0 until 64) { "Tap cell outside Tactics board" }
        val mark = if (state.pendingFrom == cell) Mark.EMPTY else Mark.FILLED
        return listOf(Move.SetMark(cell, mark))
    }
}