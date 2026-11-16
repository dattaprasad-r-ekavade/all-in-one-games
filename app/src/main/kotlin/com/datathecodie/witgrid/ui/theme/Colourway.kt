package com.datathecodie.witgrid.ui.theme

/** A selectable colourway. Only the accent changes. Paper, ink and regions stay fixed. */
enum class Colourway(val id: String, val lightAccent: Int, val darkAccent: Int) {
    INDIGO("indigo", 0x3B4FA0, 0x9AA8F0),
    FOREST("forest", 0x2F6B45, 0x7FC79A),
    PLUM("plum", 0x7A3A78, 0xD39AD0),
    RUST("rust", 0xA5472A, 0xF0A284),
    SLATE("slate", 0x3F5B6B, 0x8DB4C7),
}

/** Paper and ink base colours as 0xRRGGBB. */
object PaperInk {
    const val LIGHT_PAPER = 0xF6F1E7
    const val LIGHT_INK = 0x1C1B19
    const val DARK_PAPER = 0x000000
    const val DARK_INK = 0xEDE9E0
    const val ON_LIGHT_ACCENT = 0xFFFFFF
    const val ON_DARK_ACCENT = 0x000000
}
