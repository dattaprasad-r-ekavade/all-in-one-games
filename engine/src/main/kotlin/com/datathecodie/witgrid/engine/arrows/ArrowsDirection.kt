package com.datathecodie.witgrid.engine.arrows

/**
 * Four cardinal directions an arrow can slide on a square grid.
 */
enum class ArrowsDirection(
    val symbol: Char,
    val rowDelta: Int,
    val columnDelta: Int,
) {
    UP('^', rowDelta = -1, columnDelta = 0),
    DOWN('v', rowDelta = 1, columnDelta = 0),
    LEFT('<', rowDelta = 0, columnDelta = -1),
    RIGHT('>', rowDelta = 0, columnDelta = 1);

    val letter: Char get() = when (this) {
        UP -> 'U'
        DOWN -> 'D'
        LEFT -> 'L'
        RIGHT -> 'R'
    }

    companion object {
        fun fromChar(char: Char): ArrowsDirection = when (char) {
            '^', 'U', 'u' -> UP
            'v', 'V', 'D', 'd' -> DOWN
            '<', 'L', 'l' -> LEFT
            '>', 'R', 'r' -> RIGHT
            else -> throw IllegalArgumentException("Invalid arrow direction character: $char")
        }

        fun fromCharOrNull(char: Char): ArrowsDirection? = when (char) {
            '.' -> null
            else -> fromChar(char)
        }
    }
}
