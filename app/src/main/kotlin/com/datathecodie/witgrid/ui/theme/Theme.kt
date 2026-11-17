package com.datathecodie.witgrid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A region hue as Compose colour, with its letter and hatch pattern. */
data class RegionStyle(val color: Color, val letter: Char, val hatch: HatchPattern)

/** Witgrid colours that Material 3 has no slot for. */
data class WitgridColors(
    val paper: Color,
    val ink: Color,
    val accent: Color,
    val onAccent: Color,
    val card: Color,
    val cardEdge: Color,
    val mutedInk: Color,
    val regions: List<RegionStyle>,
    val isDark: Boolean,
)

val LocalWitgridColors = staticCompositionLocalOf<WitgridColors> { error("WitgridTheme is missing") }

object WitgridTheme {
    val colors: WitgridColors
        @Composable get() = LocalWitgridColors.current
}

private fun rgb(value: Int) = Color(0xFF000000.toInt() or value)

fun witgridColors(colourway: Colourway, darkTheme: Boolean): WitgridColors {
    val regions = (if (darkTheme) RegionPalette.dark else RegionPalette.light)
        .map { RegionStyle(rgb(it.rgb), it.letter, it.hatch) }
    return if (darkTheme) {
        WitgridColors(
            paper = rgb(PaperInk.DARK_PAPER),
            ink = rgb(PaperInk.DARK_INK),
            accent = rgb(colourway.darkAccent),
            onAccent = rgb(PaperInk.ON_DARK_ACCENT),
            card = rgb(PaperInk.DARK_CARD),
            cardEdge = rgb(PaperInk.DARK_CARD_EDGE),
            mutedInk = rgb(PaperInk.DARK_MUTED_INK),
            regions = regions,
            isDark = true,
        )
    } else {
        WitgridColors(
            paper = rgb(PaperInk.LIGHT_PAPER),
            ink = rgb(PaperInk.LIGHT_INK),
            accent = rgb(colourway.lightAccent),
            onAccent = rgb(PaperInk.ON_LIGHT_ACCENT),
            card = rgb(PaperInk.LIGHT_CARD),
            cardEdge = rgb(PaperInk.LIGHT_CARD_EDGE),
            mutedInk = rgb(PaperInk.LIGHT_MUTED_INK),
            regions = regions,
            isDark = false,
        )
    }
}

@Composable
fun WitgridTheme(
    colourway: Colourway = Colourway.INDIGO,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = witgridColors(colourway, darkTheme)
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.paper, onBackground = colors.ink,
            surface = colors.paper, onSurface = colors.ink,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.paper, onBackground = colors.ink,
            surface = colors.paper, onSurface = colors.ink,
        )
    }
    CompositionLocalProvider(LocalWitgridColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = WitgridTypography, content = content)
    }
}
