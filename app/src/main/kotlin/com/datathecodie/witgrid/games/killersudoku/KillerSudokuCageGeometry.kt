package com.datathecodie.witgrid.games.killersudoku

/**
 * Inset cage border for one Killer Sudoku cage.
 *
 * The board strokes this from the cage's highest-index cell, after the other cage cells have
 * filled. Every point stays inside a cage cell, so a later cell fill cannot cover the dashes.
 */
internal data class KillerCageSegment(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

internal object KillerSudokuCageGeometry {
    fun segments(
        cellIndices: Set<Int>,
        cellWidth: Float,
        cellHeight: Float,
        inset: Float,
        boardSize: Int = BOARD_SIZE,
    ): List<KillerCageSegment> {
        val segments = ArrayList<KillerCageSegment>(cellIndices.size * 4)
        for (index in cellIndices) {
            val row = index / boardSize
            val column = index % boardSize
            val left = column * cellWidth
            val top = row * cellHeight
            val right = left + cellWidth
            val bottom = top + cellHeight

            fun inside(neighborRow: Int, neighborColumn: Int): Boolean =
                neighborRow in 0 until boardSize &&
                    neighborColumn in 0 until boardSize &&
                    (neighborRow * boardSize + neighborColumn) in cellIndices

            if (!inside(row - 1, column)) {
                addSegment(
                    segments,
                    minEndpoint(left, inset, inside(row, column - 1), inside(row - 1, column - 1)),
                    top + inset,
                    maxEndpoint(right, inset, inside(row, column + 1), inside(row - 1, column + 1)),
                    top + inset,
                )
            }
            if (!inside(row + 1, column)) {
                addSegment(
                    segments,
                    minEndpoint(left, inset, inside(row, column - 1), inside(row + 1, column - 1)),
                    bottom - inset,
                    maxEndpoint(right, inset, inside(row, column + 1), inside(row + 1, column + 1)),
                    bottom - inset,
                )
            }
            if (!inside(row, column - 1)) {
                addSegment(
                    segments,
                    left + inset,
                    minEndpoint(top, inset, inside(row - 1, column), inside(row - 1, column - 1)),
                    left + inset,
                    maxEndpoint(bottom, inset, inside(row + 1, column), inside(row + 1, column - 1)),
                )
            }
            if (!inside(row, column + 1)) {
                addSegment(
                    segments,
                    right - inset,
                    minEndpoint(top, inset, inside(row - 1, column), inside(row - 1, column + 1)),
                    right - inset,
                    maxEndpoint(bottom, inset, inside(row + 1, column), inside(row + 1, column + 1)),
                )
            }
        }
        return segments
    }

    /**
     * Lower or left end of one border.
     * A convex corner stops [inset] inside the cell. A straight run meets the next cage cell on
     * the grid line. An inner corner extends [inset] into that neighbour so both borders share a point.
     */
    private fun minEndpoint(edge: Float, inset: Float, alongInside: Boolean, diagonalInside: Boolean): Float =
        when {
            !alongInside -> edge + inset
            !diagonalInside -> edge
            else -> edge - inset
        }

    /** Upper or right end of one border. Mirror of [minEndpoint]. */
    private fun maxEndpoint(edge: Float, inset: Float, alongInside: Boolean, diagonalInside: Boolean): Float =
        when {
            !alongInside -> edge - inset
            !diagonalInside -> edge
            else -> edge + inset
        }

    private fun addSegment(
        segments: MutableList<KillerCageSegment>,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
    ) {
        if (startX != endX || startY != endY) {
            segments += KillerCageSegment(startX, startY, endX, endY)
        }
    }

    private const val BOARD_SIZE = 9
}
