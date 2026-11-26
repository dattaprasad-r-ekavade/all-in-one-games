package com.datathecodie.witgrid.games.killersudoku

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
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class KillerSudokuGameModuleTest {
    @Test
    fun registersKillerSudokuWithGuidedIntroAndOneLevelAdInterval() {
        val module = KillerSudokuGameModule
        val introLine = File("src/main/assets/bank/killersudoku-intro.txt").readText().trim()
        val bank = FakeBankSource(
            mapOf(
                "bank/killersudoku-intro.txt" to introLine,
                "bank/killersudoku-easy.txt.gz" to introLine.replaceFirst("KS-INTRO-01", "KS-EASY-01"),
            ),
        )

        assertEquals(module, GameRegistry.find("killersudoku"))
        assertTrue(Registry.engines.any { it.id == "killersudoku" })
        assertEquals("KS-INTRO-01", Intro.definition.levelId)
        assertEquals(R.string.intro_coach_killersudoku, Intro.definition.coachLineResourceId)

        val firstReference = module.firstLevelRef(Pack.EASY, bank)
        assertTrue(firstReference.isIntro)
        assertEquals("KS-INTRO-01", firstReference.levelId)
        assertEquals(R.string.intro_coach_killersudoku, firstReference.introCoachLineResourceId)

        val introLevel = module.loadLevel(firstReference, bank)
        assertEquals(1, module.levelsPerInterstitial(introLevel))
        assertEquals(9, introLevel.size)
        assertNotNull(introLevel.puzzle)
    }

    private class FakeBankSource(private val files: Map<String, String>) : BankSource {
        override fun open(path: String): InputStream {
            val contents = files[path] ?: throw FileNotFoundException(path)
            return ByteArrayInputStream("$contents\n".toByteArray(Charsets.UTF_8))
        }
    }
}
