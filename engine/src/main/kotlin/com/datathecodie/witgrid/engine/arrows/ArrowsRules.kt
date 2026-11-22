package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell

object ArrowsRules {
    /** Maximum allowed mistakes before level fails. */
    const val MAX_MISTAKES = 3

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
     * Level is lost when mistakes reach the limit of 3.
     */
    fun isLost(mistakes: Int): Boolean = mistakes >= MAX_MISTAKES

    /**
     * Verifies that the layout contains at least one arrow and is completely clearable.
     * A layout is clearable if all arrows can slide off the board without getting deadlocked
     * in a cycle.
     */
    fun isClearable(size: Int, arrows: List<ArrowsDirection?>): Boolean {
        if (arrows.none { it != null }) return false
        val current = arrows.toMutableList()
        while (true) {
            val slideableIndex = current.indices.firstOrNull { index ->
                current[index] != null && canSlide(size, current, index)
            } ?: break
            current[slideableIndex] = null
        }
        return current.all { it == null }
    }

    /**
     * Checks if [next] is the result of exactly one legal slide on [previous].
     */
    fun isValidSlideTransition(size: Int, previous: List<ArrowsDirection?>, next: List<ArrowsDirection?>): Boolean {
        if (previous.size != next.size) return false
        var diffIndex = -1
        for (i in previous.indices) {
            if (previous[i] != next[i]) {
                if (diffIndex != -1) return false
                if (previous[i] == null || next[i] != null) return false
                diffIndex = i
            }
        }
        if (diffIndex == -1) return false
        return canSlide(size, previous, diffIndex)
    }
}
