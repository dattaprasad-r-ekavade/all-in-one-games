package com.datathecodie.witgrid.engine.testkit

/** A rectangular grid parsed from visible ASCII characters in a Kotlin raw string. */
class AsciiGridFixture private constructor(val rows: List<String>) {
    /** Number of rows in the fixture. */
    val height: Int = rows.size

    /** Number of cells in each row. */
    val width: Int = rows.first().length

    /** Returns the visible ASCII symbol at zero-based [row] and [column]. */
    fun cellAt(row: Int, column: Int): Char {
        require(row in 0 until height && column in 0 until width) {
            "Fixture coordinates outside the grid"
        }
        return rows[row][column]
    }

    companion object {
        /**
         * Parses a non-empty rectangular ASCII grid. Leading indentation and blank outer lines
         * are ignored; blank rows, internal spaces, non-ASCII symbols and ragged rows are rejected.
         */
        fun parse(text: String): AsciiGridFixture {
            val lines = text.trimIndent().lines().toMutableList()
            while (lines.firstOrNull()?.isBlank() == true) lines.removeAt(0)
            while (lines.lastOrNull()?.isBlank() == true) lines.removeAt(lines.lastIndex)
            require(lines.isNotEmpty()) { "Fixture must contain at least one row" }

            val width = lines.first().length
            require(width > 0) { "Fixture rows must not be empty" }
            require(lines.all { it.length == width }) { "Fixture rows must have equal width" }
            require(lines.all { row -> row.all { it in '\u0021'..'\u007E' } }) {
                "Fixture cells must be visible ASCII symbols without spaces"
            }
            return AsciiGridFixture(lines.toList())
        }
    }
}
