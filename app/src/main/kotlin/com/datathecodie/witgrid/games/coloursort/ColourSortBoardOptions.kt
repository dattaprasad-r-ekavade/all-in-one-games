package com.datathecodie.witgrid.games.coloursort

import androidx.compose.runtime.staticCompositionLocalOf

/** Settings the play host can supply without coupling the Colour Sort board to storage. */
data class ColourSortBoardOptions(
    val showColourAids: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalColourSortBoardOptions = staticCompositionLocalOf { ColourSortBoardOptions() }
