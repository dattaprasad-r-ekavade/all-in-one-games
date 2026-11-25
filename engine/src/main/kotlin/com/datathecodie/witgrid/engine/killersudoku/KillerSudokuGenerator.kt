package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Deterministic Killer Sudoku generator.
 *
 * A candidate is a random full grid, then connected cages of 1 to 5 cells.
 * Cage digits do not repeat and the cage sum is the solution sum.
 * Easy uses cages alone. Medium plants one single-house innie. Expert plants
 * one multi-house outie. Hard removes givens on loose cages.
 * A candidate is kept only when it has one solution and the technique grade
 * sits in the KS2 pack band.
 */
object KillerSudokuGenerator {
    private const val MAX_ATTEMPTS = 24
    private const val SPRINKLE_LIMIT = 16

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
                Pack.MEDIUM -> buildMedium(solution, rng)
                Pack.HARD -> buildHard(solution, rng)
                Pack.EXPERT -> buildExpert(solution, rng)
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
            val grade = tryGrade(groups, givens, solution, Pack.EASY, ceiling)
            val mixed = groups.any { it.size > 1 }
            if (grade != null && mixed && KillerSudokuPackBands.isGradeInPackBand(Pack.EASY, grade)) {
                return finish(groups, givens, solution, Pack.EASY, grade, rng)
            }
            if (!peelOne(groups, rng)) break
        }
        return null
    }

    private fun buildMedium(solution: IntArray, rng: SplitMix64): KillerSudokuPuzzle? {
        val narrow = mediumWindow(solution, rng, span = 2)
        if (narrow != null) return narrow
        return mediumWindow(solution, rng, span = 3)
    }

    /**
     * One row has a single innie. The other rows of the window stay empty so that
     * column is not a naked single, and the cells outside the window are givens.
     */
    private fun mediumWindow(solution: IntArray, rng: SplitMix64, span: Int): KillerSudokuPuzzle? {
        val top = rng.nextInt(KILLER_SUDOKU_SIZE - span + 1)
        val innieColumn = EVEN_COLUMNS[rng.nextInt(EVEN_COLUMNS.size)]
        val partner = (top + 1) * KILLER_SUDOKU_SIZE + innieColumn
        val groups = ArrayList<MutableList<Int>>()
        groups.add(mutableListOf(top * KILLER_SUDOKU_SIZE + innieColumn, partner))
        addContiguousPieces(groups, top, innieColumn)

        val windowRemainder = BooleanArray(KILLER_SUDOKU_CELL_COUNT) { true }
        for (row in top + 1 until top + span) {
            for (column in 0 until KILLER_SUDOKU_SIZE) {
                val cell = row * KILLER_SUDOKU_SIZE + column
                if (cell != partner) windowRemainder[cell] = false
            }
        }
        groups.addAll(grow(solution, rng, preferLoose = true, blocked = windowRemainder) { 3 + it.nextInt(3) })

        val taken = BooleanArray(KILLER_SUDOKU_CELL_COUNT)
        for (group in groups) {
            for (cell in group) taken[cell] = true
        }
        groups.addAll(grow(solution, rng, preferLoose = false, blocked = taken) { 1 + it.nextInt(3) })

        val givens = ArrayList<Int?>(KILLER_SUDOKU_CELL_COUNT)
        for (cell in 0 until KILLER_SUDOKU_CELL_COUNT) {
            val row = cell / KILLER_SUDOKU_SIZE
            val inWindow = row in top until top + span
            givens.add(if (inWindow) null else solution[cell])
        }
        val protected = hashSetOf(top * KILLER_SUDOKU_SIZE + innieColumn, partner)
        return acceptOrSprinkle(solution, rng, Pack.MEDIUM, groups, givens, protected)
    }

    private fun buildHard(solution: IntArray, rng: SplitMix64): KillerSudokuPuzzle? {
        // Loose cages keep a house from collapsing into one innie, so the 45 rule
        // does not solve the board before a Sudoku technique in the Hard band.
        val groups = grow(solution, rng, preferLoose = true) { 3 + it.nextInt(3) }
        val probe = tryGrade(groups, noGivens(), solution, Pack.HARD, maxRank(Pack.HARD))
        if (probe != null && KillerSudokuPackBands.isGradeInPackBand(Pack.HARD, probe)) {
            return finish(groups, noGivens(), solution, Pack.HARD, probe, rng)
        }
        val probeRank = probe?.hardestTechnique?.difficultyRank ?: -1
        if (probe != null && probeRank < minimumRank(Pack.HARD)) return null
        return removeGivens(solution, rng, Pack.HARD, groups)
    }

    private fun buildExpert(solution: IntArray, rng: SplitMix64): KillerSudokuPuzzle? {
        val planted = plantMultiHouseOutie(solution, rng) ?: return null
        val groups = ArrayList<MutableList<Int>>()
        groups.addAll(planted.groups)
        val blocked = BooleanArray(KILLER_SUDOKU_CELL_COUNT)
        for (cell in planted.emptyCells) blocked[cell] = true
        groups.addAll(grow(solution, rng, preferLoose = false, blocked = blocked) { 1 + it.nextInt(3) })
        val givens = ArrayList<Int?>(KILLER_SUDOKU_CELL_COUNT)
        for (cell in 0 until KILLER_SUDOKU_CELL_COUNT) {
            givens.add(if (cell in planted.emptyCells) null else solution[cell])
        }
        return acceptOrSprinkle(solution, rng, Pack.EXPERT, groups, givens, planted.protectedCells)
    }

    /**
     * Two empty rows of column dominos, plus one cage that steps into the next row.
     * That step is a single outie of the two-row region. A second pair on the outie
     * row stops the outie from being a naked single after the other givens are filled.
     */
    private fun plantMultiHouseOutie(solution: IntArray, rng: SplitMix64): ExpertPlant? {
        val top = rng.nextInt(KILLER_SUDOKU_SIZE - 2)
        val columns = (0 until KILLER_SUDOKU_SIZE).toMutableList()
        shuffle(columns, rng)
        for (outieColumn in columns) {
            val neighbors = ArrayList<Int>(2)
            if (outieColumn > 0) neighbors.add(outieColumn - 1)
            if (outieColumn < KILLER_SUDOKU_SIZE - 1) neighbors.add(outieColumn + 1)
            shuffle(neighbors, rng)
            for (neighbor in neighbors) {
                val cap = top * KILLER_SUDOKU_SIZE + outieColumn
                val sideTop = top * KILLER_SUDOKU_SIZE + neighbor
                val sideBottom = (top + 1) * KILLER_SUDOKU_SIZE + neighbor
                if (!distinctDigits(listOf(cap, sideTop, sideBottom), solution)) continue
                val extra = chooseExtraPair(outieColumn, rng) ?: continue
                val groups = ArrayList<MutableList<Int>>()
                val innie = (top + 1) * KILLER_SUDOKU_SIZE + outieColumn
                val outie = (top + 2) * KILLER_SUDOKU_SIZE + outieColumn
                groups.add(mutableListOf(innie, outie))
                groups.add(mutableListOf(cap, sideTop, sideBottom))
                for (column in 0 until KILLER_SUDOKU_SIZE) {
                    if (column == outieColumn || column == neighbor) continue
                    groups.add(
                        mutableListOf(
                            top * KILLER_SUDOKU_SIZE + column,
                            (top + 1) * KILLER_SUDOKU_SIZE + column,
                        ),
                    )
                }
                val extraA = (top + 2) * KILLER_SUDOKU_SIZE + extra.first
                val extraB = (top + 2) * KILLER_SUDOKU_SIZE + extra.second
                groups.add(mutableListOf(extraA, extraB))
                val empty = HashSet<Int>()
                for (group in groups) empty.addAll(group)
                return ExpertPlant(groups, empty, hashSetOf(innie, outie))
            }
        }
        return null
    }

    private fun chooseExtraPair(outieColumn: Int, rng: SplitMix64): Pair<Int, Int>? {
        val options = ArrayList<Int>()
        for (column in 0 until KILLER_SUDOKU_SIZE - 1) {
            if (column != outieColumn && column + 1 != outieColumn) options.add(column)
        }
        if (options.isEmpty()) return null
        val start = options[rng.nextInt(options.size)]
        return start to start + 1
    }

    private fun acceptOrSprinkle(
        solution: IntArray,
        rng: SplitMix64,
        pack: Pack,
        groups: MutableList<MutableList<Int>>,
        givens: MutableList<Int?>,
        protected: Set<Int>,
    ): KillerSudokuPuzzle? {
        val ceiling = maxRank(pack)
        val grade = tryGrade(groups, givens, solution, pack, ceiling)
        if (grade != null && KillerSudokuPackBands.isGradeInPackBand(pack, grade)) {
            return finish(groups, givens, solution, pack, grade, rng)
        }
        val rank = grade?.hardestTechnique?.difficultyRank ?: -1
        if (grade != null && rank < minimumRank(pack)) return null
        return sprinkle(solution, rng, pack, groups, givens, protected)
    }

    /**
     * Adds givens outside the planted cage.
     * The planted cells stay empty so that cage can still force the pack technique.
     */
    private fun sprinkle(
        solution: IntArray,
        rng: SplitMix64,
        pack: Pack,
        groups: MutableList<MutableList<Int>>,
        givens: MutableList<Int?>,
        protected: Set<Int>,
    ): KillerSudokuPuzzle? {
        val holes = ArrayList<Int>()
        for (index in givens.indices) {
            if (givens[index] != null || index in protected) continue
            holes.add(index)
        }
        shuffle(holes, rng)
        var added = 0
        for (index in holes) {
            if (added == SPRINKLE_LIMIT) break
            givens[index] = solution[index]
            added++
            val grade = tryGrade(groups, givens, solution, pack, maxRank(pack))
            if (grade == null) continue
            val rank = grade.hardestTechnique?.difficultyRank ?: -1
            if (rank < minimumRank(pack)) return null
            if (KillerSudokuPackBands.isGradeInPackBand(pack, grade)) {
                return finish(groups, givens, solution, pack, grade, rng)
            }
        }
        return null
    }

    /**
     * Removes givens while the technique solver still finishes within the pack ceiling.
     * Returns the first board whose grade is inside the pack band.
     */
    private fun removeGivens(
        solution: IntArray,
        rng: SplitMix64,
        pack: Pack,
        groups: MutableList<MutableList<Int>>,
    ): KillerSudokuPuzzle? {
        val givens = ArrayList<Int?>(KILLER_SUDOKU_CELL_COUNT)
        for (digit in solution) givens.add(digit)
        val order = (0 until KILLER_SUDOKU_CELL_COUNT).toMutableList()
        shuffle(order, rng)
        val ceiling = maxRank(pack)
        for (index in order) {
            val saved = givens[index] ?: continue
            givens[index] = null
            val grade = tryGrade(groups, givens, solution, pack, ceiling)
            val rank = grade?.hardestTechnique?.difficultyRank ?: -1
            if (grade == null || grade.stepCount == 0 || rank > ceiling) {
                givens[index] = saved
                continue
            }
            if (KillerSudokuPackBands.isGradeInPackBand(pack, grade)) {
                return finish(groups, givens, solution, pack, grade, rng)
            }
        }
        return null
    }

    /** Splits one row around [skipColumn] so each piece stays on adjacent columns. */
    private fun addContiguousPieces(
        groups: MutableList<MutableList<Int>>,
        row: Int,
        skipColumn: Int,
    ) {
        var segment = ArrayList<Int>()
        fun flush() {
            if (segment.isEmpty()) return
            for (piece in splitContiguous(segment)) groups.add(piece.toMutableList())
            segment = ArrayList()
        }
        for (column in 0 until KILLER_SUDOKU_SIZE) {
            if (column == skipColumn) {
                flush()
            } else {
                segment.add(row * KILLER_SUDOKU_SIZE + column)
            }
        }
        flush()
    }

    private fun splitContiguous(cells: List<Int>): List<List<Int>> {
        val pieces = ArrayList<List<Int>>()
        var start = 0
        while (start < cells.size) {
            val remaining = cells.size - start
            val size = when {
                remaining <= 5 -> remaining
                remaining == 6 -> 3
                remaining == 7 -> 4
                remaining == 8 -> 4
                else -> 5
            }
            val piece = ArrayList<Int>(size)
            for (offset in 0 until size) piece.add(cells[start + offset])
            pieces.add(piece)
            start += size
        }
        return pieces
    }

    private fun grow(
        solution: IntArray,
        rng: SplitMix64,
        preferLoose: Boolean,
        blocked: BooleanArray = BooleanArray(KILLER_SUDOKU_CELL_COUNT),
        targetSize: (SplitMix64) -> Int,
    ): MutableList<MutableList<Int>> {
        val unassigned = BooleanArray(KILLER_SUDOKU_CELL_COUNT) { index -> !blocked[index] }
        val order = ArrayList<Int>(KILLER_SUDOKU_CELL_COUNT)
        for (index in 0 until KILLER_SUDOKU_CELL_COUNT) {
            if (unassigned[index]) order.add(index)
        }
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
        return combinationCount(group.size + 1, sum)
    }

    private fun combinationCount(size: Int, sum: Int): Int {
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

    private fun tryGrade(
        groups: List<List<Int>>,
        givens: List<Int?>,
        solution: IntArray,
        pack: Pack,
        maxRank: Int,
    ): Grade? = try {
        KillerSudokuTechniqueSolver.gradeByTechniques(
            candidate(groups, givens, solution, pack),
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

    private fun minimumRank(pack: Pack): Int = when (pack) {
        Pack.EASY -> KillerSudokuTechniques.NAKED_SINGLE.difficultyRank
        Pack.MEDIUM -> KillerSudokuTechniques.RULE_45.difficultyRank
        Pack.HARD -> KillerSudokuTechniques.LOCKED_CANDIDATES.difficultyRank
        Pack.EXPERT -> KillerSudokuTechniques.INNIE_OUTIE.difficultyRank
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

    private data class ExpertPlant(
        val groups: List<MutableList<Int>>,
        val emptyCells: Set<Int>,
        val protectedCells: Set<Int>,
    )

    private val EVEN_COLUMNS = intArrayOf(0, 2, 4, 6, 8)
}
