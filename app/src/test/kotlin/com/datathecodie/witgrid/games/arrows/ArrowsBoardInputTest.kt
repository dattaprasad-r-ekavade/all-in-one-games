package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.arrows.ArrowsDirection
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArrowsBoardInputTest {
    private val puzzle = ArrowsBoardFixtures.easyPuzzle

    @Test
    fun tapsHintArrowUntilBoardIsClearWithZeroMistakes() {
        var state = ArrowsEngine.initialState(puzzle)
        assertEquals(3, ArrowsRules.MAX_MISTAKES)
        assertEquals(3, ArrowsBoardInput.livesLeft(state))
        assertFalse(ArrowsEngine.isWon(puzzle, state))

        var steps = 0
        while (!ArrowsEngine.isWon(puzzle, state)) {
            assertTrue(steps < 30)
            val hinted = ArrowsEngine.hintAction(puzzle, state)
            assertNotNull(hinted)
            val hint = hinted!!
            val label = ArrowsBoardInput.cellLabel(state, hint.cell.index)
            assertTrue(label.canSlide)
            val tap = ArrowsBoardInput.tapCell(state, hint.cell.index)
            assertEquals(ArrowTapFeedback.SLID, tap.feedback)
            assertEquals(hint, tap.action)
            state = ArrowsEngine.applyAction(state, tap.action!!)
            assertEquals(0, state.mistakes)
            steps++
        }

        assertTrue(ArrowsEngine.isWon(puzzle, state))
        assertFalse(ArrowsEngine.isLost(puzzle, state))
        assertEquals(0, state.mistakes)
        assertEquals(3, ArrowsBoardInput.livesLeft(state))
        assertEquals(4, state.slideCount)
        val afterWin = ArrowsBoardInput.tapCell(state, 2)
        assertNull(afterWin.action)
        assertEquals(ArrowTapFeedback.NONE, afterWin.feedback)
    }

    @Test
    fun blockedTapAddsOneMistakeAndLeavesTheArrow() {
        val start = ArrowsEngine.initialState(puzzle)
        val before = start.arrows
        val tap = ArrowsBoardInput.tapCell(start, ArrowsBoardFixtures.BLOCKED_CELL)

        assertEquals(ArrowTapFeedback.BLOCKED, tap.feedback)
        assertNotNull(tap.action)
        assertFalse(ArrowsBoardInput.canSlideNow(start, ArrowsBoardFixtures.BLOCKED_CELL))

        val next = ArrowsEngine.applyAction(start, tap.action!!)
        assertEquals(1, next.mistakes)
        assertEquals(2, ArrowsBoardInput.livesLeft(next))
        assertEquals(before, next.arrows)
        assertEquals(0, next.slideCount)
        assertFalse(ArrowsEngine.isLost(puzzle, next))
        assertFalse(ArrowsEngine.isWon(puzzle, next))
    }

    @Test
    fun thirdBlockedTapFailsAndAFurtherTapDoesNothing() {
        var state = ArrowsEngine.initialState(puzzle)
        repeat(3) {
            val tap = ArrowsBoardInput.tapCell(state, ArrowsBoardFixtures.BLOCKED_CELL)
            assertEquals(ArrowTapFeedback.BLOCKED, tap.feedback)
            state = ArrowsEngine.applyAction(state, tap.action!!)
        }

        assertEquals(3, state.mistakes)
        assertEquals(0, ArrowsBoardInput.livesLeft(state))
        assertTrue(ArrowsEngine.isLost(puzzle, state))
        val extra = ArrowsBoardInput.tapCell(state, ArrowsBoardFixtures.BLOCKED_CELL)
        assertNull(extra.action)
        assertEquals(ArrowTapFeedback.NONE, extra.feedback)
        val legalWhileLost = ArrowsBoardInput.tapCell(state, 2)
        assertNull(legalWhileLost.action)
        assertFalse(ArrowsBoardInput.cellLabel(state, 2).canSlide)
    }

    @Test
    fun emptyCellTapDoesNothing() {
        val state = ArrowsEngine.initialState(puzzle)
        val tap = ArrowsBoardInput.tapCell(state, 0)
        assertNull(tap.action)
        assertEquals(ArrowTapFeedback.NONE, tap.feedback)
        val label = ArrowsBoardInput.cellLabel(state, 0)
        assertEquals(1, label.row)
        assertEquals(1, label.column)
        assertNull(label.direction)
        assertFalse(label.canSlide)
    }

    @Test
    fun cellLabelsNameRowColumnDirectionAndSlide() {
        val state = ArrowsEngine.initialState(puzzle)
        val free = ArrowsBoardInput.cellLabel(state, 2)
        assertEquals(1, free.row)
        assertEquals(3, free.column)
        assertEquals(ArrowsDirection.UP, free.direction)
        assertTrue(free.canSlide)

        val blocked = ArrowsBoardInput.cellLabel(state, ArrowsBoardFixtures.BLOCKED_CELL)
        assertEquals(2, blocked.row)
        assertEquals(3, blocked.column)
        assertEquals(ArrowsDirection.UP, blocked.direction)
        assertFalse(blocked.canSlide)

        val down = ArrowsBoardInput.cellLabel(state, 14)
        assertEquals(3, down.row)
        assertEquals(5, down.column)
        assertEquals(ArrowsDirection.DOWN, down.direction)
        assertTrue(down.canSlide)
    }

    @Test
    fun directionLabelsMatchArrowDirections() {
        assertEquals(R.string.arrows_direction_up, arrowsDirectionLabelResource(ArrowsDirection.UP))
        assertEquals(R.string.arrows_direction_down, arrowsDirectionLabelResource(ArrowsDirection.DOWN))
        assertEquals(R.string.arrows_direction_left, arrowsDirectionLabelResource(ArrowsDirection.LEFT))
        assertEquals(R.string.arrows_direction_right, arrowsDirectionLabelResource(ArrowsDirection.RIGHT))
        assertEquals(R.string.arrows_can_slide, arrowsSlideLabelResource(true))
        assertEquals(R.string.arrows_cannot_slide, arrowsSlideLabelResource(false))
    }

    @Test
    fun outOfRangeTapThrows() {
        val state = ArrowsEngine.initialState(puzzle)
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsBoardInput.tapCell(state, -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArrowsBoardInput.tapCell(state, puzzle.cellCount)
        }
    }
}
