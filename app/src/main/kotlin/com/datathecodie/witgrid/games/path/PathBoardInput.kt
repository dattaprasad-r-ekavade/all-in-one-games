package com.datathecodie.witgrid.games.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.path.PathState

/** Converts Path board gestures into absolute digit updates for a single history record. */
internal object PathBoardInput {
    fun dragMoves(state: PathState, path: List<Cell>): List<Move> {
        val paintColor = paintColor(state, path) ?: return emptyList()
        return path.distinct().mapNotNull { cell ->
            require(cell.index < state.puzzle.grid.cellCount) { "Drag cell outside Path board" }
            val index = cell.index
            if (state.puzzle.walls[index] || state.puzzle.endpoints[index] != null) {
                return@mapNotNull null
            }
            when (state.digits[index]) {
                paintColor -> null
                null -> Move.SetDigit(cell, paintColor)
                else -> null
            }
        }
    }

    fun clearMoves(state: PathState, cell: Cell): List<Move> {
        require(cell.index < state.puzzle.grid.cellCount) { "Clear cell outside Path board" }
        val index = cell.index
        if (state.puzzle.walls[index] || state.puzzle.endpoints[index] != null) {
            return emptyList()
        }
        return if (state.digits[index] == null) {
            emptyList()
        } else {
            listOf(Move.SetDigit(cell, 0))
        }
    }

    private fun paintColor(state: PathState, path: List<Cell>): Int? {
        val first = path.firstOrNull() ?: return null
        val index = first.index
        if (index !in 0 until state.puzzle.grid.cellCount) return null
        if (state.puzzle.walls[index]) return null
        state.puzzle.endpoints[index]?.let { return it }
        return state.digits[index]
    }
}