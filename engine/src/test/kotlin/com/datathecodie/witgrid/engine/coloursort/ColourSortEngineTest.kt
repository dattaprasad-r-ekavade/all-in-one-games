package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ColourSortEngineTest {
    private val engine = ColourSortEngine

    private val easyFixture = ColourSortPuzzle(
        bankId = "cs-fixture-4",
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
    fun initialDealIsNotLost() {
        val puzzle = engine.decodePuzzle(engine.encodePuzzle(easyFixture))
        val start = engine.initialState(puzzle)
        assertFalse(engine.isLost(puzzle, start))
        assertFalse(engine.isWon(puzzle, start))
    }

    @Test
    fun poursToWin() {
        val start = engine.initialState(easyFixture)
        val sequence = listOf(
            ColourSortAction(2, 3),
            ColourSortAction(1, 2),
            ColourSortAction(0, 1),
            ColourSortAction(0, 3),
        )
        val won = sequence.fold(start) { state, action -> engine.applyAction(state, action) }
        assertTrue(engine.isWon(easyFixture, won))
        assertFalse(engine.isLost(easyFixture, won))
    }

    @Test
    fun illegalPourThrows() {
        val start = engine.initialState(easyFixture)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, ColourSortAction(0, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(start, ColourSortAction(3, 0))
        }
    }

    @Test
    fun stuckPositionIsLostAfterPour() {
        val stuckTubes = listOf(
            listOf(1, 2, 3, 4),
            listOf(2, 3, 4, 1),
            listOf(3, 4, 1, 2),
            listOf(4, 1, 2, 3),
        )
        val puzzle = easyFixture.copy(initialTubes = stuckTubes)
        val state = ColourSortState(puzzle, stuckTubes, pourCount = 1)
        assertTrue(engine.isLost(puzzle, state))
        assertFalse(engine.isWon(puzzle, state))
    }

    @Test
    fun continueAddsTubeAndClearsLoss() {
        val stuckTubes = listOf(
            listOf(1, 2, 3, 4),
            listOf(2, 3, 4, 1),
            listOf(3, 4, 1, 2),
            listOf(4, 1, 2, 3),
        )
        val puzzle = easyFixture.copy(initialTubes = stuckTubes)
        val lost = ColourSortState(puzzle, stuckTubes, pourCount = 2)
        assertTrue(engine.isLost(puzzle, lost))

        val continued = engine.applyContinue(lost)
        assertEquals(5, continued.tubes.size)
        assertTrue(continued.tubes.last().isEmpty())
        assertFalse(engine.isLost(puzzle, continued))
    }

    @Test
    fun continueThrowsWhenNotLost() {
        val start = engine.initialState(easyFixture)
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(start)
        }
    }

    @Test
    fun bankLineRoundTrips() {
        val encoded = engine.encodePuzzle(easyFixture)
        val puzzle = engine.decodePuzzle(encoded)
        assertEquals(easyFixture, puzzle)
    }

    @Test
    fun malformedBankLineThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("only-three|fields|here")
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("cs-bad|4|1122/2233/3311|EASY|d1")
        }
    }

    @Test
    fun stateCodecRoundTrips() {
        val mid = engine.applyAction(engine.initialState(easyFixture), ColourSortAction(2, 3))
        val restored = engine.decodeState(engine.encodeState(mid))
        assertEquals(mid, restored)
    }

    @Test
    fun malformedStateThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun deferredFeaturesThrow() {
        val start = engine.initialState(easyFixture)
        assertThrows(NotImplementedError::class.java) {
            engine.hintAction(easyFixture, start)
        }
        assertThrows(NotImplementedError::class.java) {
            engine.generate(SplitMix64(1), Pack.EASY)
        }
    }
}