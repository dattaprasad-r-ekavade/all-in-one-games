package com.datathecodie.witgrid.games.crowns

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsState
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Crowns bank behaviour plus its game-specific board. */
object CrownsGameModule : GameModule<CrownsPuzzle, CrownsState> by EngineGameModule(
    engine = CrownsEngine,
    puzzleOf = CrownsState::puzzle,
    interstitialRule = { 1 },
) {
    @Composable
    override fun Board(state: CrownsState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        CrownsBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
