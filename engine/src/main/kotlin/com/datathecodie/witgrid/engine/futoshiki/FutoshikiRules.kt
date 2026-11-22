package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Cell

/** Futoshiki grid validation, player feedback and capped solution counting. */
object FutoshikiRules {
    /**
     * Returns whether [values] forms a valid Latin square of size [puzzle.size]
     * and satisfies every inequality in [puzzle].
     */
    fun isValidSolution(puzzle: FutoshikiPuzzle, values: List<Int> = puzzle.solution): Boolean {
        val size = puzzle.size
        val cellCount = size * size
        if (values.size != cellCount || values.any { it !in 1..size }) {
            return false
        }
        val fullMask = (1 shl size) - 1

        // Check rows
        for (r in 0 until size) {
            var mask = 0
            for (c in 0 until size) {
                val digit = values[r * size + c]
                val bit = 1 shl (digit - 1)
                if ((mask and bit) != 0) return false
                mask = mask or bit
            }
            if (mask != fullMask) return false
        }

        // Check columns
        for (c in 0 until size) {
            var mask = 0
            for (r in 0 until size) {
                val digit = values[r * size + c]
                val bit = 1 shl (digit - 1)
                if ((mask and bit) != 0) return false
                mask = mask or bit
            }
            if (mask != fullMask) return false
        }

        // Check horizontal inequalities
        for (r in 0 until size) {
            for (c in 0 until size - 1) {
                val sign = puzzle.horizontalSign(r, c)
                val left = values[r * size + c]
                val right = values[r * size + c + 1]
                when (sign) {
                    FutoshikiSign.LESS -> if (left >= right) return false
                    FutoshikiSign.GREATER -> if (left <= right) return false
                    FutoshikiSign.NONE -> {}
                }
            }
        }

        // Check vertical inequalities
        for (r in 0 until size - 1) {
            for (c in 0 until size) {
                val sign = puzzle.verticalSign(r, c)
                val top = values[r * size + c]
                val bottom = values[(r + 1) * size + c]
                when (sign) {
                    FutoshikiSign.LESS -> if (top >= bottom) return false
                    FutoshikiSign.GREATER -> if (top <= bottom) return false
                    FutoshikiSign.NONE -> {}
                }
            }
        }

        return true
    }

    /**
     * Counts boards compatible with the givens and inequalities, stopping as soon as [limit] solutions are found.
     */
    fun countSolutions(puzzle: FutoshikiPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val size = puzzle.size
        val cellCount = size * size
        val values = IntArray(cellCount)
        val rowUsed = IntArray(size)
        val colUsed = IntArray(size)

        for (index in puzzle.givens.indices) {
            val given = puzzle.givens[index] ?: continue
            if (given !in 1..size) return 0
            val bit = 1 shl (given - 1)
            val r = index / size
            val c = index % size
            if ((rowUsed[r] and bit) != 0 || (colUsed[c] and bit) != 0) {
                return 0
            }
            values[index] = given
            rowUsed[r] = rowUsed[r] or bit
            colUsed[c] = colUsed[c] or bit
        }

        // Check inequalities between pairs of givens
        for (r in 0 until size) {
            for (c in 0 until size - 1) {
                val sign = puzzle.horizontalSign(r, c)
                val left = values[r * size + c]
                val right = values[r * size + c + 1]
                if (left != 0 && right != 0) {
                    if (sign == FutoshikiSign.LESS && left >= right) return 0
                    if (sign == FutoshikiSign.GREATER && left <= right) return 0
                }
            }
        }
        for (r in 0 until size - 1) {
            for (c in 0 until size) {
                val sign = puzzle.verticalSign(r, c)
                val top = values[r * size + c]
                val bottom = values[(r + 1) * size + c]
                if (top != 0 && bottom != 0) {
                    if (sign == FutoshikiSign.LESS && top >= bottom) return 0
                    if (sign == FutoshikiSign.GREATER && top <= bottom) return 0
                }
            }
        }

        val allMask = (1 shl size) - 1

        fun candidatesFor(index: Int): Int {
            val r = index / size
            val c = index % size
            var mask = allMask and (rowUsed[r] or colUsed[c]).inv()
            if (mask == 0) return 0

            // Left neighbor: (r, c - 1)
            if (c > 0) {
                val sign = puzzle.horizontalSign(r, c - 1)
                val left = values[index - 1]
                if (left != 0) {
                    when (sign) {
                        FutoshikiSign.LESS -> {
                            val removeMask = (1 shl left) - 1
                            mask = mask and removeMask.inv()
                        }
                        FutoshikiSign.GREATER -> {
                            val keepMask = (1 shl (left - 1)) - 1
                            mask = mask and keepMask
                        }
                        FutoshikiSign.NONE -> {}
                    }
                } else {
                    when (sign) {
                        FutoshikiSign.LESS -> mask = mask and 1.inv()
                        FutoshikiSign.GREATER -> mask = mask and (1 shl (size - 1)).inv()
                        FutoshikiSign.NONE -> {}
                    }
                }
            }

            // Right neighbor: (r, c + 1)
            if (c < size - 1) {
                val sign = puzzle.horizontalSign(r, c)
                val right = values[index + 1]
                if (right != 0) {
                    when (sign) {
                        FutoshikiSign.LESS -> {
                            val keepMask = (1 shl (right - 1)) - 1
                            mask = mask and keepMask
                        }
                        FutoshikiSign.GREATER -> {
                            val removeMask = (1 shl right) - 1
                            mask = mask and removeMask.inv()
                        }
                        FutoshikiSign.NONE -> {}
                    }
                } else {
                    when (sign) {
                        FutoshikiSign.LESS -> mask = mask and (1 shl (size - 1)).inv()
                        FutoshikiSign.GREATER -> mask = mask and 1.inv()
                        FutoshikiSign.NONE -> {}
                    }
                }
            }

            // Top neighbor: (r - 1, c)
            if (r > 0) {
                val sign = puzzle.verticalSign(r - 1, c)
                val top = values[index - size]
                if (top != 0) {
                    when (sign) {
                        FutoshikiSign.LESS -> {
                            val removeMask = (1 shl top) - 1
                            mask = mask and removeMask.inv()
                        }
                        FutoshikiSign.GREATER -> {
                            val keepMask = (1 shl (top - 1)) - 1
                            mask = mask and keepMask
                        }
                        FutoshikiSign.NONE -> {}
                    }
                } else {
                    when (sign) {
                        FutoshikiSign.LESS -> mask = mask and 1.inv()
                        FutoshikiSign.GREATER -> mask = mask and (1 shl (size - 1)).inv()
                        FutoshikiSign.NONE -> {}
                    }
                }
            }

            // Bottom neighbor: (r + 1, c)
            if (r < size - 1) {
                val sign = puzzle.verticalSign(r, c)
                val bottom = values[index + size]
                if (bottom != 0) {
                    when (sign) {
                        FutoshikiSign.LESS -> {
                            val keepMask = (1 shl (bottom - 1)) - 1
                            mask = mask and keepMask
                        }
                        FutoshikiSign.GREATER -> {
                            val removeMask = (1 shl bottom) - 1
                            mask = mask and removeMask.inv()
                        }
                        FutoshikiSign.NONE -> {}
                    }
                } else {
                    when (sign) {
                        FutoshikiSign.LESS -> mask = mask and (1 shl (size - 1)).inv()
                        FutoshikiSign.GREATER -> mask = mask and 1.inv()
                        FutoshikiSign.NONE -> {}
                    }
                }
            }

            return mask
        }

        var solutionCount = 0

        fun search() {
            if (solutionCount >= limit) return
            var bestCell = -1
            var bestCandidates = 0
            var bestCount = size + 1

            for (index in values.indices) {
                if (values[index] != 0) continue
                val candidates = candidatesFor(index)
                val count = Integer.bitCount(candidates)
                if (count == 0) return
                if (count < bestCount) {
                    bestCell = index
                    bestCandidates = candidates
                    bestCount = count
                    if (count == 1) break
                }
            }

            if (bestCell == -1) {
                solutionCount++
                return
            }

            val r = bestCell / size
            val c = bestCell % size
            var candidates = bestCandidates
            while (candidates != 0 && solutionCount < limit) {
                val bit = Integer.lowestOneBit(candidates)
                val digit = Integer.numberOfTrailingZeros(bit) + 1
                values[bestCell] = digit
                rowUsed[r] = rowUsed[r] or bit
                colUsed[c] = colUsed[c] or bit

                search()

                values[bestCell] = 0
                rowUsed[r] = rowUsed[r] and bit.inv()
                colUsed[c] = colUsed[c] and bit.inv()
                candidates = candidates and bit.inv()
            }
        }

        search()
        return solutionCount
    }

    /** Returns whether every digit matches the solution; empty cells are incomplete. */
    fun isSolved(puzzle: FutoshikiPuzzle, state: FutoshikiState): Boolean {
        require(state.puzzle == puzzle) { "Futoshiki state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        return state.digits.indices.all { index -> state.digits[index] == puzzle.solution[index] }
    }

    /** Returns incorrect entered digits in row-major order; empty cells and notes are not mistakes. */
    fun mistakes(puzzle: FutoshikiPuzzle, state: FutoshikiState): List<Cell> {
        require(state.puzzle == puzzle) { "Futoshiki state belongs to a different puzzle" }
        return state.digits.indices.filter { index ->
            val entered = state.digits[index]
            entered != null && entered != puzzle.solution[index]
        }.map(::Cell)
    }
}
