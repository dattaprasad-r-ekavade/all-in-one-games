package com.datathecodie.witgrid.games.futoshiki

import androidx.compose.runtime.staticCompositionLocalOf

/** Options the play screen can provide from persisted player settings. */
data class FutoshikiBoardOptions(
    val autoNotesEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalFutoshikiBoardOptions = staticCompositionLocalOf { FutoshikiBoardOptions() }
