package com.datathecodie.witgrid.games.minisudoku

import androidx.compose.runtime.staticCompositionLocalOf

/** Settings the play host supplies without coupling the board to storage. */
data class MiniSudokuBoardOptions(
    val autoNotesEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalMiniSudokuBoardOptions = staticCompositionLocalOf { MiniSudokuBoardOptions() }
