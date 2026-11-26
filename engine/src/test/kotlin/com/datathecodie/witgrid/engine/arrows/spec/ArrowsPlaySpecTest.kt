package com.datathecodie.witgrid.engine.arrows.spec

import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.BLOCKED_AND_FREE
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.CLEAR_RIGHT
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.DISTANT_BLOCKER
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.EDGE_UP
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.EASY_SIZE
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.MISTAKE_LIMIT
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.isBoardEmpty
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.pathToEdgeIsEmpty
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.slide
import com.datathecodie.witgrid.engine.core.Cell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent play cases from plan 12.4: slide, blocked tap, lives, win. */
class ArrowsPlaySpecTest {

    @Test
    fun engineIdIsArrows() {
        assertEquals("arrows", ArrowsEngine.id)
    }

    @Test
    fun dealStartsNotLostAndNotWonWhenArrowsRemain() {
        val puzzle = easyPuzzle("AR-SPEC-DEAL", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)

        assertFalse(isBoardEmpty(start.arrows))
        assertFalse(ArrowsEngine.isWon(puzzle, start))
        assertFalse(ArrowsEngine.isLost(puzzle, start))
        assertEquals(0, start.mistakes)
    }

    @Test
    fun slideSucceedsWhenEveryCellOnThePathToTheEdgeIsEmpty() {
        val puzzle = easyPuzzle("AR-SPEC-CLEAR-PATH", *CLEAR_RIGHT)
        val start = ArrowsEngine.initialState(puzzle)
        assertTrue(pathToEdgeIsEmpty(EASY_SIZE, start.arrows, index = 6))
        assertNotNull(start.arrowAt(1, 1))

        val next = ArrowsEngine.applyAction(start, slide(1, 1))

        assertNull(next.arrowAt(1, 1))
        assertTrue(isBoardEmpty(next.arrows))
        assertEquals(0, next.mistakes)
        assertNotNull(start.arrowAt(1, 1))
        assertEquals(0, start.mistakes)
    }

    @Test
    fun arrowOnTheEdgeWithAnEmptyPathSlidesOff() {
        val puzzle = easyPuzzle("AR-SPEC-EDGE", *EDGE_UP)
        val start = ArrowsEngine.initialState(puzzle)
        assertTrue(pathToEdgeIsEmpty(EASY_SIZE, start.arrows, index = 0))

        val next = ArrowsEngine.applyAction(start, slide(0, 0))

        assertTrue(isBoardEmpty(next.arrows))
        assertTrue(ArrowsEngine.isWon(puzzle, next))
        assertFalse(ArrowsEngine.isLost(puzzle, next))
        assertEquals(0, next.mistakes)
    }

    @Test
    fun winWhenTheBoardIsEmpty() {
        val puzzle = easyPuzzle("AR-SPEC-WIN", *CLEAR_RIGHT)
        val start = ArrowsEngine.initialState(puzzle)
        assertFalse(ArrowsEngine.isWon(puzzle, start))

        val won = ArrowsEngine.applyAction(start, slide(1, 1))

        assertTrue(isBoardEmpty(won.arrows))
        assertTrue(ArrowsEngine.isWon(puzzle, won))
        assertFalse(ArrowsEngine.isLost(puzzle, won))
    }

    @Test
    fun blockedTapIsAMistakeAndDoesNotChangeTheBoard() {
        val puzzle = easyPuzzle("AR-SPEC-BLOCKED", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)
        assertFalse(pathToEdgeIsEmpty(EASY_SIZE, start.arrows, index = 6))

        val after = ArrowsEngine.applyAction(start, slide(1, 1))

        assertEquals(start.arrows, after.arrows)
        assertEquals(1, after.mistakes)
        assertFalse(ArrowsEngine.isLost(puzzle, after))
        assertFalse(ArrowsEngine.isWon(puzzle, after))
        assertNotNull(after.arrowAt(1, 1))
        assertNotNull(after.arrowAt(1, 2))
    }

    @Test
    fun distantOccupantOnThePathBlocksTheSlide() {
        val puzzle = easyPuzzle("AR-SPEC-DISTANT", *DISTANT_BLOCKER)
        val start = ArrowsEngine.initialState(puzzle)
        assertFalse(pathToEdgeIsEmpty(EASY_SIZE, start.arrows, index = 6))
        assertTrue(pathToEdgeIsEmpty(EASY_SIZE, start.arrows, index = 9))

        val blocked = ArrowsEngine.applyAction(start, slide(1, 1))
        assertEquals(start.arrows, blocked.arrows)
        assertEquals(1, blocked.mistakes)

        val slid = ArrowsEngine.applyAction(start, slide(1, 4))
        assertNull(slid.arrowAt(1, 4))
        assertNotNull(slid.arrowAt(1, 1))
        assertEquals(0, slid.mistakes)
        assertTrue(pathToEdgeIsEmpty(EASY_SIZE, slid.arrows, index = 6))
    }

    @Test
    fun slidingTheFreeArrowThenTheOpenedArrowClearsTheBoard() {
        val puzzle = easyPuzzle("AR-SPEC-TWO-SLIDE", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)

        val afterDown = ArrowsEngine.applyAction(start, slide(1, 2))
        assertNull(afterDown.arrowAt(1, 2))
        assertNotNull(afterDown.arrowAt(1, 1))
        assertTrue(pathToEdgeIsEmpty(EASY_SIZE, afterDown.arrows, index = 6))
        assertFalse(ArrowsEngine.isWon(puzzle, afterDown))

        val won = ArrowsEngine.applyAction(afterDown, slide(1, 1))
        assertTrue(isBoardEmpty(won.arrows))
        assertTrue(ArrowsEngine.isWon(puzzle, won))
        assertEquals(0, won.mistakes)
    }

    @Test
    fun oneOrTwoBlockedTapsDoNotFailTheLevel() {
        val puzzle = easyPuzzle("AR-SPEC-TWO-LIVES", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)

        val one = ArrowsEngine.applyAction(start, slide(1, 1))
        val two = ArrowsEngine.applyAction(one, slide(1, 1))

        assertEquals(2, two.mistakes)
        assertEquals(start.arrows, two.arrows)
        assertFalse(ArrowsEngine.isLost(puzzle, two))
    }

    @Test
    fun threeBlockedTapsFailTheLevelAndLeaveTheBoard() {
        val puzzle = easyPuzzle("AR-SPEC-THREE-MISTAKES", *BLOCKED_AND_FREE)
        val start = ArrowsEngine.initialState(puzzle)

        var state = start
        repeat(MISTAKE_LIMIT) {
            state = ArrowsEngine.applyAction(state, slide(1, 1))
        }

        assertEquals(MISTAKE_LIMIT, state.mistakes)
        assertEquals(start.arrows, state.arrows)
        assertTrue(ArrowsEngine.isLost(puzzle, state))
        assertFalse(ArrowsEngine.isWon(puzzle, state))
    }

    @Test
    fun tapOnAnEmptyCellThrowsAndDoesNotCountAsAMistake() {
        val puzzle = easyPuzzle("AR-SPEC-EMPTY-TAP", *CLEAR_RIGHT)
        val start = ArrowsEngine.initialState(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.applyAction(start, slide(0, 0))
        }
        assertEquals(0, start.mistakes)
        assertNotNull(start.arrowAt(1, 1))
    }

    @Test
    fun tapOutsideTheBoardThrows() {
        val puzzle = easyPuzzle("AR-SPEC-BOUNDS", *CLEAR_RIGHT)
        val start = ArrowsEngine.initialState(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            ArrowsEngine.applyAction(start, ArrowsAction(Cell(EASY_SIZE * EASY_SIZE)))
        }
    }
}
