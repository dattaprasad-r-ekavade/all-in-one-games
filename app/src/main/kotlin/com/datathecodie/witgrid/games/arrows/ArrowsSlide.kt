package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.arrows.ArrowsRules
import com.datathecodie.witgrid.engine.core.Cell

/** Normalized travel positions through the source cell, path, and beyond the board edge. */
internal object ArrowsSlide {
    fun travelFractions(size: Int, cell: Cell, direction: ArrowsDirection): List<Float> {
        require(size > 0)
        val row = cell.index / size
        val column = cell.index % size
        val start = if (direction.columnDelta != 0) column else row
        val edgeDistance = if (direction.columnDelta > 0) size - column
        else if (direction.columnDelta < 0) column + 1
        else if (direction.rowDelta > 0) size - row else row + 1
        val scale = edgeDistance + 0.5f
        val sign = direction.columnDelta + direction.rowDelta
        return listOf(0f) + ArrowsRules.pathCells(size, cell, direction).map { pathCell ->
            val coordinate = if (direction.columnDelta != 0) pathCell.index % size else pathCell.index / size
            ((coordinate - start) * sign).toFloat() / scale
        } + 1f
    }
}
