package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Deterministic Killer Sudoku generator.
 *
 * Each candidate starts from a random solved 9x9 grid partitioned into orthogonally
 * connected cages of 1 to 5 distinct digits whose sum matches the solution.
 * Easy keeps zero givens and peels cages until cage combinations alone solve the board.
 * Medium, Hard, and Expert grow loose cages, absorb isolated singletons where possible,
 * and remove givens down to a minimal set while preserving uniqueness and the pack ceiling.
 */
object KillerSudokuGenerator {
    private const val MAX_ATTEMPTS = 128
    private const val MAX_EASY_SINGLETONS = 28
    private const val MAX_GIVENS = 20

    private val NEIGHBORS: Array<IntArray> = Array(KILLER_SUDOKU_CELL_COUNT) { index ->
        val row = index / KILLER_SUDOKU_SIZE
        val column = index % KILLER_SUDOKU_SIZE
        val values = ArrayList<Int>(4)
        if (row > 0) values.add(index - KILLER_SUDOKU_SIZE)
        if (row < KILLER_SUDOKU_SIZE - 1) values.add(index + KILLER_SUDOKU_SIZE)
        if (column > 0) values.add(index - 1)
        if (column < KILLER_SUDOKU_SIZE - 1) values.add(index + 1)
        values.toIntArray()
    }

    /** Builds one unique, technique-graded puzzle in [pack]. Randomness comes only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): KillerSudokuPuzzle {
        repeat(MAX_ATTEMPTS) {
            val solution = randomValidGrid(rng)
            val puzzle = when (pack) {
                Pack.EASY -> buildEasy(solution, rng)
                Pack.MEDIUM, Pack.HARD, Pack.EXPERT -> buildWithGivenRemoval(solution, rng, pack)
            }
            if (puzzle != null && KillerSudokuRules.countSolutions(puzzle, limit = 2) == 1) {
                return puzzle
            }
        }
        throw IllegalStateException(
            "Could not generate a ${pack.name} Killer Sudoku puzzle in $MAX_ATTEMPTS attempts",
        )
    }

    private fun buildEasy(solution: IntArray, rng: SplitMix64): KillerSudokuPuzzle? {
        val groups = grow(solution, rng, preferLoose = false) { easyTargetSize(it) }
        val givens = noGivens()
        val ceiling = maxRank(Pack.EASY)
        for (step in 0 until KILLER_SUDOKU_CELL_COUNT) {
            if (groups.count { it.size == 1 } > MAX_EASY_SINGLETONS) return null
            val candidatePuzzle = candidate(groups, givens, solution, Pack.EASY)
            val grade = tryGrade(candidatePuzzle, ceiling)
            val mixed = groups.any { it.size > 1 }
            if (grade != null && mixed && KillerSudokuPackBands.isGradeInPackBand(Pack.EASY, grade)) {
                return finish(groups, givens, solution, Pack.EASY, grade, rng)
            }
            if (!peelOne(groups, rng)) break
        }
        return null
    }

    private fun buildWithGivenRemoval(
        solution: IntArray,
        rng: SplitMix64,
        pack: Pack,
    ): KillerSudokuPuzzle? {
        val groups = grow(solution, rng, preferLoose = true) { 3 + it.nextInt(3) }
        mergeSingletons(groups, solution)
        return removeGivens(solution, rng, pack, groups)
    }

    /**
     * Removes as many givens as possible while keeping uniqueness and staying within the
     * pack's top technique rank, accepting only when the minimal board lands in the band.
     */
    private fun removeGivens(
        solution: IntArray,
        rng: SplitMix64,
        pack: Pack,
        groups: List<List<Int>>,
    ): KillerSudokuPuzzle? {
        val givens = ArrayList<Int?>(KILLER_SUDOKU_CELL_COUNT)
        for (digit in solution) givens.add(digit)
        val order = (0 until KILLER_SUDOKU_CELL_COUNT).toMutableList()
        shuffle(order, rng)
        val ceiling = maxRank(pack)
        var lastGrade: Grade? = null

        for (index in order) {
            val saved = givens[index] ?: continue
            givens[index] = null
            val candidatePuzzle = candidate(groups, givens, solution, pack)
            if (KillerSudokuRules.countSolutions(candidatePuzzle, limit = 2) != 1) {
                givens[index] = saved
                continue
            }
            val grade = tryGrade(candidatePuzzle, ceiling)
            val rank = grade?.hardestTechnique?.difficultyRank ?: -1
            if (grade == null || grade.stepCount == 0 || rank > ceiling) {
                givens[index] = saved
                continue
            }
            lastGrade = grade
        }

        val finalGrade = lastGrade ?: return null
        if (givens.count { it != null } > MAX_GIVENS) return null
        if (!KillerSudokuPackBands.isGradeInPackBand(pack, finalGrade)) return null
        return finish(groups, givens, solution, pack, finalGrade, rng)
    }

    private fun grow(
        solution: IntArray,
        rng: SplitMix64,
        preferLoose: Boolean,
        targetSize: (SplitMix64) -> Int,
    ): MutableList<MutableList<Int>> {
        val unassigned = BooleanArray(KILLER_SUDOKU_CELL_COUNT) { true }
        val order = ArrayList<Int>(KILLER_SUDOKU_CELL_COUNT)
        for (index in 0 until KILLER_SUDOKU_CELL_COUNT) order.add(index)
        shuffle(order, rng)
        val groups = ArrayList<MutableList<Int>>()
        for (seed in order) {
            if (!unassigned[seed]) continue
            val group = ArrayList<Int>(5)
            group.add(seed)
            unassigned[seed] = false
            var usedMask = digitBit(solution[seed])
            val target = targetSize(rng).coerceIn(1, 5)
            while (group.size < target) {
                val options = openNeighbors(group, unassigned, usedMask, solution)
                if (options.isEmpty()) break
                val pick = if (preferLoose) loosest(group, options, solution) else options[rng.nextInt(options.size)]
                group.add(pick)
                unassigned[pick] = false
                usedMask = usedMask or digitBit(solution[pick])
            }
            groups.add(group)
        }
        return groups
    }

    /** Absorbs isolated 1-cell leftovers into an adjacent cage below size 5 without repeating digits. */
    private fun mergeSingletons(groups: MutableList<MutableList<Int>>, solution: IntArray) {
        val cellToGroup = IntArray(KILLER_SUDOKU_CELL_COUNT)
        for (groupIndex in groups.indices) {
            for (cell in groups[groupIndex]) cellToGroup[cell] = groupIndex
        }
        for (groupIndex in groups.indices) {
            if (groups[groupIndex].size != 1) continue
            val cell = groups[groupIndex][0]
            val bit = digitBit(solution[cell])
            var bestTarget = -1
            var bestSize = 6
            for (neighbor in NEIGHBORS[cell]) {
                val targetIndex = cellToGroup[neighbor]
                if (targetIndex == groupIndex) continue
                val targetGroup = groups[targetIndex]
                if (targetGroup.size >= 5) continue
                var usedMask = 0
                for (member in targetGroup) usedMask = usedMask or digitBit(solution[member])
                if (usedMask and bit != 0) continue
                if (targetGroup.size < bestSize) {
                    bestSize = targetGroup.size
                    bestTarget = targetIndex
                }
            }
            if (bestTarget != -1) {
                groups[bestTarget].add(cell)
                groups[groupIndex].clear()
                cellToGroup[cell] = bestTarget
            }
        }
        groups.removeAll { it.isEmpty() }
    }

    private fun openNeighbors(
        group: List<Int>,
        unassigned: BooleanArray,
        usedMask: Int,
        solution: IntArray,
    ): List<Int> {
        val seen = BooleanArray(KILLER_SUDOKU_CELL_COUNT)
        val options = ArrayList<Int>(8)
        for (cell in group) {
            for (neighbor in NEIGHBORS[cell]) {
                if (!unassigned[neighbor] || seen[neighbor]) continue
                if (usedMask and digitBit(solution[neighbor]) != 0) continue
                seen[neighbor] = true
                options.add(neighbor)
            }
        }
        return options
    }

    private fun loosest(group: List<Int>, options: List<Int>, solution: IntArray): Int {
        var best = options[0]
        var bestScore = looseScore(group, best, solution)
        for (index in 1 until options.size) {
            val candidate = options[index]
            val score = looseScore(group, candidate, solution)
            if (score > bestScore) {
                best = candidate
                bestScore = score
            }
        }
        return best
    }

    private fun looseScore(group: List<Int>, extra: Int, solution: IntArray): Int {
        var sum = solution[extra]
        for (cell in group) sum += solution[cell]
        val size = group.size + 1
        if (size !in KILLER_SUDOKU_DIGITS || sum !in 1..KILLER_SUDOKU_MAX_CAGE_SUM) return 0
        return KillerSudokuCageMath.CAGE_COMBINATIONS[size][sum].size
    }

    private fun peelOne(groups: MutableList<MutableList<Int>>, rng: SplitMix64): Boolean {
        val multi = ArrayList<Int>()
        for (index in groups.indices) {
            if (groups[index].size > 1) multi.add(index)
        }
        if (multi.isEmpty()) return false
        shuffle(multi, rng)
        for (groupIndex in multi) {
            val group = groups[groupIndex]
            val removable = ArrayList<Int>()
            for (cell in group) {
                val rest = ArrayList<Int>(group.size - 1)
                for (other in group) {
                    if (other != cell) rest.add(other)
                }
                if (isConnected(rest)) removable.add(cell)
            }
            if (removable.isEmpty()) continue
            val cell = removable[rng.nextInt(removable.size)]
            removeCell(group, cell)
            groups.add(mutableListOf(cell))
            return true
        }
        return false
    }

    /** `MutableList<Int>.remove` is ambiguous with index removal, so drop by found index. */
    private fun removeCell(group: MutableList<Int>, cell: Int) {
        val index = group.indexOf(cell)
        check(index >= 0) { "Cell $cell is missing from its cage" }
        group.removeAt(index)
    }

    private fun tryGrade(candidatePuzzle: KillerSudokuPuzzle, maxRank: Int): Grade? = try {
        KillerSudokuTechniqueSolver.gradeByTechniques(
            candidatePuzzle,
            maxRank = maxRank,
            checkUnique = false,
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun candidate(
        groups: List<List<Int>>,
        givens: List<Int?>,
        solution: IntArray,
        pack: Pack,
    ): KillerSudokuPuzzle = KillerSudokuPuzzle(
        id = "KS-CANDIDATE",
        givens = givens,
        cages = toCages(groups, solution),
        solution = solution.toList(),
        pack = pack,
        storedGrade = "pending",
    )

    private fun finish(
        groups: List<List<Int>>,
        givens: List<Int?>,
        solution: IntArray,
        pack: Pack,
        grade: Grade,
        rng: SplitMix64,
    ): KillerSudokuPuzzle {
        for (group in groups) {
            check(group.size in 1..5) { "Killer Sudoku cage must have 1 to 5 cells" }
            check(isConnected(group)) { "Killer Sudoku cage must be orthogonally connected" }
            check(distinctDigits(group, solution)) { "Killer Sudoku cage repeats a digit" }
        }
        return KillerSudokuPuzzle(
            id = newId(pack, rng),
            givens = givens,
            cages = toCages(groups, solution),
            solution = solution.toList(),
            pack = pack,
            storedGrade = KillerSudokuGradeCodec.encode(grade),
        )
    }

    private fun toCages(groups: List<List<Int>>, solution: IntArray): List<KillerCage> {
        val cages = ArrayList<KillerCage>(groups.size)
        for (group in groups) {
            var sum = 0
            val cells = ArrayList<Cell>(group.size)
            for (index in group) {
                sum += solution[index]
                cells.add(Cell(index))
            }
            cages.add(KillerCage(cells, sum))
        }
        return cages
    }

    private fun randomValidGrid(rng: SplitMix64): IntArray {
        val grid = IntArray(KILLER_SUDOKU_CELL_COUNT)
        val rowUsed = IntArray(KILLER_SUDOKU_SIZE)
        val columnUsed = IntArray(KILLER_SUDOKU_SIZE)
        val boxUsed = IntArray(KILLER_SUDOKU_SIZE)

        fun solve(): Boolean {
            var bestCell = -1
            var bestMask = 0
            var minCandidates = KILLER_SUDOKU_SIZE + 1
            for (cell in 0 until KILLER_SUDOKU_CELL_COUNT) {
                if (grid[cell] != 0) continue
                val row = cell / KILLER_SUDOKU_SIZE
                val column = cell % KILLER_SUDOKU_SIZE
                val box = (row / 3) * 3 + (column / 3)
                val used = rowUsed[row] or columnUsed[column] or boxUsed[box]
                val mask = KILLER_SUDOKU_NOTE_MASK and used.inv()
                val count = Integer.bitCount(mask)
                if (count == 0) return false
                if (count < minCandidates) {
                    minCandidates = count
                    bestCell = cell
                    bestMask = mask
                    if (count == 1) break
                }
            }
            if (bestCell == -1) return true

            val row = bestCell / KILLER_SUDOKU_SIZE
            val column = bestCell % KILLER_SUDOKU_SIZE
            val box = (row / 3) * 3 + (column / 3)
            val digits = ArrayList<Int>(minCandidates)
            var mask = bestMask
            while (mask != 0) {
                val bit = Integer.lowestOneBit(mask)
                digits.add(Integer.numberOfTrailingZeros(bit) + 1)
                mask = mask and bit.inv()
            }
            shuffle(digits, rng)

            for (digit in digits) {
                val bit = 1 shl (digit - 1)
                grid[bestCell] = digit
                rowUsed[row] = rowUsed[row] or bit
                columnUsed[column] = columnUsed[column] or bit
                boxUsed[box] = boxUsed[box] or bit
                if (solve()) return true
                grid[bestCell] = 0
                rowUsed[row] = rowUsed[row] and bit.inv()
                columnUsed[column] = columnUsed[column] and bit.inv()
                boxUsed[box] = boxUsed[box] and bit.inv()
            }
            return false
        }

        check(solve()) { "Failed to generate a random Killer Sudoku solution" }

        val rowBands = (0 until 3).toMutableList().also { shuffle(it, rng) }
        val rowOrder = rowBands.flatMap { band ->
            (0 until 3).map { rowOffset -> band * 3 + rowOffset }.toMutableList().also { shuffle(it, rng) }
        }
        val columnStacks = (0 until 3).toMutableList().also { shuffle(it, rng) }
        val columnOrder = columnStacks.flatMap { stack ->
            (0 until 3).map { columnOffset -> stack * 3 + columnOffset }.toMutableList().also { shuffle(it, rng) }
        }
        val digitMapping = (1..KILLER_SUDOKU_SIZE).toMutableList().also { shuffle(it, rng) }
        val transpose = rng.nextInt(2) == 1
        val finalSolution = IntArray(KILLER_SUDOKU_CELL_COUNT)
        var cursor = 0
        for (row in 0 until KILLER_SUDOKU_SIZE) {
            for (column in 0 until KILLER_SUDOKU_SIZE) {
                val rowCoord = if (transpose) column else row
                val columnCoord = if (transpose) row else column
                val mappedRow = rowOrder[rowCoord]
                val mappedColumn = columnOrder[columnCoord]
                val digit = grid[mappedRow * KILLER_SUDOKU_SIZE + mappedColumn]
                finalSolution[cursor] = digitMapping[digit - 1]
                cursor++
            }
        }
        return finalSolution
    }

    private fun easyTargetSize(rng: SplitMix64): Int {
        val roll = rng.nextInt(10)
        return when {
            roll < 4 -> 1
            roll < 8 -> 2
            roll < 9 -> 3
            else -> 4
        }
    }

    private fun maxRank(pack: Pack): Int = when (pack) {
        Pack.EASY -> KillerSudokuTechniques.CAGE_COMBINATIONS.difficultyRank
        Pack.MEDIUM -> KillerSudokuTechniques.RULE_45.difficultyRank
        Pack.HARD -> KillerSudokuTechniques.NAKED_PAIR.difficultyRank
        Pack.EXPERT -> KillerSudokuTechniques.MAX_RANK
    }

    private fun noGivens(): List<Int?> = List(KILLER_SUDOKU_CELL_COUNT) { null }

    private fun newId(pack: Pack, rng: SplitMix64): String {
        val hex = rng.nextLong().toULong().toString(16).padStart(16, '0')
        return "KS-${pack.name}-$hex"
    }

    private fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    private fun distinctDigits(cells: List<Int>, solution: IntArray): Boolean {
        var mask = 0
        for (cell in cells) {
            val bit = digitBit(solution[cell])
            if (mask and bit != 0) return false
            mask = mask or bit
        }
        return true
    }

    private fun isConnected(cells: List<Int>): Boolean {
        val count = cells.size
        if (count <= 1) return true
        val present = BooleanArray(KILLER_SUDOKU_CELL_COUNT)
        for (cell in cells) present[cell] = true
        val stack = IntArray(count)
        var top = 0
        stack[top] = cells[0]
        top++
        present[cells[0]] = false
        var seen = 1
        while (top > 0) {
            top--
            val current = stack[top]
            for (neighbor in NEIGHBORS[current]) {
                if (!present[neighbor]) continue
                present[neighbor] = false
                stack[top] = neighbor
                top++
                seen++
            }
        }
        return seen == count
    }

    private fun <T> shuffle(values: MutableList<T>, rng: SplitMix64) {
        for (index in values.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val value = values[index]
            values[index] = values[other]
            values[other] = value
        }
    }
}
