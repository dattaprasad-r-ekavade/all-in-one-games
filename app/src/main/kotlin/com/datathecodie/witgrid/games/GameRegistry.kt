package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.binary.BinaryEngine
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.nonogram.NonogramEngine

object GameRegistry {
    /** Nonogram 5x5 levels are short, so their ads come every 2 levels (plan 8.1). */
    private const val SHORT_NONOGRAM_SIZE = 5

    val games: List<GameModule<*, *>> = listOf(
        EngineGameModule(CrownsEngine) { 1 },
        EngineGameModule(BinaryEngine) { 2 },
        EngineGameModule(NonogramEngine) { level ->
            if (level.size == SHORT_NONOGRAM_SIZE) 2 else 1
        },
        EngineGameModule(MiniSudokuEngine) { 2 },
    )

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): GameModule<*, *>? = gamesById[gameId]
}
