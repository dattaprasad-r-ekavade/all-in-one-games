package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Move
import java.util.ArrayList

/**
 * Immutable player state for Path.
 *
 * Each non-wall cell holds a pair color (1..[PathPuzzle.pairCount]) or null if empty.
 * Given endpoints and walls cannot be edited.
 */
class PathState(
    val puzzle: PathPuzzle,
    digits: List<Int?>,
) {
    /** Player digits for each cell, defensively copied. */
    val digits: List<Int?> = immutableList(digits)

    init {
        require(this.digits.size == puzzle.grid.cellCount) {
            "Path state needs one entry per cell"
        }
        for (index in 0 until puzzle.grid.cellCount) {
            val endpoint = puzzle.endpoints[index]
            val isWall = puzzle.walls[index]
            val digit = this.digits[index]
            if (isWall) {
                require(digit == null) {
                    "Wall cell $index cannot have a digit"
                }
            } else if (endpoint != null) {
                require(digit == endpoint) {
                    "Endpoint cell $index must retain given digit $endpoint, got: $digit"
                }
            } else if (digit != null) {
                require(digit in 1..puzzle.pairCount) {
                    "Digit at cell $index must be in 1..${puzzle.pairCount}, got: $digit"
                }
            }
        }
    }

    /**
     * Applies one absolute digit edit.
     * Setting digit 0 clears the cell.
     * Modifying givens (endpoints or walls) throws [IllegalArgumentException].
     */
    fun withMove(move: Move): PathState {
        require(move.cell.index < puzzle.grid.cellCount) {
            "Move cell ${move.cell.index} outside Path puzzle"
        }
        require(move is Move.SetDigit) {
            "Path supports digit moves only"
        }
        val cellIndex = move.cell.index
        require(!puzzle.walls[cellIndex]) {
            "Cannot edit wall cell at $cellIndex"
        }
        require(puzzle.endpoints[cellIndex] == null) {
            "Cannot edit given endpoint at $cellIndex"
        }
        require(move.digit in 0..puzzle.pairCount) {
            "Path digit must be between 0 and ${puzzle.pairCount}, got: ${move.digit}"
        }

        val updated = digits.toMutableList()
        updated[cellIndex] = move.digit.takeIf { it != 0 }
        return PathState(puzzle, updated)
    }

    override fun equals(other: Any?): Boolean = other is PathState &&
        puzzle == other.puzzle &&
        digits == other.digits

    override fun hashCode(): Int = 31 * puzzle.hashCode() + digits.hashCode()

    companion object {
        /** Creates the initial state with givens in place and all other cells empty. */
        fun initial(puzzle: PathPuzzle): PathState {
            require(PathRules.isValidSolution(puzzle)) {
                "Path puzzle solution is not rule-valid"
            }
            val initialDigits = ArrayList<Int?>(puzzle.grid.cellCount)
            for (index in 0 until puzzle.grid.cellCount) {
                initialDigits.add(puzzle.endpoints[index])
            }
            return PathState(puzzle, initialDigits)
        }
    }
}
