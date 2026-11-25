package com.datathecodie.witgrid.games.killersudoku

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillerSudokuCageGeometryTest {
    @Test
    fun singleCellCageIsAnInsetRectangle() {
        val segments = KillerSudokuCageGeometry.segments(
            cellIndices = setOf(0),
            cellWidth = 100f,
            cellHeight = 100f,
            inset = 10f,
        )

        assertEquals(4, segments.size)
        assertTrue(
            segments.all { segment ->
                listOf(segment.startX, segment.endX).all { it == 10f || it == 90f } &&
                    listOf(segment.startY, segment.endY).all { it == 10f || it == 90f }
            },
        )
    }

    @Test
    fun horizontalCageOmitsTheSharedEdgeAndJoinsTheOuterBorder() {
        val segments = KillerSudokuCageGeometry.segments(
            cellIndices = setOf(0, 1),
            cellWidth = 100f,
            cellHeight = 100f,
            inset = 10f,
        )

        assertEquals(6, segments.size)
        assertTrue(segments.none { it.startX == 100f && it.endX == 100f })
        val top = segments.filter { it.startY == 10f && it.endY == 10f }
        assertEquals(10f, top.minOf { minOf(it.startX, it.endX) }, 0f)
        assertEquals(190f, top.maxOf { maxOf(it.startX, it.endX) }, 0f)
    }

    @Test
    fun ellCageInnerCornerMeetsInsideTheCage() {
        // Cells (0,0), (0,1) and (1,0). The concave corner is the top-left of the missing cell.
        val segments = KillerSudokuCageGeometry.segments(
            cellIndices = setOf(0, 1, 9),
            cellWidth = 100f,
            cellHeight = 100f,
            inset = 10f,
        )

        assertTrue(segments.none { it.startX == 100f && it.endX == 100f })
        assertTrue(segments.none { it.startY == 100f && it.endY == 100f })
        val meeting = segments.filter { it.hasEndpoint(90f, 90f) }
        assertEquals(2, meeting.size)
    }

    private fun KillerCageSegment.hasEndpoint(x: Float, y: Float): Boolean =
        (startX == x && startY == y) || (endX == x && endY == y)
}
