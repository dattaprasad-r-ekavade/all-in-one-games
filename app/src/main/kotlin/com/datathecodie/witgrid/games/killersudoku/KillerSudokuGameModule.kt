package com.datathecodie.witgrid.games.killersudoku

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuEngine
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Killer Sudoku bank behaviour and its digit/notes board. */
object KillerSudokuGameModule : GameModule<KillerSudokuPuzzle, KillerSudokuState> by EngineGameModule(
    engine = KillerSudokuEngine,
    puzzleOf = KillerSudokuState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 1 },
) {
    @Composable
    override fun Board(state: KillerSudokuState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        KillerSudokuBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
