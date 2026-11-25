package com.datathecodie.witgrid.engine.coloursort.spec

import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.deadlockPredecessor
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.hasLegalPour
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.twoEmptyDeal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Plan 8.4: Colour Sort Continue adds one empty tube after failure. */
class ColourSortContinueSpecTest {

    @Test
    fun continueAfterLossAddsOneEmptyTubeAndClearsLost() {
        val puzzle = easyPuzzle("CS-SPEC-CONTINUE", deadlockPredecessor)
        val lost = ColourSortEngine.applyAction(
            ColourSortEngine.initialState(puzzle),
            ColourSortAction(from = 2, to = 3),
        )
        assertTrue(ColourSortEngine.isLost(puzzle, lost))

        val continued = ColourSortEngine.applyContinue(lost)

        assertEquals(lost.tubes.size + 1, continued.tubes.size)
        assertTrue(continued.tubes.last().isEmpty())
        assertEquals(lost.tubes, continued.tubes.dropLast(1))
        assertFalse(ColourSortEngine.isLost(puzzle, continued))
        assertFalse(ColourSortEngine.isWon(puzzle, continued))
        assertTrue(hasLegalPour(continued.tubes))
    }

    @Test
    fun continueWhenNotLostThrows() {
        val puzzle = easyPuzzle("CS-SPEC-CONTINUE-ILLEGAL", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)
        assertFalse(ColourSortEngine.isLost(puzzle, start))

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyContinue(start)
        }
    }
}
