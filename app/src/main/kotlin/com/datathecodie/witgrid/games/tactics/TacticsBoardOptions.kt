package com.datathecodie.witgrid.games.tactics

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted player settings. */
data class TacticsBoardOptions(
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalTacticsBoardOptions = staticCompositionLocalOf { TacticsBoardOptions() }