package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable Path puzzle data model.
 *
 * Each pair of equal numbers (1..[pairCount]) has exactly two endpoint cells.
 * Non-endpoint cells are either empty or permanent walls that paths cannot cross.
 * In a solved puzzle, all non-wall cells are filled by non-branching, non-crossing paths.
 */
class PathPuzzle(
    val id: String,
    val size: Int,
    endpoints: List<Int?>,
    walls: List<Boolean>,
    solution: List<Int>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Square board geometry. */
    val grid: Grid = Grid(size)

    /** Endpoint pair IDs (1..[pairCount]) or null if cell is not an endpoint. */
    val endpoints: List<Int?> = immutableList(endpoints)

    /** Wall flags indicating cells that cannot be entered or edited. */
    val walls: List<Boolean> = immutableList(walls)

    /** Stored solution where wall cells hold 0 and non-wall cells hold their pair color (1..[pairCount]). */
    val solution: List<Int> = immutableList(solution)

    /** Total number of endpoint pairs. Derived from the maximum endpoint digit. */
    val pairCount: Int = endpoints.filterNotNull().maxOrNull() ?: 0

    init {
        require(size in PATH_SUPPORTED_SIZES) {
            "Path puzzle size must be one of $PATH_SUPPORTED_SIZES, got: $size"
        }
        require(this.endpoints.size == grid.cellCount) {
            "Path puzzle needs one endpoint entry per cell"
        }
        require(this.walls.size == grid.cellCount) {
            "Path puzzle needs one wall flag per cell"
        }
        require(this.solution.size == grid.cellCount) {
            "Path puzzle needs one solution digit per cell"
        }
        require(pairCount in 1..MAX_PAIR_COUNT) {
            "Path puzzle must have between 1 and $MAX_PAIR_COUNT pairs, got: $pairCount"
        }

        for (index in 0 until grid.cellCount) {
            val endpoint = this.endpoints[index]
            val isWall = this.walls[index]
            require(!(isWall && endpoint != null)) {
                "Cell $index cannot be both a wall and an endpoint"
            }
            if (isWall) {
                require(this.solution[index] == 0) {
                    "Wall cell $index must have solution value 0"
                }
            } else {
                val solVal = this.solution[index]
                require(solVal in 1..pairCount) {
                    "Solution at non-wall cell $index must be in 1..$pairCount, got: $solVal"
                }
                if (endpoint != null) {
                    require(endpoint == solVal) {
                        "Endpoint at cell $index ($endpoint) must match solution value ($solVal)"
                    }
                }
            }
        }

        // Each pair 1..pairCount must have exactly two endpoints
        for (pair in 1..pairCount) {
            val count = this.endpoints.count { it == pair }
            require(count == 2) {
                "Pair $pair must have exactly 2 endpoints, found: $count"
            }
        }

        require(isSingleLineField(id)) { "Path puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Path grade must be non-blank single-line text" }
    }

    /** Returns whether the cell at [cell] is a wall. */
    fun isWall(cell: Cell): Boolean = walls[cell.index]

    /** Returns whether the cell at [index] is a wall. */
    fun isWall(index: Int): Boolean = walls[index]

    /** Returns the endpoint pair number at [cell], or null if not an endpoint. */
    fun endpointAt(cell: Cell): Int? = endpoints[cell.index]

    /** Returns the endpoint pair number at [index], or null if not an endpoint. */
    fun endpointAt(index: Int): Int? = endpoints[index]

    override fun equals(other: Any?): Boolean = other is PathPuzzle &&
        id == other.id &&
        size == other.size &&
        endpoints == other.endpoints &&
        walls == other.walls &&
        solution == other.solution &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(
        id,
        size,
        endpoints,
        walls,
        solution,
        pack,
        storedGrade,
    ).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

/** Board sizes supported by Path puzzles. */
internal val PATH_SUPPORTED_SIZES: Set<Int> = setOf(4, 5, 6, 8, 10, 12)

/** Maximum number of pairs representable by alphanumeric digits (1..9, A..Z). */
internal const val MAX_PAIR_COUNT = 35

/** Pack mappings for standard Path board sizes. */
object PathPackBands {
    fun packForSize(size: Int): Pack = when (size) {
        4, 5, 6 -> Pack.EASY
        8 -> Pack.MEDIUM
        10 -> Pack.HARD
        12 -> Pack.EXPERT
        else -> throw IllegalArgumentException("Unsupported Path size: $size")
    }

    fun sizeForPack(pack: Pack): Int = when (pack) {
        Pack.EASY -> 6
        Pack.MEDIUM -> 8
        Pack.HARD -> 10
        Pack.EXPERT -> 12
    }
}

/** Converts a pair digit or letter char to pair index (1..35). */
internal fun charToPair(char: Char): Int = when (char) {
    in '1'..'9' -> char - '0'
    in 'A'..'Z' -> char - 'A' + 10
    in 'a'..'z' -> char - 'a' + 10
    else -> throw IllegalArgumentException("Invalid pair symbol: '$char'")
}

/** Converts a pair index (1..35) to its uppercase alphanumeric character representation. */
internal fun pairToChar(pair: Int): Char = when (pair) {
    in 1..9 -> '0' + pair
    in 10..35 -> 'A' + (pair - 10)
    else -> throw IllegalArgumentException("Pair index out of range (1..35): $pair")
}

/** Defensive read-only list copy preventing external mutation. */
internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
