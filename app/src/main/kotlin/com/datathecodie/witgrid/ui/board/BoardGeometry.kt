package com.datathecodie.witgrid.ui.board

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid

/** Maps touch coordinates to cells and fills fast drag gaps in row-major board space. */
object BoardGeometry {
    fun cellAt(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        gridSize: Int,
    ): Cell? {
        if (gridSize !in 1..MAX_GRID_SIZE) return null
        if (!x.isFinite() || !y.isFinite() || !width.isFinite() || !height.isFinite()) return null
        if (width <= 0f || height <= 0f || x < 0f || y < 0f || x >= width || y >= height) return null

        val column = (x / width * gridSize).toInt().coerceAtMost(gridSize - 1)
        val row = (y / height * gridSize).toInt().coerceAtMost(gridSize - 1)
        return Grid(gridSize).cell(row, column)
    }

    /** Includes both endpoints and every cell crossed by a straight center-to-center segment. */
    fun cellsBetween(start: Cell, end: Cell, gridSize: Int): List<Cell> {
        val grid = Grid(gridSize)
        require(start.index < grid.cellCount && end.index < grid.cellCount) {
            "Drag path endpoint is outside the grid"
        }

        var x = grid.column(start)
        var y = grid.row(start)
        val endX = grid.column(end)
        val endY = grid.row(end)
        val deltaX = kotlin.math.abs(endX - x)
        val deltaY = kotlin.math.abs(endY - y)
        val stepX = if (x < endX) 1 else -1
        val stepY = if (y < endY) 1 else -1
        var crossedX = 0
        var crossedY = 0
        val result = mutableListOf<Cell>()

        while (true) {
            result += grid.cell(y, x)
            if (x == endX && y == endY) return result

            if (y == endY) {
                x += stepX
                crossedX += 1
            } else if (x == endX) {
                y += stepY
                crossedY += 1
            } else {
                val verticalBoundary = (2L * crossedX + 1L) * deltaY
                val horizontalBoundary = (2L * crossedY + 1L) * deltaX
                when {
                    verticalBoundary < horizontalBoundary -> {
                        x += stepX
                        crossedX += 1
                    }

                    verticalBoundary > horizontalBoundary -> {
                        y += stepY
                        crossedY += 1
                    }

                    else -> {
                        x += stepX
                        y += stepY
                        crossedX += 1
                        crossedY += 1
                    }
                }
            }
        }
    }

    private const val MAX_GRID_SIZE = 46_340
}
