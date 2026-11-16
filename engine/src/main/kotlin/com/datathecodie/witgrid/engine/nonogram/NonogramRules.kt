package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark

/** Clue validation, player feedback and capped Nonogram solution counting. */
object NonogramRules {
    /** Returns whether [values] is a complete pattern that produces every stored clue. */
    fun isValidSolution(puzzle: NonogramPuzzle, values: List<Int> = puzzle.solution): Boolean {
        if (values.size != puzzle.grid.cellCount || values.any { it !in 0..1 }) return false
        val rowClues = (0 until puzzle.size).map { row ->
            cluesForLine((0 until puzzle.size).map { column -> values[row * puzzle.size + column] })
        }
        if (rowClues != puzzle.rowClues) return false
        val columnClues = (0 until puzzle.size).map { column ->
            cluesForLine((0 until puzzle.size).map { row -> values[row * puzzle.size + column] })
        }
        return columnClues == puzzle.columnClues
    }

    /** Returns row clues followed by column clues derived from a complete bit pattern. */
    fun cluesForSolution(size: Int, values: List<Int>): Pair<List<List<Int>>, List<List<Int>>> {
        require(size in NonogramPuzzle.supportedSizes) { "Unsupported Nonogram board size" }
        require(values.size == size * size && values.all { it in 0..1 }) {
            "Nonogram pattern needs one zero or one per cell"
        }
        val rows = (0 until size).map { row ->
            cluesForLine((0 until size).map { column -> values[row * size + column] })
        }
        val columns = (0 until size).map { column ->
            cluesForLine((0 until size).map { row -> values[row * size + column] })
        }
        return immutableClues(rows) to immutableClues(columns)
    }

    /** Counts clue-matching patterns, stopping as soon as [limit] solutions have been found. */
    fun countSolutions(puzzle: NonogramPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        // Inconsistent totals otherwise force an exhaustive search of equivalent partial rows.
        val rowFilledCount = puzzle.rowClues.sumOf { it.sum() }
        val columnFilledCount = puzzle.columnClues.sumOf { it.sum() }
        if (rowFilledCount != columnFilledCount) return 0
        val rowOptions = puzzle.rowClues.map { clues -> lineMasks(puzzle.size, clues) }
        if (rowOptions.any { it.isEmpty() }) return 0

        val completedClues = IntArray(puzzle.size)
        val activeRuns = IntArray(puzzle.size)
        var solutionCount = 0

        fun search(row: Int, completed: IntArray, runs: IntArray) {
            if (solutionCount >= limit) return
            if (row == puzzle.size) {
                for (column in 0 until puzzle.size) {
                    val clue = puzzle.columnClues[column]
                    var clueIndex = completed[column]
                    val run = runs[column]
                    if (run > 0) {
                        if (clueIndex >= clue.size || clue[clueIndex] != run) return
                        clueIndex++
                    }
                    if (clueIndex != clue.size) return
                }
                solutionCount++
                return
            }

            val remainingRows = puzzle.size - row - 1
            for (mask in rowOptions[row]) {
                val nextCompleted = completed.copyOf()
                val nextRuns = runs.copyOf()
                var valid = true
                for (column in 0 until puzzle.size) {
                    val clue = puzzle.columnClues[column]
                    val filled = (mask ushr column) and 1
                    if (filled == 1) {
                        if (nextCompleted[column] >= clue.size) {
                            valid = false
                            break
                        }
                        nextRuns[column]++
                        if (nextRuns[column] > clue[nextCompleted[column]]) {
                            valid = false
                            break
                        }
                    } else if (nextRuns[column] > 0) {
                        if (nextRuns[column] != clue[nextCompleted[column]]) {
                            valid = false
                            break
                        }
                        nextCompleted[column]++
                        nextRuns[column] = 0
                    }
                    if (minimumCellsNeeded(clue, nextCompleted[column], nextRuns[column]) > remainingRows) {
                        valid = false
                        break
                    }
                }
                if (valid) search(row + 1, nextCompleted, nextRuns)
                if (solutionCount >= limit) return
            }
        }

        search(0, completedClues, activeRuns)
        return solutionCount
    }

    /** Returns whether every cell is explicitly marked to match the stored pattern. */
    fun isSolved(puzzle: NonogramPuzzle, state: NonogramState): Boolean {
        require(state.puzzle == puzzle) { "Nonogram state belongs to a different puzzle" }
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
     * Returns incorrect non-empty marks in row-major order. An X on a filled solution cell counts
     * as a mistake, as does a fill on an empty solution cell; empty cells are not mistakes.
     */
    fun mistakes(puzzle: NonogramPuzzle, state: NonogramState): List<Cell> {
        require(state.puzzle == puzzle) { "Nonogram state belongs to a different puzzle" }
        return state.marks.indices.filter { index ->
            when (state.marks[index]) {
                Mark.EMPTY -> false
                Mark.FILLED -> puzzle.solution[index] != 1
                Mark.CROSSED -> puzzle.solution[index] != 0
            }
        }.map(::Cell)
    }

    private fun lineMasks(size: Int, clues: List<Int>): IntArray {
        if (clues.isEmpty()) return intArrayOf(0)
        val masks = ArrayList<Int>()

        fun place(clueIndex: Int, firstAvailable: Int, mask: Int) {
            if (clueIndex == clues.size) {
                masks += mask
                return
            }
            val remainingClues = clues.size - clueIndex
            val minimumLength = clues.drop(clueIndex).sum() + remainingClues - 1
            val lastStart = size - minimumLength
            for (start in firstAvailable..lastStart) {
                val runMask = ((1 shl clues[clueIndex]) - 1) shl start
                place(clueIndex + 1, start + clues[clueIndex] + 1, mask or runMask)
            }
        }

        place(0, 0, 0)
        return masks.toIntArray()
    }

    private fun minimumCellsNeeded(clues: List<Int>, completed: Int, activeRun: Int): Int {
        if (activeRun > 0) {
            if (completed >= clues.size || activeRun > clues[completed]) return Int.MAX_VALUE
            val laterRuns = clues.drop(completed + 1)
            return clues[completed] - activeRun + laterRuns.sum() + laterRuns.size
        }
        val remainingRuns = clues.drop(completed)
        return remainingRuns.sum() + maxOf(0, remainingRuns.size - 1)
    }

    private fun cluesForLine(values: List<Int>): List<Int> {
        val clues = ArrayList<Int>()
        var runLength = 0
        for (value in values) {
            if (value == 1) {
                runLength++
            } else if (runLength > 0) {
                clues += runLength
                runLength = 0
            }
        }
        if (runLength > 0) clues += runLength
        return clues
    }

    private fun immutableClues(values: List<List<Int>>): List<List<Int>> =
        java.util.Collections.unmodifiableList(values.map { line ->
            java.util.Collections.unmodifiableList(ArrayList(line))
        })
}
