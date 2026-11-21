package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsEngineTest {

    @Test
    fun engineExposesCorrectIdAndContracts() {
        assertEquals("tactics", TacticsEngine.id)

        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val line = TacticsEngine.encodePuzzle(puzzle)
        val decoded = TacticsEngine.decodePuzzle(line)
        assertEquals(puzzle, decoded)

        val s0 = TacticsEngine.initialState(puzzle)
        assertFalse(TacticsEngine.isSolved(puzzle, s0))
        assertEquals(emptyList<Cell>(), TacticsEngine.mistakes(puzzle, s0))

        val s1 = TacticsEngine.applyMove(s0, Move.SetMark(squareToCell("d7"), Mark.FILLED))
        val s2 = TacticsEngine.applyMove(s1, Move.SetMark(squareToCell("g7"), Mark.FILLED))
        assertTrue(TacticsEngine.isSolved(puzzle, s2))

        val encodedState = TacticsEngine.encodeState(s2)
        val restoredState = TacticsEngine.decodeState(encodedState)
        assertEquals(s2, restoredState)
        assertTrue(TacticsEngine.isSolved(puzzle, restoredState))
    }

    @Test
    fun unimplementedFeaturesThrowInX1() {
        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val state = TacticsEngine.initialState(puzzle)

        val nextStepError = assertThrows(NotImplementedError::class.java) {
            TacticsEngine.nextStep(puzzle, state)
        }
        assertEquals("An operation is not implemented: tactics", nextStepError.message)

        val gradeError = assertThrows(NotImplementedError::class.java) {
            TacticsEngine.grade(puzzle)
        }
        assertEquals("An operation is not implemented: tactics", gradeError.message)

        val generateError = assertThrows(NotImplementedError::class.java) {
            TacticsEngine.generate(SplitMix64(42L), Pack.EASY)
        }
        assertEquals("An operation is not implemented: tactics", generateError.message)
    }
}
