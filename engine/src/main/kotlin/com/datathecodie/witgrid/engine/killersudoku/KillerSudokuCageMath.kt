package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import java.util.ArrayList

/** Precomputed 9x9 house geometry, multi-house regions, and exact cage-combination matching. */
internal object KillerSudokuCageMath {
    const val BOX_SIZE: Int = 3
    const val ALL_DIGITS_MASK: Int = KILLER_SUDOKU_NOTE_MASK
    const val HOUSE_SUM: Int = KILLER_SUDOKU_MAX_CAGE_SUM

    val CAGE_COMBINATIONS: Array<Array<IntArray>> = buildCageCombinations()

    val ROW_CELLS: List<List<Int>> = (0 until KILLER_SUDOKU_SIZE).map { row ->
        (0 until KILLER_SUDOKU_SIZE).map { column -> row * KILLER_SUDOKU_SIZE + column }
    }

    val COLUMN_CELLS: List<List<Int>> = (0 until KILLER_SUDOKU_SIZE).map { column ->
        (0 until KILLER_SUDOKU_SIZE).map { row -> row * KILLER_SUDOKU_SIZE + column }
    }

    val BOX_CELLS: List<List<Int>> = (0 until KILLER_SUDOKU_SIZE).map { boxIndex ->
        val boxRow = boxIndex / BOX_SIZE
        val boxColumn = boxIndex % BOX_SIZE
        buildList(KILLER_SUDOKU_SIZE) {
            for (rowOffset in 0 until BOX_SIZE) {
                for (columnOffset in 0 until BOX_SIZE) {
                    add((boxRow * BOX_SIZE + rowOffset) * KILLER_SUDOKU_SIZE + boxColumn * BOX_SIZE + columnOffset)
                }
            }
        }
    }

    val UNITS: List<KillerUnit> = buildList(27) {
        for (row in 0 until KILLER_SUDOKU_SIZE) {
            val cells = ROW_CELLS[row]
            add(KillerUnit(KillerUnitKind.ROW, "row ${row + 1}", cells, cells.toSet()))
        }
        for (column in 0 until KILLER_SUDOKU_SIZE) {
            val cells = COLUMN_CELLS[column]
            add(KillerUnit(KillerUnitKind.COLUMN, "column ${column + 1}", cells, cells.toSet()))
        }
        for (box in 0 until KILLER_SUDOKU_SIZE) {
            val cells = BOX_CELLS[box]
            add(KillerUnit(KillerUnitKind.BOX, "box ${box + 1}", cells, cells.toSet()))
        }
    }

    val LINE_UNITS: List<KillerUnit> = UNITS.filter { it.kind != KillerUnitKind.BOX }

    val MULTI_HOUSE_REGIONS: List<KillerHouseRegion> = buildMultiHouseRegions()

    private val MASKS_BY_POPCOUNT: Array<Array<IntArray>> = buildMasksByPopcount()

    fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    fun digitsInMask(mask: Int): List<Int> = KILLER_SUDOKU_DIGITS.filter { mask and digitBit(it) != 0 }

    fun boxIndex(row: Int, column: Int): Int = (row / BOX_SIZE) * BOX_SIZE + column / BOX_SIZE

    fun cellName(index: Int): String = "r${index / KILLER_SUDOKU_SIZE + 1}c${index % KILLER_SUDOKU_SIZE + 1}"

    fun cageLabel(cage: KillerCage): String =
        "cage ${cage.cells.joinToString("-") { cellName(it.index) }}"

    fun usedInRow(digits: List<Int?>, row: Int): Int =
        ROW_CELLS[row].fold(0) { mask, index ->
            val digit = digits[index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    fun usedInColumn(digits: List<Int?>, column: Int): Int =
        COLUMN_CELLS[column].fold(0) { mask, index ->
            val digit = digits[index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    fun usedInBox(digits: List<Int?>, row: Int, column: Int): Int =
        BOX_CELLS[boxIndex(row, column)].fold(0) { mask, index ->
            val digit = digits[index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    fun usedInCage(puzzle: KillerSudokuPuzzle, digits: List<Int?>, cellIndex: Int): Int =
        puzzle.cageOf(Cell(cellIndex)).cells.fold(0) { mask, cell ->
            val digit = digits[cell.index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    /**
     * Checks whether [comboMask] can be assigned bijectively to [cells] respecting [candidates].
     * For every feasible assignment, each cell's assignable digit bits are ORed into [perCellValid].
     */
    fun collectFeasibleCageAssignments(
        cells: List<Int>,
        candidates: IntArray,
        comboMask: Int,
        perCellValid: IntArray,
    ): Boolean {
        val count = cells.size
        require(Integer.bitCount(comboMask) == count) {
            "Combination size must match empty cell count"
        }
        val comboBits = IntArray(count)
        var remainingBits = comboMask
        var position = 0
        while (remainingBits != 0) {
            val bit = Integer.lowestOneBit(remainingBits)
            comboBits[position++] = bit
            remainingBits = remainingBits and bit.inv()
        }

        val cellAllowedSubmask = IntArray(count)
        for (cellOffset in 0 until count) {
            var submask = 0
            val cellCandidates = candidates[cells[cellOffset]]
            for (digitOffset in 0 until count) {
                if (cellCandidates and comboBits[digitOffset] != 0) {
                    submask = submask or (1 shl digitOffset)
                }
            }
            if (submask == 0) return false
            cellAllowedSubmask[cellOffset] = submask
        }

        if (count == 1) {
            perCellValid[0] = perCellValid[0] or comboBits[0]
            return true
        }
        if (count == 2) {
            val firstAllowed = cellAllowedSubmask[0]
            val secondAllowed = cellAllowedSubmask[1]
            var found = false
            if (firstAllowed and 1 != 0 && secondAllowed and 2 != 0) {
                perCellValid[0] = perCellValid[0] or comboBits[0]
                perCellValid[1] = perCellValid[1] or comboBits[1]
                found = true
            }
            if (firstAllowed and 2 != 0 && secondAllowed and 1 != 0) {
                perCellValid[0] = perCellValid[0] or comboBits[1]
                perCellValid[1] = perCellValid[1] or comboBits[0]
                found = true
            }
            return found
        }

        val stateCount = 1 shl count
        val fullMask = stateCount - 1
        val prefix = BooleanArray(stateCount)
        prefix[0] = true
        val popcountGroups = MASKS_BY_POPCOUNT[count]
        for (cellOffset in 0 until count) {
            val allowed = cellAllowedSubmask[cellOffset]
            for (mask in popcountGroups[cellOffset]) {
                if (!prefix[mask]) continue
                var available = allowed and mask.inv()
                while (available != 0) {
                    val bit = Integer.lowestOneBit(available)
                    prefix[mask or bit] = true
                    available = available and bit.inv()
                }
            }
        }
        if (!prefix[fullMask]) return false

        val suffix = BooleanArray(stateCount)
        suffix[fullMask] = true
        for (cellOffset in count - 1 downTo 0) {
            val allowed = cellAllowedSubmask[cellOffset]
            for (mask in popcountGroups[cellOffset]) {
                if (!prefix[mask]) continue
                var available = allowed and mask.inv()
                while (available != 0) {
                    val bit = Integer.lowestOneBit(available)
                    val nextMask = mask or bit
                    if (suffix[nextMask]) {
                        suffix[mask] = true
                        val digitOffset = Integer.numberOfTrailingZeros(bit)
                        perCellValid[cellOffset] = perCellValid[cellOffset] or comboBits[digitOffset]
                    }
                    available = available and bit.inv()
                }
            }
        }
        return suffix[0]
    }

    /** Returns whether all [cells] must contain pairwise distinct digits by sharing a house or cage. */
    fun areCellsDistinctByRules(puzzle: KillerSudokuPuzzle, cells: List<Int>): Boolean {
        if (cells.size <= 1) return true
        if (cells.map { it / KILLER_SUDOKU_SIZE }.distinct().size == 1) return true
        if (cells.map { it % KILLER_SUDOKU_SIZE }.distinct().size == 1) return true
        if (cells.map { boxIndex(it / KILLER_SUDOKU_SIZE, it % KILLER_SUDOKU_SIZE) }.distinct().size == 1) return true
        return cells.map { puzzle.cageIndexByCell[it] }.distinct().size == 1
    }

    /** Returns every single-house unit (row, column, or box) containing all of [cells]. */
    fun sharedUnits(cells: List<Int>): List<KillerUnit> {
        if (cells.isEmpty()) return emptyList()
        val result = ArrayList<KillerUnit>(2)
        val rows = cells.map { it / KILLER_SUDOKU_SIZE }.distinct()
        if (rows.size == 1) {
            result += UNITS[rows.single()]
        }
        val columns = cells.map { it % KILLER_SUDOKU_SIZE }.distinct()
        if (columns.size == 1) {
            result += UNITS[KILLER_SUDOKU_SIZE + columns.single()]
        }
        val boxes = cells.map { boxIndex(it / KILLER_SUDOKU_SIZE, it % KILLER_SUDOKU_SIZE) }.distinct()
        if (boxes.size == 1) {
            result += UNITS[2 * KILLER_SUDOKU_SIZE + boxes.single()]
        }
        return result
    }

    private fun buildMultiHouseRegions(): List<KillerHouseRegion> = buildList {
        for (houseCount in 2..4) {
            for (startRow in 0..KILLER_SUDOKU_SIZE - houseCount) {
                val rows = (startRow until startRow + houseCount)
                val cells = rows.flatMap { ROW_CELLS[it] }
                val label = if (houseCount == 2) {
                    "rows ${startRow + 1} and ${startRow + 2}"
                } else {
                    "rows ${startRow + 1} through ${startRow + houseCount}"
                }
                add(KillerHouseRegion(houseCount, label, cells, cells.toSet()))
            }
        }
        for (houseCount in 2..4) {
            for (startColumn in 0..KILLER_SUDOKU_SIZE - houseCount) {
                val columns = (startColumn until startColumn + houseCount)
                val cells = columns.flatMap { COLUMN_CELLS[it] }
                val label = if (houseCount == 2) {
                    "columns ${startColumn + 1} and ${startColumn + 2}"
                } else {
                    "columns ${startColumn + 1} through ${startColumn + houseCount}"
                }
                add(KillerHouseRegion(houseCount, label, cells, cells.toSet()))
            }
        }
        // Horizontal and vertical adjacent box pairs (3-box bands and stacks equal 3 rows/columns above).
        for (boxRow in 0 until BOX_SIZE) {
            for (boxColumn in 0 until BOX_SIZE - 1) {
                val first = boxRow * BOX_SIZE + boxColumn
                val second = first + 1
                val cells = BOX_CELLS[first] + BOX_CELLS[second]
                add(KillerHouseRegion(2, "boxes ${first + 1} and ${second + 1}", cells, cells.toSet()))
            }
        }
        for (boxRow in 0 until BOX_SIZE - 1) {
            for (boxColumn in 0 until BOX_SIZE) {
                val first = boxRow * BOX_SIZE + boxColumn
                val second = first + BOX_SIZE
                val cells = BOX_CELLS[first] + BOX_CELLS[second]
                add(KillerHouseRegion(2, "boxes ${first + 1} and ${second + 1}", cells, cells.toSet()))
            }
        }
        // 2x2 contiguous box quads.
        for (boxRow in 0 until BOX_SIZE - 1) {
            for (boxColumn in 0 until BOX_SIZE - 1) {
                val b0 = boxRow * BOX_SIZE + boxColumn
                val b1 = b0 + 1
                val b2 = b0 + BOX_SIZE
                val b3 = b2 + 1
                val cells = BOX_CELLS[b0] + BOX_CELLS[b1] + BOX_CELLS[b2] + BOX_CELLS[b3]
                add(
                    KillerHouseRegion(
                        4,
                        "boxes ${b0 + 1}, ${b1 + 1}, ${b2 + 1} and ${b3 + 1}",
                        cells,
                        cells.toSet(),
                    ),
                )
            }
        }
    }

    private fun buildCageCombinations(): Array<Array<IntArray>> {
        val buckets = Array(KILLER_SUDOKU_SIZE + 1) {
            Array(KILLER_SUDOKU_MAX_CAGE_SUM + 1) { ArrayList<Int>() }
        }
        for (mask in 0..ALL_DIGITS_MASK) {
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

    private fun buildMasksByPopcount(): Array<Array<IntArray>> =
        Array(KILLER_SUDOKU_SIZE + 1) { count ->
            if (count < 3) {
                emptyArray()
            } else {
                val groups = Array(count + 1) { ArrayList<Int>() }
                for (mask in 0 until (1 shl count)) {
                    groups[Integer.bitCount(mask)].add(mask)
                }
                Array(count + 1) { popcount -> groups[popcount].toIntArray() }
            }
        }
}

internal enum class KillerUnitKind {
    ROW,
    COLUMN,
    BOX,
}

internal data class KillerUnit(
    val kind: KillerUnitKind,
    val label: String,
    val cells: List<Int>,
    val cellSet: Set<Int>,
)

internal data class KillerHouseRegion(
    val houseCount: Int,
    val label: String,
    val cells: List<Int>,
    val cellSet: Set<Int>,
)
