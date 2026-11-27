package com.datathecodie.witgrid.engine

import com.datathecodie.witgrid.engine.binary.BinaryEngine
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiEngine
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.mosaic.MosaicEngine
import com.datathecodie.witgrid.engine.nonogram.NonogramEngine
import com.datathecodie.witgrid.engine.path.PathEngine
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersEngine
import com.datathecodie.witgrid.engine.sudoku.SudokuEngine
import com.datathecodie.witgrid.engine.tactics.TacticsEngine

/** Launch engines in product order; concrete objects retain each lane's puzzle/state types. */
object Registry {
    /**
     * Heterogeneous discovery list. Use a generic helper accepting `GameEngine<P, S>`
     * to capture an entry's types and decode before operating, rather than casting puzzles.
     */
    val engines: List<GameEngine<*, *>> = listOf(
        CrownsEngine,
        BinaryEngine,
        NonogramEngine,
        MiniSudokuEngine,
        FutoshikiEngine,
        SkyscrapersEngine,
        PathEngine,
        SudokuEngine,
        KillerSudokuEngine,
        MosaicEngine,
        TacticsEngine,
    )
}
