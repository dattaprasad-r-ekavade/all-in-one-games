package com.datathecodie.witgrid.games.path

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.path.PathEngine
import com.datathecodie.witgrid.engine.path.PathPuzzle
import com.datathecodie.witgrid.engine.path.PathState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Path bank behaviour and its drag-paint board. */
object PathGameModule : GameModule<PathPuzzle, PathState> by EngineGameModule(
    engine = PathEngine,
    puzzleOf = PathState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: PathState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        PathBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
