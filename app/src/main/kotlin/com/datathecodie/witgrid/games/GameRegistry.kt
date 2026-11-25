package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.games.binary.BinaryGameModule
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
import com.datathecodie.witgrid.games.nonogram.NonogramGameModule
import com.datathecodie.witgrid.games.sudoku.SudokuGameModule

object GameRegistry {
    val games: List<GameModule<*, *>> = listOf(
        CrownsGameModule,
        BinaryGameModule,
        NonogramGameModule,
        MiniSudokuGameModule,
        SudokuGameModule,
    )

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): GameModule<*, *>? = gamesById[gameId]
}
