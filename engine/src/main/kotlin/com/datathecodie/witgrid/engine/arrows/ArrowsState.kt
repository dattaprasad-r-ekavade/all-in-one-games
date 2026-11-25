package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell

/**
 * In-progress play state for an Arrows puzzle.
 */
data class ArrowsState(
    val puzzle: ArrowsPuzzle,
    /** Row-major cell list; null represents an empty cell. */
    val arrows: List<ArrowsDirection?>,
    /** Number of mistakes made so far; reaches 3 on failure. */
    val mistakes: Int = 0,
    /** Number of slides executed so far; 0 at initial deal. */
    val slideCount: Int = 0,
    /** Stack of prior arrow layouts before each slide; retains play history. */
    val history: List<List<ArrowsDirection?>> = emptyList(),
) {
    init {
        require(arrows.size == puzzle.cellCount) {
            "Arrows size ${arrows.size} does not match puzzle cell count ${puzzle.cellCount}"
        }
        require(mistakes in 0..ArrowsRules.MAX_MISTAKES) {
            "Mistakes must be between 0 and ${ArrowsRules.MAX_MISTAKES}: $mistakes"
        }
        require(slideCount >= 0) { "Slide count must not be negative: $slideCount" }
    }

    fun arrowAt(cell: Cell): ArrowsDirection? {
        require(cell.index in arrows.indices) { "Cell index out of range: ${cell.index}" }
        return arrows[cell.index]
    }

    fun arrowAt(row: Int, column: Int): ArrowsDirection? {
        require(row in 0 until puzzle.size && column in 0 until puzzle.size) {
            "Coordinates out of range: row=$row, column=$column for size ${puzzle.size}"
        }
        return arrows[row * puzzle.size + column]
    }
}
