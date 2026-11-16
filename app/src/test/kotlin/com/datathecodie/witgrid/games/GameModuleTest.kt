package com.datathecodie.witgrid.games

import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.crowns.CrownsCodec
import com.datathecodie.witgrid.engine.crowns.CrownsEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.GZIPOutputStream

class GameModuleTest {
    @Test
    fun levelRefsListsEveryPackLevelInBankOrder() {
        val bank = FakeBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)

        assertEquals(easyIds, refs.map { it.levelId })
        assertTrue(refs.all { it.pack == Pack.EASY && !it.daily && it.gameId == "crowns" })
    }

    @Test
    fun nextLevelRefReturnsTheFollowingLevelInTheMiddle() {
        val bank = FakeBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)

        assertEquals(refs[2], crownsModule.nextLevelRef(refs[1], bank))
        assertEquals(easyIds[2], crownsModule.nextLevelRef(refs[1], bank)?.levelId)
    }

    @Test
    fun nextLevelRefIsNullAfterTheLastLevel() {
        val bank = FakeBankSource()
        val refs = crownsModule.levelRefs(Pack.EASY, bank)

        assertNull(crownsModule.nextLevelRef(refs.last(), bank))
    }

    @Test
    fun dailyRefReadsTheDailyFileAndKeepsTheLinePack() {
        val bank = FakeBankSource()
        val ref = crownsModule.dailyRef(0, bank)

        assertEquals(LevelRef("crowns", dailyId(0), Pack.MEDIUM, daily = true), ref)
        assertEquals(listOf("bank/crowns-daily.txt.gz"), bank.openedPaths)
    }

    @Test
    fun dailyRefAcceptsTheLastDayOfTheYear() {
        val ref = crownsModule.dailyRef(364, FakeBankSource())

        assertEquals(dailyId(364), ref.levelId)
        assertTrue(ref.daily)
    }

    @Test
    fun dailyRefRejectsDayIndexesOutsideTheYear() {
        val bank = FakeBankSource()

        assertThrows(IllegalArgumentException::class.java) { crownsModule.dailyRef(-1, bank) }
        assertThrows(IllegalArgumentException::class.java) { crownsModule.dailyRef(365, bank) }
    }

    @Test
    fun loadLevelReadsTheDailyFileForDailyRefs() {
        val bank = FakeBankSource()
        val level = crownsModule.loadLevel(crownsModule.dailyRef(0, bank), bank)

        assertTrue(level.ref.daily)
        assertEquals(dailySize, level.size)
        assertEquals("bank/crowns-daily.txt.gz", bank.openedPaths.last())
    }

    @Test
    fun bankFileIsParsedOnceForRepeatedCalls() {
        val bank = FakeBankSource()
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
        val bank = FakeBankSource()
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

    /**
     * Builds a small Crowns bank in memory. Lines come from seeded generation, so the test
     * does not depend on the committed bank, which card B5 regenerates.
     */
    private class FakeBankSource : BankSource {
        val openedPaths = mutableListOf<String>()

        override fun open(path: String): InputStream {
            openedPaths += path
            val content = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream(content)
        }

        private companion object {
            val files: Map<String, ByteArray> = mapOf(
                "bank/crowns-easy.txt.gz" to gzip(easyLines),
                "bank/crowns-daily.txt.gz" to gzip(dailyLines),
            )
        }
    }

    private companion object {
        const val DAILY_COUNT = 365

        val easyLines: List<String> = listOf(11L, 12L, 13L).map { seed ->
            CrownsCodec.encode(CrownsEngine.generate(SplitMix64(seed), Pack.EASY))
        }
        val easyIds: List<String> = easyLines.map { BankLine.decode(it).id }

        // Daily lines are copies of one generated MEDIUM line with new IDs, so the file has 365 days.
        val dailyTemplate: BankLine = BankLine.decode(
            CrownsCodec.encode(CrownsEngine.generate(SplitMix64(21L), Pack.MEDIUM)),
        )
        val dailySize: Int = dailyTemplate.size
        val dailyLines: List<String> = (0 until DAILY_COUNT).map { index ->
            dailyTemplate.copy(id = dailyId(index)).encode()
        }

        fun dailyId(index: Int): String = "CR-DAILY-%03d".format(index)

        fun gzip(lines: List<String>): ByteArray {
            val bytes = ByteArrayOutputStream()
            GZIPOutputStream(bytes).bufferedWriter(Charsets.UTF_8).use { writer ->
                lines.forEach { line -> writer.write(line + "\n") }
            }
            return bytes.toByteArray()
        }
    }
}
