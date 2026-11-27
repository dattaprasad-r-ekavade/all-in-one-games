package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.tactics.TacticsState

/** Converts Tactics tap-tap picks into engine moves. */
internal object TacticsBoardInput {
    fun displayRowToRankFromWhite(isBlackToMove: Boolean, displayRow: Int): Int {
        require(displayRow in 0..7) { "Display row outside Tactics board" }
        return if (isBlackToMove) displayRow else 7 - displayRow
    }

    fun displayCellToEngineCell(isBlackToMove: Boolean, displayCell: Cell): Cell {
        require(displayCell.index in 0 until 64) { "Tap cell outside Tactics board" }
        val row = displayCell.index / 8
        val column = displayCell.index % 8
        val rankFromWhite = displayRowToRankFromWhite(isBlackToMove, row)
        return Cell((7 - rankFromWhite) * 8 + column)
    }

    fun tapMoves(state: TacticsState, cell: Cell): List<Move> {
        require(cell.index in 0 until 64) { "Tap cell outside Tactics board" }
        val engineCell = displayCellToEngineCell(state.puzzle.sideToMove == com.datathecodie.witgrid.engine.tactics.PieceColor.BLACK, cell)
        val mark = if (state.pendingFrom == engineCell) Mark.EMPTY else Mark.FILLED
        return listOf(Move.SetMark(engineCell, mark))
    }
}

internal fun tacticsBoardDisplayRow(isBlackToMove: Boolean, rankFromWhite: Int): Int {
    require(rankFromWhite in 0..7) { "Rank outside Tactics board" }
    return if (isBlackToMove) rankFromWhite else 7 - rankFromWhite
}
