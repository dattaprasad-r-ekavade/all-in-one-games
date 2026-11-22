package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable Skyscrapers puzzle model representing an N by N Latin square grid
 * constrained by visibility clues outside the grid and optional cell givens.
 */
class SkyscrapersPuzzle(
    val id: String,
    val size: Int,
    topClues: List<Int?>,
    bottomClues: List<Int?>,
    leftClues: List<Int?>,
    rightClues: List<Int?>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
    givens: List<Int?> = Collections.nCopies(size * size, null),
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Outside visibility clues along the top edge, looking down each column. */
    val topClues: List<Int?> = immutableList(topClues)

    /** Outside visibility clues along the bottom edge, looking up each column. */
    val bottomClues: List<Int?> = immutableList(bottomClues)

    /** Outside visibility clues along the left edge, looking right across each row. */
    val leftClues: List<Int?> = immutableList(leftClues)

    /** Outside visibility clues along the right edge, looking left across each row. */
    val rightClues: List<Int?> = immutableList(rightClues)

    /** Optional pre-filled building heights in row-major order, with null representing empty cells. */
    val givens: List<Int?> = immutableList(givens)

    /** Stored complete solution in row-major order. */
    val solution: List<Int> = immutableList(solution)

    init {
        require(size in SKYSCRAPERS_SUPPORTED_SIZES) {
            "Skyscrapers board size must be between 4 and 7"
        }
        require(this.topClues.size == size) { "Skyscrapers needs $size top clues" }
        require(this.bottomClues.size == size) { "Skyscrapers needs $size bottom clues" }
        require(this.leftClues.size == size) { "Skyscrapers needs $size left clues" }
        require(this.rightClues.size == size) { "Skyscrapers needs $size right clues" }
        require(this.givens.size == grid.cellCount) { "Skyscrapers needs one given per cell" }
        require(this.solution.size == grid.cellCount) { "Skyscrapers solution needs one digit per cell" }

        val validDigits = 1..size
        require(this.topClues.all { it == null || it in validDigits }) {
            "Top clues must be empty or between 1 and $size"
        }
        require(this.bottomClues.all { it == null || it in validDigits }) {
            "Bottom clues must be empty or between 1 and $size"
        }
        require(this.leftClues.all { it == null || it in validDigits }) {
            "Left clues must be empty or between 1 and $size"
        }
        require(this.rightClues.all { it == null || it in validDigits }) {
            "Right clues must be empty or between 1 and $size"
        }
        require(this.givens.all { it == null || it in validDigits }) {
            "Skyscrapers givens must be empty or between 1 and $size"
        }
        require(this.solution.all { it in validDigits }) {
            "Skyscrapers solution digits must be between 1 and $size"
        }
        require(pack == SkyscrapersPackBands.packForSize(size)) {
            "Skyscrapers pack must match its board size"
        }
        require(isSingleLineField(id)) { "Skyscrapers ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Skyscrapers grade must be non-blank single-line text" }
    }

    override fun equals(other: Any?): Boolean = other is SkyscrapersPuzzle &&
        id == other.id &&
        size == other.size &&
        topClues == other.topClues &&
        bottomClues == other.bottomClues &&
        leftClues == other.leftClues &&
        rightClues == other.rightClues &&
        givens == other.givens &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(
        id,
        size,
        topClues,
        bottomClues,
        leftClues,
        rightClues,
        givens,
        solution,
        pack,
        storedGrade,
    ).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Board sizes supported by the four Skyscrapers packs (Easy 4x4, Medium 5x5, Hard 6x6, Expert 7x7). */
internal val SKYSCRAPERS_SUPPORTED_SIZES: Set<Int> = setOf(4, 5, 6, 7)

/** The plan defines bands by board size; every supported technique is allowed in each band. */
object SkyscrapersPackBands {
    /** Returns the pack for a supported Skyscrapers board size. */
    fun packForSize(size: Int): Pack = when (size) {
        4 -> Pack.EASY
        5 -> Pack.MEDIUM
        6 -> Pack.HARD
        7 -> Pack.EXPERT
        else -> throw IllegalArgumentException("Skyscrapers size must be 4, 5, 6 or 7")
    }

    /** Returns the board size for a Skyscrapers pack. */
    fun sizeForPack(pack: Pack): Int = when (pack) {
        Pack.EASY -> 4
        Pack.MEDIUM -> 5
        Pack.HARD -> 6
        Pack.EXPERT -> 7
    }

    /** Compares a grade's size-derived pack with the requested band. */
    fun isGradeInPackBand(pack: Pack, grade: Grade): Boolean =
        grade.pack == pack
}

/** Defensive read-only list copy preventing external mutation. */
internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
