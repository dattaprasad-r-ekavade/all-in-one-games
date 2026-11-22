package com.datathecodie.witgrid.games.futoshiki

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiEngine
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiPuzzle
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Futoshiki bank behaviour and its digit/notes board with inequality signs. Registration lives in FUC. */
object FutoshikiGameModule : GameModule<FutoshikiPuzzle, FutoshikiState> by EngineGameModule(
    engine = FutoshikiEngine,
    puzzleOf = FutoshikiState::puzzle,
    intro = null,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: FutoshikiState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        FutoshikiBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
