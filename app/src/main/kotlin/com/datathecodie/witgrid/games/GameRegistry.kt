package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuEngine
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.games.binary.BinaryGameModule
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.games.nonogram.NonogramGameModule

object GameRegistry {
    val games: List<GameModule<*, *>> = listOf(
        CrownsGameModule,
        BinaryGameModule,
        NonogramGameModule,
        EngineGameModule(MiniSudokuEngine, MiniSudokuState::puzzle) { 2 },
    )

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): GameModule<*, *>? = gamesById[gameId]
}
