package com.datathecodie.witgrid.games.mosaic

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.mosaic.MosaicEngine
import com.datathecodie.witgrid.engine.mosaic.MosaicPuzzle
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Mosaic bank behaviour and its fill/cross board. Registration lives in MOC. */
object MosaicGameModule : GameModule<MosaicPuzzle, MosaicState> by EngineGameModule(
    engine = MosaicEngine,
    puzzleOf = MosaicState::puzzle,
    intro = null,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: MosaicState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        MosaicBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
