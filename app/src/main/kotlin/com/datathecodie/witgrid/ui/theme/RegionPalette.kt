package com.datathecodie.witgrid.ui.theme

/** Hatch pattern ids. The board canvas (A4) draws the pattern for each id. */
enum class HatchPattern(val id: Int) {
    DIAGONAL_UP(0),
    DIAGONAL_DOWN(1),
    HORIZONTAL(2),
    VERTICAL(3),
    GRID(4),
    DOTS(5),
    CROSS_DIAGONAL(6),
    ZIGZAG(7),
}

/** One region hue: colour as 0xRRGGBB, a letter and a hatch pattern for colour-blind players. */
data class RegionHue(val rgb: Int, val letter: Char, val hatch: HatchPattern)

/**
 * Eight hues with equal OKLab lightness, spaced evenly around the hue wheel.
 * Light regions are pale fills under dark ink. Dark regions are deep fills on true black under light ink.
 */
object RegionPalette {
    const val LIGHT_LIGHTNESS = 0.82
    const val DARK_LIGHTNESS = 0.40
    private const val CHROMA = 0.06
    private const val FIRST_HUE = 25.0
    private const val HUE_STEP = 45.0

    val light: List<RegionHue> = build(LIGHT_LIGHTNESS)
    val dark: List<RegionHue> = build(DARK_LIGHTNESS)

    private fun build(lightness: Double): List<RegionHue> = HatchPattern.entries.mapIndexed { index, hatch ->
        val rgb = ColorMath.oklchToRgb(lightness, CHROMA, FIRST_HUE + HUE_STEP * index)
            ?: error("Region hue $index is outside the sRGB gamut")
        RegionHue(rgb, 'A' + index, hatch)
    }
}
