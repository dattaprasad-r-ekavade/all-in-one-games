package com.datathecodie.witgrid.games.sudoku

import androidx.compose.runtime.Composable
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.sudoku.SudokuEngine
import com.datathecodie.witgrid.engine.sudoku.SudokuPuzzle
import com.datathecodie.witgrid.engine.sudoku.SudokuState
import com.datathecodie.witgrid.games.EngineGameModule
import com.datathecodie.witgrid.games.GameModule

/** Sudoku bank behaviour and its digit/notes board. */
object SudokuGameModule : GameModule<SudokuPuzzle, SudokuState> by EngineGameModule(
    engine = SudokuEngine,
    puzzleOf = SudokuState::puzzle,
    intro = Intro.definition,
    interstitialRule = { 1 },
) {
    @Composable
    override fun Board(state: SudokuState, onMoves: (List<Move>) -> Unit, onUndo: () -> Unit) {
        SudokuBoard(state = state, onMoves = onMoves, onUndo = onUndo)
    }
}
