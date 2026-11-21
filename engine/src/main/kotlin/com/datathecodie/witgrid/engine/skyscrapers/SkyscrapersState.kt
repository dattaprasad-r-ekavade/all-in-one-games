package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Move

/**
 * Immutable player state for a Skyscrapers puzzle, holding placed building heights
 * and pencil notes for empty cells.
 */
class SkyscrapersState(
    val puzzle: SkyscrapersPuzzle,
    digits: List<Int?>,
    notes: List<Int>,
) {
    /** Player-entered building heights, with null representing an empty cell. */
    val digits: List<Int?> = immutableList(digits)

    /** Per-cell pencil note bitmasks, where bit `digit - 1` represents digits 1 through size. */
    val notes: List<Int> = immutableList(notes)

    init {
        require(this.digits.size == puzzle.grid.cellCount) {
            "Skyscrapers state needs one digit per cell"
        }
        require(this.notes.size == puzzle.grid.cellCount) {
            "Skyscrapers state needs one note mask per cell"
        }
        val validDigits = 1..puzzle.size
        val maxNoteMask = (1 shl puzzle.size) - 1
        require(this.digits.all { it == null || it in validDigits }) {
            "Skyscrapers state digits must be empty or between 1 and ${puzzle.size}"
        }
        require(this.notes.all { it in 0..maxNoteMask }) {
            "Skyscrapers note mask may contain only digits 1 through ${puzzle.size}"
        }
        require(this.digits.indices.all { index -> this.digits[index] == null || this.notes[index] == 0 }) {
            "A digit value cannot keep pencil notes in its cell"
        }
        puzzle.givens.forEachIndexed { index, given ->
            require(given == null || this.digits[index] == given) {
                "Skyscrapers state must retain every given"
            }
            require(given == null || this.notes[index] == 0) {
                "Skyscrapers givens cannot have pencil notes"
            }
        }
    }

    /**
     * Applies one absolute digit or note move.
     * Setting or clearing a digit clears that cell's pencil notes.
     */
    fun withMove(move: Move): SkyscrapersState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Skyscrapers puzzle" }
        require(puzzle.givens[move.cell.index] == null) { "Skyscrapers givens cannot be changed" }
        val updatedDigits = digits.toMutableList()
        val updatedNotes = notes.toMutableList()
        val maxNoteMask = (1 shl puzzle.size) - 1
        when (move) {
            is Move.SetDigit -> {
                require(move.digit in 0..puzzle.size) {
                    "Skyscrapers digit must be zero through ${puzzle.size}"
                }
                updatedDigits[move.cell.index] = move.digit.takeIf { it != 0 }
                updatedNotes[move.cell.index] = 0
            }

            is Move.SetNotes -> {
                require(digits[move.cell.index] == null) {
                    "Skyscrapers notes cannot be set in a filled cell"
                }
                require(move.digitMask and maxNoteMask == move.digitMask) {
                    "Skyscrapers note mask may contain only digits 1 through ${puzzle.size}"
                }
                updatedNotes[move.cell.index] = move.digitMask
            }

            is Move.SetMark -> throw IllegalArgumentException("Skyscrapers supports digits and notes only")
        }
        return SkyscrapersState(puzzle, updatedDigits, updatedNotes)
    }

    override fun equals(other: Any?): Boolean = other is SkyscrapersState &&
        puzzle == other.puzzle &&
        digits == other.digits &&
        notes == other.notes

    override fun hashCode(): Int = 31 * puzzle.hashCode() + 31 * digits.hashCode() + notes.hashCode()

    companion object {
        /** Creates the initial board state with givens populated and no notes. */
        fun initial(puzzle: SkyscrapersPuzzle): SkyscrapersState = SkyscrapersState(
            puzzle = puzzle,
            digits = puzzle.givens,
            notes = List(puzzle.grid.cellCount) { 0 },
        )
    }
}
