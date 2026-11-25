package com.datathecodie.witgrid.engine.coloursort.spec

import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.afterPartialTopRunFits
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.deadlockAfterPour
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.deadlockPredecessor
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.easyPuzzle
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.fullMatchingDest
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.hasLegalPour
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.isWon
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.onePourFromWin
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.partialTopRunFits
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.pouredTubes
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.twoEmptyAfterFirstPour
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.twoEmptyDeal
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.wholeTopRunOntoMatch
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.wonPartialSingleColour
import com.datathecodie.witgrid.engine.coloursort.spec.ColourSortSpecFixtures.wonSingleColourOrEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Colour Sort play cases from plan 12.2 and the CS1 lead note. */
class ColourSortPlaySpecTest {

    @Test
    fun pouringWholeTopRunOntoEmptyLeavesLowerColours() {
        val puzzle = easyPuzzle("CS-SPEC-EMPTY-DEST", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)
        val next = ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 2))

        assertEquals(twoEmptyAfterFirstPour, next.tubes)
        assertEquals(twoEmptyDeal, start.tubes)
        assertFalse(ColourSortEngine.isWon(puzzle, next))
    }

    @Test
    fun pouringWholeTopRunOntoMatchingDestWithSpaceWins() {
        val puzzle = easyPuzzle("CS-SPEC-MATCH-RUN", wholeTopRunOntoMatch)
        val start = ColourSortEngine.initialState(puzzle)
        val next = ColourSortEngine.applyAction(start, ColourSortAction(from = 1, to = 0))

        assertEquals(pouredTubes(wholeTopRunOntoMatch, from = 1, to = 0), next.tubes)
        assertTrue(isWon(next.tubes))
        assertTrue(ColourSortEngine.isWon(puzzle, next))
    }

    @Test
    fun pouringMovesOnlyTheTopRunUnitsThatFit() {
        val puzzle = easyPuzzle("CS-SPEC-PARTIAL-RUN", partialTopRunFits)
        val start = ColourSortEngine.initialState(puzzle)
        val next = ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 2))

        assertEquals(afterPartialTopRunFits, next.tubes)
        assertEquals(listOf(2, 1), next.tubes[0])
        assertEquals(listOf(2, 1, 1, 1), next.tubes[2])
    }

    @Test
    fun winWhenEveryTubeIsEmptyOrASingleColour() {
        val puzzle = easyPuzzle("CS-SPEC-WON", wonSingleColourOrEmpty)
        val state = ColourSortEngine.initialState(puzzle)

        assertTrue(isWon(state.tubes))
        assertTrue(ColourSortEngine.isWon(puzzle, state))
        assertFalse(ColourSortEngine.isLost(puzzle, state))
    }

    @Test
    fun winAllowsASingleColourThatDoesNotFillTheTube() {
        val puzzle = easyPuzzle("CS-SPEC-WON-PARTIAL", wonPartialSingleColour)
        val state = ColourSortEngine.initialState(puzzle)

        assertTrue(ColourSortEngine.isWon(puzzle, state))
    }

    @Test
    fun mixedTubeIsNotWon() {
        val puzzle = easyPuzzle("CS-SPEC-MIXED", twoEmptyDeal)
        val state = ColourSortEngine.initialState(puzzle)

        assertFalse(isWon(state.tubes))
        assertFalse(ColourSortEngine.isWon(puzzle, state))
    }

    @Test
    fun oneMatchingPourReachesWon() {
        val puzzle = easyPuzzle("CS-SPEC-WIN-POUR", onePourFromWin)
        val start = ColourSortEngine.initialState(puzzle)
        assertFalse(ColourSortEngine.isWon(puzzle, start))

        val won = ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 2))
        assertEquals(
            listOf(
                listOf(1, 1),
                listOf(1, 1),
                listOf(2, 2, 2, 2),
                emptyList(),
            ),
            won.tubes,
        )
        assertTrue(ColourSortEngine.isWon(puzzle, won))
        assertFalse(ColourSortEngine.isLost(puzzle, won))
    }

    @Test
    fun isLostIsFalseOnTheDealEvenWhenALaterPourCanDeadlock() {
        val puzzle = easyPuzzle("CS-SPEC-LOSE-DEAL", deadlockPredecessor)
        val start = ColourSortEngine.initialState(puzzle)

        assertTrue(hasLegalPour(start.tubes))
        assertFalse(ColourSortEngine.isWon(puzzle, start))
        assertFalse(ColourSortEngine.isLost(puzzle, start))
    }

    @Test
    fun isLostAfterALegalPourLeavesNoLegalPourAndNotWon() {
        val puzzle = easyPuzzle("CS-SPEC-LOSE", deadlockPredecessor)
        val start = ColourSortEngine.initialState(puzzle)
        val lost = ColourSortEngine.applyAction(start, ColourSortAction(from = 2, to = 3))

        assertEquals(deadlockAfterPour, lost.tubes)
        assertFalse(hasLegalPour(lost.tubes))
        assertFalse(isWon(lost.tubes))
        assertFalse(ColourSortEngine.isWon(puzzle, lost))
        assertTrue(ColourSortEngine.isLost(puzzle, lost))
    }

    @Test
    fun pourOntoMismatchedNonEmptyDestThrows() {
        val puzzle = easyPuzzle("CS-SPEC-MISMATCH", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)
        val afterEmptyDest = ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 2))

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyAction(afterEmptyDest, ColourSortAction(from = 0, to = 2))
        }
    }

    @Test
    fun pourFromEmptySourceThrows() {
        val puzzle = easyPuzzle("CS-SPEC-EMPTY-SRC", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyAction(start, ColourSortAction(from = 3, to = 2))
        }
    }

    @Test
    fun pourOntoFullDestThrowsEvenWhenColoursMatch() {
        val puzzle = easyPuzzle("CS-SPEC-FULL-DEST", fullMatchingDest)
        val start = ColourSortEngine.initialState(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyAction(start, ColourSortAction(from = 1, to = 0))
        }
    }

    @Test
    fun pourFromATubeOntoItselfThrows() {
        val puzzle = easyPuzzle("CS-SPEC-SAME-TUBE", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 0))
        }
    }

    @Test
    fun pourWithOutOfRangeIndexThrows() {
        val puzzle = easyPuzzle("CS-SPEC-BOUNDS", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyAction(start, ColourSortAction(from = 0, to = 4))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ColourSortEngine.applyAction(start, ColourSortAction(from = -1, to = 2))
        }
    }

    @Test
    fun easyDealHasFourTubesOfCapacityFour() {
        val puzzle = easyPuzzle("CS-SPEC-EASY-SIZE", twoEmptyDeal)
        val start = ColourSortEngine.initialState(puzzle)

        assertEquals(4, start.tubes.size)
        assertTrue(start.tubes.all { it.size <= ColourSortSpecFixtures.CAPACITY })
        assertEquals(2, start.tubes.count { it.isEmpty() })
    }

    @Test
    fun engineIdIsColourSort() {
        assertEquals("coloursort", ColourSortEngine.id)
    }
}
