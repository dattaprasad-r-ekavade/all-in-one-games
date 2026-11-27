package com.datathecodie.witgrid.ui.preview

import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.PlayRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GameMotifTest {
    @Test
    fun everyRegisteredGameHasItsOwnMotif() {
        val gameIds = (GameRegistry.games.map { it.id } + PlayRegistry.games.map { it.id }).toSet()

        assertEquals(
            setOf(
                "crowns", "binary", "nonogram", "minisudoku", "sudoku", "skyscrapers", "futoshiki",
                "path", "killersudoku", "mosaic", "tactics", "coloursort", "arrows", "klondike",
            ),
            gameIds,
        )
        val motifs = gameIds.map(GameMotif::kindFor)
        assertEquals(gameIds.size, motifs.toSet().size)
        assertNotEquals(GameMotifKind.UNKNOWN, GameMotif.kindFor("crowns"))
        assertEquals(GameMotifKind.UNKNOWN, GameMotif.kindFor("unknown"))
    }
}
