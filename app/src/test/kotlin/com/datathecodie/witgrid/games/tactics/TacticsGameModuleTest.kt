package com.datathecodie.witgrid.games.tactics

import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.Registry
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class TacticsGameModuleTest {
    @Test
    fun registersTacticsWithNumberedLevelsAndTwoLevelAdInterval() {
        val module = TacticsGameModule
        val introLine = java.io.File("src/main/assets/bank/tactics-intro.txt").readText().trim()
        val bank = FakeBankSource(
            mapOf(
                "bank/tactics-intro.txt" to introLine,
                "bank/tactics-easy.txt.gz" to introLine.replaceFirst("CT-INTRO-01", "CT-EASY-01"),
            ),
        )

        assertEquals(module, GameRegistry.find("tactics"))
        assertTrue(Registry.engines.any { it.id == "tactics" })

        val firstReference = module.firstLevelRef(Pack.EASY, bank)
        assertTrue(!firstReference.isIntro)
        assertEquals(1L, firstReference.levelNumber)

        val level = module.loadLevel(firstReference, bank)
        assertEquals(2, module.levelsPerInterstitial(level))
        assertEquals(8, level.size)
        assertNotNull(level.puzzle)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val contents = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$contents\n".toByteArray(Charsets.UTF_8))
        }
    }
}
