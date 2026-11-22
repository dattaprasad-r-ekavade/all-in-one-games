package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import java.util.concurrent.ConcurrentHashMap

/** Clue validation, player feedback and capped Mosaic solution counting. */
object MosaicRules {
    private val areaCache = ConcurrentHashMap<Int, Array<List<Cell>>>()

    /** Returns the unmodifiable 3x3 neighbourhood (self and 8-way neighbours) for [cell]. */
    fun areaCells(grid: Grid, cell: Cell): List<Cell> =
        areaCache.computeIfAbsent(grid.size) {
            Array(grid.cellCount) { index ->
                val center = Cell(index)
                immutableList(listOf(center) + grid.neighbours(center))
            }
        }[cell.index]

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
        val trail = IntArray(cellCount)
        var trailTop = 0
        var solutionCount = 0

        fun assign(cell: Int, value: Int): Boolean {
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
            trail[trailTop++] = cell
            for (clueCell in cluesForCell[cell]) {
                unassignedCount[clueCell]--
                if (value == 1) {
                    currentSum[clueCell]++
                }
            }
            return true
        }

        fun propagate(fromIndex: Int): Boolean {
            var checkIndex = fromIndex
            while (checkIndex < trailTop) {
                val cell = trail[checkIndex++]
                for (clueCell in cluesForCell[cell]) {
                    val sum = currentSum[clueCell]
                    val target = targetSum[clueCell]
                    val rem = unassignedCount[clueCell]
                    if (rem > 0) {
                        if (sum == target) {
                            for (neighbour in areaByCell[clueCell]) {
                                if (values[neighbour] == -1) {
                                    if (!assign(neighbour, 0)) return false
                                }
                            }
                        } else if (sum + rem == target) {
                            for (neighbour in areaByCell[clueCell]) {
                                if (values[neighbour] == -1) {
                                    if (!assign(neighbour, 1)) return false
                                }
                            }
                        }
                    }
                }
            }
            return true
        }

        fun undoTo(targetTop: Int) {
            while (trailTop > targetTop) {
                val cell = trail[--trailTop]
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
                var bestNeighbour = -1
                var maxClues = -1
                for (neighbour in areaByCell[bestClue]) {
                    if (values[neighbour] == -1) {
                        val count = cluesForCell[neighbour].size
                        if (count > maxClues) {
                            maxClues = count
                            bestNeighbour = neighbour
                        }
                    }
                }
                bestNeighbour
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

            var neededOnes = 0
            var neededZeros = 0
            for (clueCell in cluesForCell[branchCell]) {
                val ones = targetSum[clueCell] - currentSum[clueCell]
                neededOnes += ones
                neededZeros += (unassignedCount[clueCell] - ones)
            }
            val firstVal = if (neededOnes < neededZeros) 0 else 1
            val secondVal = 1 - firstVal

            val savedTop1 = trailTop
            if (assign(branchCell, firstVal) && propagate(savedTop1)) {
                search()
            }
            undoTo(savedTop1)
            if (solutionCount >= limit) return

            val savedTop0 = trailTop
            if (assign(branchCell, secondVal) && propagate(savedTop0)) {
                search()
            }
            undoTo(savedTop0)
        }

        for (clueCell in clueCells) {
            val target = targetSum[clueCell]
            val totalArea = areaByCell[clueCell].size
            if (target == 0) {
                for (neighbour in areaByCell[clueCell]) {
                    if (values[neighbour] == -1) {
                        if (!assign(neighbour, 0)) return 0
                    }
                }
            } else if (target == totalArea) {
                for (neighbour in areaByCell[clueCell]) {
                    if (values[neighbour] == -1) {
                        if (!assign(neighbour, 1)) return 0
                    }
                }
            }
        }
        if (!propagate(0)) {
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
        return immutableList(state.marks.indices.filter { index ->
            when (state.marks[index]) {
                Mark.EMPTY -> false
                Mark.FILLED -> puzzle.solution[index] != 1
                Mark.CROSSED -> puzzle.solution[index] != 0
            }
        }.map(::Cell))
    }
}
