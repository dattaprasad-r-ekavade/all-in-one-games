package com.datathecodie.witgrid.games.path

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted player settings. */
data class PathBoardOptions(
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalPathBoardOptions = staticCompositionLocalOf { PathBoardOptions() }