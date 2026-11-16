package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark

/** Rule checks and capped brute-force search for Crowns puzzles. */
object CrownsRules {
    /** Returns whether [columnsByRow] places one crown in every column and region without touching. */
    fun isValidSolution(puzzle: CrownsPuzzle, columnsByRow: List<Int> = puzzle.solutionColumns): Boolean {
        if (columnsByRow.size != puzzle.size || columnsByRow.any { it !in 0 until puzzle.size }) return false
        if (columnsByRow.toSet().size != puzzle.size) return false
        for (row in 1 until puzzle.size) {
            if (kotlin.math.abs(columnsByRow[row] - columnsByRow[row - 1]) <= 1) return false
        }
        val usedRegions = columnsByRow.mapIndexed { row, column ->
            puzzle.regions[row * puzzle.size + column]
        }.toSet()
        return usedRegions.size == puzzle.size
    }

    /** Counts rule-valid solutions, stopping as soon as [limit] solutions have been found. */
    fun countSolutions(puzzle: CrownsPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val columnsByRow = IntArray(puzzle.size)
        val usedColumns = BooleanArray(puzzle.size)
        val usedRegions = BooleanArray(puzzle.size)
        var solutionCount = 0

        fun search(row: Int) {
            if (solutionCount >= limit) return
            if (row == puzzle.size) {
                solutionCount++
                return
            }

            for (column in 0 until puzzle.size) {
                if (usedColumns[column]) continue
                if (row > 0 && kotlin.math.abs(column - columnsByRow[row - 1]) <= 1) continue
                val region = puzzle.regions[row * puzzle.size + column]
                if (usedRegions[region]) continue

                columnsByRow[row] = column
                usedColumns[column] = true
                usedRegions[region] = true
                search(row + 1)
                usedRegions[region] = false
                usedColumns[column] = false
                if (solutionCount >= limit) return
            }
        }

        search(0)
        return solutionCount
    }

    /** Returns whether the player's crowns exactly match the stored solution. X marks are optional. */
    fun isSolved(puzzle: CrownsPuzzle, state: CrownsState): Boolean {
        require(state.puzzle == puzzle) { "Crowns state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        val solutionCells = puzzle.solutionColumns.mapIndexed { row, column ->
            row * puzzle.size + column
        }.toSet()
        return state.marks.indices.all { index ->
            (state.marks[index] == Mark.FILLED) == (index in solutionCells)
        }
    }

    /**
     * Returns incorrect committed entries in row-major order. A crown outside the stored solution
     * and an X on a stored crown cell both count as mistakes; empty cells are ignored.
     */
    fun mistakes(puzzle: CrownsPuzzle, state: CrownsState): List<Cell> {
        require(state.puzzle == puzzle) { "Crowns state belongs to a different puzzle" }
        val solutionCells = puzzle.solutionColumns.mapIndexed { row, column ->
            row * puzzle.size + column
        }.toSet()
        return state.marks.indices.filter { index ->
            when (state.marks[index]) {
                Mark.EMPTY -> false
                Mark.FILLED -> index !in solutionCells
                Mark.CROSSED -> index in solutionCells
            }
        }.map(::Cell)
    }
}
