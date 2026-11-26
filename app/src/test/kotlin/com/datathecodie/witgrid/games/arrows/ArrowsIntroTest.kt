package com.datathecodie.witgrid.games.arrows

import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.PlayRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class ArrowsIntroTest {
    @Test
    fun introUsesArrowsLevelIdAndCoachLine() {
        assertEquals("AR-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_arrows, Intro.definition.coachLineResourceId)
    }

    @Test
    fun arrowsIsRegisteredInPlayRegistryWithInterstitialIntervalTwo() {
        assertEquals(ArrowsGameModule, PlayRegistry.find("arrows"))
        assertTrue(PlayRegistry.games.contains(ArrowsGameModule))
        val level = ArrowsGameModule.loadLevel(
            ArrowsGameModule.firstLevelRef(Pack.EASY, assetFileBankSource()),
            assetFileBankSource(),
        )
        assertEquals(2, ArrowsGameModule.levelsPerInterstitial(level))
    }

    @Test
    fun introAssetLoadsAsFirstEasyLevelAndSolvesToNextRegularLevel() {
        val bank = assetFileBankSource()
        val refs = ArrowsGameModule.levelRefs(Pack.EASY, bank)
        val introRef = refs.first()

        assertTrue(introRef.isIntro)
        assertEquals("AR-INTRO-01", introRef.levelId)
        assertEquals(R.string.intro_coach_arrows, introRef.introCoachLineResourceId)

        val level = ArrowsGameModule.loadLevel(introRef, bank)
        assertEquals(5, level.size)
        var state = ArrowsGameModule.newState(level)
        assertFalse(ArrowsGameModule.isWon(level, state))
        while (!ArrowsGameModule.isWon(level, state)) {
            val hint = ArrowsGameModule.hintAction(level, state)
            state = ArrowsGameModule.applyAction(state, requireNotNull(hint))
        }
        assertTrue(ArrowsGameModule.isWon(level, state))

        val next = ArrowsGameModule.nextLevelRef(introRef, bank)
        assertEquals(1L, next?.levelNumber)
        assertFalse(next!!.isIntro)
        assertEquals(refs[1].levelId, next.levelId)
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
