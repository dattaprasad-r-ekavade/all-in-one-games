package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PathStateCodecTest {
    private val sampleLine = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|EASY|easy:1"

    @Test
    fun roundTripInitialAndModifiedState() {
        val puzzle = PathCodec.decode(sampleLine)
        val initial = PathState.initial(puzzle)

        val initialEncoded = PathStateCodec.encode(initial)
        val initialDecoded = PathStateCodec.decode(initialEncoded)
        assertEquals(initial, initialDecoded)

        val modified = initial
            .withMove(Move.SetDigit(Cell(1), 1))
            .withMove(Move.SetDigit(Cell(2), 1))
        val modifiedEncoded = PathStateCodec.encode(modified)
        val modifiedDecoded = PathStateCodec.decode(modifiedEncoded)
        assertEquals(modified, modifiedDecoded)
    }

    @Test
    fun rejectsCorruptedStateSave() {
        assertThrows(IllegalArgumentException::class.java) {
            PathStateCodec.decode(ByteArray(10) { 0 })
        }
    }
}
