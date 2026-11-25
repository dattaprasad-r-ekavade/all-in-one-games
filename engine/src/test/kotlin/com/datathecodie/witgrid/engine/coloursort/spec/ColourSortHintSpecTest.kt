package com.datathecodie.witgrid.engine.coloursort.spec

import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.colourRuns
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.deadlockPredecessor
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.isLegalPour
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.twoEmptyDeal
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.wonSingleColourOrEmpty
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Independent hint cases. Plan 12.2: one pour that does not increase the number of colour runs.
 * PlayEngine: null when already won, already lost, or no improving move.
 */
class ColourSortHintSpecTest {

    @Test
    fun hintOnWonStateIsNull() {
        val puzzle = easyPuzzle("CS-SPEC-HINT-WON", wonSingleColourOrEmpty)
        val state = ColourSortEngine.initialState(puzzle)
        assertTrue(ColourSortEngine.isWon(puzzle, state))
        assertNull(ColourSortEngine.hintAction(puzzle, state))
    }

    @Test
    fun hintOnLostStateIsNull() {
        val puzzle = easyPuzzle("CS-SPEC-HINT-LOST", deadlockPredecessor)
        val lost = ColourSortEngine.applyAction(
            ColourSortEngine.initialState(puzzle),
            ColourSortAction(from = 2, to = 3),
        )
        assertTrue(ColourSortEngine.isLost(puzzle, lost))
        assertNull(ColourSortEngine.hintAction(puzzle, lost))
    }

    @Test
    fun hintOnMidGameReturnsLegalImprovingPourOrNull() {
        val puzzle = easyPuzzle("CS-SPEC-HINT-MID", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)
        val mid = ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 2))

        assertNotEquals(start.tubes, mid.tubes)
        assertFalse(ColourSortEngine.isWon(puzzle, mid))
        assertFalse(ColourSortEngine.isLost(puzzle, mid))

        // Always invoked on a mid-game state. Null is allowed; record it in the CS3 handoff.
        val hint = ColourSortEngine.hintAction(puzzle, mid)
        if (hint != null) {
            assertTrue(
                isLegalPour(mid.tubes, hint.from, hint.to),
                "hint pour ${hint.from}→${hint.to} must be legal on the mid-game board",
            )
            val afterHint = ColourSortEngine.applyAction(mid, hint)
            assertTrue(
                colourRuns(afterHint.tubes) <= colourRuns(mid.tubes),
                "plan 12.2: a hint pour must not increase colour runs",
            )
        }
    }
}
