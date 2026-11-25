package com.datathecodie.witgrid.engine.mosaic.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicCodec
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.engine.mosaic.MosaicStateCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MosaicCodecSpecTest {
    @Test
    fun bankLineRoundTripsAHandMadePuzzle() {
        val puzzle = MosaicSpecFixtures.launchPuzzle()
        assertEquals(puzzle, MosaicCodec.decode(MosaicCodec.encode(puzzle)))
    }

    @Test
    fun malformedBankLinesThrow() {
        val puzzle = MosaicSpecFixtures.launchPuzzle()
        val valid = MosaicCodec.encode(puzzle)
        val malformed = listOf(
            "missing-fields",
            valid.replaceFirst("MO-SPEC-BANK", ""),
            valid.replaceFirst("empty-area:1", "not-a-technique:1"),
            valid.replaceFirst("0.......", "x......."),
        )

        malformed.forEach { line ->
            assertThrows(IllegalArgumentException::class.java) { MosaicCodec.decode(line) }
        }
    }

    @Test
    fun savedStateRoundTripsAllMarksAndPuzzle() {
        val puzzle = MosaicSpecFixtures.launchPuzzle()
        val state = MosaicState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.CROSSED))
            .withMove(Move.SetMark(Cell(9), Mark.FILLED))

        assertEquals(state, MosaicStateCodec.decode(MosaicStateCodec.encode(state)))
    }
}
