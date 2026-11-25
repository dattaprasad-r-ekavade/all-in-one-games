package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable Killer Sudoku cage with sorted row-major [cells] and a positive target [sum].
 * Sorting [cells] keeps the top-left-most cell first and preserves canonical puzzle equality.
 */
class KillerCage(
    cells: List<Cell>,
    val sum: Int,
) {
    /** Distinct cells belonging to this cage, sorted in row-major order. */
    val cells: List<Cell>

    init {
        require(cells.isNotEmpty() && cells.size <= KILLER_SUDOKU_SIZE) {
            "Killer Sudoku cage must contain between 1 and 9 cells"
        }
        require(cells.all { it.index < KILLER_SUDOKU_CELL_COUNT }) {
            "Killer Sudoku cage cell outside 9x9 grid"
        }
        require(cells.distinct().size == cells.size) {
            "Killer Sudoku cage cannot contain duplicate cells"
        }
        require(sum > 0) { "Killer Sudoku cage sum must be positive" }
        this.cells = immutableList(cells.sortedBy { it.index })
    }

    override fun equals(other: Any?): Boolean = other is KillerCage &&
        cells == other.cells &&
        sum == other.sum

    override fun hashCode(): Int = 31 * cells.hashCode() + sum

    override fun toString(): String =
        "KillerCage(sum=$sum, cells=${cells.joinToString(prefix = "[", postfix = "]") { it.index.toString() }})"
}

/**
 * Immutable Killer Sudoku puzzle with optional row-major givens, partitioning [cages],
 * and a stored 9x9 solution.
 */
class KillerSudokuPuzzle(
    val id: String,
    givens: List<Int?>,
    cages: List<KillerCage>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Fixed 9 by 9 board geometry. */
    val size: Int = KILLER_SUDOKU_SIZE

    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Given digits, copied to a read-only list with null representing an empty cell. */
    val givens: List<Int?> = immutableList(givens)

    /** Cages partitioning every cell in the 9x9 grid, sorted by each cage's first cell index. */
    val cages: List<KillerCage> = immutableList(cages.sortedBy { it.cells.first().index })

    /** Stored complete solution, copied to a read-only list. */
    val solution: List<Int> = immutableList(solution)

    /** Row-major mapping from each cell index to its owning index in [cages]. */
    val cageIndexByCell: List<Int>

    init {
        require(this.givens.size == grid.cellCount) { "Killer Sudoku needs one given per cell" }
        require(this.solution.size == grid.cellCount) { "Killer Sudoku solution needs one digit per cell" }
        require(this.givens.all { it == null || it in KILLER_SUDOKU_DIGITS }) {
            "Killer Sudoku givens must be empty or digits 1 through 9"
        }
        require(this.solution.all { it in KILLER_SUDOKU_DIGITS }) {
            "Killer Sudoku solution digits must be from 1 through 9"
        }
        require(this.cages.isNotEmpty()) { "Killer Sudoku puzzle must define cages" }
        require(isSingleLineField(id)) { "Killer Sudoku ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Killer Sudoku grade must be non-blank single-line text" }

        val cellToCage = IntArray(grid.cellCount) { -1 }
        this.cages.forEachIndexed { cageIndex, cage ->
            for (cell in cage.cells) {
                require(cellToCage[cell.index] == -1) {
                    "Cell ${cell.index} appears in multiple Killer Sudoku cages"
                }
                cellToCage[cell.index] = cageIndex
            }
        }
        require(cellToCage.all { it >= 0 }) {
            "Killer Sudoku cages must cover every cell in the 9x9 grid"
        }
        cageIndexByCell = immutableList(cellToCage.toList())
    }

    /** Returns the index in [cages] containing [cell]. */
    fun cageIndexOf(cell: Cell): Int {
        require(cell.index < grid.cellCount) { "Cell outside Killer Sudoku puzzle" }
        return cageIndexByCell[cell.index]
    }

    /** Returns the [KillerCage] containing [cell]. */
    fun cageOf(cell: Cell): KillerCage = cages[cageIndexOf(cell)]

    override fun equals(other: Any?): Boolean = other is KillerSudokuPuzzle &&
        id == other.id &&
        givens == other.givens &&
        cages == other.cages &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(id, givens, cages, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

internal const val KILLER_SUDOKU_SIZE = 9
internal const val KILLER_SUDOKU_CELL_COUNT = KILLER_SUDOKU_SIZE * KILLER_SUDOKU_SIZE
internal val KILLER_SUDOKU_DIGITS: IntRange = 1..9
internal const val KILLER_SUDOKU_NOTE_MASK = (1 shl KILLER_SUDOKU_SIZE) - 1
internal const val KILLER_SUDOKU_MAX_CAGE_SUM = 45

internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
