package com.datathecodie.witgrid.games.nonogram

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.nonogram.NonogramEngine
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Nonogram bank behaviour and its clue board. */
object NonogramGameModule : GameModule<NonogramPuzzle, NonogramState> by EngineGameModule(
    engine = NonogramEngine,
    puzzleOf = NonogramState::puzzle,
    interstitialRule = { level -> if (level.size == SHORT_NONOGRAM_SIZE) 2 else 1 },
) {
    @Composable
    override fun Board(state: NonogramState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        NonogramBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}

private const val SHORT_NONOGRAM_SIZE = 5
