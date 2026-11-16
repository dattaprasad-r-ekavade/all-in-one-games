package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.binary.BinaryEngine
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.nonogram.NonogramEngine

object GameRegistry {
    val games: List<GameModule<*, *>> = listOf(
        EngineGameModule(CrownsEngine, levelsPerInterstitial = 1),
        EngineGameModule(BinaryEngine, levelsPerInterstitial = 2),
        EngineGameModule(NonogramEngine, levelsPerInterstitial = 1),
        EngineGameModule(MiniSudokuEngine, levelsPerInterstitial = 2),
    )

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): GameModule<*, *>? = gamesById[gameId]
}
