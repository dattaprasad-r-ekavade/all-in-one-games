package com.datathecodie.witgrid.engine.arrows.spec

import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.BLOCKED_AND_FREE
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.MISTAKE_LIMIT
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.isBoardEmpty
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.slide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Plan 12.4 / 8.4: Continue gives one extra life and keeps the board. */
class ArrowsContinueSpecTest {

    @Test
    fun continueAfterThreeMistakesGivesOneLifeAndKeepsTheBoard() {
        val puzzle = easyPuzzle("AR-SPEC-CONTINUE", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)
        var lost = start
        repeat(MISTAKE_LIMIT) {
            lost = ArrowsEngine.applyAction(lost, slide(1, 1))
        }
        assertTrue(ArrowsEngine.isLost(puzzle, lost))

        val continued = ArrowsEngine.applyContinue(lost)

        assertEquals(lost.arrows, continued.arrows)
        assertFalse(ArrowsEngine.isLost(puzzle, continued))
        assertFalse(ArrowsEngine.isWon(puzzle, continued))
        assertFalse(isBoardEmpty(continued.arrows))

        val failedAgain = ArrowsEngine.applyAction(continued, slide(1, 1))
        assertEquals(continued.arrows, failedAgain.arrows)
        assertTrue(ArrowsEngine.isLost(puzzle, failedAgain))
    }

    @Test
    fun continueLeavesALegalSlidePlayable() {
        val puzzle = easyPuzzle("AR-SPEC-CONTINUE-PLAY", *BLOCKED_AND_FREE)
        var lost = ArrowsEngine.initialState(puzzle)
        repeat(MISTAKE_LIMIT) {
            lost = ArrowsEngine.applyAction(lost, slide(1, 1))
        }

        val continued = ArrowsEngine.applyContinue(lost)
        val afterDown = ArrowsEngine.applyAction(continued, slide(1, 2))
        val won = ArrowsEngine.applyAction(afterDown, slide(1, 1))

        assertTrue(ArrowsEngine.isWon(puzzle, won))
        assertFalse(ArrowsEngine.isLost(puzzle, won))
        assertTrue(isBoardEmpty(won.arrows))
    }

    @Test
    fun continueWhenNotLostThrows() {
        val puzzle = easyPuzzle("AR-SPEC-CONTINUE-ILLEGAL", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)
        assertFalse(ArrowsEngine.isLost(puzzle, start))

        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.applyContinue(start)
        }
    }
}
