package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NonogramStateCodecTest {
    @Test
    fun roundTripRetainsPuzzleCluesAndPlayerMarks() {
        val puzzle = puzzle()
        val state = NonogramEngine.initialState(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.FILLED))
            .withMove(Move.SetMark(Cell(1), Mark.CROSSED))

        val restored = NonogramEngine.decodeState(NonogramEngine.encodeState(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionAndTruncatedSaves() {
        val encoded = NonogramEngine.encodeState(NonogramEngine.initialState(puzzle()))
        val unsupported = encoded.copyOf().apply { this[4] = 2 }

        assertThrows(IllegalArgumentException::class.java) { NonogramEngine.decodeState(unsupported) }
        assertThrows(IllegalArgumentException::class.java) {
            NonogramEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
    }

    private fun puzzle() = NonogramPuzzle(
        id = "NO-STATE-5",
        size = 5,
        rowClues = listOf(listOf(5), emptyList(), listOf(5), emptyList(), listOf(5)),
        columnClues = List(5) { listOf(1, 1, 1) },
        solution = listOf("11111", "00000", "11111", "00000", "11111")
            .flatMap { row -> row.map(Char::digitToInt) },
        pack = Pack.EASY,
        storedGrade = "fixture",
    )
}
