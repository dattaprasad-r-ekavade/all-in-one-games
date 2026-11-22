package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack

/**
 * Fixed deal: initial arrows grid and metadata from the bank line.
 */
data class ArrowsPuzzle(
    val bankId: String,
    val size: Int,
    val pack: Pack,
    val difficulty: String,
    /** Row-major cell list; null represents an empty cell. */
    val initialArrows: List<ArrowsDirection?>,
) {
    val id: String get() = bankId

    val cellCount: Int get() = size * size

    init {
        require(size in SUPPORTED_SIZES) { "Unsupported Arrows board size: $size" }
        require(expectedGridSize(pack) == size) {
            "Pack $pack requires size ${expectedGridSize(pack)}, not $size"
        }
        require(initialArrows.size == size * size) {
            "Expected ${size * size} cells for size $size, got ${initialArrows.size}"
        }
        require(bankId.isNotBlank() && bankId.none { it == '|' || it == '\n' || it == '\r' }) {
            "Invalid bankId: $bankId"
        }
        require(difficulty.isNotBlank() && difficulty.none { it == '|' || it == '\n' || it == '\r' }) {
            "Invalid difficulty: $difficulty"
        }
        require(ArrowsRules.isClearable(size, initialArrows)) {
            "Arrows layout is not clearable (deadlocked or cannot clear board)"
        }
    }

    fun arrowAt(cell: Cell): ArrowsDirection? {
        require(cell.index in initialArrows.indices) { "Cell index out of range: ${cell.index}" }
        return initialArrows[cell.index]
    }

    fun arrowAt(row: Int, column: Int): ArrowsDirection? {
        require(row in 0 until size && column in 0 until size) {
            "Coordinates out of range: row=$row, column=$column for size $size"
        }
        return initialArrows[row * size + column]
    }

    companion object {
        val SUPPORTED_SIZES = listOf(5, 6, 8, 10)

        fun expectedGridSize(pack: Pack): Int = when (pack) {
            Pack.EASY -> 5
            Pack.MEDIUM -> 6
            Pack.HARD -> 8
            Pack.EXPERT -> 10
        }
    }
}
