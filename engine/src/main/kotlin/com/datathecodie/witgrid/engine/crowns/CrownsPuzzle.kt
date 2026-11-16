package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/** Immutable Crowns puzzle data; [solutionColumns] stores one zero-based column per row. */
class CrownsPuzzle(
    val id: String,
    val size: Int,
    regions: List<Int>,
    solutionColumns: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Row-major region IDs, copied so callers cannot mutate the puzzle after construction. */
    val regions: List<Int> = immutableList(regions)

    /** Stored solution column for each row, copied so callers cannot mutate the puzzle. */
    val solutionColumns: List<Int> = immutableList(solutionColumns)

    init {
        require(size in 1..10) { "Crowns puzzle size must be between 1 and 10" }
        require(regions.size == grid.cellCount) { "Crowns puzzle needs one region ID per cell" }
        require(regions.all { it in 0 until size }) { "Crowns region ID outside the puzzle" }
        require(regions.toSet() == (0 until size).toSet()) { "Crowns puzzle needs every region ID" }
        require(solutionColumns.size == size) { "Crowns solution needs one column per row" }
        require(solutionColumns.all { it in 0 until size }) { "Crowns solution column outside the puzzle" }
        require(isSingleLineField(id)) { "Crowns puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Crowns grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is CrownsPuzzle &&
        id == other.id &&
        size == other.size &&
        regions == other.regions &&
        solutionColumns == other.solutionColumns &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(id, size, regions, solutionColumns, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Returns a defensive read-only copy for engine puzzle and state data. */
internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
