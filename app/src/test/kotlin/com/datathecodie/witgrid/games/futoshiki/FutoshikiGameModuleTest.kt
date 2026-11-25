package com.datathecodie.witgrid.games.futoshiki

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

class FutoshikiGameModuleTest {
    @Test
    fun registersFutoshikiWithGuidedIntroAndTwoLevelAdInterval() {
        val module = FutoshikiGameModule
        val introLine = java.io.File("src/main/assets/bank/futoshiki-intro.txt").readText().trim()
        val bank = FakeBankSource(
            mapOf(
                "bank/futoshiki-intro.txt" to introLine,
                "bank/futoshiki-easy.txt.gz" to introLine.replaceFirst("FU-INTRO-01", "FU-EASY-01"),
            ),
        )

        assertEquals(module, GameRegistry.find("futoshiki"))
        assertTrue(Registry.engines.any { it.id == "futoshiki" })

        val firstReference = module.firstLevelRef(Pack.EASY, bank)
        assertTrue(firstReference.isIntro)
        assertEquals("FU-INTRO-01", firstReference.levelId)
        assertEquals(R.string.intro_coach_futoshiki, firstReference.introCoachLineResourceId)

        val introLevel = module.loadLevel(firstReference, bank)
        assertEquals(2, module.levelsPerInterstitial(introLevel))
        assertEquals(5, introLevel.size)
        assertNotNull(introLevel.puzzle)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val contents = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$contents\n".toByteArray(Charsets.UTF_8))
        }
    }
}
