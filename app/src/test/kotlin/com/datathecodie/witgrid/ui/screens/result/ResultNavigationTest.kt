package com.datathecodie.witgrid.ui.screens.result

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ResultNavigationTest {
    @Test
    fun nextAdvancesAndLoopsWithinCurrentPackOnly() {
        val first = LevelRef("binary", "easy-one", Pack.EASY)
        val second = LevelRef("binary", "easy-two", Pack.EASY)
        val levels = listOf(first, second, LevelRef("binary", "medium-one", Pack.MEDIUM))
        assertEquals(second.copy(levelNumber = 2, bankIndex = 1), nextResultLevel(first, levels))
        assertEquals(first.copy(levelNumber = 3, bankIndex = 0), nextResultLevel(second, levels))
        assertEquals(
            second.copy(levelNumber = 102, bankIndex = 1),
            nextResultLevel(first.copy(levelNumber = 101, bankIndex = 0), levels),
        )
        assertNull(nextResultLevel(first.copy(daily = true), levels))
    }
}
