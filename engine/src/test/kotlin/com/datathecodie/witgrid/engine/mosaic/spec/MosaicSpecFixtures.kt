package com.datathecodie.witgrid.engine.mosaic.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.mosaic.MosaicPuzzle
import com.datathecodie.witgrid.engine.mosaic.MosaicRules

/** Hand-built boards shared by independent Mosaic rule, codec and solver checks. */
internal object MosaicSpecFixtures {
    fun allEmptySmallPuzzle(): MosaicPuzzle {
        val solution = List(9) { 0 }
        val clues = MutableList<Int?>(9) { null }
        clues[4] = 0
        return MosaicPuzzle("MO-SPEC-EMPTY", 3, clues, solution, Pack.EASY, "empty-area:1")
    }

    fun launchPuzzle(): MosaicPuzzle {
        val solution = List(64) { 0 }
        val grid = Grid(8)
        val clues = MutableList<Int?>(64) { null }
        // A corner clue makes its four-cell neighborhood observable in the encoded fixture.
        clues[0] = MosaicRules.clueAt(grid, solution, Cell(0))
        return MosaicPuzzle("MO-SPEC-BANK", 8, clues, solution, Pack.EASY, "empty-area:1")
    }
}
