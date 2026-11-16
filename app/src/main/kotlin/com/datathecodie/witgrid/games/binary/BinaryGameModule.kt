package com.datathecodie.witgrid.games.binary

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.binary.BinaryEngine
import com.datathecodie.witgrid.engine.binary.BinaryPuzzle
import com.datathecodie.witgrid.engine.binary.BinaryState
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Binary bank behaviour plus its game-specific board. */
object BinaryGameModule : GameModule<BinaryPuzzle, BinaryState> by EngineGameModule(
    engine = BinaryEngine,
    puzzleOf = BinaryState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: BinaryState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        BinaryBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
