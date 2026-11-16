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

    @Test
    fun rejectsDigitsOutsideOneThroughSix() {
        val encoded = MiniSudokuEngine.encodeState(MiniSudokuEngine.initialState(puzzle()))
        val digitSeven = encoded.copyOf().apply { this[cellByteOffset(0)] = 7 }
        val digitMax = encoded.copyOf().apply { this[cellByteOffset(0)] = -1 }

        assertThrows(IllegalArgumentException::class.java) { MiniSudokuEngine.decodeState(digitSeven) }
        assertThrows(IllegalArgumentException::class.java) { MiniSudokuEngine.decodeState(digitMax) }
    }

    @Test
    fun rejectsNoteBitsAboveDigitSix() {
        val encoded = MiniSudokuEngine.encodeState(MiniSudokuEngine.initialState(puzzle()))
        val noteSeven = encoded.copyOf().apply { this[cellByteOffset(0) + 1] = 0b100_0000 }

        assertThrows(IllegalArgumentException::class.java) { MiniSudokuEngine.decodeState(noteSeven) }
    }

    @Test
    fun acceptsEveryNoteBitForDigitsOneThroughSix() {
        val state = MiniSudokuEngine.initialState(puzzle()).withMove(Move.SetNotes(Cell(0), 0b11_1111))

        assertEquals(state, MiniSudokuEngine.decodeState(MiniSudokuEngine.encodeState(state)))
    }

    /** Byte offset of a cell's digit: 5 header bytes, a length-prefixed puzzle text, a cell count, then pairs. */
    private fun cellByteOffset(index: Int): Int {
        val puzzleTextBytes = MiniSudokuCodec.encode(puzzle()).toByteArray(Charsets.UTF_8).size
        return 5 + 2 + puzzleTextBytes + 4 + 2 * index
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
