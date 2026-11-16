package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Move
import java.util.ArrayList
import java.util.Collections

/** Immutable Mini Sudoku digits and pencil notes tied to one puzzle. */
class MiniSudokuState(
    val puzzle: MiniSudokuPuzzle,
    digits: List<Int?>,
    notes: List<Int>,
) {
    /** Player digits, with null representing an empty cell. */
    val digits: List<Int?> = immutableList(digits)

    /** Per-cell note masks, where bit `digit - 1` represents digits 1 through 6. */
    val notes: List<Int> = immutableList(notes)

    init {
        require(this.digits.size == puzzle.grid.cellCount) { "Mini Sudoku state needs one digit per cell" }
        require(this.notes.size == puzzle.grid.cellCount) { "Mini Sudoku state needs one note mask per cell" }
        require(this.digits.all { it == null || it in MINI_SUDOKU_DIGITS }) {
            "Mini Sudoku state digits must be empty or between 1 and 6"
        }
        require(this.notes.all { it in 0..MINI_SUDOKU_NOTE_MASK }) {
            "Mini Sudoku note mask may contain only digits 1 through 6"
        }
        require(this.digits.indices.all { index -> this.digits[index] == null || this.notes[index] == 0 }) {
            "A digit value cannot keep pencil notes in its cell"
        }
        puzzle.givens.forEachIndexed { index, given ->
            require(given == null || this.digits[index] == given) {
                "Mini Sudoku state must retain every given"
            }
            require(given == null || this.notes[index] == 0) {
                "Mini Sudoku givens cannot have pencil notes"
            }
        }
    }

    /** Applies one absolute digit or note edit. Entering or clearing a digit clears that cell's notes. */
    fun withMove(move: Move): MiniSudokuState {
        require(move.cell.index < puzzle.grid.cellCount) { "Move cell outside Mini Sudoku puzzle" }
        require(puzzle.givens[move.cell.index] == null) { "Mini Sudoku givens cannot be changed" }
        val updatedDigits = digits.toMutableList()
        val updatedNotes = notes.toMutableList()
        when (move) {
            is Move.SetDigit -> {
                require(move.digit in 0..MINI_SUDOKU_SIZE) { "Mini Sudoku digit must be zero through 6" }
                updatedDigits[move.cell.index] = move.digit.takeIf { it != 0 }
                updatedNotes[move.cell.index] = 0
            }

            is Move.SetNotes -> {
                require(digits[move.cell.index] == null) {
                    "Mini Sudoku notes cannot be set in a filled cell"
                }
                require(move.digitMask and MINI_SUDOKU_NOTE_MASK == move.digitMask) {
                    "Mini Sudoku note mask may contain only digits 1 through 6"
                }
                updatedNotes[move.cell.index] = move.digitMask
            }

            is Move.SetMark -> throw IllegalArgumentException("Mini Sudoku supports digits and notes only")
        }
        return MiniSudokuState(puzzle, updatedDigits, updatedNotes)
    }

    override fun equals(other: Any?): Boolean = other is MiniSudokuState &&
        puzzle == other.puzzle && digits == other.digits && notes == other.notes

    override fun hashCode(): Int = 31 * puzzle.hashCode() + 31 * digits.hashCode() + notes.hashCode()

    companion object {
        /** Creates the initial board with puzzle givens entered and no notes. */
        fun initial(puzzle: MiniSudokuPuzzle): MiniSudokuState = MiniSudokuState(
            puzzle = puzzle,
            digits = puzzle.givens,
            notes = List(puzzle.grid.cellCount) { 0 },
        )
    }
}

private fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
