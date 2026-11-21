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

        // Quick clue impossibility checks
        for (c in 0 until size) {
            val t = puzzle.topClues[c]
            val b = puzzle.bottomClues[c]
            if (t != null && b != null && (t + b > size + 1 || (t == 1 && b == 1 && size > 1))) return 0
        }
        for (r in 0 until size) {
            val l = puzzle.leftClues[r]
            val right = puzzle.rightClues[r]
            if (l != null && right != null && (l + right > size + 1 || (l == 1 && right == 1 && size > 1))) return 0
        }

        // Check given conflicts in rows and columns
        val rowGivenMask = IntArray(size)
        val colGivenMask = IntArray(size)
        for (index in puzzle.givens.indices) {
            val given = puzzle.givens[index] ?: continue
            val r = index / size
            val c = index % size
            val bit = 1 shl (given - 1)
            if (rowGivenMask[r] and bit != 0 || colGivenMask[c] and bit != 0) return 0
            rowGivenMask[r] = rowGivenMask[r] or bit
            colGivenMask[c] = colGivenMask[c] or bit
        }

        val allPerms = PERMUTATIONS_BY_SIZE.getValue(size)

        // Filter valid permutations per row
        val rowCandidates = Array(size) { row ->
            val leftClue = puzzle.leftClues[row]
            val rightClue = puzzle.rightClues[row]
            allPerms.filter { perm ->
                var matchesGivens = true
                for (col in 0 until size) {
                    val given = puzzle.givens[row * size + col]
                    if (given != null && perm[col] != given) {
                        matchesGivens = false
                        break
                    }
                }
                if (!matchesGivens) return@filter false

                if (leftClue != null && countVisibleIntArray(perm) != leftClue) return@filter false
                if (rightClue != null && countVisibleReversedIntArray(perm) != rightClue) return@filter false

                true
            }
        }

        if (rowCandidates.any { it.isEmpty() }) return 0

        val colMask = IntArray(size)
        val colMax = IntArray(size)
        val colVisible = IntArray(size)
        val grid = Array(size) { IntArray(size) }
        var solutionCount = 0

        fun search(row: Int) {
            if (solutionCount >= limit) return
            if (row == size) {
                solutionCount++
                return
            }

            val candidates = rowCandidates[row]
            for (perm in candidates) {
                var collision = false
                for (c in 0 until size) {
                    val bit = 1 shl (perm[c] - 1)
                    if (colMask[c] and bit != 0) {
                        collision = true
                        break
                    }
                }
                if (collision) continue

                // Verify top and bottom clues constraints along columns
                var columnCompatible = true
                val prevMax = IntArray(size)
                val prevVis = IntArray(size)
                for (c in 0 until size) {
                    val height = perm[c]
                    prevMax[c] = colMax[c]
                    prevVis[c] = colVisible[c]
                    val newMax = if (height > colMax[c]) height else colMax[c]
                    val newVis = if (height > colMax[c]) colVisible[c] + 1 else colVisible[c]

                    val topClue = puzzle.topClues[c]
                    if (topClue != null) {
                        if (newVis > topClue) {
                            columnCompatible = false
                            break
                        }
                        val remainingRows = size - 1 - row
                        val maxPossibleAdd = if (size - newMax < remainingRows) size - newMax else remainingRows
                        if (newVis + maxPossibleAdd < topClue) {
                            columnCompatible = false
                            break
                        }
                        if (row == size - 1 && newVis != topClue) {
                            columnCompatible = false
                            break
                        }
                    }

                    if (row == size - 1) {
                        val bottomClue = puzzle.bottomClues[c]
                        if (bottomClue != null) {
                            var bVis = 1
                            var bMax = height
                            for (r in size - 2 downTo 0) {
                                val valAbove = grid[r][c]
                                if (valAbove > bMax) {
                                    bVis++
                                    bMax = valAbove
                                }
                            }
                            if (bVis != bottomClue) {
                                columnCompatible = false
                                break
                            }
                        }
                    }
                }
                if (!columnCompatible) continue

                // Apply row
                for (c in 0 until size) {
                    val height = perm[c]
                    val bit = 1 shl (height - 1)
                    colMask[c] = colMask[c] or bit
                    colMax[c] = if (height > colMax[c]) height else colMax[c]
                    colVisible[c] = if (height > prevMax[c]) prevVis[c] + 1 else prevVis[c]
                    grid[row][c] = height
                }

                search(row + 1)

                // Backtrack
                for (c in 0 until size) {
                    val height = perm[c]
                    val bit = 1 shl (height - 1)
                    colMask[c] = colMask[c] and bit.inv()
                    colMax[c] = prevMax[c]
                    colVisible[c] = prevVis[c]
                }

                if (solutionCount >= limit) return
            }
        }

        search(0)
        return solutionCount
    }

    private fun countVisibleIntArray(heights: IntArray): Int {
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

    private fun countVisibleReversedIntArray(heights: IntArray): Int {
        var visibleCount = 0
        var maxHeight = 0
        for (i in heights.indices.reversed()) {
            val height = heights[i]
            if (height > maxHeight) {
                visibleCount++
                maxHeight = height
            }
        }
        return visibleCount
    }

    private val PERMUTATIONS_BY_SIZE: Map<Int, List<IntArray>> =
        SKYSCRAPERS_SUPPORTED_SIZES.associateWith { buildPermutations(it) }

    private fun buildPermutations(n: Int): List<IntArray> {
        val results = ArrayList<IntArray>()
        val current = IntArray(n)
        val used = BooleanArray(n + 1)

        fun backtrack(index: Int) {
            if (index == n) {
                results.add(current.clone())
                return
            }
            for (v in 1..n) {
                if (!used[v]) {
                    used[v] = true
                    current[index] = v
                    backtrack(index + 1)
                    used[v] = false
                }
            }
        }

        backtrack(0)
        return results
    }
}
