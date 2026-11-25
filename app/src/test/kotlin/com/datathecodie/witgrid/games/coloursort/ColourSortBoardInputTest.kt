package com.datathecodie.witgrid.games.coloursort

import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.ColourSortPuzzle
import com.datathecodie.witgrid.engine.coloursort.ColourSortState
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColourSortBoardInputTest {
    private val easyFixture = ColourSortPuzzle(
        bankId = "cs-easy-fixture",
        pack = Pack.EASY,
        difficulty = "d1",
        initialTubes = listOf(
            listOf(1, 1, 2, 2),
            listOf(2, 2, 3, 3),
            listOf(3, 3, 1, 1),
            emptyList(),
        ),
    )

    @Test
    fun poursEasyFixtureToWonViaTapSourceTapDestination() {
        var state = ColourSortEngine.initialState(easyFixture)
        var selectedTube: Int? = null
        assertFalse(ColourSortEngine.isWon(easyFixture, state))

        val tapPairs = listOf(
            2 to 3,
            1 to 2,
            0 to 1,
        )

        for ((source, dest) in tapPairs) {
            val selectResult = ColourSortBoardInput.tapTube(state, selectedTube, source)
            assertEquals(source, selectResult.selectedTube)
            assertNull(selectResult.action)
            assertEquals(TapFeedback.SELECTED, selectResult.feedback)
            assertTrue(ColourSortBoardInput.isLegalDestination(state, selectResult.selectedTube, dest))
            selectedTube = selectResult.selectedTube

            val pourResult = ColourSortBoardInput.tapTube(state, selectedTube, dest)
            assertNull(pourResult.selectedTube)
            assertEquals(ColourSortAction(source, dest), pourResult.action)
            assertEquals(TapFeedback.POURED, pourResult.feedback)
            selectedTube = pourResult.selectedTube

            state = ColourSortEngine.applyAction(state, pourResult.action!!)
        }

        assertTrue(ColourSortEngine.isWon(easyFixture, state))
        assertFalse(ColourSortEngine.isLost(easyFixture, state))
        assertEquals(3, state.pourCount)
        assertTrue(ColourSortBoardInput.isTubeSortedAndFull(state.tubes[1]))
        assertTrue(ColourSortBoardInput.isTubeSortedAndFull(state.tubes[2]))
    }

    @Test
    fun tappingEmptyTubeWithNoSelectionIsANoOp() {
        val state = ColourSortEngine.initialState(easyFixture)

        val result = ColourSortBoardInput.tapTube(state, selectedTube = null, tappedTube = 3)

        assertNull(result.selectedTube)
        assertNull(result.action)
        assertEquals(TapFeedback.NONE, result.feedback)
    }

    @Test
    fun tappingSelectedTubeDeselectsIt() {
        val state = ColourSortEngine.initialState(easyFixture)

        val first = ColourSortBoardInput.tapTube(state, selectedTube = null, tappedTube = 0)
        assertEquals(0, first.selectedTube)

        val second = ColourSortBoardInput.tapTube(state, selectedTube = first.selectedTube, tappedTube = 0)
        assertNull(second.selectedTube)
        assertNull(second.action)
        assertEquals(TapFeedback.DESELECTED, second.feedback)
    }

    @Test
    fun tappingIncompatibleNonEmptyTubeSwitchesSelection() {
        val state = ColourSortEngine.initialState(easyFixture)

        val first = ColourSortBoardInput.tapTube(state, selectedTube = null, tappedTube = 0)
        assertEquals(0, first.selectedTube)
        assertFalse(ColourSortBoardInput.isLegalDestination(state, first.selectedTube, 1))

        val switched = ColourSortBoardInput.tapTube(state, selectedTube = first.selectedTube, tappedTube = 1)
        assertEquals(1, switched.selectedTube)
        assertNull(switched.action)
        assertEquals(TapFeedback.SELECTED, switched.feedback)
    }

    @Test
    fun tapsDoNothingOnceWonOrDeadlocked() {
        val wonTubes = listOf(
            listOf(1, 1, 1, 1),
            listOf(2, 2, 2, 2),
            listOf(3, 3, 3, 3),
            emptyList(),
        )
        val wonState = ColourSortState(easyFixture, wonTubes, pourCount = 4)
        val wonTap = ColourSortBoardInput.tapTube(wonState, selectedTube = null, tappedTube = 0)
        assertEquals(TapFeedback.NONE, wonTap.feedback)
        assertNull(wonTap.selectedTube)
        assertNull(wonTap.action)
        assertFalse(ColourSortBoardInput.isLegalDestination(wonState, 0, 3))

        val deadlockDeal = listOf(
            listOf(1, 2, 4, 5),
            listOf(2, 3, 5, 1),
            listOf(4, 5, 3, 2),
            listOf(4, 5, 2),
        )
        val deadlockPuzzle = easyFixture.copy(initialTubes = deadlockDeal)
        val lostState = ColourSortEngine.applyAction(
            ColourSortEngine.initialState(deadlockPuzzle),
            ColourSortAction(2, 3),
        )
        assertTrue(ColourSortEngine.isLost(deadlockPuzzle, lostState))
        val lostTap = ColourSortBoardInput.tapTube(lostState, selectedTube = null, tappedTube = 0)
        assertEquals(TapFeedback.NONE, lostTap.feedback)
        assertNull(lostTap.selectedTube)
        assertNull(lostTap.action)
    }

    @Test
    fun staleOrOutOfRangeSelectionIsHandledSafely() {
        val state = ColourSortEngine.initialState(easyFixture)

        val staleEmpty = ColourSortBoardInput.tapTube(state, selectedTube = 3, tappedTube = 0)
        assertEquals(0, staleEmpty.selectedTube)
        assertEquals(TapFeedback.SELECTED, staleEmpty.feedback)

        val staleOutOfBounds = ColourSortBoardInput.tapTube(state, selectedTube = 99, tappedTube = 2)
        assertEquals(2, staleOutOfBounds.selectedTube)
        assertEquals(TapFeedback.SELECTED, staleOutOfBounds.feedback)

        assertThrows(IllegalArgumentException::class.java) {
            ColourSortBoardInput.tapTube(state, selectedTube = null, tappedTube = 4)
        }
    }

    @Test
    fun colourLettersMapDeterministicallyAndSortedCheckRequiresFullTube() {
        assertEquals('A', ColourSortBoardInput.colourLetter(1))
        assertEquals('H', ColourSortBoardInput.colourLetter(8))
        assertEquals('I', ColourSortBoardInput.colourLetter(9))
        assertFalse(ColourSortBoardInput.isTubeSortedAndFull(emptyList()))
        assertFalse(ColourSortBoardInput.isTubeSortedAndFull(listOf(1, 1, 1)))
        assertFalse(ColourSortBoardInput.isTubeSortedAndFull(listOf(1, 1, 1, 2)))
        assertTrue(ColourSortBoardInput.isTubeSortedAndFull(listOf(2, 2, 2, 2)))
    }
}
