package com.datathecodie.witgrid.games.binary

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted player settings. */
data class BinaryBoardOptions(
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalBinaryBoardOptions = staticCompositionLocalOf { BinaryBoardOptions() }
