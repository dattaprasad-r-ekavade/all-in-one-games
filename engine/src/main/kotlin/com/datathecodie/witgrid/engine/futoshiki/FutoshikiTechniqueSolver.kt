package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique
import java.util.ArrayList

/** Deterministic, guess-free Futoshiki deductions and technique grading. */
object FutoshikiTechniqueSolver {
    /** Returns the easiest next deduction, or null when solved, mistaken or technique-stuck. */
    fun nextStep(puzzle: FutoshikiPuzzle, state: FutoshikiState): HintStep? {
        require(state.puzzle == puzzle) { "Futoshiki state belongs to a different puzzle" }
        if (FutoshikiRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (FutoshikiRules.isSolved(puzzle, state)) return null

        var candidates = candidateMasks(puzzle, state.digits)
        if (candidates.anyIndexed { index, mask -> state.digits[index] == null && mask == 0 }) return null

        forcedPlacement(state.digits, candidates, puzzle.size)?.let { forced ->
            return directHint(forced, FutoshikiTechniques.NAKED_SINGLE, null)
        }
        hiddenPlacement(state.digits, candidates, puzzle.size)?.let { forced ->
            return directHint(forced, FutoshikiTechniques.HIDDEN_SINGLE, null)
        }

        val deductions = ArrayList<String>()
        val evidence = linkedSetOf<Int>()
        var hardestTechnique: Technique? = null
        while (true) {
            val elimination = findSignBoundsElimination(puzzle, state.digits, candidates)
                ?: findSignChainsElimination(puzzle, state.digits, candidates)
                ?: findNakedPairElimination(puzzle, state.digits, candidates)
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

            val forced = forcedPlacement(state.digits, candidates, puzzle.size)
                ?: hiddenPlacement(state.digits, candidates, puzzle.size)
            if (forced != null) {
                val explanation = deductions.joinToString(separator = "; and ") +
                    "; this leaves ${forced.conclusionFragment.removePrefix("this leaves ")}."
                return HintStep(
                    technique = requireNotNull(hardestTechnique),
                    cells = immutableList((evidence + forced.cell).distinct().map(::Cell)),
                    explanation = explanation,
                    moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
                )
            }
        }
    }

    /** Solves from the givens and returns the hardest technique and number of hint steps. */
    fun grade(puzzle: FutoshikiPuzzle): Grade {
        val grade = gradeByTechniques(puzzle)
        require(FutoshikiPackBands.sizeForPack(puzzle.pack) == puzzle.size) {
            "Futoshiki board size does not match ${puzzle.pack}"
        }
        require(FutoshikiPackBands.isGradeInPackBand(puzzle.pack, grade)) {
            "Futoshiki grade does not fit the ${puzzle.pack} technique band"
        }
        return grade
    }

    /** Computes a technique grade without enforcing the pack band, for generator filtering. */
    internal fun gradeByTechniques(puzzle: FutoshikiPuzzle): Grade {
        require(FutoshikiRules.isValidSolution(puzzle)) {
            "Futoshiki grading requires a valid stored solution"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Futoshiki givens must agree with the stored solution" }
        require(FutoshikiRules.countSolutions(puzzle, limit = 2) == 1) {
            "Futoshiki grading requires exactly one solution"
        }

        var state = FutoshikiState.initial(puzzle)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        val visited = hashSetOf(state)
        while (!FutoshikiRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Futoshiki puzzle is stuck with the supported techniques"
            }
            val nextState = step.moves.fold(state, FutoshikiState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Futoshiki hint sequence did not make unique progress"
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
        size: Int,
    ): ForcedPlacement? {
        for (index in candidates.indices) {
            if (digits[index] != null || Integer.bitCount(candidates[index]) != 1) continue
            val digit = Integer.numberOfTrailingZeros(candidates[index]) + 1
            return ForcedPlacement(
                cell = index,
                digit = digit,
                directExplanation = "Only digit $digit fits ${cellName(index, size)}.",
                conclusionFragment = "this leaves ${cellName(index, size)} with only digit $digit",
            )
        }
        return null
    }

    private fun hiddenPlacement(
        digits: List<Int?>,
        candidates: IntArray,
        size: Int,
    ): ForcedPlacement? {
        for (unit in units(size)) {
            for (digit in 1..size) {
                val bit = digitBit(digit)
                val possible = unit.cells.filter { digits[it] == null && candidates[it] and bit != 0 }
                if (possible.size != 1) continue
                val index = possible.single()
                return ForcedPlacement(
                    cell = index,
                    digit = digit,
                    directExplanation = "Digit $digit can go in only one cell in ${unit.label}: ${cellName(index, size)}.",
                    conclusionFragment = "this leaves ${cellName(index, size)} as the only place for digit $digit in ${unit.label}",
                )
            }
        }
        return null
    }

    private fun candidateMasks(puzzle: FutoshikiPuzzle, digits: List<Int?>): IntArray {
        val size = puzzle.size
        val allMask = (1 shl size) - 1
        return IntArray(puzzle.grid.cellCount) { index ->
            if (digits[index] != null) {
                0
            } else {
                val row = index / size
                val column = index % size
                val used = usedInRow(digits, row, size) or usedInColumn(digits, column, size)
                allMask and used.inv()
            }
        }
    }

    private fun findSignBoundsElimination(
        puzzle: FutoshikiPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        val size = puzzle.size
        val extremeMaxBit = 1 shl (size - 1)
        for (inequality in puzzle.inequalities) {
            val u = inequality.lesser.index
            val v = inequality.greater.index
            if (digits[u] == null && (candidates[u] and extremeMaxBit) != 0) {
                return CandidateElimination(
                    technique = FutoshikiTechniques.SIGN_BOUNDS,
                    removals = mapOf(u to extremeMaxBit),
                    evidence = listOf(u, v),
                    reason = "${cellName(u, size)} is less than ${cellName(v, size)}, so it cannot be $size",
                )
            }
            if (digits[v] == null && (candidates[v] and 1) != 0) {
                return CandidateElimination(
                    technique = FutoshikiTechniques.SIGN_BOUNDS,
                    removals = mapOf(v to 1),
                    evidence = listOf(u, v),
                    reason = "${cellName(v, size)} is greater than ${cellName(u, size)}, so it cannot be 1",
                )
            }
        }
        return null
    }

    private fun findSignChainsElimination(
        puzzle: FutoshikiPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        val size = puzzle.size
        val allMask = (1 shl size) - 1
        for (inequality in puzzle.inequalities) {
            val u = inequality.lesser.index
            val v = inequality.greater.index

            val maxV = digits[v] ?: maxCandidate(candidates[v], size)
            if (maxV > 0 && digits[u] == null) {
                val illegalMaskU = allMask and ((1 shl (maxV - 1)) - 1).inv()
                val toRemoveU = candidates[u] and illegalMaskU
                if (toRemoveU != 0) {
                    val dList = digitsInMask(toRemoveU, size)
                    val dStr = dList.joinToString(" and ")
                    val label = if (dList.size == 1) "digit" else "digits"
                    val verb = if (dList.size == 1) "is" else "are"
                    val vDesc = if (digits[v] != null) {
                        "${cellName(v, size)} (${digits[v]})"
                    } else {
                        "${cellName(v, size)} with maximum candidate $maxV"
                    }
                    return CandidateElimination(
                        technique = FutoshikiTechniques.SIGN_CHAINS,
                        removals = mapOf(u to toRemoveU),
                        evidence = listOf(u, v),
                        reason = "${cellName(u, size)} is less than $vDesc, so $label $dStr $verb removed from ${cellName(u, size)}",
                    )
                }
            }

            val minU = digits[u] ?: minCandidate(candidates[u])
            if (minU > 0 && digits[v] == null) {
                val illegalMaskV = (1 shl minU) - 1
                val toRemoveV = candidates[v] and illegalMaskV
                if (toRemoveV != 0) {
                    val dList = digitsInMask(toRemoveV, size)
                    val dStr = dList.joinToString(" and ")
                    val label = if (dList.size == 1) "digit" else "digits"
                    val verb = if (dList.size == 1) "is" else "are"
                    val uDesc = if (digits[u] != null) {
                        "${cellName(u, size)} (${digits[u]})"
                    } else {
                        "${cellName(u, size)} with minimum candidate $minU"
                    }
                    return CandidateElimination(
                        technique = FutoshikiTechniques.SIGN_CHAINS,
                        removals = mapOf(v to toRemoveV),
                        evidence = listOf(u, v),
                        reason = "${cellName(v, size)} is greater than $uDesc, so $label $dStr $verb removed from ${cellName(v, size)}",
                    )
                }
            }
        }
        return null
    }

    private fun findNakedPairElimination(
        puzzle: FutoshikiPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        val size = puzzle.size
        for (unit in units(size)) {
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
                        it != first && it != second && digits[it] == null && (candidates[it] and pairMask) != 0
                    }.associateWith { pairMask }
                    if (removals.isEmpty()) continue
                    val pairDigits = digitsInMask(pairMask, size).joinToString(" and ")
                    return CandidateElimination(
                        technique = FutoshikiTechniques.NAKED_PAIR,
                        removals = removals,
                        evidence = (listOf(first, second) + removals.keys).distinct(),
                        reason = "${cellName(first, size)} and ${cellName(second, size)} form a naked pair in ${unit.label}, so digits $pairDigits are removed from the other cells in that unit",
                    )
                }
            }
        }
        return null
    }

    private fun units(size: Int): List<FutoshikiUnit> = buildList(2 * size) {
        for (row in 0 until size) {
            add(FutoshikiUnit(UnitKind.ROW, "row ${row + 1}", (0 until size).map { row * size + it }))
        }
        for (column in 0 until size) {
            add(FutoshikiUnit(UnitKind.COLUMN, "column ${column + 1}", (0 until size).map { it * size + column }))
        }
    }

    private fun usedInRow(digits: List<Int?>, row: Int, size: Int): Int =
        (0 until size).fold(0) { mask, column ->
            val digit = digits[row * size + column]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun usedInColumn(digits: List<Int?>, column: Int, size: Int): Int =
        (0 until size).fold(0) { mask, row ->
            val digit = digits[row * size + column]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    private fun digitsInMask(mask: Int, size: Int): List<Int> =
        (1..size).filter { mask and digitBit(it) != 0 }

    private fun minCandidate(mask: Int): Int =
        if (mask == 0) 0 else Integer.numberOfTrailingZeros(mask) + 1

    private fun maxCandidate(mask: Int, size: Int): Int {
        if (mask == 0) return 0
        for (d in size downTo 1) {
            if ((mask and (1 shl (d - 1))) != 0) return d
        }
        return 0
    }

    private fun cellName(index: Int, size: Int): String =
        "r${index / size + 1}c${index % size + 1}"

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

    private data class FutoshikiUnit(val kind: UnitKind, val label: String, val cells: List<Int>)

    private enum class UnitKind {
        ROW,
        COLUMN,
    }

    private fun IntArray.anyIndexed(predicate: (index: Int, value: Int) -> Boolean): Boolean =
        indices.any { predicate(it, this[it]) }
}
