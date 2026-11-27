package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.tactics.TacticsCodec
import com.datathecodie.witgrid.engine.tactics.TacticsEngine
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class TacticsIntroTest {
    @Test
    fun introUsesTacticsLevelIdAndCoachLine() {
        assertEquals("CT-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_tactics, Intro.definition.coachLineResourceId)
    }

    @Test
    fun introLineIsCanonicalUniqueMateInOne() {
        val introLine = File("src/main/assets/bank/tactics-intro.txt").readText().trim()
        assertEquals(introLine, TacticsCodec.encode(TacticsCodec.decode(introLine)))
        val puzzle = TacticsEngine.decodePuzzle(introLine)
        assertEquals("CT-INTRO-01", puzzle.id)
        assertEquals(Pack.EASY, puzzle.pack)
        assertEquals(1, puzzle.mateIn)
        assertEquals(1, TacticsEngine.countSolutions(puzzle, limit = 2))
        assertEquals("checkmate:1", puzzle.storedGrade)
        assertEquals("checkmate:1", "${TacticsEngine.grade(puzzle).hardestTechnique?.id}:${TacticsEngine.grade(puzzle).stepCount}")
    }

    @Test
    fun tacticsIsRegisteredInGameRegistryWithInterstitialIntervalTwo() {
        assertEquals(TacticsGameModule, GameRegistry.find("tactics"))
        assertTrue(GameRegistry.games.contains(TacticsGameModule))
        val bank = assetFileBankSource()
        val level = TacticsGameModule.loadLevel(
            TacticsGameModule.firstLevelRef(Pack.EASY, bank),
            bank,
        )
        assertEquals(2, TacticsGameModule.levelsPerInterstitial(level))
    }

    @Test
    fun introAssetLoadsAsFirstEasyLevel() {
        val bank = assetFileBankSource()
        val refs = TacticsGameModule.levelRefs(Pack.EASY, bank)
        val introRef = refs.first()

        assertTrue(introRef.isIntro)
        assertEquals("CT-INTRO-01", introRef.levelId)
        assertEquals(R.string.intro_coach_tactics, introRef.introCoachLineResourceId)

        val level = TacticsGameModule.loadLevel(introRef, bank)
        assertEquals(8, level.size)
        val start = TacticsGameModule.newState(level)
        assertFalse(TacticsGameModule.isSolved(level, start))

        val next = TacticsGameModule.nextLevelRef(introRef, bank)
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
