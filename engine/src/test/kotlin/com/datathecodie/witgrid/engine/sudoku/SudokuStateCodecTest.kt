package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SudokuStateCodecTest {
    @Test
    fun roundTripRetainsPuzzleGivensDigitsAndNotes() {
        val puzzle = puzzle()
        val state = SudokuEngine.initialState(puzzle)
            .withMove(Move.SetDigit(Cell(0), 1))
            .withMove(Move.SetNotes(Cell(1), 0b101001010))

        val restored = SudokuEngine.decodeState(SudokuEngine.encodeState(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionAndTruncatedSaves() {
        val encoded = SudokuEngine.encodeState(SudokuEngine.initialState(puzzle()))
        val unsupported = encoded.copyOf().apply { this[4] = 2 }

        assertThrows(IllegalArgumentException::class.java) { SudokuEngine.decodeState(unsupported) }
        assertThrows(IllegalArgumentException::class.java) {
            SudokuEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
    }

    @Test
    fun rejectsDigitsOutsideOneThroughNine() {
        val encoded = SudokuEngine.encodeState(SudokuEngine.initialState(puzzle()))
        val digitTen = encoded.copyOf().apply { this[cellByteOffset(0)] = 10 }

        assertThrows(IllegalArgumentException::class.java) { SudokuEngine.decodeState(digitTen) }
    }

    @Test
    fun rejectsNoteBitsAboveDigitNine() {
        val encoded = SudokuEngine.encodeState(SudokuEngine.initialState(puzzle()))
        val noteTen = encoded.copyOf().apply {
            val offset = cellByteOffset(0)
            this[offset + 1] = 0x02
            this[offset + 2] = 0x00
        }

        assertThrows(IllegalArgumentException::class.java) { SudokuEngine.decodeState(noteTen) }
    }

    @Test
    fun acceptsEveryNoteBitForDigitsOneThroughNine() {
        val state = SudokuEngine.initialState(puzzle()).withMove(Move.SetNotes(Cell(0), SUDOKU_NOTE_MASK))

        assertEquals(state, SudokuEngine.decodeState(SudokuEngine.encodeState(state)))
    }

    /** Byte offset of a cell's digit: header, puzzle text, cell count, then digit plus two-byte notes per cell. */
    private fun cellByteOffset(index: Int): Int {
        val puzzleTextBytes = SudokuCodec.encode(puzzle()).toByteArray(Charsets.UTF_8).size
        return 5 + 2 + puzzleTextBytes + 4 + 3 * index
    }

    private fun puzzle() = SudokuPuzzle(
        id = "SU-STATE-9",
        givens = List(81) { null },
        solution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) },
        pack = Pack.EASY,
        storedGrade = "fixture",
    )
}