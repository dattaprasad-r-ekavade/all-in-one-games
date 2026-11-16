package com.datathecodie.witgrid.games.nonogram

import androidx.compose.runtime.staticCompositionLocalOf

/** Settings supplied by the play host without coupling the board to storage. */
data class NonogramBoardOptions(
    val autoCrossCompletedLines: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalNonogramBoardOptions = staticCompositionLocalOf { NonogramBoardOptions() }
