package com.datathecodie.witgrid.ui.screens.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DailyPuzzleIndexTest {
    @Test
    fun dailyIndexChangesAtUtcMidnightAndWrapsInBothDirections() {
        val epoch = 1_767_225_600_000L
        val day = 86_400_000L

        assertEquals(0, dailyPuzzleIndex(epoch))
        assertEquals(0, dailyPuzzleIndex(epoch + day - 1))
        assertEquals(1, dailyPuzzleIndex(epoch + day))
        assertEquals(0, dailyPuzzleIndex(epoch + 365 * day))
        assertEquals(364, dailyPuzzleIndex(epoch - 1))
    }
}
