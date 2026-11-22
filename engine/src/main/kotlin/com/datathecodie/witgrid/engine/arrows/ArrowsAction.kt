package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell

/**
 * Slide the arrow at [cell] off the board in its pointing direction.
 */
data class ArrowsAction(val cell: Cell) {
    val cellIndex: Int get() = cell.index

    companion object {
        fun fromIndex(index: Int): ArrowsAction = ArrowsAction(Cell(index))
    }
}

typealias Slide = ArrowsAction
