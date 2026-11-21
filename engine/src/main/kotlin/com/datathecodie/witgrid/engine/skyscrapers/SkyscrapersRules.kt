package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell

/**
 * Skyscrapers grid validation, player feedback, and capped solution counting.
 */
object SkyscrapersRules {

    /** Returns how many buildings are visible looking along [heights] from start to end. */
    fun countVisible(heights: List<Int>): Int {
        var visibleCount = 0
        var maxHeight = 0
        for (height in heights) {
            if (height > maxHeight) {
                visibleCount++
                maxHeight = height
            }
        }
        return visibleCount
    }

    /** Returns whether [values] is a valid completed Latin square meeting all visibility clues. */
    fun isValidSolution(puzzle: SkyscrapersPuzzle, values: List<Int> = puzzle.solution): Boolean {
        val size = puzzle.size
        if (values.size != puzzle.grid.cellCount || values.any { it !in 1..size }) {
            return false
        }

        // Check givens match
        for (index in values.indices) {
            val given = puzzle.givens[index]
            if (given != null && values[index] != given) return false
        }

        val fullMask = (1 shl size) - 1

        // Check rows: Latin square property and left/right visibility clues
        for (row in 0 until size) {
            var rowMask = 0
            val rowValues = ArrayList<Int>(size)
            for (col in 0 until size) {
                val digit = values[row * size + col]
                val bit = 1 shl (digit - 1)
                if (rowMask and bit != 0) return false
                rowMask = rowMask or bit
                rowValues.add(digit)
            }
            if (rowMask != fullMask) return false

            val leftClue = puzzle.leftClues[row]
            if (leftClue != null && countVisible(rowValues) != leftClue) {
                return false
            }

            val rightClue = puzzle.rightClues[row]
            if (rightClue != null && countVisible(rowValues.reversed()) != rightClue) {
                return false
            }
        }

        // Check columns: Latin square property and top/bottom visibility clues
        for (col in 0 until size) {
            var colMask = 0
            val colValues = ArrayList<Int>(size)
            for (row in 0 until size) {
                val digit = values[row * size + col]
                val bit = 1 shl (digit - 1)
                if (colMask and bit != 0) return false
                colMask = colMask or bit
                colValues.add(digit)
            }
            if (colMask != fullMask) return false

            val topClue = puzzle.topClues[col]
            if (topClue != null && countVisible(colValues) != topClue) {
                return false
            }

            val bottomClue = puzzle.bottomClues[col]
            if (bottomClue != null && countVisible(colValues.reversed()) != bottomClue) {
                return false
            }
        }

        return true
    }

    /** Returns whether every cell matches the puzzle's stored valid solution. */
    fun isSolved(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): Boolean {
        require(state.puzzle == puzzle) { "Skyscrapers state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        return state.digits.indices.all { index -> state.digits[index] == puzzle.solution[index] }
    }

    /**
     * Returns incorrect entered building heights in row-major order; empty cells and pencil notes are not mistakes.
     */
    fun mistakes(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): List<Cell> {
        require(state.puzzle == puzzle) { "Skyscrapers state belongs to a different puzzle" }
        return state.digits.indices.filter { index ->
            val entered = state.digits[index]
            entered != null && entered != puzzle.solution[index]
        }.map(::Cell)
    }

    /**
     * Counts solutions compatible with the clues and givens, stopping as soon as [limit] solutions are found.
     */
    fun countSolutions(puzzle: SkyscrapersPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val size = puzzle.size

        val permutations = linePermutations.getValue(size)
        val lines = Array(2 * size) { line ->
            if (line < size) {
                permutations[clueIndex(puzzle.leftClues[line], puzzle.rightClues[line])]
            } else {
                val column = line - size
                permutations[clueIndex(puzzle.topClues[column], puzzle.bottomClues[column])]
            }
        }
        if (lines.any { it.isEmpty() }) return 0

        val allHeights = (1 shl size) - 1
        val initialHeights = IntArray(puzzle.grid.cellCount) { cell ->
            puzzle.givens[cell]?.let { 1 shl (it - 1) } ?: allHeights
        }
        val lineCells = Array(2 * size) { line ->
            IntArray(size) { position ->
                if (line < size) line * size + position else position * size + line - size
            }
        }

        // Each permutation and candidate set uses one byte of height bits per position.
        // Propagating supports across perpendicular lines prunes both edge clues on partial boards.
        fun propagate(heights: IntArray, changedLines: Int): Boolean {
            var pendingLines = changedLines
            while (pendingLines != 0) {
                val line = Integer.numberOfTrailingZeros(pendingLines)
                pendingLines = pendingLines and (pendingLines - 1)
                val cells = lineCells[line]
                var allowed = 0L
                for (position in cells.indices) {
                    allowed = allowed or (heights[cells[position]].toLong() shl (8 * position))
                }

                var supported = 0L
                for (permutation in lines[line]) {
                    if (permutation and allowed == permutation) {
                        supported = supported or permutation
                        if (supported == allowed) break
                    }
                }
                if (supported == 0L) return false
                if (supported == allowed) continue

                for (position in cells.indices) {
                    val cell = cells[position]
                    val narrowed = (supported ushr (8 * position)).toInt() and allHeights
                    if (narrowed == heights[cell]) continue
                    heights[cell] = narrowed
                    val perpendicular = if (line < size) size + position else position
                    pendingLines = pendingLines or (1 shl perpendicular)
                }
            }
            return true
        }

        var solutionCount = 0
        fun search(heights: IntArray, changedLines: Int) {
            if (!propagate(heights, changedLines)) return

            var branchCell = -1
            var fewestHeights = size + 1
            for (cell in heights.indices) {
                val count = Integer.bitCount(heights[cell])
                if (count in 2 until fewestHeights) {
                    branchCell = cell
                    fewestHeights = count
                    if (count == 2) break
                }
            }
            if (branchCell == -1) {
                solutionCount++
                return
            }

            val affectedLines = (1 shl (branchCell / size)) or (1 shl (size + branchCell % size))
            var choices = heights[branchCell]
            while (choices != 0 && solutionCount < limit) {
                val branch = heights.copyOf()
                branch[branchCell] = Integer.lowestOneBit(choices)
                choices = choices and (choices - 1)
                search(branch, affectedLines)
            }
        }

        search(initialHeights, (1 shl lines.size) - 1)
        return solutionCount
    }

    private const val CLUE_SLOTS = 8

    private fun clueIndex(front: Int?, back: Int?): Int =
        (front ?: 0) * CLUE_SLOTS + (back ?: 0)

    private val linePermutations: Map<Int, Array<LongArray>> by lazy {
        SKYSCRAPERS_SUPPORTED_SIZES.associateWith(::buildLinePermutations)
    }

    private fun buildLinePermutations(size: Int): Array<LongArray> {
        val groups = Array(CLUE_SLOTS * CLUE_SLOTS) { ArrayList<Long>() }
        val line = IntArray(size)
        val allHeights = (1 shl size) - 1

        fun visibleFrom(indices: IntProgression): Int {
            var tallest = 0
            var visible = 0
            for (index in indices) {
                val height = line[index]
                if (height > tallest) {
                    tallest = height
                    visible++
                }
            }
            return visible
        }

        fun addPermutations(length: Int, used: Int, permutation: Long) {
            if (length == size) {
                val front = visibleFrom(line.indices)
                val back = visibleFrom(line.indices.reversed())
                groups[clueIndex(front, back)].add(permutation)
                groups[clueIndex(front, null)].add(permutation)
                groups[clueIndex(null, back)].add(permutation)
                groups[clueIndex(null, null)].add(permutation)
                return
            }

            var remaining = allHeights and used.inv()
            while (remaining != 0) {
                val bit = Integer.lowestOneBit(remaining)
                line[length] = Integer.numberOfTrailingZeros(bit) + 1
                addPermutations(length + 1, used or bit, permutation or (bit.toLong() shl (8 * length)))
                remaining = remaining and (remaining - 1)
            }
        }

        addPermutations(0, 0, 0L)
        return Array(groups.size) { groups[it].toLongArray() }
    }
}
