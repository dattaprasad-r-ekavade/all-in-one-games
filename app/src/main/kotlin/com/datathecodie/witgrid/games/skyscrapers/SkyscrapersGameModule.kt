package com.datathecodie.witgrid.games.skyscrapers

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersEngine
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Skyscrapers bank behaviour and its digit/notes board with outside clues. */
object SkyscrapersGameModule : GameModule<SkyscrapersPuzzle, SkyscrapersState> by EngineGameModule(
    engine = SkyscrapersEngine,
    puzzleOf = SkyscrapersState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: SkyscrapersState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        SkyscrapersBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
