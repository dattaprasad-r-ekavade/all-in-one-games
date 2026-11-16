package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/** Immutable Binary puzzle with row-major givens and its row-major solution. */
class BinaryPuzzle(
    val id: String,
    val size: Int,
    givens: List<Int?>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Given symbols as 0, 1 or null for an empty cell, defensively copied. */
    val givens: List<Int?> = immutableList(givens)

    /** Complete solution symbols as 0 or 1, defensively copied. */
    val solution: List<Int> = immutableList(solution)

    init {
        require(size in supportedSizes) { "Binary size must be 6, 8, 10 or 12" }
        require(givens.size == grid.cellCount) { "Binary puzzle needs one given value per cell" }
        require(solution.size == grid.cellCount) { "Binary solution needs one value per cell" }
        require(givens.all { it == null || it in 0..1 }) { "Binary givens must be zero, one or empty" }
        require(solution.all { it in 0..1 }) { "Binary solution values must be zero or one" }
        require(isSingleLineField(id)) { "Binary puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Binary grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is BinaryPuzzle &&
        id == other.id &&
        size == other.size &&
        givens == other.givens &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(id, size, givens, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }

    companion object {
        /** Board sizes supported by the four Binary packs. */
        internal val supportedSizes: Set<Int> = setOf(6, 8, 10, 12)
    }
}

/** Lane-owned copy that prevents callers from changing a puzzle or state through a mutable cast. */
internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))

/** Maps the plan's board sizes to their packs. */
object BinaryPackBands {
    /** Returns the pack for a supported Binary board size. */
    fun packForSize(size: Int): Pack = when (size) {
        6 -> Pack.EASY
        8 -> Pack.MEDIUM
        10 -> Pack.HARD
        12 -> Pack.EXPERT
        else -> throw IllegalArgumentException("Binary size must be 6, 8, 10 or 12")
    }
}
