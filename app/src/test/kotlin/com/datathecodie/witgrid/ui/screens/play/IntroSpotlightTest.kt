package com.datathecodie.witgrid.ui.screens.play

import com.datathecodie.witgrid.engine.core.Cell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntroSpotlightTest {
    @Test
    fun spotlightShowsNextStepUntilThreeMovesThenFreesIntro() {
        val cells = setOf(Cell(2), Cell(5))
        assertEquals(cells, introSpotlightCells(true, 0, cells))
        assertEquals(cells, introSpotlightCells(true, 2, cells))
        assertEquals(emptySet<Cell>(), introSpotlightCells(true, 3, cells))
        assertEquals(emptySet<Cell>(), introSpotlightCells(false, 0, cells))
    }
}
