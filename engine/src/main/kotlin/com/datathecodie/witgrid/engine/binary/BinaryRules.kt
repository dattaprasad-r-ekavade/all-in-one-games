package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell

/** Binary grid validation, player feedback and capped solution counting. */
object BinaryRules {
    /** Returns whether [values] forms a complete grid that follows every Binary rule. */
    fun isValidSolution(puzzle: BinaryPuzzle, values: List<Int> = puzzle.solution): Boolean {
        val size = puzzle.size
        if (values.size != size * size || values.any { it !in 0..1 }) return false
        val rowPatterns = HashSet<Int>()
        val columnPatterns = HashSet<Int>()

        for (row in 0 until size) {
            val line = IntArray(size) { column -> values[row * size + column] }
            if (!isValidLine(line, size) || !rowPatterns.add(lineToMask(line))) return false
        }
        for (column in 0 until size) {
            val line = IntArray(size) { row -> values[row * size + column] }
            if (!isValidLine(line, size) || !columnPatterns.add(lineToMask(line))) return false
        }
        return true
    }

    /** Counts complete grids compatible with the givens, stopping at [limit]. */
    fun countSolutions(puzzle: BinaryPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val size = puzzle.size
        val half = size / 2
        val rowOptions = (0 until size).map { row -> rowMasks(size, puzzle.givens, row, half) }
        if (rowOptions.any { it.isEmpty() }) return 0

        val usedRows = BooleanArray(1 shl size)
        val columnOneCounts = IntArray(size)
        val columnPatterns = IntArray(size)
        var solutionCount = 0

        fun search(row: Int) {
            if (solutionCount >= limit) return
            if (row == size) {
                if (columnPatterns.toSet().size == size) solutionCount++
                return
            }

            val remainingRows = size - row - 1
            for (mask in rowOptions[row]) {
                if (usedRows[mask]) continue
                var valid = true
                for (column in 0 until size) {
                    val value = (mask ushr column) and 1
                    val priorOneCount = columnOneCounts[column]
                    val onesAfter = priorOneCount + value
                    val zerosAfter = row + 1 - onesAfter
                    if (
                        onesAfter > half ||
                        zerosAfter > half ||
                        onesAfter + remainingRows < half ||
                        zerosAfter + remainingRows < half
                    ) {
                        valid = false
                        break
                    }
                    if (
                        row >= 2 &&
                        bitAt(columnPatterns[column], row - 1) == value &&
                        bitAt(columnPatterns[column], row - 2) == value
                    ) {
                        valid = false
                        break
                    }
                }
                if (!valid) continue

                usedRows[mask] = true
                for (column in 0 until size) {
                    val value = (mask ushr column) and 1
                    columnOneCounts[column] += value
                    columnPatterns[column] = columnPatterns[column] or (value shl row)
                }
                search(row + 1)
                usedRows[mask] = false
                for (column in 0 until size) {
                    val value = (mask ushr column) and 1
                    columnOneCounts[column] -= value
                    columnPatterns[column] = columnPatterns[column] and (1 shl row).inv()
                }
                if (solutionCount >= limit) return
            }
        }

        search(0)
        return solutionCount
    }

    /** Returns whether the player's entries match the stored solution; empty cells are incomplete. */
    fun isSolved(puzzle: BinaryPuzzle, state: BinaryState): Boolean {
        require(state.puzzle == puzzle) { "Binary state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        return state.symbols.indices.all { index ->
            state.symbols[index].toDigit() == puzzle.solution[index]
        }
    }

    /** Returns incorrect non-empty symbols in row-major order; empty cells are not mistakes. */
    fun mistakes(puzzle: BinaryPuzzle, state: BinaryState): List<Cell> {
        require(state.puzzle == puzzle) { "Binary state belongs to a different puzzle" }
        return state.symbols.indices.filter { index ->
            val entered = state.symbols[index].toDigit()
            entered != null && entered != puzzle.solution[index]
        }.map(::Cell)
    }

    private fun rowMasks(size: Int, givens: List<Int?>, row: Int, half: Int): IntArray =
        (0 until (1 shl size)).filter { mask ->
            Integer.bitCount(mask) == half &&
                hasNoTriple(mask, size) &&
                (0 until size).all { column ->
                    val given = givens[row * size + column]
                    given == null || ((mask ushr column) and 1) == given
                }
        }.toIntArray()

    private fun hasNoTriple(mask: Int, size: Int): Boolean = (0 until size - 2).all { column ->
        val first = (mask ushr column) and 1
        val second = (mask ushr (column + 1)) and 1
        val third = (mask ushr (column + 2)) and 1
        first != second || second != third
    }

    private fun isValidLine(values: IntArray, size: Int): Boolean {
        if (values.count { it == 1 } != size / 2) return false
        for (index in 0 until size - 2) {
            if (values[index] == values[index + 1] && values[index + 1] == values[index + 2]) return false
        }
        return true
    }

    private fun lineToMask(values: IntArray): Int = values.mapIndexed { index, value -> value shl index }.sum()

    private fun bitAt(mask: Int, index: Int): Int = (mask ushr index) and 1
}
