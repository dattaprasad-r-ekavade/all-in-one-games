package com.datathecodie.witgrid.engine.core

/** Square board geometry, independent of regions, clues, values and player state. */
data class Grid(val size: Int) {
    init {
        require(size in 1..46340) { "Grid area must fit in a positive Int" }
    }

    /** Number of addressable cells, calculated without integer overflow. */
    val cellCount: Int = size * size

    /** Returns a cell at zero-based [row] and [column], rejecting coordinates outside the grid. */
    fun cell(row: Int, column: Int): Cell {
        require(row in 0 until size && column in 0 until size) { "Coordinates outside grid" }
        return Cell(row * size + column)
    }

    /** Returns the zero-based row of [cell], rejecting a cell outside this grid. */
    fun row(cell: Cell): Int {
        require(cell.index < cellCount) { "Cell outside grid" }
        return cell.index / size
    }

    /** Returns the zero-based column of [cell], rejecting a cell outside this grid. */
    fun column(cell: Cell): Int {
        require(cell.index < cellCount) { "Cell outside grid" }
        return cell.index % size
    }

    /** Returns up to eight touching cells, including diagonals, in row-major order, without wrap. */
    fun neighbours(cell: Cell): List<Cell> {
        val row = row(cell)
        val column = column(cell)
        val rows = maxOf(0, row - 1)..minOf(size - 1, row + 1)
        val columns = maxOf(0, column - 1)..minOf(size - 1, column + 1)
        return buildList {
            for (nextRow in rows) {
                for (nextColumn in columns) {
                    if (nextRow != row || nextColumn != column) {
                        add(cell(nextRow, nextColumn))
                    }
                }
            }
        }
    }
}
