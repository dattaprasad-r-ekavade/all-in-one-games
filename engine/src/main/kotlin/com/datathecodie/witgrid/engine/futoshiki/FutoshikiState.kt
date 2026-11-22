package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Move

/** Immutable Futoshiki digits and pencil notes tied to one puzzle. */
class FutoshikiState(
    val puzzle: FutoshikiPuzzle,
    digits: List<Int?>,
    notes: List<Int>,
) {
    /** Player digits, with null representing an empty cell. */
    val digits: List<Int?> = immutableList(digits)

    /** Per-cell note masks, where bit `digit - 1` represents digits 1 through [puzzle.size]. */
    val notes: List<Int> = immutableList(notes)

    private val noteMask: Int = (1 shl puzzle.size) - 1

    init {
        require(this.digits.size == puzzle.grid.cellCount) { "Futoshiki state needs one digit per cell" }
        require(this.notes.size == puzzle.grid.cellCount) { "Futoshiki state needs one note mask per cell" }
        require(this.digits.all { it == null || it in 1..puzzle.size }) {
            "Futoshiki state digits must be empty or between 1 and ${puzzle.size}"
        }
        require(this.notes.all { it in 0..noteMask }) {
            "Futoshiki note mask may contain only digits 1 through ${puzzle.size}"
        }
        require(this.digits.indices.all { index -> this.digits[index] == null || this.notes[index] == 0 }) {
            "A digit value cannot keep pencil notes in its cell"
        }
        puzzle.givens.forEachIndexed { index, given ->
            require(given == null || this.digits[index] == given) {
                "Futoshiki state must retain every given"
            }
            require(given == null || this.notes[index] == 0) {
                "Futoshiki givens cannot have pencil notes"
            }
        }
    }

    /** Applies one absolute digit or note edit. Entering or clearing a digit clears that cell's notes. */
    fun withMove(move: Move): FutoshikiState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Futoshiki puzzle" }
        require(puzzle.givens[move.cell.index] == null) { "Futoshiki givens cannot be changed" }
        val updatedDigits = digits.toMutableList()
        val updatedNotes = notes.toMutableList()
        when (move) {
            is Move.SetDigit -> {
                require(move.digit in 0..puzzle.size) {
                    "Futoshiki digit must be zero through ${puzzle.size}"
                }
                updatedDigits[move.cell.index] = move.digit.takeIf { it != 0 }
                updatedNotes[move.cell.index] = 0
            }

            is Move.SetNotes -> {
                require(digits[move.cell.index] == null) {
                    "Futoshiki notes cannot be set in a filled cell"
                }
                require(move.digitMask and noteMask == move.digitMask) {
                    "Futoshiki note mask may contain only digits 1 through ${puzzle.size}"
                }
                updatedNotes[move.cell.index] = move.digitMask
            }

            is Move.SetMark -> throw IllegalArgumentException("Futoshiki supports digits and notes only")
        }
        return FutoshikiState(puzzle, updatedDigits, updatedNotes)
    }

    override fun equals(other: Any?): Boolean = other is FutoshikiState &&
        puzzle == other.puzzle && digits == other.digits && notes == other.notes

    override fun hashCode(): Int = 31 * puzzle.hashCode() + 31 * digits.hashCode() + notes.hashCode()

    companion object {
        /** Creates the initial board with puzzle givens entered and no notes. */
        fun initial(puzzle: FutoshikiPuzzle): FutoshikiState = FutoshikiState(
            puzzle = puzzle,
            digits = puzzle.givens,
            notes = List(puzzle.grid.cellCount) { 0 },
        )
    }
}
