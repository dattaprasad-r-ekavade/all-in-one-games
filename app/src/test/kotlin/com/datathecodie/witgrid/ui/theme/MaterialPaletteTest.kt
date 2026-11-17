package com.datathecodie.witgrid.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MaterialPaletteTest {
    @Test
    fun materialSurfacesUseApprovedPaperCardAndInkInEveryColourway() {
        Colourway.entries.forEach { colourway ->
            listOf(false, true).forEach { dark ->
                val colors = witgridColors(colourway, dark)
                val scheme = witgridColorScheme(colors)
                assertEquals(colors.paper, scheme.background)
                assertEquals(colors.paper, scheme.surface)
                assertEquals(colors.paper, scheme.surfaceContainerLowest)
                assertEquals(colors.card, scheme.surfaceContainer)
                assertEquals(colors.card, scheme.surfaceContainerLow)
                assertEquals(colors.card, scheme.surfaceContainerHigh)
                assertEquals(colors.cardEdge, scheme.surfaceContainerHighest)
                assertEquals(colors.ink, scheme.onSurface)
                assertEquals(colors.mutedInk, scheme.onSurfaceVariant)
                assertEquals(colors.cardEdge, scheme.outlineVariant)
                assertEquals(colors.mutedInk, scheme.outline)
                assertEquals(colors.accent, scheme.primary)
                assertEquals(colors.accent, scheme.secondary)
                assertEquals(colors.onAccent, scheme.onPrimary)
                assertEquals(colors.accentSoft, scheme.primaryContainer)
                assertEquals(colors.ink, scheme.onPrimaryContainer)
                assertEquals(colors.paper, scheme.inversePrimary)
                assertEquals(colors.ink, scheme.inverseSurface)
            }
        }
    }

    @Test
    fun softAccentIsOpaqueAndReadableOnBothBackgrounds() {
        assertEquals(Color(0xFFE5E8F5), witgridColors(Colourway.INDIGO, false).accentSoft)
        Colourway.entries.forEach { colourway ->
            val colors = witgridColors(colourway, true)
            assertEquals(1f, colors.accentSoft.alpha)
            assertTrue(colors.accentSoft.red < colors.accent.red)
            assertTrue(colors.accentSoft.green < colors.accent.green)
            assertTrue(colors.accentSoft.blue < colors.accent.blue)
            assertTrue(ColorMath.contrastRatio(colors.accentSoft.toArgb() and 0xFFFFFF, PaperInk.DARK_INK) >= 7.0)
        }
    }
}
