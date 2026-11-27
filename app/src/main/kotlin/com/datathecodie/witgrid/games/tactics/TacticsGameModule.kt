package com.datathecodie.witgrid.games.tactics

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.tactics.TacticsEngine
import com.datathecodie.witgrid.engine.tactics.TacticsPuzzle
import com.datathecodie.witgrid.engine.tactics.TacticsState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Tactics bank behaviour and its tap-move board. */
object TacticsGameModule : GameModule<TacticsPuzzle, TacticsState> by EngineGameModule(
    engine = TacticsEngine,
    puzzleOf = TacticsState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: TacticsState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        TacticsBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}