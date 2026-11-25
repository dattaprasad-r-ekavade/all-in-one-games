package com.datathecodie.witgrid.games.skyscrapers

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

class SkyscrapersGameModuleTest {
    @Test
    fun registersSkyscrapersWithGuidedIntroAndTwoLevelAdInterval() {
        val module = SkyscrapersGameModule
        val bank = FakeBankSource(
            mapOf(
                "bank/skyscrapers-intro.txt" to
                    "SK-INTRO-01|4|4321/1222/4321/1222/1234............|1234234134124123|EASY|edge-clue:1",
                "bank/skyscrapers-easy.txt.gz" to
                    "SK-EASY-01|4|4321/1222/4321/1222|1234234134124123|EASY|edge-clue:1",
            ),
        )

        assertEquals(module, GameRegistry.find("skyscrapers"))
        assertTrue(Registry.engines.any { it.id == "skyscrapers" })

        val firstReference = module.firstLevelRef(Pack.EASY, bank)
        assertTrue(firstReference.isIntro)
        assertEquals("SK-INTRO-01", firstReference.levelId)
        assertEquals(R.string.intro_coach_skyscrapers, firstReference.introCoachLineResourceId)

        val introLevel = module.loadLevel(firstReference, bank)
        assertEquals(2, module.levelsPerInterstitial(introLevel))
        assertEquals(4, introLevel.size)
        assertNotNull(introLevel.puzzle)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val contents = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$contents\n".toByteArray(Charsets.UTF_8))
        }
    }
}
