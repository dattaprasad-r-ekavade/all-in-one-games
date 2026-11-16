package com.datathecodie.witgrid.ui.theme

import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Colour maths in plain Kotlin so JVM tests can check the palette. Colours are 0xRRGGBB Ints. */
object ColorMath {
    /** Converts OKLCH (lightness 0..1, chroma, hue in degrees) to sRGB. Null when outside the sRGB gamut. */
    fun oklchToRgb(lightness: Double, chroma: Double, hueDegrees: Double): Int? {
        val radians = Math.toRadians(hueDegrees)
        val a = chroma * cos(radians)
        val b = chroma * sin(radians)
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).let { it * it * it }
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).let { it * it * it }
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).let { it * it * it }
        val linear = doubleArrayOf(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
        if (linear.any { it < -EPSILON || it > 1.0 + EPSILON }) return null
        val channels = linear.map { (encode(it.coerceIn(0.0, 1.0)) * 255.0 + 0.5).toInt() }
        return (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
    }

    /** OKLab lightness (0..1) of an sRGB colour. */
    fun oklabLightness(rgb: Int): Double = oklab(rgb)[0]

    /** Euclidean distance between two sRGB colours in OKLab. */
    fun oklabDistance(first: Int, second: Int): Double {
        val a = oklab(first)
        val b = oklab(second)
        return sqrt((a[0] - b[0]).pow(2) + (a[1] - b[1]).pow(2) + (a[2] - b[2]).pow(2))
    }

    private fun oklab(rgb: Int): DoubleArray {
        val r = decode(channel(rgb, 16))
        val g = decode(channel(rgb, 8))
        val b = decode(channel(rgb, 0))
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    /** WCAG relative luminance. */
    fun relativeLuminance(rgb: Int): Double =
        0.2126 * decode(channel(rgb, 16)) + 0.7152 * decode(channel(rgb, 8)) + 0.0722 * decode(channel(rgb, 0))

    /** WCAG contrast ratio, 1..21. Argument order does not matter. */
    fun contrastRatio(first: Int, second: Int): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private const val EPSILON = 1e-6

    private fun channel(rgb: Int, shift: Int): Int = (rgb shr shift) and 0xFF

    private fun decode(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun encode(c: Double): Double =
        if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055
}
