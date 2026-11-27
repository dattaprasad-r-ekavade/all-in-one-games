package com.datathecodie.witgrid.games.coloursort

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColourSortMotionTest {
    @Test
    fun pourUsesShortAnimationUnlessMotionIsReduced() {
        assertEquals(240, ColourSortMotion.pourDurationMillis(reduceMotion = false))
        assertEquals(0, ColourSortMotion.pourDurationMillis(reduceMotion = true))
    }

    @Test
    fun completionPopUsesShortAnimationUnlessMotionIsReduced() {
        assertEquals(180, ColourSortMotion.completePopDurationMillis(reduceMotion = false))
        assertEquals(0, ColourSortMotion.completePopDurationMillis(reduceMotion = true))
    }
}
