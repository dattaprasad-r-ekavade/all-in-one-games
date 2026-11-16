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
 * Eight hues with equal OKLab lightness. Hue and chroma were searched so that the smallest OKLab
 * distance between any two hues is as large as the sRGB gamut allows (chroma capped at 0.16).
 * Light and dark sets use different hues because the gamut has a different shape at each lightness.
 * The order puts far-apart hues next to each other, because regions that touch need the most contrast.
 */
object RegionPalette {
    const val LIGHT_LIGHTNESS = 0.82
    const val DARK_LIGHTNESS = 0.40

    private class Spec(val hueDegrees: Double, val chroma: Double)

    private val lightSpecs = listOf(
        Spec(43.0, 0.100),
        Spec(152.0, 0.160),
        Spec(294.0, 0.095),
        Spec(81.0, 0.160),
        Spec(189.0, 0.135),
        Spec(347.0, 0.115),
        Spec(116.0, 0.160),
        Spec(236.0, 0.100),
    )

    private val darkSpecs = listOf(
        Spec(4.0, 0.150),
        Spec(143.0, 0.125),
        Spec(297.0, 0.160),
        Spec(41.0, 0.115),
        Spec(213.0, 0.065),
        Spec(330.0, 0.160),
        Spec(95.0, 0.075),
        Spec(264.0, 0.160),
    )

    val light: List<RegionHue> = build(LIGHT_LIGHTNESS, lightSpecs)
    val dark: List<RegionHue> = build(DARK_LIGHTNESS, darkSpecs)

    private fun build(lightness: Double, specs: List<Spec>): List<RegionHue> =
        HatchPattern.entries.mapIndexed { index, hatch ->
            val spec = specs[index]
            val rgb = ColorMath.oklchToRgb(lightness, spec.chroma, spec.hueDegrees)
                ?: error("Region hue $index is outside the sRGB gamut")
            RegionHue(rgb, 'A' + index, hatch)
        }
}
