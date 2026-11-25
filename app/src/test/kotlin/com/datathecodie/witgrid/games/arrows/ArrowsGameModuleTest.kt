package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.engine.arrows.ArrowsCodec
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class ArrowsGameModuleTest {
    private val module = ArrowsGameModule

    @Test
    fun tapsHintArrowFromFixtureUntilClearWithZeroMistakes() {
        val bank = FakeBankSource(
            mapOf(
                "bank/arrows-easy.txt.gz" to ArrowsCodec.encodePuzzle(ArrowsBoardFixtures.easyPuzzle),
            ),
        )
        val levelRef = module.firstLevelRef(Pack.EASY, bank)
        val level = module.loadLevel(levelRef, bank)
        assertEquals("ar-easy-1", level.ref.levelId)
        assertEquals(2, module.levelsPerInterstitial(level))

        var state = module.newState(level)
        assertFalse(module.isWon(level, state))
        assertFalse(module.isLost(level, state))
        assertEquals(0, state.mistakes)

        var steps = 0
        while (!module.isWon(level, state)) {
            assertTrue(steps < 30)
            val hinted = module.hintAction(level, state)
            assertNotNull(hinted)
            val hint = hinted!!
            val tap = ArrowsBoardInput.tapCell(state, hint.cell.index)
            assertEquals(ArrowTapFeedback.SLID, tap.feedback)
            assertEquals(hint, tap.action)
            state = module.applyAction(state, tap.action!!)
            assertEquals(0, state.mistakes)
            steps++
        }

        assertTrue(module.isWon(level, state))
        assertFalse(module.isLost(level, state))
        assertEquals(0, state.mistakes)
        assertEquals(3, ArrowsBoardInput.livesLeft(state))
        val restored = module.decodeState(module.encodeState(state))
        assertTrue(module.stateMatchesLevel(level, restored))
        assertTrue(module.isWon(level, restored))
    }

    @Test
    fun blockedTapAddsMistakeThroughGameModule() {
        val bank = FakeBankSource(
            mapOf(
                "bank/arrows-easy.txt.gz" to ArrowsCodec.encodePuzzle(ArrowsBoardFixtures.easyPuzzle),
            ),
        )
        val level = module.loadLevel(module.firstLevelRef(Pack.EASY, bank), bank)
        val start = module.newState(level)
        val tap = ArrowsBoardInput.tapCell(start, ArrowsBoardFixtures.BLOCKED_CELL)

        assertEquals(ArrowTapFeedback.BLOCKED, tap.feedback)
        val next = module.applyAction(start, tap.action!!)
        assertEquals(1, next.mistakes)
        assertEquals(2, ArrowsBoardInput.livesLeft(next))
        assertEquals(start.arrows, next.arrows)
        assertFalse(module.isLost(level, next))
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val text = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$text\n".toByteArray(Charsets.UTF_8))
        }
    }
}
