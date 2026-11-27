package com.datathecodie.witgrid.games.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeCodec
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikePuzzle
import com.datathecodie.witgrid.games.BankSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class KlondikeGameModuleTest {
    private val module = KlondikeGameModule

    @Test
    fun drawsStockThroughTheModuleAndKeepsTheLevel() {
        val puzzle = KlondikePuzzle(
            bankId = "KL-EASY-test",
            pack = Pack.EASY,
            difficulty = "d1",
            seed = 99L,
        )
        val intro = puzzle.copy(bankId = "KL-INTRO-01")
        val bank = FakeBankSource(
            mapOf(
                "bank/klondike-intro.txt" to KlondikeCodec.encodePuzzle(intro),
                "bank/klondike-easy.txt.gz" to KlondikeCodec.encodePuzzle(puzzle),
            ),
        )
        val level = module.loadLevel(module.levelRefs(Pack.EASY, bank)[1], bank)
        assertEquals("klondike", module.id)
        assertEquals(1, module.levelsPerInterstitial(level))
        assertEquals(puzzle, level.puzzle)

        val start = module.newState(level)
        assertFalse(module.isWon(level, start))
        assertTrue(start.stock.isNotEmpty())
        assertTrue(start.waste.isEmpty())

        val tap = KlondikeBoardInput.tap(start, selection = null, hit = KlondikeHit.Stock)
        assertEquals(KlondikeTapFeedback.MOVED, tap.feedback)
        assertEquals(CardMove.DrawStock, tap.action)

        val next = module.applyAction(start, tap.action!!)
        assertEquals(start.stock.size - 1, next.stock.size)
        assertEquals(1, next.waste.size)
        assertTrue(next.waste.last().faceUp)
        assertFalse(module.isWon(level, next))

        val restored = module.decodeState(module.encodeState(next))
        assertEquals(next, restored)
        assertTrue(module.stateMatchesLevel(level, restored))
        assertEquals(KlondikeEngine.id, module.engine.id)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val text = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$text\n".toByteArray(Charsets.UTF_8))
        }
    }
}
