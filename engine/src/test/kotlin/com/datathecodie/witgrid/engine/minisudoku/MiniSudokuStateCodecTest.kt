package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MiniSudokuStateCodecTest {
    @Test
    fun roundTripRetainsPuzzleGivensDigitsAndNotes() {
        val puzzle = puzzle()
        val state = MiniSudokuEngine.initialState(puzzle)
            .withMove(Move.SetDigit(Cell(0), 1))
            .withMove(Move.SetNotes(Cell(1), 0b1010))

        val restored = MiniSudokuEngine.decodeState(MiniSudokuEngine.encodeState(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionAndTruncatedSaves() {
        val encoded = MiniSudokuEngine.encodeState(MiniSudokuEngine.initialState(puzzle()))
        val unsupported = encoded.copyOf().apply { this[4] = 2 }

        assertThrows(IllegalArgumentException::class.java) { MiniSudokuEngine.decodeState(unsupported) }
        assertThrows(IllegalArgumentException::class.java) {
            MiniSudokuEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
    }

    private fun puzzle() = MiniSudokuPuzzle(
        id = "MS-STATE-6",
        givens = List(36) { null },
        solution = listOf("123456", "456123", "234561", "561234", "345612", "612345")
            .flatMap { row -> row.map(Char::digitToInt) },
        pack = Pack.EASY,
        storedGrade = "fixture",
    )
}
