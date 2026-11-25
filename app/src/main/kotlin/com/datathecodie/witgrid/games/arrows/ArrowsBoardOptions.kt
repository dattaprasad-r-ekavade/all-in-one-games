package com.datathecodie.witgrid.games.arrows

import androidx.compose.runtime.staticCompositionLocalOf

/** Settings the play host can supply without coupling the Arrows board to storage. */
data class ArrowsBoardOptions(
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalArrowsBoardOptions = staticCompositionLocalOf { ArrowsBoardOptions() }
