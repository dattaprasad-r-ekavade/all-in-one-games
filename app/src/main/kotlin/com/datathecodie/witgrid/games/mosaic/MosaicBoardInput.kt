package com.datathecodie.witgrid.games.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicRules
import com.datathecodie.witgrid.engine.mosaic.MosaicState

internal enum class MosaicMarkMode {
    FILL,
    CROSS,
}

internal enum class MosaicClueStatus {
    NORMAL,
    SATISFIED,
    ERROR,
}

/** Converts Mosaic board gestures and clue conditions into mark updates and visual feedback. */
internal object MosaicBoardInput {
    fun markMoves(
        state: MosaicState,
        cells: List<Cell>,
        mode: MosaicMarkMode,
    ): List<Move> {
        val cellCount = state.puzzle.grid.cellCount
        require(cells.all { cell -> cell.index in 0 until cellCount }) {
            "Paint cell outside Mosaic board"
        }

        val targetMark = when (mode) {
            MosaicMarkMode.FILL -> Mark.FILLED
            MosaicMarkMode.CROSS -> Mark.CROSSED
        }
        val moves = mutableListOf<Move>()
        var updatedState = state

        cells.distinct().forEach { cell ->
            if (updatedState.marks[cell.index] != targetMark) {
                val move = Move.SetMark(cell, targetMark)
                moves += move
                updatedState = updatedState.withMove(move)
            }
        }

        return moves
    }

    fun clearMoves(state: MosaicState, cell: Cell): List<Move> {
        require(cell.index in 0 until state.puzzle.grid.cellCount) {
            "Clear cell outside Mosaic board"
        }
        return if (state.marks[cell.index] == Mark.EMPTY) {
            emptyList()
        } else {
            listOf(Move.SetMark(cell, Mark.EMPTY))
        }
    }

    fun clueStatus(
        grid: Grid,
        clue: Int,
        cell: Cell,
        marks: List<Mark>,
    ): MosaicClueStatus {
        val area = MosaicRules.areaCells(grid, cell)
        var filledCount = 0
        var emptyCount = 0
        for (neighbour in area) {
            when (marks[neighbour.index]) {
                Mark.FILLED -> filledCount++
                Mark.EMPTY -> emptyCount++
                Mark.CROSSED -> Unit
            }
        }

        return when {
            // A clue whose area already has too many fills, or too many crosses to reach its count, is invalid.
            filledCount > clue || filledCount + emptyCount < clue -> MosaicClueStatus.ERROR
            // A clue is satisfied only when its whole 3x3 area is marked and the filled count matches.
            emptyCount == 0 && filledCount == clue -> MosaicClueStatus.SATISFIED
            else -> MosaicClueStatus.NORMAL
        }
    }
}
