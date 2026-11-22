package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/** Inequality relation between adjacent cells. */
enum class FutoshikiSign(val horizontalSymbol: Char, val verticalSymbol: Char) {
    NONE('-', '-'),
    LESS('<', '^'),
    GREATER('>', 'v');

    companion object {
        fun fromHorizontalChar(char: Char): FutoshikiSign = when (char) {
            '-', '.' -> NONE
            '<' -> LESS
            '>' -> GREATER
            else -> throw IllegalArgumentException("Invalid horizontal sign: '$char'")
        }

        fun fromVerticalChar(char: Char): FutoshikiSign = when (char) {
            '-', '.' -> NONE
            '^', '<' -> LESS
            'v', 'V', '>' -> GREATER
            else -> throw IllegalArgumentException("Invalid vertical sign: '$char'")
        }
    }
}

/** Directed inequality constraint asserting that [lesser] < [greater]. */
data class FutoshikiInequality(
    val lesser: Cell,
    val greater: Cell,
)

/** Immutable Futoshiki puzzle with givens, inequality signs and row-major solution digits. */
class FutoshikiPuzzle(
    val id: String,
    val size: Int,
    givens: List<Int?>,
    horizontalSigns: List<FutoshikiSign>,
    verticalSigns: List<FutoshikiSign>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Given digits, copied to a read-only list. Empty cells are null. */
    val givens: List<Int?> = immutableList(givens)

    /** Row-major horizontal signs between cell (r, c) and (r, c+1), length size * (size - 1). */
    val horizontalSigns: List<FutoshikiSign> = immutableList(horizontalSigns)

    /** Row-major vertical signs between cell (r, c) and (r+1, c), length (size - 1) * size. */
    val verticalSigns: List<FutoshikiSign> = immutableList(verticalSigns)

    /** Stored complete solution, copied to a read-only list. */
    val solution: List<Int> = immutableList(solution)

    /** Derived list of all pairwise inequalities present in the puzzle. */
    val inequalities: List<FutoshikiInequality>

    /** Total number of inequality signs present on the board. */
    val signCount: Int get() = inequalities.size

    init {
        require(size in 2..9) { "Futoshiki puzzle size must be between 2 and 9" }
        require(this.givens.size == grid.cellCount) { "Futoshiki puzzle needs one given per cell" }
        require(this.horizontalSigns.size == size * (size - 1)) {
            "Futoshiki puzzle needs one horizontal sign per adjacent cell pair in each row"
        }
        require(this.verticalSigns.size == (size - 1) * size) {
            "Futoshiki puzzle needs one vertical sign per adjacent cell pair in each column"
        }
        require(this.solution.size == grid.cellCount) { "Futoshiki solution needs one digit per cell" }
        require(this.givens.all { it == null || it in 1..size }) {
            "Futoshiki givens must be empty or digits 1 through $size"
        }
        require(this.solution.all { it in 1..size }) {
            "Futoshiki solution digits must be from 1 through $size"
        }
        require(isSingleLineField(id)) { "Futoshiki puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Futoshiki grade must be non-blank single-line text" }

        val ineqList = mutableListOf<FutoshikiInequality>()
        for (r in 0 until size) {
            for (c in 0 until size - 1) {
                val sign = this.horizontalSigns[r * (size - 1) + c]
                val left = Cell(r * size + c)
                val right = Cell(r * size + c + 1)
                when (sign) {
                    FutoshikiSign.LESS -> ineqList.add(FutoshikiInequality(lesser = left, greater = right))
                    FutoshikiSign.GREATER -> ineqList.add(FutoshikiInequality(lesser = right, greater = left))
                    FutoshikiSign.NONE -> {}
                }
            }
        }
        for (r in 0 until size - 1) {
            for (c in 0 until size) {
                val sign = this.verticalSigns[r * size + c]
                val top = Cell(r * size + c)
                val bottom = Cell((r + 1) * size + c)
                when (sign) {
                    FutoshikiSign.LESS -> ineqList.add(FutoshikiInequality(lesser = top, greater = bottom))
                    FutoshikiSign.GREATER -> ineqList.add(FutoshikiInequality(lesser = bottom, greater = top))
                    FutoshikiSign.NONE -> {}
                }
            }
        }
        inequalities = Collections.unmodifiableList(ineqList)
    }

    /** Returns the sign between cell (row, column) and cell (row, column + 1). */
    fun horizontalSign(row: Int, column: Int): FutoshikiSign {
        require(row in 0 until size && column in 0 until size - 1) {
            "Horizontal sign coordinates out of bounds: ($row, $column)"
        }
        return horizontalSigns[row * (size - 1) + column]
    }

    /** Returns the sign between cell (row, column) and cell (row + 1, column). */
    fun verticalSign(row: Int, column: Int): FutoshikiSign {
        require(row in 0 until size - 1 && column in 0 until size) {
            "Vertical sign coordinates out of bounds: ($row, $column)"
        }
        return verticalSigns[row * size + column]
    }

    override fun equals(other: Any?): Boolean = other is FutoshikiPuzzle &&
        id == other.id &&
        size == other.size &&
        givens == other.givens &&
        horizontalSigns == other.horizontalSigns &&
        verticalSigns == other.verticalSigns &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int =
        listOf(id, size, givens, horizontalSigns, verticalSigns, solution, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Supported launch board sizes and pack mappings for Futoshiki. */
object FutoshikiPackBands {
    const val MIN_HARD_SIGNS = 10
    const val MAX_EXPERT_SIGNS = 8

    val supportedSizes: Set<Int> = setOf(5, 6, 7)

    fun sizeForPack(pack: Pack): Int = when (pack) {
        Pack.EASY -> 5
        Pack.MEDIUM -> 6
        Pack.HARD, Pack.EXPERT -> 7
    }

    /**
     * Returns whether [grade] uses the technique level assigned to [pack].
     * Easy uses techniques up to sign bounds; Medium up to sign chains;
     * Hard requires sign chains; Expert requires naked pairs.
     */
    fun isGradeInPackBand(pack: Pack, grade: Grade): Boolean {
        if (grade.pack != pack || grade.stepCount == 0) return false
        val hardestRank = grade.hardestTechnique?.difficultyRank ?: return false
        return when (pack) {
            Pack.EASY -> hardestRank <= FutoshikiTechniques.SIGN_BOUNDS.difficultyRank
            Pack.MEDIUM -> hardestRank <= FutoshikiTechniques.SIGN_CHAINS.difficultyRank
            Pack.HARD -> hardestRank == FutoshikiTechniques.SIGN_CHAINS.difficultyRank
            Pack.EXPERT -> hardestRank == FutoshikiTechniques.NAKED_PAIR.difficultyRank
        }
    }

    /**
     * Returns whether [signCount] satisfies the pack sign constraints.
     * Plan §12.8 specifies "Hard 7x7, Expert 7x7 with fewer signs".
     */
    fun isSignCountInPackBand(pack: Pack, signCount: Int): Boolean = when (pack) {
        Pack.EXPERT -> signCount <= MAX_EXPERT_SIGNS
        Pack.HARD -> signCount >= MIN_HARD_SIGNS
        else -> true
    }
}

internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
