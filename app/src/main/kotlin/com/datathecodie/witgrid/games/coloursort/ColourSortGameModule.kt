package com.datathecodie.witgrid.games.coloursort

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.ColourSortPuzzle
import com.datathecodie.witgrid.engine.coloursort.ColourSortState
import com.datathecodie.witgrid.games.EnginePlayGameModule
import com.datathecodie.witgrid.games.PlayGameModule

/** Colour Sort bank behaviour and its tube board. */
object ColourSortGameModule : PlayGameModule<ColourSortPuzzle, ColourSortState, ColourSortAction> by EnginePlayGameModule(
    engine = ColourSortEngine,
    puzzleOf = ColourSortState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(
        state: ColourSortState,
        onAction: (ColourSortAction) -> Unit,
        onUndo: () -> Unit,
    ) {
        ColourSortBoard(state = state, onAction = onAction, onUndo = onUndo)
    }
}
