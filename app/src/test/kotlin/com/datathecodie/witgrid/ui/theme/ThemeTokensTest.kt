package com.datathecodie.witgrid.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeTokensTest {
    private data class Setup(val regions: List<RegionHue>, val paper: Int, val ink: Int)

    private val setups = mapOf(
        "light" to Setup(RegionPalette.light, PaperInk.LIGHT_PAPER, PaperInk.LIGHT_INK),
        "dark" to Setup(RegionPalette.dark, PaperInk.DARK_PAPER, PaperInk.DARK_INK),
    )

    @Test
    fun contrastRatioMatchesKnownValues() {
        assertEquals(21.0, ColorMath.contrastRatio(0x000000, 0xFFFFFF), 0.001)
        assertEquals(1.0, ColorMath.contrastRatio(0x777777, 0x777777), 0.001)
    }

    @Test
    fun oklabLightnessOfBlackAndWhite() {
        assertEquals(0.0, ColorMath.oklabLightness(0x000000), 0.001)
        assertEquals(1.0, ColorMath.oklabLightness(0xFFFFFF), 0.001)
    }

    @Test
    fun oklchRoundTripKeepsLightness() {
        val rgb = ColorMath.oklchToRgb(0.6, 0.1, 140.0)
        assertNotNull(rgb)
        assertEquals(0.6, ColorMath.oklabLightness(rgb!!), 0.005)
    }

    @Test
    fun outOfGamutColourIsRejected() {
        assertNull(ColorMath.oklchToRgb(0.5, 0.4, 140.0))
    }

    @Test
    fun eightRegionsWithUniqueLettersHatchesAndColours() {
        setups.forEach { (name, setup) ->
            val regions = setup.regions
            assertEquals(8, regions.size, name)
            assertEquals("ABCDEFGH", regions.map { it.letter }.joinToString(""), name)
            assertEquals(8, regions.map { it.hatch }.toSet().size, name)
            assertEquals(8, regions.map { it.rgb }.toSet().size, name)
        }
    }

    @Test
    fun regionLightnessIsNearlyEqual() {
        setups.forEach { (name, setup) ->
            val lightness = setup.regions.map { ColorMath.oklabLightness(it.rgb) }
            assertTrue(lightness.max() - lightness.min() < 0.01, "$name spread")
        }
    }

    @Test
    fun cellTextIsReadableOnEveryRegion() {
        setups.forEach { (name, setup) ->
            setup.regions.forEach {
                val ratio = ColorMath.contrastRatio(it.rgb, setup.ink)
                assertTrue(ratio >= 7.0, "$name ${it.letter} text contrast $ratio")
            }
        }
    }

    @Test
    fun regionsStandOutFromPaper() {
        setups.forEach { (name, setup) ->
            setup.regions.forEach {
                val ratio = ColorMath.contrastRatio(it.rgb, setup.paper)
                assertTrue(ratio >= 1.3, "$name ${it.letter} paper contrast $ratio")
            }
        }
    }

    @Test
    fun accentIsReadableInEveryColourway() {
        Colourway.entries.forEach {
            assertTrue(ColorMath.contrastRatio(it.lightAccent, PaperInk.ON_LIGHT_ACCENT) >= 4.5, "${it.id} light")
            assertTrue(ColorMath.contrastRatio(it.darkAccent, PaperInk.ON_DARK_ACCENT) >= 4.5, "${it.id} dark")
            assertTrue(ColorMath.contrastRatio(it.lightAccent, PaperInk.LIGHT_PAPER) >= 4.5, "${it.id} on paper")
            assertTrue(ColorMath.contrastRatio(it.darkAccent, PaperInk.DARK_PAPER) >= 4.5, "${it.id} on black")
        }
    }

    @Test
    fun fiveColourwaysAndInkOnPaperContrast() {
        assertEquals(5, Colourway.entries.map { it.id }.toSet().size)
        assertTrue(ColorMath.contrastRatio(PaperInk.LIGHT_INK, PaperInk.LIGHT_PAPER) >= 7.0)
        assertTrue(ColorMath.contrastRatio(PaperInk.DARK_INK, PaperInk.DARK_PAPER) >= 7.0)
        assertEquals(0x000000, PaperInk.DARK_PAPER)
    }
}
