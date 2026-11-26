package com.datathecodie.witgrid.engine.arrows.spec

import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsPuzzle
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.BLOCKED_AND_FREE
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.CLEAR_RIGHT
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.HINT_CHAIN
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.MISTAKE_LIMIT
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.isBoardEmpty
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.pathToEdgeIsEmpty
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.slide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Plan 12.4: a hint names one arrow that can slide now.
 * Following hints must clear a hand-made board with zero mistakes.
 */
class ArrowsHintSpecTest {

    @Test
    fun hintNamesAnArrowWhosePathToTheEdgeIsEmpty() {
        val puzzle = easyPuzzle("AR-SPEC-HINT-NOW", *HINT_CHAIN)
        val start = ArrowsEngine.initialState(puzzle)

        val hint = ArrowsEngine.hintAction(puzzle, start)
        assertNotNull(hint)
        assertTrue(
            pathToEdgeIsEmpty(puzzle.size, start.arrows, hint!!.cell.index),
            "hint cell ${hint.cell.index} must be able to slide now",
        )
        assertEquals(13, hint.cell.index)
    }

    @Test
    fun hintOnAMidGameBoardNamesTheArrowThatCanSlideNow() {
        val puzzle = easyPuzzle("AR-SPEC-HINT-MID", *HINT_CHAIN)
        val start = ArrowsEngine.initialState(puzzle)
        val mid = ArrowsEngine.applyAction(start, slide(2, 3))

        assertFalse(ArrowsEngine.isWon(puzzle, mid))
        assertFalse(ArrowsEngine.isLost(puzzle, mid))
        assertNull(mid.arrowAt(2, 3))

        val hint = ArrowsEngine.hintAction(puzzle, mid)
        assertNotNull(hint)
        assertTrue(pathToEdgeIsEmpty(puzzle.size, mid.arrows, hint!!.cell.index))
        assertEquals(8, hint.cell.index)
    }

    @Test
    fun followingHintsClearsTheHandMadeBoardWithZeroMistakes() {
        val puzzle = easyPuzzle("AR-SPEC-HINT-CLEAR", *HINT_CHAIN)
        val won = clearWithHints(puzzle)

        assertTrue(isBoardEmpty(won.arrows))
        assertTrue(ArrowsEngine.isWon(puzzle, won))
        assertFalse(ArrowsEngine.isLost(puzzle, won))
        assertEquals(0, won.mistakes)
        assertNull(ArrowsEngine.hintAction(puzzle, won))
    }

    @Test
    fun followingHintsClearsTheBlockedAndFreeBoard() {
        val puzzle = easyPuzzle("AR-SPEC-HINT-TWO", *BLOCKED_AND_FREE)
        val won = clearWithHints(puzzle)

        assertTrue(ArrowsEngine.isWon(puzzle, won))
        assertEquals(0, won.mistakes)
    }

    @Test
    fun hintOnWonStateIsNull() {
        val puzzle = easyPuzzle("AR-SPEC-HINT-WON", *CLEAR_RIGHT)
        val won = ArrowsEngine.applyAction(
            ArrowsEngine.initialState(puzzle),
            slide(1, 1),
        )
        assertTrue(ArrowsEngine.isWon(puzzle, won))
        assertNull(ArrowsEngine.hintAction(puzzle, won))
    }

    @Test
    fun hintOnLostStateIsNull() {
        val puzzle = easyPuzzle("AR-SPEC-HINT-LOST", *BLOCKED_AND_FREE)
        var lost = ArrowsEngine.initialState(puzzle)
        repeat(MISTAKE_LIMIT) {
            lost = ArrowsEngine.applyAction(lost, slide(1, 1))
        }
        assertTrue(ArrowsEngine.isLost(puzzle, lost))
        assertNull(ArrowsEngine.hintAction(puzzle, lost))
    }

    private fun clearWithHints(puzzle: ArrowsPuzzle): ArrowsState {
        var state = ArrowsEngine.initialState(puzzle)
        var steps = 0
        while (!ArrowsEngine.isWon(puzzle, state)) {
            assertTrue(steps < puzzle.cellCount, "hint loop exceeded cell count")
            val hint = ArrowsEngine.hintAction(puzzle, state)
            assertNotNull(hint, "hintAction returned null before the board was empty")
            assertTrue(
                pathToEdgeIsEmpty(puzzle.size, state.arrows, hint!!.cell.index),
                "hint cell ${hint.cell.index} must be able to slide now",
            )
            state = ArrowsEngine.applyAction(state, hint)
            assertEquals(0, state.mistakes)
            steps++
        }
        return state
    }
}
