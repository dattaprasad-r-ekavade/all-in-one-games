package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark

/** Clue validation, player feedback and capped Mosaic solution counting. */
object MosaicRules {
    /** Returns the 3x3 neighbourhood cells (self and 8-way adjacent neighbours) for [cell]. */
    fun areaCells(grid: Grid, cell: Cell): List<Cell> =
        listOf(cell) + grid.neighbours(cell)

    /** Returns the clue count (number of filled cells in 3x3 area) for [cell] given a complete [solution]. */
    fun clueAt(grid: Grid, solution: List<Int>, cell: Cell): Int =
        areaCells(grid, cell).count { solution[it.index] == 1 }

    /** Returns whether [values] is a complete binary pattern that satisfies every clue in [puzzle]. */
    fun isValidSolution(puzzle: MosaicPuzzle, values: List<Int> = puzzle.solution): Boolean {
        val cellCount = puzzle.grid.cellCount
        if (values.size != cellCount || values.any { it !in 0..1 }) return false
        for (index in 0 until cellCount) {
            val clue = puzzle.clues[index] ?: continue
            val count = clueAt(puzzle.grid, values, Cell(index))
            if (count != clue) return false
        }
        return true
    }

    /**
     * Counts rule-valid solutions, stopping as soon as [limit] solutions have been found.
     * Uses constraint propagation with unit deductions and minimum-remaining-values branching.
     */
    fun countSolutions(puzzle: MosaicPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val cellCount = puzzle.grid.cellCount

        val areaByCell = Array(cellCount) { index ->
            val neighbours = puzzle.grid.neighbours(Cell(index))
            IntArray(1 + neighbours.size) { idx ->
                if (idx == 0) index else neighbours[idx - 1].index
            }
        }

        val clueCells = ArrayList<Int>()
        val targetSum = IntArray(cellCount)
        for (i in 0 until cellCount) {
            val clue = puzzle.clues[i]
            if (clue != null) {
                clueCells.add(i)
                targetSum[i] = clue
                if (clue < 0 || clue > areaByCell[i].size) return 0
            }
        }

        val cluesForCell = Array(cellCount) { j ->
            areaByCell[j].filter { c -> puzzle.clues[c] != null }.toIntArray()
        }

        val values = IntArray(cellCount) { -1 }
        val currentSum = IntArray(cellCount)
        val unassignedCount = IntArray(cellCount) { i -> areaByCell[i].size }
        var solutionCount = 0

        fun assign(cell: Int, value: Int, stepAssignments: ArrayList<Int>): Boolean {
            val current = values[cell]
            if (current != -1) {
                return current == value
            }
            for (clueCell in cluesForCell[cell]) {
                val nextSum = currentSum[clueCell] + if (value == 1) 1 else 0
                val nextRem = unassignedCount[clueCell] - 1
                val target = targetSum[clueCell]
                if (nextSum > target || nextSum + nextRem < target) {
                    return false
                }
            }
            values[cell] = value
            stepAssignments.add(cell)
            for (clueCell in cluesForCell[cell]) {
                unassignedCount[clueCell]--
                if (value == 1) {
                    currentSum[clueCell]++
                }
            }
            return true
        }

        fun propagate(stepAssignments: ArrayList<Int>): Boolean {
            var checkIndex = 0
            while (checkIndex < stepAssignments.size) {
                val cell = stepAssignments[checkIndex++]
                for (clueCell in cluesForCell[cell]) {
                    val sum = currentSum[clueCell]
                    val target = targetSum[clueCell]
                    val rem = unassignedCount[clueCell]
                    if (rem > 0) {
                        if (sum == target) {
                            for (neighbour in areaByCell[clueCell]) {
                                if (values[neighbour] == -1) {
                                    if (!assign(neighbour, 0, stepAssignments)) return false
                                }
                            }
                        } else if (sum + rem == target) {
                            for (neighbour in areaByCell[clueCell]) {
                                if (values[neighbour] == -1) {
                                    if (!assign(neighbour, 1, stepAssignments)) return false
                                }
                            }
                        }
                    }
                }
            }
            return true
        }

        fun undo(stepAssignments: ArrayList<Int>) {
            for (i in stepAssignments.size - 1 downTo 0) {
                val cell = stepAssignments[i]
                val v = values[cell]
                for (clueCell in cluesForCell[cell]) {
                    unassignedCount[clueCell]++
                    if (v == 1) {
                        currentSum[clueCell]--
                    }
                }
                values[cell] = -1
            }
        }

        fun search() {
            if (solutionCount >= limit) return

            var bestClue = -1
            var minRem = Int.MAX_VALUE
            for (clueCell in clueCells) {
                val rem = unassignedCount[clueCell]
                if (rem in 1 until minRem) {
                    minRem = rem
                    bestClue = clueCell
                }
            }

            val branchCell = if (bestClue != -1) {
                var found = -1
                for (neighbour in areaByCell[bestClue]) {
                    if (values[neighbour] == -1) {
                        found = neighbour
                        break
                    }
                }
                found
            } else {
                var found = -1
                for (i in 0 until cellCount) {
                    if (values[i] == -1) {
                        found = i
                        break
                    }
                }
                found
            }

            if (branchCell == -1) {
                solutionCount++
                return
            }

            val step1 = ArrayList<Int>()
            if (assign(branchCell, 1, step1) && propagate(step1)) {
                search()
            }
            undo(step1)
            if (solutionCount >= limit) return

            val step0 = ArrayList<Int>()
            if (assign(branchCell, 0, step0) && propagate(step0)) {
                search()
            }
            undo(step0)
        }

        val initialStep = ArrayList<Int>()
        for (clueCell in clueCells) {
            val target = targetSum[clueCell]
            val rem = unassignedCount[clueCell]
            if (target == 0) {
                for (neighbour in areaByCell[clueCell]) {
                    if (values[neighbour] == -1) {
                        if (!assign(neighbour, 0, initialStep)) return 0
                    }
                }
            } else if (target == rem) {
                for (neighbour in areaByCell[clueCell]) {
                    if (values[neighbour] == -1) {
                        if (!assign(neighbour, 1, initialStep)) return 0
                    }
                }
            }
        }
        if (!propagate(initialStep)) {
            return 0
        }

        search()
        return solutionCount
    }

    /**
     * Returns whether every cell is explicitly marked to match the stored solution:
     * filled cells (1) must be [Mark.FILLED], and empty cells (0) must be [Mark.CROSSED].
     */
    fun isSolved(puzzle: MosaicPuzzle, state: MosaicState): Boolean {
        require(state.puzzle == puzzle) { "Mosaic state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        return state.marks.indices.all { index ->
            when (puzzle.solution[index]) {
                0 -> state.marks[index] == Mark.CROSSED
                1 -> state.marks[index] == Mark.FILLED
                else -> false
            }
        }
    }

    /**
     * Returns incorrect committed entries in row-major order.
     * A fill on an empty (0) solution cell and a cross on a filled (1) solution cell both count as
     * mistakes; empty cells are ignored.
     */
    fun mistakes(puzzle: MosaicPuzzle, state: MosaicState): List<Cell> {
        require(state.puzzle == puzzle) { "Mosaic state belongs to a different puzzle" }
        return state.marks.indices.filter { index ->
            when (state.marks[index]) {
                Mark.EMPTY -> false
                Mark.FILLED -> puzzle.solution[index] != 1
                Mark.CROSSED -> puzzle.solution[index] != 0
            }
        }.map(::Cell)
    }
}
