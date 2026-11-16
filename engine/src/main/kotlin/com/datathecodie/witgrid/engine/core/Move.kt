package com.datathecodie.witgrid.engine.core

/** Player marks for placement and two-symbol grids; the lane defines the meaning of each symbol. */
enum class Mark {
    EMPTY,
    FILLED,
    CROSSED,
}

/**
 * Absolute edits shared by boards and hints, so replay never depends on a toggle mode.
 *
 * Lanes handle these sealed variants with an exhaustive `when`, without casts. Engines
 * reject unsupported variants or invalid cells and values with [IllegalArgumentException].
 */
sealed interface Move {
    /** Target cell, checked against the puzzle geometry by the engine. */
    val cell: Cell

    /** Sets or clears a placement mark or a two-symbol value. */
    data class SetMark(override val cell: Cell, val mark: Mark) : Move

    /** Sets a positive digit; zero clears it. The engine checks the game's digit range. */
    data class SetDigit(override val cell: Cell, val digit: Int) : Move {
        init {
            require(digit >= 0) { "Digit must not be negative" }
        }
    }

    /**
     * Replaces all pencil notes. Bit `digit - 1` represents digits 1 through 31;
     * zero clears the notes. The engine checks its digit range and note/value policy.
     * A bit mask keeps the edit immutable without a mutable collection backing it.
     */
    data class SetNotes(override val cell: Cell, val digitMask: Int) : Move {
        init {
            require(digitMask >= 0) { "Note mask must not use the sign bit" }
        }
    }
}
