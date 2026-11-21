package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/** Immutable Sudoku puzzle with row-major digits and empty cells represented by null. */
class SudokuPuzzle(
    val id: String,
    givens: List<Int?>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Fixed 9 by 9 board geometry. */
    val size: Int = SUDOKU_SIZE

    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Given digits, copied to a read-only list. */
    val givens: List<Int?> = immutableDigits(givens)

    /** Stored complete solution, copied to a read-only list. */
    val solution: List<Int> = immutableDigits(solution)

    init {
        require(this.givens.size == grid.cellCount) { "Sudoku needs one given per cell" }
        require(this.solution.size == grid.cellCount) { "Sudoku solution needs one digit per cell" }
        require(this.givens.all { it == null || it in SUDOKU_DIGITS }) {
            "Sudoku givens must be empty or digits 1 through 9"
        }
        require(this.solution.all { it in SUDOKU_DIGITS }) {
            "Sudoku solution digits must be from 1 through 9"
        }
        require(isSingleLineField(id)) { "Sudoku ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Sudoku grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is SudokuPuzzle &&
        id == other.id &&
        givens == other.givens &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(id, givens, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Launch-board constants shared by the Sudoku model and rules. */
internal const val SUDOKU_SIZE = 9
internal const val SUDOKU_CELL_COUNT = SUDOKU_SIZE * SUDOKU_SIZE
internal val SUDOKU_DIGITS: IntRange = 1..9
internal const val SUDOKU_NOTE_MASK = (1 shl SUDOKU_SIZE) - 1

private fun <T> immutableDigits(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))