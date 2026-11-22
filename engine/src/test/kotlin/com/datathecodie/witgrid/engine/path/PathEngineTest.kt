package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathEngineTest {
    private val sampleLine = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|EASY|easy:1"

    @Test
    fun delegatesSharedContractMethods() {
        assertEquals("path", PathEngine.id)

        val puzzle = PathEngine.decodePuzzle(sampleLine)
        assertEquals(sampleLine, PathEngine.encodePuzzle(puzzle))

        val state = PathEngine.initialState(puzzle)
        assertFalse(PathEngine.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), PathEngine.mistakes(puzzle, state))
        assertEquals(1, PathEngine.countSolutions(puzzle, limit = 2))

        val moved = PathEngine.applyMove(state, Move.SetDigit(Cell(1), 1))
        assertEquals(1, moved.digits[1])

        val bytes = PathEngine.encodeState(moved)
        val decoded = PathEngine.decodeState(bytes)
        assertEquals(moved, decoded)
    }

    @Test
    fun engineGeneratesValidPuzzle() {
        val puzzle = PathEngine.decodePuzzle(sampleLine)
        val state = PathEngine.initialState(puzzle)

        val step = PathEngine.nextStep(puzzle, state)
        assertTrue(step != null)

        val grade = PathEngine.grade(puzzle)
        assertEquals(Pack.EASY, grade.pack)

        val generated = PathEngine.generate(SplitMix64(1), Pack.EASY)
        assertEquals(6, generated.size)
        assertEquals(Pack.EASY, generated.pack)
        assertTrue(PathRules.isValidSolution(generated))
    }
}
