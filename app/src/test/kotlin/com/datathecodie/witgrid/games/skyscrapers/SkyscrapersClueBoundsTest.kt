package com.datathecodie.witgrid.games.skyscrapers

import androidx.compose.ui.unit.IntRect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkyscrapersClueBoundsTest {
    @Test
    fun clueBandsUseCellBoundariesOnA328PixelBoard() {
        val gridPixels = 256
        val bandPixels = 36
        val boardSize = 7

        assertEquals(IntRect(36, 0, 72, 36), clueBounds(ClueSide.TOP, 0, gridPixels, bandPixels, boardSize))
        assertEquals(IntRect(255, 0, 292, 36), clueBounds(ClueSide.TOP, 6, gridPixels, bandPixels, boardSize))
        assertEquals(IntRect(255, 292, 292, 328), clueBounds(ClueSide.BOTTOM, 6, gridPixels, bandPixels, boardSize))
        assertEquals(IntRect(0, 255, 36, 292), clueBounds(ClueSide.LEFT, 6, gridPixels, bandPixels, boardSize))
        assertEquals(IntRect(292, 255, 328, 292), clueBounds(ClueSide.RIGHT, 6, gridPixels, bandPixels, boardSize))
    }
}
