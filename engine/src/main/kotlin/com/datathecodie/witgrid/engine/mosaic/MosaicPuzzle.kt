package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable Mosaic puzzle clues and row-major binary solution (1 = filled, 0 = crossed).
 *
 * Each clue gives the exact number of filled cells in its 3x3 neighbourhood (the cell
 * and its 8-way adjacent neighbours). Clues can appear on any cell, including filled cells.
 */
class MosaicPuzzle(
    val id: String,
    val size: Int,
    clues: List<Int?>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Row-major clues (null for no clue, 0..9 for clue count), defensively copied. */
    val clues: List<Int?> = immutableList(clues)

    /** Complete row-major solution bits (1 = filled, 0 = empty/crossed), defensively copied. */
    val solution: List<Int> = immutableList(solution)

    init {
        require(size in 1..15) { "Mosaic puzzle size must be between 1 and 15" }
        if (size in MosaicPackBands.launchSizes) {
            require(MosaicPackBands.packForSize(size) == pack) {
                "Mosaic pack must match its board size"
            }
        }
        require(this.clues.size == grid.cellCount) { "Mosaic puzzle needs one clue slot per cell" }
        require(this.solution.size == grid.cellCount) { "Mosaic solution needs one bit per cell" }
        require(this.solution.all { it in 0..1 }) { "Mosaic solution bits must be zero or one" }
        for (i in 0 until grid.cellCount) {
            val clue = this.clues[i] ?: continue
            val areaSize = 1 + grid.neighbours(Cell(i)).size
            require(clue in 0..areaSize) {
                "Mosaic clue at cell $i must be between 0 and $areaSize but was $clue"
            }
        }
        require(isSingleLineField(id)) { "Mosaic puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Mosaic grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is MosaicPuzzle &&
        id == other.id &&
        size == other.size &&
        clues == other.clues &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int =
        listOf(id, size, clues, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Pack sizes and bands for Mosaic. */
object MosaicPackBands {
    /** Supported launch board sizes: Easy 8x8, Medium 10x10, Hard 12x12, Expert 15x15. */
    val launchSizes: Set<Int> = setOf(8, 10, 12, 15)

    /** Returns the plan-defined board size for [pack]. */
    fun sizeForPack(pack: Pack): Int = when (pack) {
        Pack.EASY -> 8
        Pack.MEDIUM -> 10
        Pack.HARD -> 12
        Pack.EXPERT -> 15
    }

    /** Returns the plan-defined pack for a launch size. */
    fun packForSize(size: Int): Pack = when (size) {
        8 -> Pack.EASY
        10 -> Pack.MEDIUM
        12 -> Pack.HARD
        15 -> Pack.EXPERT
        else -> throw IllegalArgumentException("No launch pack for Mosaic size $size")
    }
}

internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
