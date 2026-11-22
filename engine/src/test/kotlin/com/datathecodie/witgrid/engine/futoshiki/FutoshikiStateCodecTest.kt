package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FutoshikiStateCodecTest {
    private val solution = listOf(
        1, 2, 3, 4, 5,
        2, 3, 4, 5, 1,
        3, 4, 5, 1, 2,
        4, 5, 1, 2, 3,
        5, 1, 2, 3, 4,
    )
    private val horizontalSigns = ("<<<<" + "<<<>" + "<<><" + "<><<" + "><<<").map {
        FutoshikiSign.fromHorizontalChar(it)
    }
    private val verticalSigns = ("^^^^v" + "^^^v^" + "^^v^^" + "^v^^^").map {
        FutoshikiSign.fromVerticalChar(it)
    }

    private fun puzzle(): FutoshikiPuzzle = FutoshikiPuzzle(
        id = "test-fu-save",
        size = 5,
        givens = solution.mapIndexed { index, digit -> if (index == 0) digit else null },
        horizontalSigns = horizontalSigns,
        verticalSigns = verticalSigns,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "sign-bounds:1",
    )

    @Test
    fun roundTripsStateWithDigitsAndNotes() {
        val puzzle = puzzle()
        val initial = FutoshikiState.initial(puzzle)
        val state = initial
            .withMove(Move.SetNotes(Cell(1), (1 shl 1) or (1 shl 4)))
            .withMove(Move.SetDigit(Cell(2), 3))

        val bytes = FutoshikiStateCodec.encode(state)
        val restored = FutoshikiStateCodec.decode(bytes)

        assertEquals(state.puzzle, restored.puzzle)
        assertEquals(state.digits, restored.digits)
        assertEquals(state.notes, restored.notes)
    }

    @Test
    fun rejectsCorruptedMagicAndLength() {
        val puzzle = puzzle()
        val state = FutoshikiState.initial(puzzle)
        val bytes = FutoshikiStateCodec.encode(state)

        val corruptedMagic = bytes.clone().also { it[0] = 0 }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiStateCodec.decode(corruptedMagic)
        }

        val truncated = bytes.copyOf(bytes.size - 2)
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiStateCodec.decode(truncated)
        }
    }

    @Test
    fun rejectsSaveWithNotesAboveSize() {
        val puzzle = puzzle()
        val state = FutoshikiState.initial(puzzle)
        val bytes = FutoshikiStateCodec.encode(state)

        // Modify the note byte of cell 1 to include bit 5 (value 32) which exceeds 5x5 max note mask (31)
        // Find cell data offset after magic (4), version (1), UTF string (2 + len), cellCount (4)
        val utfLen = ((bytes[5].toInt() and 0xFF) shl 8) or (bytes[6].toInt() and 0xFF)
        val cellOffset = 7 + utfLen + 4
        // Cell 0: digit (1 byte) + note (2 bytes) = 3 bytes
        // Cell 1 note is at offset cellOffset + 3 + 1
        val invalidNote = bytes.clone().also {
            it[cellOffset + 3 + 2] = 32
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiStateCodec.decode(invalidNote)
        }
    }
}
