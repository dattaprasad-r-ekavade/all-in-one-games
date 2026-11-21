package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Collections

class SkyscrapersStateCodecTest {

    @Test
    fun roundTripRetainsPuzzleGivensDigitsAndNotes() {
        val puzzle = puzzle4()
        val state = SkyscrapersEngine.initialState(puzzle)
            .withMove(Move.SetDigit(Cell(0), 1))
            .withMove(Move.SetNotes(Cell(1), 0b1010))

        val encoded = SkyscrapersEngine.encodeState(state)
        val restored = SkyscrapersEngine.decodeState(encoded)

        assertEquals(state, restored)
        assertEquals(puzzle, restored.puzzle)
    }

    @Test
    fun rejectsUnsupportedVersionAndTruncatedSaves() {
        val encoded = SkyscrapersEngine.encodeState(SkyscrapersEngine.initialState(puzzle4()))
        val unsupportedVersion = encoded.copyOf().apply { this[4] = 2 }

        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersEngine.decodeState(unsupportedVersion)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersEngine.decodeState(encoded.copyOf(encoded.size - 1))
        }
    }

    @Test
    fun rejectsDigitsOutsideOneThroughSize() {
        val encoded = SkyscrapersEngine.encodeState(SkyscrapersEngine.initialState(puzzle4()))
        val digitOutOfRange = encoded.copyOf().apply { this[cellByteOffset(0)] = 5 }

        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersEngine.decodeState(digitOutOfRange)
        }
    }

    @Test
    fun rejectsNoteBitsAboveSize() {
        val encoded = SkyscrapersEngine.encodeState(SkyscrapersEngine.initialState(puzzle4()))
        val noteOutOfRange = encoded.copyOf().apply { this[cellByteOffset(0) + 1] = 0b10000 }

        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersEngine.decodeState(noteOutOfRange)
        }
    }

    private fun cellByteOffset(index: Int): Int {
        val baseState = SkyscrapersEngine.initialState(puzzle4())
        val encoded = SkyscrapersEngine.encodeState(baseState)
        val withDigit = SkyscrapersEngine.encodeState(
            baseState.withMove(Move.SetDigit(Cell(index), 1)),
        )
        val offset = encoded.indices.first { encoded[it] != withDigit[it] }
        return offset
    }

    private fun puzzle4(): SkyscrapersPuzzle = SkyscrapersPuzzle(
        id = "SK-4-STATE-TEST",
        size = 4,
        topClues = listOf(4, 3, 2, 1),
        bottomClues = listOf(1, 2, 2, 2),
        leftClues = listOf(4, 3, 2, 1),
        rightClues = listOf(1, 2, 2, 2),
        solution = listOf(
            1, 2, 3, 4,
            2, 3, 4, 1,
            3, 4, 1, 2,
            4, 1, 2, 3,
        ),
        pack = Pack.EASY,
        storedGrade = "pending",
        givens = Collections.nCopies(16, null),
    )
}
