package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.arrows.ArrowsRules
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.core.Cell

internal enum class ArrowTapFeedback {
    NONE,
    SLID,
    BLOCKED,
}

internal data class ArrowTapResult(
    val action: ArrowsAction?,
    val feedback: ArrowTapFeedback,
)

/** TalkBack parts for one cell. [row] and [column] are 1-based. */
internal data class ArrowCellLabel(
    val row: Int,
    val column: Int,
    val direction: ArrowsDirection?,
    val canSlide: Boolean,
)

/** Converts a cell tap into an [ArrowsAction] slide, or into no action when the tap cannot change the board. */
internal object ArrowsBoardInput {
    fun tapCell(state: ArrowsState, cellIndex: Int): ArrowTapResult {
        require(cellIndex in state.arrows.indices) { "Tapped cell $cellIndex outside board" }
        if (ArrowsRules.isWon(state.arrows) || ArrowsRules.isLost(state.mistakes)) {
            return ArrowTapResult(action = null, feedback = ArrowTapFeedback.NONE)
        }
        if (state.arrows[cellIndex] == null) {
            return ArrowTapResult(action = null, feedback = ArrowTapFeedback.NONE)
        }
        val action = ArrowsAction(Cell(cellIndex))
        return if (canSlideNow(state, cellIndex)) {
            ArrowTapResult(action = action, feedback = ArrowTapFeedback.SLID)
        } else {
            ArrowTapResult(action = action, feedback = ArrowTapFeedback.BLOCKED)
        }
    }

    fun canSlideNow(state: ArrowsState, cellIndex: Int): Boolean {
        if (cellIndex !in state.arrows.indices) return false
        if (ArrowsRules.isWon(state.arrows) || ArrowsRules.isLost(state.mistakes)) return false
        return ArrowsRules.canSlide(state.puzzle.size, state.arrows, cellIndex)
    }

    /** Lives still available. Three mistakes fail the level, so this starts at 3. */
    fun livesLeft(state: ArrowsState): Int = ArrowsRules.MAX_MISTAKES - state.mistakes

    fun cellLabel(state: ArrowsState, cellIndex: Int): ArrowCellLabel {
        require(cellIndex in state.arrows.indices) { "Cell $cellIndex outside board" }
        val size = state.puzzle.size
        return ArrowCellLabel(
            row = cellIndex / size + 1,
            column = cellIndex % size + 1,
            direction = state.arrows[cellIndex],
            canSlide = canSlideNow(state, cellIndex),
        )
    }
}
