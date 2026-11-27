package com.datathecodie.witgrid.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ContinuePreviewCountTest {
    @Test
    fun unknownContinueStateUsesFiveCells() {
        assertEquals(5, continuePreviewCellCount(null))
        assertEquals(5, continuePreviewCellCount("not-a-board"))
    }
}
