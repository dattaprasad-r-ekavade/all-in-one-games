package com.datathecodie.witgrid.ui.screens.result

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ResultNavigationTest {
    @Test
    fun nextAdvancesWithinCurrentPackOnly() {
        val first = LevelRef("binary", "easy-one", Pack.EASY)
        val second = LevelRef("binary", "easy-two", Pack.EASY)
        val medium = LevelRef("binary", "medium-one", Pack.MEDIUM)
        val levels = listOf(first, second, medium)

        assertEquals(second, nextResultLevel(first, levels))
        assertNull(nextResultLevel(second, levels))
        assertNull(nextResultLevel(first.copy(daily = true), levels))
        assertNull(nextResultLevel(first.copy(levelId = "missing"), levels))
    }
}
