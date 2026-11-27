package com.datathecodie.witgrid.games.crowns

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted settings. */
data class CrownsBoardOptions(
    val showRegionAids: Boolean = false,
    val autoXEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

/** The settings screen and play host can supply these options without coupling the board to storage. */
val LocalCrownsBoardOptions = staticCompositionLocalOf { CrownsBoardOptions() }
