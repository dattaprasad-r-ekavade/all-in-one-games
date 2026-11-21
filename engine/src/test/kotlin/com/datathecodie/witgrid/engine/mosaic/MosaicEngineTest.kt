package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicEngineTest {
    private val solution8x8 = listOf(
        1, 0, 0, 1, 1, 0, 0, 1,
        0, 1, 1, 0, 0, 1, 1, 0,
        0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1,
        1, 0, 0, 1, 1, 0, 0, 1,
        0, 1, 1, 0, 0, 1, 1, 0,
        0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1,
    )
    private val grid8 = Grid(8)
    private val clues8x8 = (0 until 64).map { MosaicRules.clueAt(grid8, solution8x8, Cell(it)) }
    private val puzzle = MosaicPuzzle("MO-ENG-1", 8, clues8x8, solution8x8, Pack.EASY, "none:1")

    @Test
    fun engineContractIdIsMosaic() {
        assertEquals("mosaic", MosaicEngine.id)
    }

    @Test
    fun initialStateHasEmptyMarksAndChecksSolution() {
        val state = MosaicEngine.initialState(puzzle)
        assertEquals(64, state.marks.size)
        assertTrue(state.marks.all { it == Mark.EMPTY })
    }

    @Test
    fun applyMoveUpdatesMarksAndRejectsInvalidMoves() {
        val state = MosaicEngine.initialState(puzzle)
        val updated = MosaicEngine.applyMove(state, Move.SetMark(Cell(0), Mark.FILLED))
        assertEquals(Mark.FILLED, updated.marks[0])

        assertThrows(IllegalArgumentException::class.java) {
            MosaicEngine.applyMove(state, Move.SetDigit(Cell(0), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicEngine.applyMove(state, Move.SetNotes(Cell(0), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicEngine.applyMove(state, Move.SetMark(Cell(64), Mark.FILLED))
        }
    }

    @Test
    fun rulesDelegationOperatesCorrectly() {
        val state = MosaicEngine.initialState(puzzle)
        assertFalse(MosaicEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), MosaicEngine.mistakes(puzzle, state))
        assertEquals(1, MosaicEngine.countSolutions(puzzle, limit = 2))
    }

    @Test
    fun codecDelegationOperatesCorrectly() {
        val line = MosaicEngine.encodePuzzle(puzzle)
        val decoded = MosaicEngine.decodePuzzle(line)
        assertEquals(puzzle, decoded)

        val state = MosaicEngine.initialState(puzzle)
        val bytes = MosaicEngine.encodeState(state)
        val decodedState = MosaicEngine.decodeState(bytes)
        assertEquals(state.puzzle, decodedState.puzzle)
        assertEquals(state.marks, decodedState.marks)
    }

    @Test
    fun x2AndGeneratorOperationsThrowNotImplementedError() {
        val state = MosaicEngine.initialState(puzzle)
        val rng = SplitMix64(42L)

        val nextStepError = assertThrows(NotImplementedError::class.java) {
            MosaicEngine.nextStep(puzzle, state)
        }
        assertEquals("MO", nextStepError.message)

        val gradeError = assertThrows(NotImplementedError::class.java) {
            MosaicEngine.grade(puzzle)
        }
        assertEquals("MO", gradeError.message)

        val generateError = assertThrows(NotImplementedError::class.java) {
            MosaicEngine.generate(rng, Pack.EASY)
        }
        assertEquals("MO", generateError.message)
    }
}
