package com.datathecodie.witgrid.ui.screens.result

import com.datathecodie.witgrid.games.LevelRef

internal fun nextResultLevel(current: LevelRef, packLevels: List<LevelRef>): LevelRef? {
    if (current.daily) return null
    val pack = packLevels.filter {
        it.gameId == current.gameId && it.pack == current.pack && !it.daily
    }
    val bankLevels = pack.filterNot { it.isIntro }
    if (current.isIntro) return LevelRef.forLevel(1L, bankLevels)
    val index = bankLevels.indexOfFirst { it.levelId == current.levelId }
    require(index >= 0) { "Result level must belong to its pack" }
    return LevelRef.forLevel(Math.addExact(current.levelNumber ?: (index + 1L), 1L), bankLevels)
}
