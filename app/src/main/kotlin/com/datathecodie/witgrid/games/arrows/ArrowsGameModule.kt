package com.datathecodie.witgrid.games.arrows

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.arrows.ArrowsAction
import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsPuzzle
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.games.EnginePlayGameModule
import com.datathecodie.witgrid.games.PlayGameModule

/** Arrows bank behaviour and its tap-to-slide board. */
object ArrowsGameModule : PlayGameModule<ArrowsPuzzle, ArrowsState, ArrowsAction> by EnginePlayGameModule(
    engine = ArrowsEngine,
    puzzleOf = ArrowsState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(
        state: ArrowsState,
        onAction: (ArrowsAction) -> Unit,
        onUndo: () -> Unit,
    ) {
        ArrowsBoard(state = state, onAction = onAction, onUndo = onUndo)
    }
}
