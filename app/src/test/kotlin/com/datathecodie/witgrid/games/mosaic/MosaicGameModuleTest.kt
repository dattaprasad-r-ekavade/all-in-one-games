package com.datathecodie.witgrid.games.mosaic

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

class MosaicGameModuleTest {
    @Test
    fun registersMosaicWithGuidedIntroAndTwoLevelAdInterval() {
        val module = MosaicGameModule
        val introLine = java.io.File("src/main/assets/bank/mosaic-intro.txt").readText().trim()
        val bank = FakeBankSource(
            mapOf(
                "bank/mosaic-intro.txt" to introLine,
                "bank/mosaic-easy.txt.gz" to introLine.replaceFirst("MO-INTRO-01", "MO-EASY-01"),
            ),
        )

        assertEquals(module, GameRegistry.find("mosaic"))
        assertTrue(Registry.engines.any { it.id == "mosaic" })

        val firstReference = module.firstLevelRef(Pack.EASY, bank)
        assertTrue(firstReference.isIntro)
        assertEquals("MO-INTRO-01", firstReference.levelId)
        assertEquals(R.string.intro_coach_mosaic, firstReference.introCoachLineResourceId)

        val introLevel = module.loadLevel(firstReference, bank)
        assertEquals(2, module.levelsPerInterstitial(introLevel))
        assertEquals(8, introLevel.size)
        assertNotNull(introLevel.puzzle)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val contents = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$contents\n".toByteArray(Charsets.UTF_8))
        }
    }
}
