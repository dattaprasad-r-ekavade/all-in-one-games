package com.datathecodie.witgrid.games.mosaic

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted player settings. */
data class MosaicBoardOptions(
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalMosaicBoardOptions = staticCompositionLocalOf { MosaicBoardOptions() }
