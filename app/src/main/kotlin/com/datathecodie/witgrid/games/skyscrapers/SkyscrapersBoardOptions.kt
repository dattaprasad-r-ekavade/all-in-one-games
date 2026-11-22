package com.datathecodie.witgrid.games.skyscrapers

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted player settings. */
data class SkyscrapersBoardOptions(
    val autoNotesEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalSkyscrapersBoardOptions = staticCompositionLocalOf { SkyscrapersBoardOptions() }
