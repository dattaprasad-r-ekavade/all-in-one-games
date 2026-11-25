package com.datathecodie.witgrid.games.sudoku

import androidx.compose.runtime.staticCompositionLocalOf

/** Settings the play host supplies without coupling the board to storage. */
data class SudokuBoardOptions(
    val autoNotesEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalSudokuBoardOptions = staticCompositionLocalOf { SudokuBoardOptions() }
