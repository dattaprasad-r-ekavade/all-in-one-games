package com.datathecodie.witgrid.games.minisudoku

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuPuzzle
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Mini Sudoku bank behaviour and its digit/notes board. */
object MiniSudokuGameModule : GameModule<MiniSudokuPuzzle, MiniSudokuState> by EngineGameModule(
    engine = MiniSudokuEngine,
    puzzleOf = MiniSudokuState::puzzle,
    interstitialRule = { 2 },
) {
    @Composable
    override fun Board(state: MiniSudokuState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        MiniSudokuBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
