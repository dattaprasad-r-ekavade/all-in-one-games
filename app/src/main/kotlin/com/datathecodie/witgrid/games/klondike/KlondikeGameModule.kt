package com.datathecodie.witgrid.games.klondike

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikePuzzle
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.games.EnginePlayGameModule
import com.datathecodie.witgrid.games.PlayGameModule

/** Klondike bank behaviour and its card table. */
object KlondikeGameModule : PlayGameModule<KlondikePuzzle, KlondikeState, CardMove> by EnginePlayGameModule(
    engine = KlondikeEngine,
    puzzleOf = KlondikeState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 1 },
) {
    @Composable
    override fun Board(
        state: KlondikeState,
        onAction: (CardMove) -> Unit,
        onUndo: () -> Unit,
    ) {
        KlondikeBoard(state = state, onAction = onAction, onUndo = onUndo)
    }
}
