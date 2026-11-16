package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/** Immutable Mini Sudoku puzzle with row-major digits and empty cells represented by null. */
class MiniSudokuPuzzle(
    val id: String,
    givens: List<Int?>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Fixed 6 by 6 board geometry. */
    val size: Int = MINI_SUDOKU_SIZE

    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Given digits, copied to a read-only list. */
    val givens: List<Int?> = immutableDigits(givens)

    /** Stored complete solution, copied to a read-only list. */
    val solution: List<Int> = immutableDigits(solution)

    init {
        require(this.givens.size == grid.cellCount) { "Mini Sudoku needs one given per cell" }
        require(this.solution.size == grid.cellCount) { "Mini Sudoku solution needs one digit per cell" }
        require(this.givens.all { it == null || it in MINI_SUDOKU_DIGITS }) {
            "Mini Sudoku givens must be empty or digits 1 through 6"
        }
        require(this.solution.all { it in MINI_SUDOKU_DIGITS }) {
            "Mini Sudoku solution digits must be from 1 through 6"
        }
        require(isSingleLineField(id)) { "Mini Sudoku ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Mini Sudoku grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is MiniSudokuPuzzle &&
        id == other.id &&
        givens == other.givens &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(id, givens, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Launch-board constants shared by the Mini Sudoku model and rules. */
internal const val MINI_SUDOKU_SIZE = 6
internal const val MINI_SUDOKU_CELL_COUNT = MINI_SUDOKU_SIZE * MINI_SUDOKU_SIZE
internal val MINI_SUDOKU_DIGITS: IntRange = 1..6
internal const val MINI_SUDOKU_NOTE_MASK = (1 shl MINI_SUDOKU_SIZE) - 1

private fun <T> immutableDigits(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
