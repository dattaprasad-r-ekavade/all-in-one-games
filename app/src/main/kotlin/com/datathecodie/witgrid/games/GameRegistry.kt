package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.engine.nonogram.NonogramEngine
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import com.datathecodie.witgrid.games.binary.BinaryGameModule
import com.datathecodie.witgrid.games.crowns.CrownsGameModule

object GameRegistry {
    /** Nonogram 5x5 levels are short, so their ads come every 2 levels (plan 8.1). */
    private const val SHORT_NONOGRAM_SIZE = 5

    val games: List<GameModule<*, *>> = listOf(
        CrownsGameModule,
        BinaryGameModule,
        EngineGameModule(NonogramEngine, NonogramState::puzzle) { level ->
            if (level.size == SHORT_NONOGRAM_SIZE) 2 else 1
        },
        EngineGameModule(MiniSudokuEngine, MiniSudokuState::puzzle) { 2 },
    )

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): GameModule<*, *>? = gamesById[gameId]
}
