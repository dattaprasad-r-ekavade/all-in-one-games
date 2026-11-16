package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream

class GameModuleTest {
    @Test
    fun levelRefsListsEveryPackLevelInBankOrder() {
        val bank = FolderBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)

        assertEquals(150, refs.size)
        assertEquals("CR-EASY-448630a7e7849bdd", refs.first().levelId)
        assertTrue(refs.all { it.pack == Pack.EASY && !it.daily && it.gameId == "crowns" })
        assertEquals(refs.size, refs.map { it.levelId }.distinct().size)
    }

    @Test
    fun nextLevelRefReturnsTheFollowingLevelInTheMiddle() {
        val bank = FolderBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)

        assertEquals(refs[75], crownsModule.nextLevelRef(refs[74], bank))
    }

    @Test
    fun nextLevelRefIsNullAfterTheLastLevel() {
        val bank = FolderBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)

        assertNull(crownsModule.nextLevelRef(refs.last(), bank))
    }

    @Test
    fun dailyRefReadsTheDailyFileAndKeepsTheLinePack() {
        val bank = FolderBankSource()
        val ref = crownsModule.dailyRef(0, bank)

        assertEquals(LevelRef("crowns", "CR-EASY--36a535be1984d926", Pack.EASY, daily = true), ref)
        assertEquals(listOf("bank/crowns-daily.txt.gz"), bank.openedPaths)
    }

    @Test
    fun dailyRefAcceptsTheLastDayOfTheYear() {
        val ref = crownsModule.dailyRef(364, FolderBankSource())

        assertTrue(ref.daily)
    }

    @Test
    fun dailyRefRejectsDayIndexesOutsideTheYear() {
        val bank = FolderBankSource()

        assertThrows(IllegalArgumentException::class.java) { crownsModule.dailyRef(-1, bank) }
        assertThrows(IllegalArgumentException::class.java) { crownsModule.dailyRef(365, bank) }
    }

    @Test
    fun loadLevelReadsTheDailyFileForDailyRefs() {
        val bank = FolderBankSource()
        val level = crownsModule.loadLevel(crownsModule.dailyRef(0, bank), bank)

        assertTrue(level.ref.daily)
        assertEquals(6, level.size)
        assertEquals("bank/crowns-daily.txt.gz", bank.openedPaths.last())
    }

    @Test
    fun bankFileIsParsedOnceForRepeatedCalls() {
        val bank = FolderBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)
        crownsModule.nextLevelRef(refs.first(), bank)
        crownsModule.firstLevelRef(Pack.EASY, bank)

        assertEquals(1, bank.openedPaths.count { it == "bank/crowns-easy.txt.gz" })
    }

    @Test
    fun interstitialRuleIsSetPerGameAndNonogramSize() {
        assertEquals(1, levelsPerInterstitialFor("crowns", size = 6))
        assertEquals(2, levelsPerInterstitialFor("binary", size = 6))
        assertEquals(2, levelsPerInterstitialFor("minisudoku", size = 6))
        assertEquals(2, levelsPerInterstitialFor("nonogram", size = 5))
        assertEquals(1, levelsPerInterstitialFor("nonogram", size = 10))
        assertEquals(1, levelsPerInterstitialFor("nonogram", size = 15))
    }

    @Test
    fun interstitialRuleMustBePositive() {
        val module = EngineGameModule(CrownsEngine) { 0 }
        val bank = FolderBankSource()
        val level = crownsModule.loadLevel(crownsModule.dailyRef(0, bank), bank)

        assertThrows(IllegalArgumentException::class.java) { module.levelsPerInterstitial(level) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun levelsPerInterstitialFor(gameId: String, size: Int): Int {
        val module = requireNotNull(GameRegistry.find(gameId)) as GameModule<Any, Any>
        val level = GameLevel<Any>(LevelRef(gameId, "$gameId-test-1", Pack.EASY), size, Unit)
        return module.levelsPerInterstitial(level)
    }

    private val crownsModule = EngineGameModule(CrownsEngine) { 1 }

    /** Serves the committed bank files from the app assets folder and records each open. */
    private class FolderBankSource : BankSource {
        val openedPaths = mutableListOf<String>()

        override fun open(path: String): InputStream {
            openedPaths += path
            return File(ASSETS_FOLDER, path).inputStream()
        }

        private companion object {
            val ASSETS_FOLDER = File("src/main/assets")
        }
    }
}
