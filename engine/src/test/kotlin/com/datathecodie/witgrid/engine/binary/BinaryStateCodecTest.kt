package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BinaryStateCodecTest {
    @Test
    fun roundTripRetainsPuzzleContextAndPlayerSymbols() {
        val puzzle = puzzle()
        val state = BinaryEngine.initialState(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.FILLED))
            .withMove(Move.SetMark(Cell(1), Mark.CROSSED))

        val restored = BinaryEngine.decodeState(BinaryEngine.encodeState(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionAndTruncatedSaves() {
        val encoded = BinaryEngine.encodeState(BinaryEngine.initialState(puzzle()))
        val unsupported = encoded.copyOf().apply { this[4] = 2 }

        assertThrows(IllegalArgumentException::class.java) { BinaryEngine.decodeState(unsupported) }
        assertThrows(IllegalArgumentException::class.java) {
            BinaryEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
    }

    private fun puzzle() = BinaryPuzzle(
        id = "BI-STATE-6",
        size = 6,
        givens = List(36) { null },
        solution = listOf("001011", "001101", "110010", "010011", "101100", "110100")
            .flatMap { row -> row.map(Char::digitToInt) },
        pack = Pack.EASY,
        storedGrade = "fixture",
    )
}
