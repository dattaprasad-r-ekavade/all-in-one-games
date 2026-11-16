package com.datathecodie.witgrid.ui.screens.result

import com.datathecodie.witgrid.games.LevelRef

internal fun nextResultLevel(current: LevelRef, packLevels: List<LevelRef>): LevelRef? {
    if (current.daily) return null
    val pack = packLevels.filter { it.gameId == current.gameId && it.pack == current.pack && !it.daily }
    val index = pack.indexOfFirst { it.levelId == current.levelId }
    return if (index < 0) null else pack.getOrNull(index + 1)
}
