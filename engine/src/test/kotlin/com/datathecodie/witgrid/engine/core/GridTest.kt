package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GridTest {
    @Test
    fun convertsEveryCoordinateToRowMajorCellAndBack() {
        for (size in listOf(1, 5, 6, 8, 10, 12, 15)) {
            val grid = Grid(size)
            assertEquals(size * size, grid.cellCount)
            for (row in 0 until size) {
                for (column in 0 until size) {
                    val cell = grid.cell(row, column)
                    assertEquals(row * size + column, cell.index)
                    assertEquals(row, grid.row(cell))
                    assertEquals(column, grid.column(cell))
                }
            }
        }
    }

    @Test
    fun neighboursAreOrderedAndDoNotWrapAtEdges() {
        val grid = Grid(3)
        assertEquals(listOf(1, 3, 4), grid.neighbours(Cell(0)).map { it.index })
        assertEquals(listOf(0, 2, 3, 4, 5), grid.neighbours(Cell(1)).map { it.index })
        assertEquals(listOf(1, 4, 5), grid.neighbours(Cell(2)).map { it.index })
        assertEquals(listOf(0, 1, 2, 3, 5, 6, 7, 8), grid.neighbours(Cell(4)).map { it.index })
        assertEquals(listOf(3, 4, 7), grid.neighbours(Cell(6)).map { it.index })
        assertEquals(listOf(4, 5, 7), grid.neighbours(Cell(8)).map { it.index })
        assertEquals(emptyList<Cell>(), Grid(1).neighbours(Cell(0)))
    }

    @Test
    fun everyNeighbourIsAdjacentAndReciprocalAcrossSupportedBoardSizes() {
        for (size in listOf(1, 2, 6, 10, 15)) {
            val grid = Grid(size)
            for (index in 0 until grid.cellCount) {
                val cell = Cell(index)
                val expected = (0 until grid.cellCount).filter { candidate ->
                    val rowDistance = kotlin.math.abs(candidate / size - index / size)
                    val columnDistance = kotlin.math.abs(candidate % size - index % size)
                    candidate != index && rowDistance <= 1 && columnDistance <= 1
                }
                assertEquals(expected, grid.neighbours(cell).map { it.index })
                for (neighbour in grid.neighbours(cell)) {
                    org.junit.jupiter.api.Assertions.assertTrue(cell in grid.neighbours(neighbour))
                }
            }
        }
    }

    @Test
    fun rejectsInvalidCoordinatesIndicesAndSizes() {
        assertThrows(IllegalArgumentException::class.java) { Cell(-1) }
        for (size in listOf(Int.MIN_VALUE, -1, 0, 46341, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { Grid(size) }
        }
        val grid = Grid(3)
        for ((row, column) in listOf(-1 to 0, 0 to -1, 3 to 0, 0 to 3)) {
            assertThrows(IllegalArgumentException::class.java) { grid.cell(row, column) }
        }
        for (cell in listOf(Cell(9), Cell(Int.MAX_VALUE))) {
            assertThrows(IllegalArgumentException::class.java) { grid.row(cell) }
            assertThrows(IllegalArgumentException::class.java) { grid.column(cell) }
            assertThrows(IllegalArgumentException::class.java) { grid.neighbours(cell) }
        }
        assertEquals(2147395600, Grid(46340).cellCount)
    }
}
