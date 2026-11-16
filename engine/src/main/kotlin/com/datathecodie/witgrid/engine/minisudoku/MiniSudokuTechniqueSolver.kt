package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/** Deterministic, guess-free Mini Sudoku deductions and technique grading. */
object MiniSudokuTechniqueSolver {
    private const val ALL_DIGITS_MASK = MINI_SUDOKU_NOTE_MASK

    /** Returns the easiest next deduction, or null when solved, mistaken or technique-stuck. */
    fun nextStep(puzzle: MiniSudokuPuzzle, state: MiniSudokuState): HintStep? {
        require(state.puzzle == puzzle) { "Mini Sudoku state belongs to a different puzzle" }
        if (!MiniSudokuRules.mistakes(puzzle, state).isEmpty()) return null
        if (MiniSudokuRules.isSolved(puzzle, state)) return null

        var candidates = candidateMasks(state.digits)
        if (candidates.anyIndexed { index, mask -> state.digits[index] == null && mask == 0 }) return null

        forcedPlacement(state.digits, candidates)?.let { forced ->
            return directHint(forced, MiniSudokuTechniques.NAKED_SINGLE, null)
        }
        hiddenPlacement(state.digits, candidates)?.let { forced ->
            return directHint(forced, MiniSudokuTechniques.HIDDEN_SINGLE, null)
        }

        val deductions = ArrayList<String>()
        val evidence = linkedSetOf<Int>()
        var hardestTechnique: Technique? = null
        while (true) {
            val elimination = findLockedCandidateElimination(state.digits, candidates)
                ?: findNakedPairElimination(state.digits, candidates)
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
    fun grade(puzzle: MiniSudokuPuzzle): Grade {
        require(MiniSudokuRules.isValidSolution(puzzle)) {
            "Mini Sudoku grading requires a valid stored solution"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Mini Sudoku givens must agree with the stored solution" }
        require(MiniSudokuRules.countSolutions(puzzle, limit = 2) == 1) {
            "Mini Sudoku grading requires exactly one solution"
        }

        var state = MiniSudokuState.initial(puzzle)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        val visited = hashSetOf(state)
        while (!MiniSudokuRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Mini Sudoku puzzle is stuck with the supported techniques"
            }
            val nextState = step.moves.fold(state, MiniSudokuState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Mini Sudoku hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }

        val grade = Grade(hardestTechnique, stepCount, puzzle.pack)
        require(MiniSudokuPackBands.isGradeInPackBand(puzzle.pack, grade)) {
            "Mini Sudoku grade does not fit the ${puzzle.pack} technique band"
        }
        return grade
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
            for (digit in MINI_SUDOKU_DIGITS) {
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

    private fun candidateMasks(digits: List<Int?>): IntArray = IntArray(MINI_SUDOKU_CELL_COUNT) { index ->
        if (digits[index] != null) {
            0
        } else {
            val row = index / MINI_SUDOKU_SIZE
            val column = index % MINI_SUDOKU_SIZE
            val used = usedInRow(digits, row) or usedInColumn(digits, column) or usedInBox(digits, row, column)
            ALL_DIGITS_MASK and used.inv()
        }
    }

    private fun findLockedCandidateElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        for (boxRow in 0 until MINI_SUDOKU_SIZE / 2) {
            for (boxColumn in 0 until MINI_SUDOKU_SIZE / 3) {
                val boxCells = boxCells(boxRow, boxColumn)
                val boxNumber = boxRow * (MINI_SUDOKU_SIZE / 3) + boxColumn + 1
                for (digit in MINI_SUDOKU_DIGITS) {
                    val bit = digitBit(digit)
                    val possible = boxCells.filter { digits[it] == null && candidates[it] and bit != 0 }
                    if (possible.size < 2) continue
                    val rows = possible.map { it / MINI_SUDOKU_SIZE }.distinct()
                    if (rows.size == 1) {
                        val row = rows.single()
                        val removals = (0 until MINI_SUDOKU_SIZE).map { row * MINI_SUDOKU_SIZE + it }
                            .filter { it !in boxCells && digits[it] == null && candidates[it] and bit != 0 }
                        if (removals.isNotEmpty()) {
                            return CandidateElimination(
                                technique = MiniSudokuTechniques.LOCKED_CANDIDATES,
                                removals = removals.associateWith { bit },
                                evidence = possible + removals,
                                reason = "digit $digit in box $boxNumber is confined to row ${row + 1}, so it is removed from the other cells in that row",
                            )
                        }
                    }
                    val columns = possible.map { it % MINI_SUDOKU_SIZE }.distinct()
                    if (columns.size == 1) {
                        val column = columns.single()
                        val removals = (0 until MINI_SUDOKU_SIZE).map { it * MINI_SUDOKU_SIZE + column }
                            .filter { it !in boxCells && digits[it] == null && candidates[it] and bit != 0 }
                        if (removals.isNotEmpty()) {
                            return CandidateElimination(
                                technique = MiniSudokuTechniques.LOCKED_CANDIDATES,
                                removals = removals.associateWith { bit },
                                evidence = possible + removals,
                                reason = "digit $digit in box $boxNumber is confined to column ${column + 1}, so it is removed from the other cells in that column",
                            )
                        }
                    }
                }
            }
        }

        for (unit in units().filter { it.kind != UnitKind.BOX }) {
            for (digit in MINI_SUDOKU_DIGITS) {
                val bit = digitBit(digit)
                val possible = unit.cells.filter { digits[it] == null && candidates[it] and bit != 0 }
                if (possible.size < 2) continue
                val boxes = possible.map { boxIndex(it / MINI_SUDOKU_SIZE, it % MINI_SUDOKU_SIZE) }.distinct()
                if (boxes.size != 1) continue
                val box = boxCellsByIndex(boxes.single())
                val removals = box.filter { it !in unit.cells && digits[it] == null && candidates[it] and bit != 0 }
                if (removals.isEmpty()) continue
                return CandidateElimination(
                    technique = MiniSudokuTechniques.LOCKED_CANDIDATES,
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
                        technique = MiniSudokuTechniques.NAKED_PAIR,
                        removals = removals,
                        evidence = listOf(first, second) + removals.keys,
                        reason = "${cellName(first)} and ${cellName(second)} form a naked pair in ${unit.label}, so digits $pairDigits are removed from the other cells in that unit",
                    )
                }
            }
        }
        return null
    }

    private fun units(): List<SudokuUnit> = buildList(18) {
        for (row in 0 until MINI_SUDOKU_SIZE) {
            add(SudokuUnit(UnitKind.LINE, "row ${row + 1}", (0 until MINI_SUDOKU_SIZE).map { row * MINI_SUDOKU_SIZE + it }))
        }
        for (column in 0 until MINI_SUDOKU_SIZE) {
            add(SudokuUnit(UnitKind.LINE, "column ${column + 1}", (0 until MINI_SUDOKU_SIZE).map { it * MINI_SUDOKU_SIZE + column }))
        }
        for (boxRow in 0 until MINI_SUDOKU_SIZE / 2) {
            for (boxColumn in 0 until MINI_SUDOKU_SIZE / 3) {
                val boxNumber = boxRow * (MINI_SUDOKU_SIZE / 3) + boxColumn + 1
                add(SudokuUnit(UnitKind.BOX, "box $boxNumber", boxCells(boxRow, boxColumn)))
            }
        }
    }

    private fun boxCells(boxRow: Int, boxColumn: Int): List<Int> = buildList(6) {
        for (rowOffset in 0 until 2) {
            for (columnOffset in 0 until 3) {
                add((boxRow * 2 + rowOffset) * MINI_SUDOKU_SIZE + boxColumn * 3 + columnOffset)
            }
        }
    }

    private fun boxCellsByIndex(box: Int): List<Int> = boxCells(box / (MINI_SUDOKU_SIZE / 3), box % (MINI_SUDOKU_SIZE / 3))

    private fun usedInRow(digits: List<Int?>, row: Int): Int =
        (0 until MINI_SUDOKU_SIZE).fold(0) { mask, column ->
            val digit = digits[row * MINI_SUDOKU_SIZE + column]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun usedInColumn(digits: List<Int?>, column: Int): Int =
        (0 until MINI_SUDOKU_SIZE).fold(0) { mask, row ->
            val digit = digits[row * MINI_SUDOKU_SIZE + column]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun usedInBox(digits: List<Int?>, row: Int, column: Int): Int =
        boxCells(row / 2, column / 3).fold(0) { mask, index ->
            val digit = digits[index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    private fun digitsInMask(mask: Int): List<Int> = MINI_SUDOKU_DIGITS.filter { mask and digitBit(it) != 0 }

    private fun boxIndex(row: Int, column: Int): Int = row / 2 * (MINI_SUDOKU_SIZE / 3) + column / 3

    private fun cellName(index: Int): String = "r${index / MINI_SUDOKU_SIZE + 1}c${index % MINI_SUDOKU_SIZE + 1}"

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
