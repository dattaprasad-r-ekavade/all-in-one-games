package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Move

/** Immutable Killer Sudoku digits and pencil notes tied to one puzzle. */
class KillerSudokuState(
    val puzzle: KillerSudokuPuzzle,
    digits: List<Int?>,
    notes: List<Int>,
) {
    /** Player digits, with null representing an empty cell. */
    val digits: List<Int?> = immutableList(digits)

    /** Per-cell note masks, where bit `digit - 1` represents digits 1 through 9. */
    val notes: List<Int> = immutableList(notes)

    init {
        require(this.digits.size == puzzle.grid.cellCount) {
            "Killer Sudoku state needs one digit per cell"
        }
        require(this.notes.size == puzzle.grid.cellCount) {
            "Killer Sudoku state needs one note mask per cell"
        }
        require(this.digits.all { it == null || it in KILLER_SUDOKU_DIGITS }) {
            "Killer Sudoku state digits must be empty or between 1 and 9"
        }
        require(this.notes.all { it in 0..KILLER_SUDOKU_NOTE_MASK }) {
            "Killer Sudoku note mask may contain only digits 1 through 9"
        }
        require(this.digits.indices.all { index -> this.digits[index] == null || this.notes[index] == 0 }) {
            "A digit value cannot keep pencil notes in its cell"
        }
        puzzle.givens.forEachIndexed { index, given ->
            require(given == null || this.digits[index] == given) {
                "Killer Sudoku state must retain every given"
            }
            require(given == null || this.notes[index] == 0) {
                "Killer Sudoku givens cannot have pencil notes"
            }
        }
    }

    /** Applies one absolute digit or note edit. Entering or clearing a digit clears that cell's notes. */
    fun withMove(move: Move): KillerSudokuState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Killer Sudoku puzzle" }
        require(puzzle.givens[move.cell.index] == null) { "Killer Sudoku givens cannot be changed" }
        val updatedDigits = digits.toMutableList()
        val updatedNotes = notes.toMutableList()
        when (move) {
            is Move.SetDigit -> {
                require(move.digit in 0..KILLER_SUDOKU_SIZE) {
                    "Killer Sudoku digit must be zero through 9"
                }
                updatedDigits[move.cell.index] = move.digit.takeIf { it != 0 }
                updatedNotes[move.cell.index] = 0
            }

            is Move.SetNotes -> {
                require(digits[move.cell.index] == null) {
                    "Killer Sudoku notes cannot be set in a filled cell"
                }
                require(move.digitMask and KILLER_SUDOKU_NOTE_MASK == move.digitMask) {
                    "Killer Sudoku note mask may contain only digits 1 through 9"
                }
                updatedNotes[move.cell.index] = move.digitMask
            }

            is Move.SetMark -> {
                throw IllegalArgumentException("Killer Sudoku supports digits and notes only")
            }
        }
        return KillerSudokuState(puzzle, updatedDigits, updatedNotes)
    }

    override fun equals(other: Any?): Boolean = other is KillerSudokuState &&
        puzzle == other.puzzle && digits == other.digits && notes == other.notes

    override fun hashCode(): Int = 31 * puzzle.hashCode() + 31 * digits.hashCode() + notes.hashCode()

    companion object {
        /** Creates the initial board with puzzle givens entered and no notes. */
        fun initial(puzzle: KillerSudokuPuzzle): KillerSudokuState = KillerSudokuState(
            puzzle = puzzle,
            digits = puzzle.givens,
            notes = List(puzzle.grid.cellCount) { 0 },
        )
    }
}
