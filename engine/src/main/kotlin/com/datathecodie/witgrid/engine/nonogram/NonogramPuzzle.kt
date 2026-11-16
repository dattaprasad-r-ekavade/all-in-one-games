package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/** Immutable Nonogram clues and row-major solution bits, where one means a filled cell. */
class NonogramPuzzle(
    val id: String,
    val size: Int,
    rowClues: List<List<Int>>,
    columnClues: List<List<Int>>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Clue run lengths for each row, copied to read-only lists. */
    val rowClues: List<List<Int>> = immutableClues(rowClues)

    /** Clue run lengths for each column, copied to read-only lists. */
    val columnClues: List<List<Int>> = immutableClues(columnClues)

    /** Complete row-major solution bits, defensively copied. */
    val solution: List<Int> = immutableList(solution)

    init {
        require(size in supportedSizes) { "Nonogram size must be 5, 10 or 15" }
        require(NonogramPackBands.sizeForPack(pack) == size) {
            "Nonogram pack must match its board size"
        }
        require(this.rowClues.size == size) { "Nonogram needs one clue list per row" }
        require(this.columnClues.size == size) { "Nonogram needs one clue list per column" }
        require(this.rowClues.all { it.fits(size) }) { "Nonogram row clues do not fit the board" }
        require(this.columnClues.all { it.fits(size) }) { "Nonogram column clues do not fit the board" }
        require(this.solution.size == grid.cellCount) { "Nonogram solution needs one bit per cell" }
        require(this.solution.all { it in 0..1 }) { "Nonogram solution bits must be zero or one" }
        require(isSingleLineField(id)) { "Nonogram puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Nonogram grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is NonogramPuzzle &&
        id == other.id &&
        size == other.size &&
        rowClues == other.rowClues &&
        columnClues == other.columnClues &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int =
        listOf(id, size, rowClues, columnClues, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }

    companion object {
        /** Supported launch board sizes. */
        internal val supportedSizes: Set<Int> = setOf(5, 10, 15)
    }
}

/** Pack sizes from the Nonogram plan; Medium and Hard both use 10 by 10. */
object NonogramPackBands {
    /** Returns the plan-defined board size for [pack]. */
    fun sizeForPack(pack: Pack): Int = when (pack) {
        Pack.EASY -> 5
        Pack.MEDIUM, Pack.HARD -> 10
        Pack.EXPERT -> 15
    }

    /**
     * Returns whether a grade fits the launch band. Easy uses Overlap in at most two passes;
     * Medium allows Edge rules in at most four; Hard needs Line by line in at least two passes;
     * Expert needs Line by line in at least four. Hard and Expert size bands distinguish the two.
     */
    fun isGradeInPackBand(pack: Pack, grade: Grade): Boolean {
        if (grade.pack != pack) return false
        val hardestRank = grade.hardestTechnique?.difficultyRank ?: 0
        val linePasses = grade.stepCount
        return when (pack) {
            Pack.EASY -> hardestRank == 0 && linePasses in 1..2
            Pack.MEDIUM -> hardestRank in 0..1 && linePasses in 1..4
            Pack.HARD -> hardestRank == 2 && linePasses >= 2
            Pack.EXPERT -> hardestRank == 2 && linePasses >= 4
        }
    }
}

private fun List<Int>.fits(lineSize: Int): Boolean =
    all { it > 0 } && sumOf { it.toLong() } + maxOf(0, size - 1) <= lineSize

private fun immutableClues(values: List<List<Int>>): List<List<Int>> =
    Collections.unmodifiableList(values.map(::immutableList))

private fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
