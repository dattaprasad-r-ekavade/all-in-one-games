package com.datathecodie.witgrid.engine.path.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.path.PathRules
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.corridorPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.rowsPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.solvedDigits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Path plan-rule cases: input, givens, solved check and mistakes. */
class PathStateSpecTest {

    @Test
    fun initialStateKeepsOnlyEndpointsAsGivens() {
        val state = PathState.initial(rowsPuzzle)
        assertEquals(1, state.digits[0])
        assertEquals(1, state.digits[3])
        assertNull(state.digits[1])
        assertNull(state.digits[15])
    }

    @Test
    fun dragPaintSetsOneCellAndZeroClearsIt() {
        val painted = PathState.initial(corridorPuzzle).withMove(Move.SetDigit(Cell(1), 1))
        assertEquals(1, painted.digits[1])

        val cleared = painted.withMove(Move.SetDigit(Cell(1), 0))
        assertNull(cleared.digits[1])
    }

    @Test
    fun givenEndpointCannotBeEdited() {
        val state = PathState.initial(corridorPuzzle)
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(0), 0))
        }
    }

    @Test
    fun wallCannotBeEdited() {
        val state = PathState.initial(rowsPuzzle)
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(15), 4))
        }
    }

    @Test
    fun colourAbovePairCountIsRejected() {
        val state = PathState.initial(corridorPuzzle)
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(1), 2))
        }
    }

    @Test
    fun fullStoredSolutionCountsAsSolved() {
        val state = PathState(rowsPuzzle, solvedDigits(rowsPuzzle))
        assertTrue(PathRules.isSolved(rowsPuzzle, state))
    }

    @Test
    fun oneEmptyCellMeansNotSolved() {
        val digits = solvedDigits(corridorPuzzle).toMutableList().also { it[7] = null }
        assertFalse(PathRules.isSolved(corridorPuzzle, PathState(corridorPuzzle, digits)))
    }

    @Test
    fun wrongColourMeansNotSolved() {
        val state = PathState.initial(rowsPuzzle).withMove(Move.SetDigit(Cell(5), 3))
        assertFalse(PathRules.isSolved(rowsPuzzle, state))
    }

    @Test
    fun wrongColourIsReportedAsMistake() {
        val state = PathState.initial(rowsPuzzle).withMove(Move.SetDigit(Cell(5), 3))
        assertEquals(listOf(5), PathRules.mistakes(rowsPuzzle, state).map { it.index })
    }

    @Test
    fun emptyCellsAndGivensAreNeverMistakes() {
        val state = PathState.initial(rowsPuzzle)
        assertTrue(PathRules.mistakes(rowsPuzzle, state).isEmpty())
    }
}
