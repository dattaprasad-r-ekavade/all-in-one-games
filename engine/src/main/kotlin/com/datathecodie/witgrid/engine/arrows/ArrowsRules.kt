package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell

object ArrowsRules {
    /**
     * Sequence of cells on the path from [cell] to the board edge in [direction],
     * excluding [cell] itself.
     */
    fun pathCells(size: Int, cell: Cell, direction: ArrowsDirection): List<Cell> {
        require(size > 0) { "Grid size must be positive" }
        require(cell.index in 0 until (size * size)) { "Cell outside grid: ${cell.index}" }
        val cells = ArrayList<Cell>()
        var row = cell.index / size + direction.rowDelta
        var column = cell.index % size + direction.columnDelta
        while (row in 0 until size && column in 0 until size) {
            cells.add(Cell(row * size + column))
            row += direction.rowDelta
            column += direction.columnDelta
        }
        return cells
    }

    /**
     * Checks if the arrow at [cellIndex] can slide off the board in its direction.
     * An arrow can slide only if every cell along its path to the edge is empty.
     */
    fun canSlide(size: Int, arrows: List<ArrowsDirection?>, cellIndex: Int): Boolean {
        if (cellIndex !in arrows.indices) return false
        val direction = arrows[cellIndex] ?: return false
        var row = cellIndex / size + direction.rowDelta
        var column = cellIndex % size + direction.columnDelta
        while (row in 0 until size && column in 0 until size) {
            if (arrows[row * size + column] != null) return false
            row += direction.rowDelta
            column += direction.columnDelta
        }
        return true
    }

    fun isSlideLegal(size: Int, arrows: List<ArrowsDirection?>, cell: Cell): Boolean =
        canSlide(size, arrows, cell.index)

    /**
     * Applies a slide by clearing the arrow at [cell] from the board.
     * Throws [IllegalArgumentException] if the slide is illegal or cell is empty.
     */
    fun applySlide(size: Int, arrows: List<ArrowsDirection?>, cell: Cell): List<ArrowsDirection?> {
        require(cell.index in arrows.indices) { "Cell index ${cell.index} is outside board" }
        require(isSlideLegal(size, arrows, cell)) { "Illegal slide at cell ${cell.index}" }
        val next = arrows.toMutableList()
        next[cell.index] = null
        return next.toList()
    }

    /**
     * Returns all legal slides on the current board.
     */
    fun legalSlides(size: Int, arrows: List<ArrowsDirection?>): List<ArrowsAction> {
        val actions = ArrayList<ArrowsAction>()
        for (index in arrows.indices) {
            if (arrows[index] != null && canSlide(size, arrows, index)) {
                actions.add(ArrowsAction(Cell(index)))
            }
        }
        return actions
    }

    /**
     * Level is won when all arrows have slid off the board.
     */
    fun isWon(arrows: List<ArrowsDirection?>): Boolean = arrows.all { it == null }

    /**
     * Level is lost when arrows remain on the board, none can slide, and at least one
     * action has been taken since deal.
     */
    fun isLost(size: Int, arrows: List<ArrowsDirection?>, slideCount: Int): Boolean =
        slideCount > 0 && !isWon(arrows) && legalSlides(size, arrows).isEmpty()
}
