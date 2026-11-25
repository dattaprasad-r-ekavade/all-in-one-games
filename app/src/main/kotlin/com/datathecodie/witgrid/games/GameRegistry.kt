package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.games.binary.BinaryGameModule
import com.datathecodie.witgrid.games.crowns.CrownsGameModule
import com.datathecodie.witgrid.games.futoshiki.FutoshikiGameModule
import com.datathecodie.witgrid.games.minisudoku.MiniSudokuGameModule
import com.datathecodie.witgrid.games.nonogram.NonogramGameModule
import com.datathecodie.witgrid.games.path.PathGameModule
import com.datathecodie.witgrid.games.skyscrapers.SkyscrapersGameModule
import com.datathecodie.witgrid.games.sudoku.SudokuGameModule

object GameRegistry {
    val games: List<GameModule<*, *>> = listOf(
        CrownsGameModule,
        BinaryGameModule,
        NonogramGameModule,
        MiniSudokuGameModule,
        SudokuGameModule,
        SkyscrapersGameModule,
        FutoshikiGameModule,
        PathGameModule,
    )

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): GameModule<*, *>? = gamesById[gameId]
}
