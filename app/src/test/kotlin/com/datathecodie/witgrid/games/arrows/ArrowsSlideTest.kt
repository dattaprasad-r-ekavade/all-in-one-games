package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.core.Cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowsSlideTest {
    @Test
    fun rightwardSlideMovesInIncreasingXAndPastTheLastCell() {
        val fractions = ArrowsSlide.travelFractions(4, Cell(4), ArrowsDirection.RIGHT)
        assertEquals(0f, fractions.first())
        assertTrue(fractions.zipWithNext().all { (left, right) -> right > left })
        assertTrue(fractions.last() * (4 - 0 + 0.5f) > 4f)
    }
}
