package com.datathecodie.witgrid.ui.board

import com.datathecodie.witgrid.engine.core.Cell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BoardGeometryTest {
    @Test
    fun cellAtUsesRowMajorCoordinatesAndCellEdges() {
        assertEquals(Cell(0), BoardGeometry.cellAt(0f, 0f, 400f, 200f, 4))
        assertEquals(Cell(5), BoardGeometry.cellAt(100f, 50f, 400f, 200f, 4))
        assertEquals(Cell(15), BoardGeometry.cellAt(399.9f, 199.9f, 400f, 200f, 4))
    }

    @Test
    fun cellAtRejectsPointsOutsideOrInvalidDimensions() {
        assertNull(BoardGeometry.cellAt(-0.1f, 1f, 100f, 100f, 4))
        assertNull(BoardGeometry.cellAt(100f, 1f, 100f, 100f, 4))
        assertNull(BoardGeometry.cellAt(1f, 100f, 100f, 100f, 4))
        assertNull(BoardGeometry.cellAt(1f, 1f, 0f, 100f, 4))
        assertNull(BoardGeometry.cellAt(1f, 1f, 100f, 100f, 0))
        assertNull(BoardGeometry.cellAt(Float.NaN, 1f, 100f, 100f, 4))
    }

    @Test
    fun cellsBetweenFillsHorizontalAndVerticalDragGaps() {
        assertEquals(
            listOf(Cell(4), Cell(5), Cell(6), Cell(7)),
            BoardGeometry.cellsBetween(Cell(4), Cell(7), gridSize = 4),
        )
        assertEquals(
            listOf(Cell(1), Cell(5), Cell(9), Cell(13)),
            BoardGeometry.cellsBetween(Cell(1), Cell(13), gridSize = 4),
        )
    }

    @Test
    fun cellsBetweenIncludesAdjacentDiagonalCellsWithoutDuplicates() {
        assertEquals(
            listOf(Cell(0), Cell(5), Cell(10)),
            BoardGeometry.cellsBetween(Cell(0), Cell(10), gridSize = 4),
        )
        assertEquals(
            listOf(Cell(11), Cell(10), Cell(6), Cell(5), Cell(1), Cell(0)),
            BoardGeometry.cellsBetween(Cell(11), Cell(0), gridSize = 4),
        )
    }

    @Test
    fun cellsBetweenIncludesEveryCellCrossedByShallowAndSteepLines() {
        assertEquals(
            listOf(Cell(0), Cell(1), Cell(5), Cell(6)),
            BoardGeometry.cellsBetween(Cell(0), Cell(6), gridSize = 4),
        )
        assertEquals(
            listOf(Cell(0), Cell(4), Cell(5), Cell(9)),
            BoardGeometry.cellsBetween(Cell(0), Cell(9), gridSize = 4),
        )
        assertEquals(
            listOf(Cell(6), Cell(5), Cell(1), Cell(0)),
            BoardGeometry.cellsBetween(Cell(6), Cell(0), gridSize = 4),
        )
    }

    @Test
    fun cellsBetweenRejectsAnEndpointOutsideTheGrid() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardGeometry.cellsBetween(Cell(0), Cell(16), gridSize = 4)
        }
    }
}
