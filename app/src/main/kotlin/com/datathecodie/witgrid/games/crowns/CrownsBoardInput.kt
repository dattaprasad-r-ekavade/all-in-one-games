package com.datathecodie.witgrid.games.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.crowns.CrownsState

/** Converts Crowns board gestures into absolute edits for a single history record. */
internal object CrownsBoardInput {
    fun nextMark(mark: Mark): Mark = when (mark) {
        Mark.EMPTY -> Mark.CROSSED
        Mark.CROSSED -> Mark.FILLED
        Mark.FILLED -> Mark.EMPTY
    }

    fun tapMoves(state: CrownsState, cell: Cell, autoXEnabled: Boolean): List<Move> {
        require(cell.index < state.marks.size) { "Tap cell outside Crowns board" }
        val nextMark = nextMark(state.marks[cell.index])
        return buildList {
            add(Move.SetMark(cell, nextMark))
            if (nextMark == Mark.FILLED && autoXEnabled) {
                state.puzzle.grid.neighbours(cell).forEach { neighbour ->
                    if (state.marks[neighbour.index] == Mark.EMPTY) {
                        add(Move.SetMark(neighbour, Mark.CROSSED))
                    }
                }
            }
        }
    }

    fun dragMoves(state: CrownsState, path: List<Cell>): List<Move> =
        path.distinct().mapNotNull { cell ->
            require(cell.index < state.marks.size) { "Drag cell outside Crowns board" }
            if (state.marks[cell.index] == Mark.EMPTY) {
                Move.SetMark(cell, Mark.CROSSED)
            } else {
                null
            }
        }

    fun clearMoves(state: CrownsState, cell: Cell): List<Move> {
        require(cell.index < state.marks.size) { "Clear cell outside Crowns board" }
        return if (state.marks[cell.index] == Mark.EMPTY) {
            emptyList()
        } else {
            listOf(Move.SetMark(cell, Mark.EMPTY))
        }
    }
}
