package com.datathecodie.witgrid.ui.board

import androidx.compose.ui.geometry.Rect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardDragPreviewTest {
    @Test
    fun squareCellDrawsCrossInsideBounds() {
        assertCrossInside(Rect(10f, 20f, 110f, 120f))
    }

    @Test
    fun nonSquareCellDrawsCrossInsideBoundsUsingShorterSide() {
        assertCrossInside(Rect(10f, 20f, 210f, 100f))
    }

    private fun assertCrossInside(bounds: Rect) {
        val preview = BoardDragPreview.forCell(bounds)
        assertEquals(2, preview.strokes.size)
        assertTrue(preview.strokeWidth > 0f)

        preview.strokes.forEach { (start, end) ->
            assertTrue(start.x in bounds.left..bounds.right)
            assertTrue(start.y in bounds.top..bounds.bottom)
            assertTrue(end.x in bounds.left..bounds.right)
            assertTrue(end.y in bounds.top..bounds.bottom)
            assertEquals(bounds.center.x, (start.x + end.x) / 2f, 0.001f)
            assertEquals(bounds.center.y, (start.y + end.y) / 2f, 0.001f)
        }
    }
}
