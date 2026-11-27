package com.datathecodie.witgrid.games.klondike

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

class KlondikeIntroTest {
    @Test
    fun introUsesKlondikeLevelIdAndCoachLine() {
        assertEquals("KL-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_klondike, Intro.definition.coachLineResourceId)
    }

    @Test
    fun klondikeIsRegisteredInPlayRegistryWithInterstitialIntervalOne() {
        assertEquals(KlondikeGameModule, PlayRegistry.find("klondike"))
        assertTrue(PlayRegistry.games.contains(KlondikeGameModule))
        val bank = assetFileBankSource()
        val level = KlondikeGameModule.loadLevel(
            KlondikeGameModule.firstLevelRef(Pack.EASY, bank),
            bank,
        )
        assertEquals(1, KlondikeGameModule.levelsPerInterstitial(level))
    }

    @Test
    fun introAssetLoadsAsFirstEasyLevel() {
        val bank = assetFileBankSource()
        val refs = KlondikeGameModule.levelRefs(Pack.EASY, bank)
        val introRef = refs.first()

        assertTrue(introRef.isIntro)
        assertEquals("KL-INTRO-01", introRef.levelId)
        assertEquals(R.string.intro_coach_klondike, introRef.introCoachLineResourceId)

        val level = KlondikeGameModule.loadLevel(introRef, bank)
        assertEquals(52, level.size)
        val start = KlondikeGameModule.newState(level)
        assertFalse(KlondikeGameModule.isWon(level, start))
        assertFalse(KlondikeGameModule.isLost(level, start))

        val next = KlondikeGameModule.nextLevelRef(introRef, bank)
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
