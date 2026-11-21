package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MosaicStateCodecTest {
    private val solution8x8 = listOf(
        1, 0, 0, 1, 1, 0, 0, 1,
        0, 1, 1, 0, 0, 1, 1, 0,
        0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1,
        1, 0, 0, 1, 1, 0, 0, 1,
        0, 1, 1, 0, 0, 1, 1, 0,
        0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1,
    )
    private val grid8 = Grid(8)
    private val clues8x8 = (0 until 64).map { MosaicRules.clueAt(grid8, solution8x8, Cell(it)) }
    private val puzzle = MosaicPuzzle("MO-SAVE-1", 8, clues8x8, solution8x8, Pack.EASY, "none:1")

    @Test
    fun roundtripPreservesPlayerMarks() {
        val state = MosaicState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.FILLED))
            .withMove(Move.SetMark(Cell(1), Mark.CROSSED))
            .withMove(Move.SetMark(Cell(2), Mark.EMPTY))

        val bytes = MosaicStateCodec.encode(state)
        val decoded = MosaicStateCodec.decode(bytes)

        assertEquals(state.puzzle, decoded.puzzle)
        assertEquals(state.marks, decoded.marks)
    }

    @Test
    fun decodeRejectsCorruptedDataAndWrongMagic() {
        val state = MosaicState.initial(puzzle)
        val bytes = MosaicStateCodec.encode(state)

        // Invalidate magic header byte
        val corruptedBytes = bytes.copyOf()
        corruptedBytes[0] = 0x00

        assertThrows(IllegalArgumentException::class.java) {
            MosaicStateCodec.decode(corruptedBytes)
        }
    }
}
