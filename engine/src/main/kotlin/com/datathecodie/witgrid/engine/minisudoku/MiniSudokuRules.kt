package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Cell

/** Mini Sudoku grid validation, player feedback and capped solution counting. */
object MiniSudokuRules {
    /** Returns whether [values] has digits 1 through 6 once in every row, column and 2x3 box. */
    fun isValidSolution(puzzle: MiniSudokuPuzzle, values: List<Int> = puzzle.solution): Boolean {
        if (values.size != MINI_SUDOKU_CELL_COUNT || values.any { it !in MINI_SUDOKU_DIGITS }) {
            return false
        }
        for (row in 0 until MINI_SUDOKU_SIZE) {
            if (!hasAllDigits((0 until MINI_SUDOKU_SIZE).map { column -> values[row * MINI_SUDOKU_SIZE + column] })) {
                return false
            }
        }
        for (column in 0 until MINI_SUDOKU_SIZE) {
            if (!hasAllDigits((0 until MINI_SUDOKU_SIZE).map { row -> values[row * MINI_SUDOKU_SIZE + column] })) {
                return false
            }
        }
        for (boxRow in 0 until MINI_SUDOKU_SIZE / 2) {
            for (boxColumn in 0 until MINI_SUDOKU_SIZE / 3) {
                val digits = buildList(6) {
                    for (rowOffset in 0 until 2) {
                        for (columnOffset in 0 until 3) {
                            val row = boxRow * 2 + rowOffset
                            val column = boxColumn * 3 + columnOffset
                            add(values[row * MINI_SUDOKU_SIZE + column])
                        }
                    }
                }
                if (!hasAllDigits(digits)) return false
            }
        }
        return true
    }

    /** Counts boards compatible with the givens, stopping as soon as [limit] solutions are found. */
    fun countSolutions(puzzle: MiniSudokuPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val values = IntArray(MINI_SUDOKU_CELL_COUNT)
        val rowUsed = IntArray(MINI_SUDOKU_SIZE)
        val columnUsed = IntArray(MINI_SUDOKU_SIZE)
        val boxUsed = IntArray(MINI_SUDOKU_SIZE)
        for (index in puzzle.givens.indices) {
            val given = puzzle.givens[index] ?: continue
            val bit = digitBit(given)
            val row = index / MINI_SUDOKU_SIZE
            val column = index % MINI_SUDOKU_SIZE
            val box = boxIndex(row, column)
            if (rowUsed[row] and bit != 0 || columnUsed[column] and bit != 0 || boxUsed[box] and bit != 0) {
                return 0
            }
            values[index] = given
            rowUsed[row] = rowUsed[row] or bit
            columnUsed[column] = columnUsed[column] or bit
            boxUsed[box] = boxUsed[box] or bit
        }

        var solutionCount = 0
        fun search() {
            if (solutionCount >= limit) return
            var bestCell = -1
            var bestCandidates = 0
            var bestCount = MINI_SUDOKU_SIZE + 1
            for (index in values.indices) {
                if (values[index] != 0) continue
                val row = index / MINI_SUDOKU_SIZE
                val column = index % MINI_SUDOKU_SIZE
                val used = rowUsed[row] or columnUsed[column] or boxUsed[boxIndex(row, column)]
                val candidates = MINI_SUDOKU_NOTE_MASK and used.inv()
                val candidateCount = Integer.bitCount(candidates)
                if (candidateCount == 0) return
                if (candidateCount < bestCount) {
                    bestCell = index
                    bestCandidates = candidates
                    bestCount = candidateCount
                    if (candidateCount == 1) break
                }
            }
            if (bestCell == -1) {
                solutionCount++
                return
            }

            val row = bestCell / MINI_SUDOKU_SIZE
            val column = bestCell % MINI_SUDOKU_SIZE
            val box = boxIndex(row, column)
            var candidates = bestCandidates
            while (candidates != 0 && solutionCount < limit) {
                val bit = Integer.lowestOneBit(candidates)
                val digit = Integer.numberOfTrailingZeros(bit) + 1
                values[bestCell] = digit
                rowUsed[row] = rowUsed[row] or bit
                columnUsed[column] = columnUsed[column] or bit
                boxUsed[box] = boxUsed[box] or bit
                search()
                values[bestCell] = 0
                rowUsed[row] = rowUsed[row] and bit.inv()
                columnUsed[column] = columnUsed[column] and bit.inv()
                boxUsed[box] = boxUsed[box] and bit.inv()
                candidates = candidates and bit.inv()
            }
        }

        search()
        return solutionCount
    }

    /** Returns whether every digit matches the solution; empty cells are incomplete. */
    fun isSolved(puzzle: MiniSudokuPuzzle, state: MiniSudokuState): Boolean {
        require(state.puzzle == puzzle) { "Mini Sudoku state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        return state.digits.indices.all { index -> state.digits[index] == puzzle.solution[index] }
    }

    /** Returns incorrect entered digits in row-major order; empty cells and notes are not mistakes. */
    fun mistakes(puzzle: MiniSudokuPuzzle, state: MiniSudokuState): List<Cell> {
        require(state.puzzle == puzzle) { "Mini Sudoku state belongs to a different puzzle" }
        return state.digits.indices.filter { index ->
            val entered = state.digits[index]
            entered != null && entered != puzzle.solution[index]
        }.map(::Cell)
    }

    private fun hasAllDigits(values: List<Int>): Boolean {
        if (values.size != MINI_SUDOKU_SIZE || values.any { it !in MINI_SUDOKU_DIGITS }) return false
        var mask = 0
        values.forEach { value ->
            val bit = digitBit(value)
            if (mask and bit != 0) return false
            mask = mask or bit
        }
        return mask == MINI_SUDOKU_NOTE_MASK
    }

    private fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    private fun boxIndex(row: Int, column: Int): Int = (row / 2) * (MINI_SUDOKU_SIZE / 3) + column / 3
}
