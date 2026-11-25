package com.datathecodie.witgrid.games.killersudoku

import androidx.compose.runtime.staticCompositionLocalOf

/** Settings the play host supplies without coupling the board to storage. */
data class KillerSudokuBoardOptions(
    val autoNotesEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalKillerSudokuBoardOptions = staticCompositionLocalOf { KillerSudokuBoardOptions() }
