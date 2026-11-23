package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique

/** Deterministic, guess-free Sudoku deductions and technique grading. */
object SudokuTechniqueSolver {
    private const val ALL_DIGITS_MASK = SUDOKU_NOTE_MASK
    private const val BOX_SIZE = 3

    /** Returns the easiest next deduction, or null when solved, mistaken or technique-stuck. */
    fun nextStep(puzzle: SudokuPuzzle, state: SudokuState): HintStep? {
        require(state.puzzle == puzzle) { "Sudoku state belongs to a different puzzle" }
        if (!SudokuRules.mistakes(puzzle, state).isEmpty()) return null
        if (SudokuRules.isSolved(puzzle, state)) return null

        var candidates = candidateMasks(state.digits)
        if (candidates.anyIndexed { index, mask -> state.digits[index] == null && mask == 0 }) return null

        forcedPlacement(state.digits, candidates)?.let { forced ->
            return directHint(forced, SudokuTechniques.NAKED_SINGLE, null)
        }
        hiddenPlacement(state.digits, candidates)?.let { forced ->
            return directHint(forced, SudokuTechniques.HIDDEN_SINGLE, null)
        }

        val deductions = ArrayList<String>()
        val evidence = linkedSetOf<Int>()
        var hardestTechnique: Technique? = null
        while (true) {
            val elimination = findLockedCandidateElimination(state.digits, candidates)
                ?: findNakedPairElimination(state.digits, candidates)
                ?: findHiddenPairElimination(state.digits, candidates)
                ?: findNakedTripleElimination(state.digits, candidates)
                ?: findXWingElimination(state.digits, candidates)
                ?: return null
            val reduced = candidates.copyOf()
            elimination.removals.forEach { (index, mask) -> reduced[index] = reduced[index] and mask.inv() }
            if (reduced.contentEquals(candidates) || reduced.anyIndexed { index, mask ->
                    state.digits[index] == null && mask == 0
                }
            ) {
                return null
            }
            candidates = reduced
            deductions += elimination.reason
            evidence += elimination.evidence
            if (hardestTechnique == null || elimination.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = elimination.technique
            }

            val forced = forcedPlacement(state.digits, candidates)
                ?: hiddenPlacement(state.digits, candidates)
            if (forced != null) {
                val explanation = deductions.joinToString(separator = "; and ") +
                    "; this leaves ${forced.conclusionFragment.removePrefix("this leaves ")}."
                return HintStep(
                    technique = requireNotNull(hardestTechnique),
                    cells = immutableList((evidence + forced.cell).map(::Cell)),
                    explanation = explanation,
                    moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
                )
            }
        }
    }

    /** Solves from the givens and returns the hardest technique and number of hint steps. */
    fun grade(puzzle: SudokuPuzzle): Grade {
        val grade = gradeByTechniques(puzzle)
        require(SudokuPackBands.isGradeInPackBand(puzzle.pack, grade)) {
            "Sudoku grade does not fit the ${puzzle.pack} technique band"
        }
        return grade
    }

    /** Computes a technique grade without enforcing the pack band, for generator filtering. */
    internal fun gradeByTechniques(
        puzzle: SudokuPuzzle,
        maxRank: Int = Int.MAX_VALUE,
        checkUnique: Boolean = true,
    ): Grade {
        require(SudokuRules.isValidSolution(puzzle)) {
            "Sudoku grading requires a valid stored solution"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Sudoku givens must agree with the stored solution" }
        if (checkUnique) {
            require(SudokuRules.countSolutions(puzzle, limit = 2) == 1) {
                "Sudoku grading requires exactly one solution"
            }
        }

        var state = SudokuState.initial(puzzle)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        val visited = hashSetOf(state)
        while (!SudokuRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Sudoku puzzle is stuck with the supported techniques"
            }
            if (step.technique.difficultyRank > maxRank) {
                throw IllegalArgumentException("Technique rank exceeded max rank: ${step.technique}")
            }
            val nextState = step.moves.fold(state, SudokuState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Sudoku hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }

        return Grade(hardestTechnique, stepCount, puzzle.pack)
    }

    private fun directHint(forced: ForcedPlacement, technique: Technique, evidence: List<Int>?): HintStep {
        val cells = immutableList((evidence.orEmpty() + forced.cell).distinct().map(::Cell))
        return HintStep(
            technique = technique,
            cells = cells,
            explanation = forced.directExplanation,
            moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
        )
    }

    private fun forcedPlacement(
        digits: List<Int?>,
        candidates: IntArray,
    ): ForcedPlacement? {
        for (index in candidates.indices) {
            if (digits[index] != null || Integer.bitCount(candidates[index]) != 1) continue
            val digit = Integer.numberOfTrailingZeros(candidates[index]) + 1
            return ForcedPlacement(
                cell = index,
                digit = digit,
                directExplanation = "Only digit $digit fits ${cellName(index)}.",
                conclusionFragment = "this leaves ${cellName(index)} with only digit $digit",
            )
        }
        return null
    }

    private fun hiddenPlacement(
        digits: List<Int?>,
        candidates: IntArray,
    ): ForcedPlacement? {
        for (unit in units()) {
            for (digit in SUDOKU_DIGITS) {
                val bit = digitBit(digit)
                val possible = unit.cells.filter { digits[it] == null && candidates[it] and bit != 0 }
                if (possible.size != 1) continue
                val index = possible.single()
                return ForcedPlacement(
                    cell = index,
                    digit = digit,
                    directExplanation = "Digit $digit can go in only one cell in ${unit.label}: ${cellName(index)}.",
                    conclusionFragment = "this leaves ${cellName(index)} as the only place for digit $digit in ${unit.label}",
                )
            }
        }
        return null
    }

    private fun candidateMasks(digits: List<Int?>): IntArray = IntArray(SUDOKU_CELL_COUNT) { index ->
        if (digits[index] != null) {
            0
        } else {
            val row = index / SUDOKU_SIZE
            val column = index % SUDOKU_SIZE
            val used = usedInRow(digits, row) or usedInColumn(digits, column) or usedInBox(digits, row, column)
            ALL_DIGITS_MASK and used.inv()
        }
    }

    private fun findLockedCandidateElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        for (boxRow in 0 until BOX_SIZE) {
            for (boxColumn in 0 until BOX_SIZE) {
                val boxCells = boxCells(boxRow, boxColumn)
                val boxNumber = boxRow * BOX_SIZE + boxColumn + 1
                for (digit in SUDOKU_DIGITS) {
                    val bit = digitBit(digit)
                    val possible = boxCells.filter { digits[it] == null && candidates[it] and bit != 0 }
                    if (possible.size < 2) continue
                    val rows = possible.map { it / SUDOKU_SIZE }.distinct()
                    if (rows.size == 1) {
                        val row = rows.single()
                        val removals = (0 until SUDOKU_SIZE).map { row * SUDOKU_SIZE + it }
                            .filter { it !in boxCells && digits[it] == null && candidates[it] and bit != 0 }
                        if (removals.isNotEmpty()) {
                            return CandidateElimination(
                                technique = SudokuTechniques.LOCKED_CANDIDATES,
                                removals = removals.associateWith { bit },
                                evidence = possible + removals,
                                reason = "digit $digit in box $boxNumber is confined to row ${row + 1}, so it is removed from the other cells in that row",
                            )
                        }
                    }
                    val columns = possible.map { it % SUDOKU_SIZE }.distinct()
                    if (columns.size == 1) {
                        val column = columns.single()
                        val removals = (0 until SUDOKU_SIZE).map { it * SUDOKU_SIZE + column }
                            .filter { it !in boxCells && digits[it] == null && candidates[it] and bit != 0 }
                        if (removals.isNotEmpty()) {
                            return CandidateElimination(
                                technique = SudokuTechniques.LOCKED_CANDIDATES,
                                removals = removals.associateWith { bit },
                                evidence = possible + removals,
                                reason = "digit $digit in box $boxNumber is confined to column ${column + 1}, so it is removed from the other cells in that column",
                            )
                        }
                    }
                }
            }
        }

        for (unit in LINE_UNITS) {
            for (digit in SUDOKU_DIGITS) {
                val bit = digitBit(digit)
                val possible = unit.cells.filter { digits[it] == null && candidates[it] and bit != 0 }
                if (possible.size < 2) continue
                val boxes = possible.map { boxIndex(it / SUDOKU_SIZE, it % SUDOKU_SIZE) }.distinct()
                if (boxes.size != 1) continue
                val box = boxCellsByIndex(boxes.single())
                val removals = box.filter { it !in unit.cells && digits[it] == null && candidates[it] and bit != 0 }
                if (removals.isEmpty()) continue
                return CandidateElimination(
                    technique = SudokuTechniques.LOCKED_CANDIDATES,
                    removals = removals.associateWith { bit },
                    evidence = possible + removals,
                    reason = "digit $digit in ${unit.label} is confined to box ${boxes.single() + 1}, so it is removed from the other cells in that box",
                )
            }
        }
        return null
    }

    private fun findNakedPairElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        for (unit in units()) {
            val pairs = unit.cells.filter { digits[it] == null && Integer.bitCount(candidates[it]) == 2 }
            for (firstPosition in pairs.indices) {
                val first = pairs[firstPosition]
                val pairMask = candidates[first]
                for (secondPosition in firstPosition + 1 until pairs.size) {
                    val second = pairs[secondPosition]
                    if (candidates[second] != pairMask) continue
                    val matching = pairs.count { candidates[it] == pairMask }
                    if (matching != 2) continue
                    val removals = unit.cells.filter {
                        it != first && it != second && digits[it] == null && candidates[it] and pairMask != 0
                    }.associateWith { pairMask }
                    if (removals.isEmpty()) continue
                    val pairDigits = digitsInMask(pairMask).joinToString(" and ")
                    return CandidateElimination(
                        technique = SudokuTechniques.NAKED_PAIR,
                        removals = removals,
                        evidence = listOf(first, second) + removals.keys,
                        reason = "${cellName(first)} and ${cellName(second)} form a naked pair in ${unit.label}, so digits $pairDigits are removed from the other cells in that unit",
                    )
                }
            }
        }
        return null
    }

    private fun findHiddenPairElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        for (unit in units()) {
            val empty = unit.cells.filter { digits[it] == null }
            for (firstDigit in SUDOKU_DIGITS) {
                val firstBit = digitBit(firstDigit)
                val firstCells = empty.filter { candidates[it] and firstBit != 0 }
                if (firstCells.size != 2) continue
                for (secondDigit in firstDigit + 1..SUDOKU_SIZE) {
                    val secondBit = digitBit(secondDigit)
                    val secondCells = empty.filter { candidates[it] and secondBit != 0 }
                    if (secondCells != firstCells) continue
                    val pairMask = firstBit or secondBit
                    val extra = pairMask.inv()
                    val removals = firstCells.filter { candidates[it] and extra != 0 }
                        .associateWith { candidates[it] and extra }
                    if (removals.isEmpty()) continue
                    return CandidateElimination(
                        technique = SudokuTechniques.HIDDEN_PAIR,
                        removals = removals,
                        evidence = firstCells,
                        reason = "digits $firstDigit and $secondDigit form a hidden pair in ${unit.label} at ${cellName(firstCells[0])} and ${cellName(firstCells[1])}, so other candidates are removed from those cells",
                    )
                }
            }
        }
        return null
    }

    private fun findNakedTripleElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        for (unit in units()) {
            val empty = unit.cells.filter { digits[it] == null && Integer.bitCount(candidates[it]) in 2..3 }
            for (firstPosition in empty.indices) {
                val first = empty[firstPosition]
                for (secondPosition in firstPosition + 1 until empty.size) {
                    val second = empty[secondPosition]
                    for (thirdPosition in secondPosition + 1 until empty.size) {
                        val third = empty[thirdPosition]
                        val tripleMask = candidates[first] or candidates[second] or candidates[third]
                        if (Integer.bitCount(tripleMask) != 3) continue
                        if (candidates[first] and tripleMask.inv() != 0 ||
                            candidates[second] and tripleMask.inv() != 0 ||
                            candidates[third] and tripleMask.inv() != 0
                        ) {
                            continue
                        }
                        val triple = setOf(first, second, third)
                        val removals = unit.cells.filter {
                            it !in triple && digits[it] == null && candidates[it] and tripleMask != 0
                        }.associateWith { tripleMask }
                        if (removals.isEmpty()) continue
                        val tripleDigits = digitsInMask(tripleMask).joinToString(" and ")
                        return CandidateElimination(
                            technique = SudokuTechniques.NAKED_TRIPLE,
                            removals = removals,
                            evidence = listOf(first, second, third) + removals.keys,
                            reason = "${cellName(first)}, ${cellName(second)} and ${cellName(third)} form a naked triple in ${unit.label}, so digits $tripleDigits are removed from the other cells in that unit",
                        )
                    }
                }
            }
        }
        return null
    }

    private fun findXWingElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        for (digit in SUDOKU_DIGITS) {
            val bit = digitBit(digit)
            findLineXWing(
                digit = digit,
                bit = bit,
                digits = digits,
                candidates = candidates,
                primaryCount = SUDOKU_SIZE,
                secondaryCount = SUDOKU_SIZE,
                cellAt = { row, column -> row * SUDOKU_SIZE + column },
                primaryName = "rows",
                secondaryName = "columns",
            )?.let { return it }
            findLineXWing(
                digit = digit,
                bit = bit,
                digits = digits,
                candidates = candidates,
                primaryCount = SUDOKU_SIZE,
                secondaryCount = SUDOKU_SIZE,
                cellAt = { column, row -> row * SUDOKU_SIZE + column },
                primaryName = "columns",
                secondaryName = "rows",
            )?.let { return it }
        }
        return null
    }

    private fun findLineXWing(
        digit: Int,
        bit: Int,
        digits: List<Int?>,
        candidates: IntArray,
        primaryCount: Int,
        secondaryCount: Int,
        cellAt: (primary: Int, secondary: Int) -> Int,
        primaryName: String,
        secondaryName: String,
    ): CandidateElimination? {
        val lines = ArrayList<Pair<Int, List<Int>>>(primaryCount)
        for (primary in 0 until primaryCount) {
            val positions = (0 until secondaryCount).filter { secondary ->
                val index = cellAt(primary, secondary)
                digits[index] == null && candidates[index] and bit != 0
            }
            if (positions.size == 2) {
                lines += primary to positions
            }
        }
        for (firstPosition in lines.indices) {
            val (firstLine, firstSlots) = lines[firstPosition]
            for (secondPosition in firstPosition + 1 until lines.size) {
                val (secondLine, secondSlots) = lines[secondPosition]
                if (firstSlots != secondSlots) continue
                val slotA = firstSlots[0]
                val slotB = firstSlots[1]
                val corners = listOf(
                    cellAt(firstLine, slotA),
                    cellAt(firstLine, slotB),
                    cellAt(secondLine, slotA),
                    cellAt(secondLine, slotB),
                )
                val removals = ArrayList<Int>()
                for (slot in firstSlots) {
                    for (primary in 0 until primaryCount) {
                        if (primary == firstLine || primary == secondLine) continue
                        val index = cellAt(primary, slot)
                        if (digits[index] == null && candidates[index] and bit != 0) {
                            removals += index
                        }
                    }
                }
                if (removals.isEmpty()) continue
                return CandidateElimination(
                    technique = SudokuTechniques.X_WING,
                    removals = removals.associateWith { bit },
                    evidence = corners + removals,
                    reason = "digit $digit forms an X-Wing on $primaryName ${firstLine + 1} and ${secondLine + 1} in $secondaryName ${slotA + 1} and ${slotB + 1}, so it is removed from the other cells in those $secondaryName",
                )
            }
        }
        return null
    }

    private val BOX_CELLS: List<List<Int>> = (0 until SUDOKU_SIZE).map { boxIndex ->
        val boxRow = boxIndex / BOX_SIZE
        val boxColumn = boxIndex % BOX_SIZE
        buildList(9) {
            for (rowOffset in 0 until BOX_SIZE) {
                for (columnOffset in 0 until BOX_SIZE) {
                    add((boxRow * BOX_SIZE + rowOffset) * SUDOKU_SIZE + boxColumn * BOX_SIZE + columnOffset)
                }
            }
        }
    }

    private val UNITS: List<SudokuUnit> = buildList(27) {
        for (row in 0 until SUDOKU_SIZE) {
            add(SudokuUnit(UnitKind.LINE, "row ${row + 1}", (0 until SUDOKU_SIZE).map { row * SUDOKU_SIZE + it }))
        }
        for (column in 0 until SUDOKU_SIZE) {
            add(SudokuUnit(UnitKind.LINE, "column ${column + 1}", (0 until SUDOKU_SIZE).map { it * SUDOKU_SIZE + column }))
        }
        for (box in 0 until SUDOKU_SIZE) {
            add(SudokuUnit(UnitKind.BOX, "box ${box + 1}", BOX_CELLS[box]))
        }
    }

    private val LINE_UNITS: List<SudokuUnit> = UNITS.filter { it.kind != UnitKind.BOX }

    private fun units(): List<SudokuUnit> = UNITS

    private fun boxCells(boxRow: Int, boxColumn: Int): List<Int> = BOX_CELLS[boxRow * BOX_SIZE + boxColumn]

    private fun boxCellsByIndex(box: Int): List<Int> = BOX_CELLS[box]

    private fun usedInRow(digits: List<Int?>, row: Int): Int =
        (0 until SUDOKU_SIZE).fold(0) { mask, column ->
            val digit = digits[row * SUDOKU_SIZE + column]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun usedInColumn(digits: List<Int?>, column: Int): Int =
        (0 until SUDOKU_SIZE).fold(0) { mask, row ->
            val digit = digits[row * SUDOKU_SIZE + column]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun usedInBox(digits: List<Int?>, row: Int, column: Int): Int =
        boxCells(row / BOX_SIZE, column / BOX_SIZE).fold(0) { mask, index ->
            val digit = digits[index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    private fun digitsInMask(mask: Int): List<Int> = SUDOKU_DIGITS.filter { mask and digitBit(it) != 0 }

    private fun boxIndex(row: Int, column: Int): Int = row / BOX_SIZE * BOX_SIZE + column / BOX_SIZE

    private fun cellName(index: Int): String = "r${index / SUDOKU_SIZE + 1}c${index % SUDOKU_SIZE + 1}"

    private data class ForcedPlacement(
        val cell: Int,
        val digit: Int,
        val directExplanation: String,
        val conclusionFragment: String,
    )

    private data class CandidateElimination(
        val technique: Technique,
        val removals: Map<Int, Int>,
        val evidence: List<Int>,
        val reason: String,
    )

    private data class SudokuUnit(val kind: UnitKind, val label: String, val cells: List<Int>)

    private enum class UnitKind {
        LINE,
        BOX,
    }

    private fun IntArray.anyIndexed(predicate: (index: Int, value: Int) -> Boolean): Boolean =
        indices.any { predicate(it, this[it]) }
}

private fun <T> immutableList(values: List<T>): List<T> =
    java.util.Collections.unmodifiableList(ArrayList(values))
