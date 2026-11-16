package com.datathecodie.witgrid.games.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.nonogram.NonogramRules
import com.datathecodie.witgrid.engine.nonogram.NonogramState

internal enum class NonogramMarkMode {
    FILL,
    CROSS,
}

/** Converts a tap or drag into one grouped list of absolute mark updates. */
internal object NonogramBoardInput {
    fun markMoves(
        state: NonogramState,
        cells: List<Cell>,
        mode: NonogramMarkMode,
        autoCrossCompletedLines: Boolean,
    ): List<Move> {
        val cellCount = state.puzzle.grid.cellCount
        require(cells.all { cell -> cell.index in 0 until cellCount }) {
            "Paint cell outside Nonogram board"
        }

        val targetMark = when (mode) {
            NonogramMarkMode.FILL -> Mark.FILLED
            NonogramMarkMode.CROSS -> Mark.CROSSED
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

        if (autoCrossCompletedLines) {
            val currentClues = NonogramRules.cluesForSolution(
                state.puzzle.size,
                updatedState.marks.map { mark -> if (mark == Mark.FILLED) 1 else 0 },
            )
            val completedRows = currentClues.first.mapIndexed { index, clues ->
                clues == state.puzzle.rowClues[index]
            }
            val completedColumns = currentClues.second.mapIndexed { index, clues ->
                clues == state.puzzle.columnClues[index]
            }

            // Matching filled runs fix every remaining empty in that line as an X.
            for (index in 0 until cellCount) {
                val row = index / state.puzzle.size
                val column = index % state.puzzle.size
                if (
                    updatedState.marks[index] == Mark.EMPTY &&
                    (completedRows[row] || completedColumns[column])
                ) {
                    val move = Move.SetMark(Cell(index), Mark.CROSSED)
                    moves += move
                    updatedState = updatedState.withMove(move)
                }
            }
        }

        return moves
    }

    fun clearMoves(state: NonogramState, cell: Cell): List<Move> {
        require(cell.index in state.marks.indices) { "Clear cell outside Nonogram board" }
        return if (state.marks[cell.index] == Mark.EMPTY) {
            emptyList()
        } else {
            listOf(Move.SetMark(cell, Mark.EMPTY))
        }
    }
}
