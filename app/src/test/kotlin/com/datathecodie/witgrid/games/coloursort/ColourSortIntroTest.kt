package com.datathecodie.witgrid.games.coloursort

import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.coloursort.ColourSortAction
import com.datathecodie.witgrid.engine.coloursort.ColourSortPuzzle
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameLevel
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.games.PlayRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class ColourSortIntroTest {
    @Test
    fun introUsesColourSortLevelIdAndCoachLine() {
        assertEquals("CS-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_coloursort, Intro.definition.coachLineResourceId)
    }

    @Test
    fun colourSortIsRegisteredInPlayRegistryWithInterstitialIntervalTwo() {
        assertEquals(ColourSortGameModule, PlayRegistry.find("coloursort"))
        assertTrue(PlayRegistry.games.contains(ColourSortGameModule))
        val bank = assetFileBankSource()
        val level = ColourSortGameModule.loadLevel(
            ColourSortGameModule.firstLevelRef(Pack.EASY, bank),
            bank,
        )
        assertEquals(2, ColourSortGameModule.levelsPerInterstitial(level))
    }

    @Test
    fun easyStartsWithNumberedLevelOne() {
        val bank = assetFileBankSource()
        val refs = ColourSortGameModule.levelRefs(Pack.EASY, bank)
        val firstRef = refs.first()
        assertFalse(firstRef.isIntro)
        assertEquals(1L, firstRef.levelNumber)

        val level = ColourSortGameModule.loadLevel(firstRef, bank)
        assertEquals(4, level.size)
        var state = ColourSortGameModule.newState(level)
        assertFalse(ColourSortGameModule.isWon(level, state))
        while (!ColourSortGameModule.isWon(level, state)) {
            val hint = ColourSortGameModule.hintAction(level, state)
            state = ColourSortGameModule.applyAction(state, requireNotNull(hint))
        }
        assertTrue(ColourSortGameModule.isWon(level, state))

    }

    @Test
    fun continueAddsOneEmptyTubeAfterADeadlock() {
        val deadlockPuzzle = ColourSortPuzzle(
            bankId = "cs-deadlock-1",
            pack = Pack.EASY,
            difficulty = "d1",
            initialTubes = listOf(
                listOf(1, 2, 4, 5),
                listOf(2, 3, 5, 1),
                listOf(4, 5, 3, 2),
                listOf(4, 5, 2),
            ),
        )
        val level = GameLevel(
            ref = LevelRef("coloursort", deadlockPuzzle.bankId, Pack.EASY, levelNumber = 1L),
            size = 4,
            puzzle = deadlockPuzzle,
        )
        val lost = ColourSortGameModule.applyAction(
            ColourSortGameModule.newState(level),
            ColourSortAction(2, 3),
        )
        assertTrue(ColourSortGameModule.isLost(level, lost))

        val continued = ColourSortGameModule.applyContinue(lost)
        assertEquals(lost.tubes.size + 1, continued.tubes.size)
        assertTrue(continued.tubes.last().isEmpty())
        assertFalse(ColourSortGameModule.isLost(level, continued))
    }

    private fun assetFileBankSource(): BankSource {
        val bankRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).first { it.isDirectory }
        return object : BankSource {
            override fun open(path: String): InputStream {
                val raw = File(bankRoot, path)
                if (raw.isFile) {
                    return if (path.endsWith(".gz")) GZIPInputStream(raw.inputStream()) else raw.inputStream()
                }
                val plain = File(bankRoot, path.removeSuffix(".gz"))
                if (plain.isFile) return plain.inputStream()
                throw FileNotFoundException(path)
            }
        }
    }
}
