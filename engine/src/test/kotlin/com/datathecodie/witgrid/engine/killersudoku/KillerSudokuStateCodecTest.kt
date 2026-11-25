package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class KillerSudokuStateCodecTest {
    @Test
    fun roundTripRetainsPuzzleCagesGivensDigitsAndNotes() {
        val puzzle = puzzle(givens = List(81) { index -> if (index == 80) solution[80] else null })
        val state = KillerSudokuEngine.initialState(puzzle)
            .withMove(Move.SetDigit(Cell(0), 1))
            .withMove(Move.SetNotes(Cell(1), 0b101001010))

        val restored = KillerSudokuEngine.decodeState(KillerSudokuEngine.encodeState(state))

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionWrongMagicAndTruncatedSaves() {
        val encoded = KillerSudokuEngine.encodeState(KillerSudokuEngine.initialState(puzzle()))
        val unsupported = encoded.copyOf().apply { this[4] = 2 }
        val wrongMagic = encoded.copyOf().apply { this[0] = 0x00 }

        assertThrows(IllegalArgumentException::class.java) { KillerSudokuEngine.decodeState(unsupported) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuEngine.decodeState(wrongMagic) }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuEngine.decodeState(encoded + byteArrayOf(0))
        }
    }

    @Test
    fun rejectsDigitsOutsideOneThroughNine() {
        val encoded = KillerSudokuEngine.encodeState(KillerSudokuEngine.initialState(puzzle()))
        val digitTen = encoded.copyOf().apply { this[cellByteOffset(0)] = 10 }

        assertThrows(IllegalArgumentException::class.java) { KillerSudokuEngine.decodeState(digitTen) }
    }

    @Test
    fun rejectsNoteBitsAboveDigitNine() {
        val encoded = KillerSudokuEngine.encodeState(KillerSudokuEngine.initialState(puzzle()))
        val noteTen = encoded.copyOf().apply {
            val offset = cellByteOffset(0)
            this[offset + 1] = 0x02
            this[offset + 2] = 0x00
        }

        assertThrows(IllegalArgumentException::class.java) { KillerSudokuEngine.decodeState(noteTen) }
    }

    @Test
    fun acceptsEveryNoteBitForDigitsOneThroughNine() {
        val state = KillerSudokuEngine.initialState(puzzle())
            .withMove(Move.SetNotes(Cell(0), KILLER_SUDOKU_NOTE_MASK))

        assertEquals(state, KillerSudokuEngine.decodeState(KillerSudokuEngine.encodeState(state)))
    }

    @Test
    fun rejectsFilledCellWithNotesAndWrongCellCount() {
        val encoded = KillerSudokuEngine.encodeState(
            KillerSudokuEngine.initialState(puzzle()).withMove(Move.SetDigit(Cell(0), 5)),
        )
        val notesOnFilled = encoded.copyOf().apply {
            val offset = cellByteOffset(0)
            this[offset + 2] = 0x01
        }
        val wrongCellCountOffset = 5 + 2 + KillerSudokuCodec.encode(puzzle()).toByteArray(Charsets.UTF_8).size
        val wrongCellCount = encoded.copyOf().apply {
            this[wrongCellCountOffset + 3] = 80
        }

        assertThrows(IllegalArgumentException::class.java) { KillerSudokuEngine.decodeState(notesOnFilled) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuEngine.decodeState(wrongCellCount) }
    }

    /** Byte offset of a cell's digit: header, puzzle text, cell count, then digit plus two-byte notes per cell. */
    private fun cellByteOffset(index: Int): Int {
        val puzzleTextBytes = KillerSudokuCodec.encode(puzzle()).toByteArray(Charsets.UTF_8).size
        return 5 + 2 + puzzleTextBytes + 4 + 3 * index
    }

    private fun puzzle(givens: List<Int?> = List(81) { null }): KillerSudokuPuzzle {
        val cages = (0 until 9).map { row ->
            KillerCage((0 until 9).map { column -> Cell(row * 9 + column) }, sum = 45)
        }
        return KillerSudokuPuzzle(
            id = "KS-STATE-9",
            givens = givens,
            cages = cages,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "fixture",
        )
    }

    private companion object {
        val solution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
