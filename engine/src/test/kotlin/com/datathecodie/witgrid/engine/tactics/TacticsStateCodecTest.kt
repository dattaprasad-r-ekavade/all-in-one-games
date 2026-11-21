package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
        val s1 = s0.withMove(Move.SetMark(squareToCell("d3"), Mark.FILLED))
        val s2 = s1.withMove(Move.SetMark(squareToCell("d8"), Mark.FILLED))

        val encoded = TacticsStateCodec.encode(s2)
        val decoded = TacticsStateCodec.decode(encoded)

        assertEquals(s2, decoded)
        assertEquals(s2.currentBoard, decoded.currentBoard)
        assertEquals(s2.lastOpponentMove, decoded.lastOpponentMove)
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
