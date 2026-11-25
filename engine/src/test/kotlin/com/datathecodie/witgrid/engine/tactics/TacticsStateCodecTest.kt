package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsStateCodecTest {

    @Test
    fun roundTripsEmptyState() {
        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val state = TacticsState.initial(puzzle)

        val encoded = TacticsStateCodec.encode(state)
        val decoded = TacticsStateCodec.decode(encoded)

        assertEquals(state, decoded)
        assertEquals(emptyList<Cell>(), decoded.picks)
    }

    @Test
    fun roundTripsInProgressState() {
        val puzzle = TacticsRulesTest.mate2UniquePuzzle()
        val s0 = TacticsState.initial(puzzle)
        val s1 = s0.withMove(Move.SetMark(squareToCell("d3"), Mark.FILLED))

        val encoded = TacticsStateCodec.encode(s1)
        val decoded = TacticsStateCodec.decode(encoded)

        assertEquals(s1, decoded)
        assertEquals(squareToCell("d3"), decoded.pendingFrom)
    }

    @Test
    fun roundTripsCompletedAndMistakeStates() {
        val puzzle = TacticsRulesTest.mate2UniquePuzzle()
        val s0 = TacticsState.initial(puzzle)

        // 1. Wrong legal move (mistake state)
        val mistakeMove = ChessMove.fromUci("c4a2")
        val sMistake = s0.withMove(Move.SetMark(mistakeMove.from, Mark.FILLED))
            .withMove(Move.SetMark(mistakeMove.to, Mark.FILLED))
        assertEquals(listOf(mistakeMove.to), TacticsRules.mistakes(puzzle, sMistake))
        assertNull(sMistake.lastOpponentMove)

        val encodedMistake = TacticsStateCodec.encode(sMistake)
        val decodedMistake = TacticsStateCodec.decode(encodedMistake)

        assertEquals(sMistake, decodedMistake)
        assertEquals(listOf(mistakeMove.to), TacticsRules.mistakes(puzzle, decodedMistake))
        // Verify that restored state still blocks defender input
        val attemptedDefenderPick = decodedMistake.withMove(Move.SetMark(squareToCell("d8"), Mark.FILLED))
        assertEquals(decodedMistake, attemptedDefenderPick)
        assertNull(attemptedDefenderPick.pendingFrom)

        // 2. Completed state
        val s1 = s0.withMove(Move.SetMark(squareToCell("d3"), Mark.FILLED))
            .withMove(Move.SetMark(squareToCell("d8"), Mark.FILLED))
        val s2 = s1.withMove(Move.SetMark(squareToCell("e1"), Mark.FILLED))
            .withMove(Move.SetMark(squareToCell("e8"), Mark.FILLED))
        assertTrue(TacticsRules.isSolved(puzzle, s2))

        val encodedCompleted = TacticsStateCodec.encode(s2)
        val decodedCompleted = TacticsStateCodec.decode(encodedCompleted)

        assertEquals(s2, decodedCompleted)
        assertTrue(TacticsRules.isSolved(puzzle, decodedCompleted))
    }

    @Test
    fun rejectsMalformedBytes() {
        assertThrows(IllegalArgumentException::class.java) {
            TacticsStateCodec.decode(byteArrayOf())
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsStateCodec.decode(byteArrayOf(1, 2, 3, 4, 5))
        }
    }
}
