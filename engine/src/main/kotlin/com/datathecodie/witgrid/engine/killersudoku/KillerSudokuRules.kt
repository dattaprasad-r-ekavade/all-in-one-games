package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell

/** Killer Sudoku grid and cage validation, player feedback, and capped solution counting. */
object KillerSudokuRules {
    private const val BOX_SIZE = 3
    private val CAGE_COMBINATIONS: Array<Array<IntArray>> = buildCageCombinations()

    /**
     * Returns whether [values] satisfies all Killer Sudoku rules for [puzzle]:
     * digits 1 through 9 once in every row, column, and 3x3 box, matching any givens,
     * with distinct digits summing to each cage's target sum.
     */
    fun isValidSolution(puzzle: KillerSudokuPuzzle, values: List<Int> = puzzle.solution): Boolean {
        if (values.size != KILLER_SUDOKU_CELL_COUNT || values.any { it !in KILLER_SUDOKU_DIGITS }) {
            return false
        }
        for (index in values.indices) {
            val given = puzzle.givens[index]
            if (given != null && values[index] != given) return false
        }
        for (row in 0 until KILLER_SUDOKU_SIZE) {
            if (!hasAllDigits((0 until KILLER_SUDOKU_SIZE).map { column -> values[row * KILLER_SUDOKU_SIZE + column] })) {
                return false
            }
        }
        for (column in 0 until KILLER_SUDOKU_SIZE) {
            if (!hasAllDigits((0 until KILLER_SUDOKU_SIZE).map { row -> values[row * KILLER_SUDOKU_SIZE + column] })) {
                return false
            }
        }
        for (boxRow in 0 until BOX_SIZE) {
            for (boxColumn in 0 until BOX_SIZE) {
                val boxDigits = buildList(KILLER_SUDOKU_SIZE) {
                    for (rowOffset in 0 until BOX_SIZE) {
                        for (columnOffset in 0 until BOX_SIZE) {
                            val row = boxRow * BOX_SIZE + rowOffset
                            val column = boxColumn * BOX_SIZE + columnOffset
                            add(values[row * KILLER_SUDOKU_SIZE + column])
                        }
                    }
                }
                if (!hasAllDigits(boxDigits)) return false
            }
        }
        for (cage in puzzle.cages) {
            var usedMask = 0
            var total = 0
            for (cell in cage.cells) {
                val digit = values[cell.index]
                val bit = digitBit(digit)
                if (usedMask and bit != 0) return false
                usedMask = usedMask or bit
                total += digit
            }
            if (total != cage.sum) return false
        }
        return true
    }

    /**
     * Independently counts boards compatible with [puzzle]'s givens, Sudoku constraints,
     * cage uniqueness, and cage sums, stopping as soon as [limit] solutions are found.
     */
    fun countSolutions(puzzle: KillerSudokuPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val cages = puzzle.cages
        val cageCount = cages.size
        val cellToCage = puzzle.cageIndexByCell
        val values = IntArray(KILLER_SUDOKU_CELL_COUNT)
        val rowUsed = IntArray(KILLER_SUDOKU_SIZE)
        val columnUsed = IntArray(KILLER_SUDOKU_SIZE)
        val boxUsed = IntArray(KILLER_SUDOKU_SIZE)
        val cageUsed = IntArray(cageCount)
        val cageEmptyCount = IntArray(cageCount) { cages[it].cells.size }
        val cageRemainingSum = IntArray(cageCount) { cages[it].sum }

        for (index in puzzle.givens.indices) {
            val given = puzzle.givens[index] ?: continue
            val bit = digitBit(given)
            val row = index / KILLER_SUDOKU_SIZE
            val column = index % KILLER_SUDOKU_SIZE
            val box = boxIndex(row, column)
            val cageIndex = cellToCage[index]
            if (rowUsed[row] and bit != 0 ||
                columnUsed[column] and bit != 0 ||
                boxUsed[box] and bit != 0 ||
                cageUsed[cageIndex] and bit != 0
            ) {
                return 0
            }
            values[index] = given
            rowUsed[row] = rowUsed[row] or bit
            columnUsed[column] = columnUsed[column] or bit
            boxUsed[box] = boxUsed[box] or bit
            cageUsed[cageIndex] = cageUsed[cageIndex] or bit
            cageEmptyCount[cageIndex]--
            cageRemainingSum[cageIndex] -= given
            if (cageRemainingSum[cageIndex] < 0) return 0
        }

        for (cageIndex in 0 until cageCount) {
            val remainingCount = cageEmptyCount[cageIndex]
            val remainingSum = cageRemainingSum[cageIndex]
            if (remainingCount == 0) {
                if (remainingSum != 0) return 0
            } else if (remainingSum !in 1..KILLER_SUDOKU_MAX_CAGE_SUM ||
                CAGE_COMBINATIONS[remainingCount][remainingSum].isEmpty()
            ) {
                return 0
            }
        }

        val trailCells = IntArray(KILLER_SUDOKU_CELL_COUNT)
        var trailTop = 0
        var solutionCount = 0

        fun assign(cellIndex: Int, digit: Int): Boolean {
            val bit = digitBit(digit)
            val row = cellIndex / KILLER_SUDOKU_SIZE
            val column = cellIndex % KILLER_SUDOKU_SIZE
            val box = boxIndex(row, column)
            val cageIndex = cellToCage[cellIndex]
            if (rowUsed[row] and bit != 0 ||
                columnUsed[column] and bit != 0 ||
                boxUsed[box] and bit != 0 ||
                cageUsed[cageIndex] and bit != 0
            ) {
                return false
            }
            val nextRemainingSum = cageRemainingSum[cageIndex] - digit
            val nextRemainingCount = cageEmptyCount[cageIndex] - 1
            if (nextRemainingSum < 0) return false
            if (nextRemainingCount == 0) {
                if (nextRemainingSum != 0) return false
            } else if (nextRemainingSum !in 1..KILLER_SUDOKU_MAX_CAGE_SUM ||
                CAGE_COMBINATIONS[nextRemainingCount][nextRemainingSum].isEmpty()
            ) {
                return false
            }
            values[cellIndex] = digit
            rowUsed[row] = rowUsed[row] or bit
            columnUsed[column] = columnUsed[column] or bit
            boxUsed[box] = boxUsed[box] or bit
            cageUsed[cageIndex] = cageUsed[cageIndex] or bit
            cageEmptyCount[cageIndex] = nextRemainingCount
            cageRemainingSum[cageIndex] = nextRemainingSum
            trailCells[trailTop++] = cellIndex
            return true
        }

        fun undoTo(savedTop: Int) {
            while (trailTop > savedTop) {
                val cellIndex = trailCells[--trailTop]
                val digit = values[cellIndex]
                val bit = digitBit(digit)
                val row = cellIndex / KILLER_SUDOKU_SIZE
                val column = cellIndex % KILLER_SUDOKU_SIZE
                val box = boxIndex(row, column)
                val cageIndex = cellToCage[cellIndex]
                values[cellIndex] = 0
                rowUsed[row] = rowUsed[row] and bit.inv()
                columnUsed[column] = columnUsed[column] and bit.inv()
                boxUsed[box] = boxUsed[box] and bit.inv()
                cageUsed[cageIndex] = cageUsed[cageIndex] and bit.inv()
                cageEmptyCount[cageIndex]++
                cageRemainingSum[cageIndex] += digit
            }
        }

        fun removeCandidateBit(candidates: IntArray, cellIndex: Int, bit: Int): Boolean {
            if (values[cellIndex] != 0) return true
            val updated = candidates[cellIndex] and bit.inv()
            if (updated == 0) return false
            candidates[cellIndex] = updated
            return true
        }

        fun assignAndUpdateCandidates(candidates: IntArray, cellIndex: Int, digit: Int): Boolean {
            if (!assign(cellIndex, digit)) return false
            val bit = digitBit(digit)
            candidates[cellIndex] = 0
            val row = cellIndex / KILLER_SUDOKU_SIZE
            val column = cellIndex % KILLER_SUDOKU_SIZE
            for (otherColumn in 0 until KILLER_SUDOKU_SIZE) {
                if (!removeCandidateBit(candidates, row * KILLER_SUDOKU_SIZE + otherColumn, bit)) return false
            }
            for (otherRow in 0 until KILLER_SUDOKU_SIZE) {
                if (!removeCandidateBit(candidates, otherRow * KILLER_SUDOKU_SIZE + column, bit)) return false
            }
            val boxRowStart = (row / BOX_SIZE) * BOX_SIZE
            val boxColumnStart = (column / BOX_SIZE) * BOX_SIZE
            for (rowOffset in 0 until BOX_SIZE) {
                for (columnOffset in 0 until BOX_SIZE) {
                    val peer = (boxRowStart + rowOffset) * KILLER_SUDOKU_SIZE + boxColumnStart + columnOffset
                    if (!removeCandidateBit(candidates, peer, bit)) return false
                }
            }
            for (peer in cages[cellToCage[cellIndex]].cells) {
                if (!removeCandidateBit(candidates, peer.index, bit)) return false
            }
            return true
        }

        fun propagateHouseHiddenSingles(
            candidates: IntArray,
            usedMask: Int,
            houseCells: IntArray,
        ): Int {
            var changed = 0
            for (digit in KILLER_SUDOKU_DIGITS) {
                val bit = digitBit(digit)
                if (usedMask and bit != 0) continue
                var count = 0
                var targetCell = -1
                for (cellIndex in houseCells) {
                    if (values[cellIndex] == 0 && candidates[cellIndex] and bit != 0) {
                        count++
                        targetCell = cellIndex
                        if (count > 1) break
                    }
                }
                if (count == 0) return -1
                if (count == 1) {
                    if (!assignAndUpdateCandidates(candidates, targetCell, digit)) return -1
                    changed = 1
                }
            }
            return changed
        }

        fun propagate(candidates: IntArray): Boolean {
            while (true) {
                var changed = false

                // Assign naked singles.
                for (index in 0 until KILLER_SUDOKU_CELL_COUNT) {
                    if (values[index] != 0) continue
                    val mask = candidates[index]
                    if (mask == 0) return false
                    if (Integer.bitCount(mask) == 1) {
                        val digit = Integer.numberOfTrailingZeros(mask) + 1
                        if (!assignAndUpdateCandidates(candidates, index, digit)) return false
                        changed = true
                    }
                }

                // Prune by feasible cage digit combinations and locked cage candidates.
                for (cageIndex in 0 until cageCount) {
                    val remainingCount = cageEmptyCount[cageIndex]
                    val remainingSum = cageRemainingSum[cageIndex]
                    if (remainingCount == 0) {
                        if (remainingSum != 0) return false
                        continue
                    }
                    if (remainingSum !in 1..KILLER_SUDOKU_MAX_CAGE_SUM) return false
                    val combinations = CAGE_COMBINATIONS[remainingCount][remainingSum]
                    if (combinations.isEmpty()) return false

                    var availableMask = 0
                    var commonRow = -1
                    var commonColumn = -1
                    var commonBox = -1
                    var sameRow = true
                    var sameColumn = true
                    var sameBox = true

                    for (cell in cages[cageIndex].cells) {
                        val cellIndex = cell.index
                        if (values[cellIndex] != 0) continue
                        availableMask = availableMask or candidates[cellIndex]
                        val row = cellIndex / KILLER_SUDOKU_SIZE
                        val column = cellIndex % KILLER_SUDOKU_SIZE
                        val box = boxIndex(row, column)
                        if (commonRow == -1) {
                            commonRow = row
                            commonColumn = column
                            commonBox = box
                        } else {
                            if (row != commonRow) sameRow = false
                            if (column != commonColumn) sameColumn = false
                            if (box != commonBox) sameBox = false
                        }
                    }

                    var validUnion = 0
                    var validIntersection = KILLER_SUDOKU_NOTE_MASK
                    val usedInCage = cageUsed[cageIndex]
                    for (combination in combinations) {
                        if (combination and usedInCage == 0 && combination and availableMask == combination) {
                            validUnion = validUnion or combination
                            validIntersection = validIntersection and combination
                        }
                    }
                    if (validUnion == 0) return false

                    for (cell in cages[cageIndex].cells) {
                        val cellIndex = cell.index
                        if (values[cellIndex] != 0) continue
                        val narrowed = candidates[cellIndex] and validUnion
                        if (narrowed == 0) return false
                        if (narrowed != candidates[cellIndex]) {
                            candidates[cellIndex] = narrowed
                            changed = true
                        }
                    }

                    if (validIntersection != 0) {
                        if (sameRow && commonRow != -1) {
                            for (column in 0 until KILLER_SUDOKU_SIZE) {
                                val cellIndex = commonRow * KILLER_SUDOKU_SIZE + column
                                if (cellToCage[cellIndex] != cageIndex && values[cellIndex] == 0) {
                                    val narrowed = candidates[cellIndex] and validIntersection.inv()
                                    if (narrowed == 0) return false
                                    if (narrowed != candidates[cellIndex]) {
                                        candidates[cellIndex] = narrowed
                                        changed = true
                                    }
                                }
                            }
                        }
                        if (sameColumn && commonColumn != -1) {
                            for (row in 0 until KILLER_SUDOKU_SIZE) {
                                val cellIndex = row * KILLER_SUDOKU_SIZE + commonColumn
                                if (cellToCage[cellIndex] != cageIndex && values[cellIndex] == 0) {
                                    val narrowed = candidates[cellIndex] and validIntersection.inv()
                                    if (narrowed == 0) return false
                                    if (narrowed != candidates[cellIndex]) {
                                        candidates[cellIndex] = narrowed
                                        changed = true
                                    }
                                }
                            }
                        }
                        if (sameBox && commonBox != -1) {
                            for (cellIndex in BOX_CELLS[commonBox]) {
                                if (cellToCage[cellIndex] != cageIndex && values[cellIndex] == 0) {
                                    val narrowed = candidates[cellIndex] and validIntersection.inv()
                                    if (narrowed == 0) return false
                                    if (narrowed != candidates[cellIndex]) {
                                        candidates[cellIndex] = narrowed
                                        changed = true
                                    }
                                }
                            }
                        }
                    }
                }

                // Assign hidden singles in rows, columns, and boxes.
                for (row in 0 until KILLER_SUDOKU_SIZE) {
                    val status = propagateHouseHiddenSingles(candidates, rowUsed[row], ROW_CELLS[row])
                    if (status < 0) return false
                    if (status > 0) changed = true
                }
                for (column in 0 until KILLER_SUDOKU_SIZE) {
                    val status = propagateHouseHiddenSingles(candidates, columnUsed[column], COLUMN_CELLS[column])
                    if (status < 0) return false
                    if (status > 0) changed = true
                }
                for (box in 0 until KILLER_SUDOKU_SIZE) {
                    val status = propagateHouseHiddenSingles(candidates, boxUsed[box], BOX_CELLS[box])
                    if (status < 0) return false
                    if (status > 0) changed = true
                }

                if (!changed) return true
            }
        }

        fun search() {
            if (solutionCount >= limit) return
            val savedTop = trailTop
            val candidates = IntArray(KILLER_SUDOKU_CELL_COUNT)
            for (index in 0 until KILLER_SUDOKU_CELL_COUNT) {
                if (values[index] != 0) continue
                val row = index / KILLER_SUDOKU_SIZE
                val column = index % KILLER_SUDOKU_SIZE
                val box = boxIndex(row, column)
                val cageIndex = cellToCage[index]
                val used = rowUsed[row] or columnUsed[column] or boxUsed[box] or cageUsed[cageIndex]
                val mask = KILLER_SUDOKU_NOTE_MASK and used.inv()
                if (mask == 0) {
                    undoTo(savedTop)
                    return
                }
                candidates[index] = mask
            }

            if (!propagate(candidates)) {
                undoTo(savedTop)
                return
            }

            var bestCell = -1
            var bestCount = KILLER_SUDOKU_SIZE + 1
            for (index in 0 until KILLER_SUDOKU_CELL_COUNT) {
                if (values[index] != 0) continue
                val count = Integer.bitCount(candidates[index])
                if (count < bestCount) {
                    bestCount = count
                    bestCell = index
                    if (count == 2) break
                }
            }

            if (bestCell == -1) {
                for (cageIndex in 0 until cageCount) {
                    if (cageEmptyCount[cageIndex] != 0 || cageRemainingSum[cageIndex] != 0) {
                        undoTo(savedTop)
                        return
                    }
                }
                solutionCount++
                undoTo(savedTop)
                return
            }

            val branchTop = trailTop
            var remaining = candidates[bestCell]
            while (remaining != 0 && solutionCount < limit) {
                val bit = Integer.lowestOneBit(remaining)
                val digit = Integer.numberOfTrailingZeros(bit) + 1
                remaining = remaining and bit.inv()
                if (assign(bestCell, digit)) {
                    search()
                    undoTo(branchTop)
                }
            }
            undoTo(savedTop)
        }

        search()
        return solutionCount
    }

    /** Returns whether every entered digit matches the stored valid solution; empty cells are incomplete. */
    fun isSolved(puzzle: KillerSudokuPuzzle, state: KillerSudokuState): Boolean {
        require(state.puzzle == puzzle) { "Killer Sudoku state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        return state.digits.indices.all { index -> state.digits[index] == puzzle.solution[index] }
    }

    /**
     * Returns cells in row-major order where the player entered a digit that is not the stored
     * solution digit. Empty cells and pencil notes are never counted as mistakes.
     */
    fun mistakes(puzzle: KillerSudokuPuzzle, state: KillerSudokuState): List<Cell> {
        require(state.puzzle == puzzle) { "Killer Sudoku state belongs to a different puzzle" }
        return state.digits.indices.filter { index ->
            val entered = state.digits[index]
            entered != null && entered != puzzle.solution[index]
        }.map(::Cell)
    }

    private fun hasAllDigits(values: List<Int>): Boolean {
        if (values.size != KILLER_SUDOKU_SIZE || values.any { it !in KILLER_SUDOKU_DIGITS }) return false
        var mask = 0
        values.forEach { value ->
            val bit = digitBit(value)
            if (mask and bit != 0) return false
            mask = mask or bit
        }
        return mask == KILLER_SUDOKU_NOTE_MASK
    }

    private fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    private fun boxIndex(row: Int, column: Int): Int = (row / BOX_SIZE) * BOX_SIZE + column / BOX_SIZE

    private val ROW_CELLS: Array<IntArray> = Array(KILLER_SUDOKU_SIZE) { row ->
        IntArray(KILLER_SUDOKU_SIZE) { column -> row * KILLER_SUDOKU_SIZE + column }
    }

    private val COLUMN_CELLS: Array<IntArray> = Array(KILLER_SUDOKU_SIZE) { column ->
        IntArray(KILLER_SUDOKU_SIZE) { row -> row * KILLER_SUDOKU_SIZE + column }
    }

    private val BOX_CELLS: Array<IntArray> = Array(KILLER_SUDOKU_SIZE) { box ->
        val boxRow = box / BOX_SIZE
        val boxColumn = box % BOX_SIZE
        IntArray(KILLER_SUDOKU_SIZE) { offset ->
            val row = boxRow * BOX_SIZE + offset / BOX_SIZE
            val column = boxColumn * BOX_SIZE + offset % BOX_SIZE
            row * KILLER_SUDOKU_SIZE + column
        }
    }

    private fun buildCageCombinations(): Array<Array<IntArray>> {
        val buckets = Array(KILLER_SUDOKU_SIZE + 1) {
            Array(KILLER_SUDOKU_MAX_CAGE_SUM + 1) { ArrayList<Int>() }
        }
        for (mask in 0..KILLER_SUDOKU_NOTE_MASK) {
            val count = Integer.bitCount(mask)
            var sum = 0
            var bits = mask
            while (bits != 0) {
                val bit = Integer.lowestOneBit(bits)
                sum += Integer.numberOfTrailingZeros(bit) + 1
                bits = bits and bit.inv()
            }
            buckets[count][sum].add(mask)
        }
        return Array(KILLER_SUDOKU_SIZE + 1) { count ->
            Array(KILLER_SUDOKU_MAX_CAGE_SUM + 1) { sum ->
                buckets[count][sum].toIntArray()
            }
        }
    }
}
