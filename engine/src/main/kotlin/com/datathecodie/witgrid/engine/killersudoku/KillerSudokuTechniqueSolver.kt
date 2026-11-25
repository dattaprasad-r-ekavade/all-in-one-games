package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.ALL_DIGITS_MASK
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.BOX_CELLS
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.BOX_SIZE
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.CAGE_COMBINATIONS
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.HOUSE_SUM
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.LINE_UNITS
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.MULTI_HOUSE_REGIONS
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.UNITS
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.areCellsDistinctByRules
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.boxIndex
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.cageLabel
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.cellName
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.collectFeasibleCageAssignments
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.digitBit
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.digitsInMask
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.sharedUnits
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.usedInBox
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.usedInCage
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.usedInColumn
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCageMath.usedInRow
import java.util.ArrayList
import java.util.LinkedHashMap

/** Deterministic, guess-free Killer Sudoku deductions and technique grading. */
object KillerSudokuTechniqueSolver {
    /** Returns the easiest next deduction, or null when solved, mistaken, or technique-stuck. */
    fun nextStep(puzzle: KillerSudokuPuzzle, state: KillerSudokuState): HintStep? =
        nextStepWithinRank(puzzle, state, KillerSudokuTechniques.MAX_RANK)

    /** Solves from the givens and returns the hardest technique and number of hint steps. */
    fun grade(puzzle: KillerSudokuPuzzle): Grade {
        val grade = gradeByTechniques(puzzle)
        require(KillerSudokuPackBands.isGradeInPackBand(puzzle.pack, grade)) {
            "Killer Sudoku grade does not fit the ${puzzle.pack} technique band"
        }
        return grade
    }

    /** Computes a technique grade without enforcing the pack band, for generator filtering. */
    internal fun gradeByTechniques(
        puzzle: KillerSudokuPuzzle,
        maxRank: Int = KillerSudokuTechniques.MAX_RANK,
        checkUnique: Boolean = true,
    ): Grade {
        require(KillerSudokuRules.isValidSolution(puzzle)) {
            "Killer Sudoku grading requires a valid stored solution"
        }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Killer Sudoku givens must agree with the stored solution" }
        if (checkUnique) {
            require(KillerSudokuRules.countSolutions(puzzle, limit = 2) == 1) {
                "Killer Sudoku grading requires exactly one solution"
            }
        }

        var state = KillerSudokuState.initial(puzzle)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        val visited = hashSetOf(state)
        while (!KillerSudokuRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStepWithinRank(puzzle, state, maxRank)) {
                "Killer Sudoku puzzle is stuck with the supported techniques"
            }
            if (step.technique.difficultyRank > maxRank) {
                throw IllegalArgumentException("Technique rank exceeded max rank: ${step.technique}")
            }
            val nextState = step.moves.fold(state, KillerSudokuState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Killer Sudoku hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }

        return Grade(hardestTechnique, stepCount, puzzle.pack)
    }

    private fun nextStepWithinRank(
        puzzle: KillerSudokuPuzzle,
        state: KillerSudokuState,
        maxRank: Int,
    ): HintStep? {
        require(state.puzzle == puzzle) { "Killer Sudoku state belongs to a different puzzle" }
        if (KillerSudokuRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (KillerSudokuRules.isSolved(puzzle, state)) return null

        val candidates = candidateMasks(puzzle, state.digits)
        if (candidates.indices.any { state.digits[it] == null && candidates[it] == 0 }) return null

        forcedPlacement(state.digits, candidates)?.let { forced ->
            return directHint(forced, KillerSudokuTechniques.NAKED_SINGLE, listOf(forced.cell))
        }
        if (maxRank < KillerSudokuTechniques.HIDDEN_SINGLE.difficultyRank) return null

        hiddenPlacement(state.digits, candidates)?.let { forced ->
            return directHint(forced, KillerSudokuTechniques.HIDDEN_SINGLE, listOf(forced.cell))
        }
        if (maxRank < KillerSudokuTechniques.CAGE_COMBINATIONS.difficultyRank) return null

        when (val cageDirect = findDirectCageCompletion(puzzle, state.digits, candidates)) {
            is DirectResult.Found -> return cageDirect.step
            DirectResult.Contradiction -> return null
            DirectResult.None -> {}
        }

        val applied = ArrayList<CandidateElimination>()
        val upperRank = minOf(maxRank, KillerSudokuTechniques.MAX_RANK)
        for (allowedMaxRank in KillerSudokuTechniques.CAGE_COMBINATIONS.difficultyRank..upperRank) {
            if (allowedMaxRank == KillerSudokuTechniques.RULE_45.difficultyRank) {
                when (val rule45Direct = findDirectSingleHouse45Placement(puzzle, state.digits, candidates)) {
                    is DirectResult.Found -> return rule45Direct.step
                    DirectResult.Contradiction -> return null
                    DirectResult.None -> {}
                }
            }
            if (allowedMaxRank == KillerSudokuTechniques.INNIE_OUTIE.difficultyRank) {
                when (val innieDirect = findDirectMultiHouseInnieOutiePlacement(puzzle, state.digits, candidates)) {
                    is DirectResult.Found -> return innieDirect.step
                    DirectResult.Contradiction -> return null
                    DirectResult.None -> {}
                }
            }

            while (true) {
                val search = findNextElimination(puzzle, state.digits, candidates, allowedMaxRank)
                val elimination = when (search) {
                    is EliminationResult.Found -> search.elimination
                    EliminationResult.Contradiction -> return null
                    EliminationResult.None -> break
                }
                var changed = false
                for ((index, mask) in elimination.removals) {
                    val updated = candidates[index] and mask.inv()
                    if (state.digits[index] == null && updated == 0) return null
                    if (updated != candidates[index]) {
                        candidates[index] = updated
                        changed = true
                    }
                }
                if (!changed) return null
                applied += elimination

                val forced = forcedPlacement(state.digits, candidates)
                    ?: hiddenPlacement(state.digits, candidates)
                if (forced != null) {
                    return buildChainHint(state.digits, applied, forced)
                }
            }
        }
        return null
    }

    private fun buildChainHint(
        digits: List<Int?>,
        applied: List<CandidateElimination>,
        forced: ForcedPlacement,
    ): HintStep {
        val relevant = backwardSlice(digits, applied, forced)
        val stepsToUse = if (relevant.isNotEmpty()) relevant else applied
        val hardest = stepsToUse.maxBy { it.technique.difficultyRank }.technique
        val evidence = linkedSetOf<Int>()
        stepsToUse.forEach { evidence.addAll(it.evidence) }
        evidence.add(forced.cell)
        val explanation = (
            stepsToUse.joinToString(separator = "; and ") { it.reason } +
                "; this leaves ${forced.conclusionFragment}."
            ).replaceFirstChar { it.uppercaseChar() }
        return HintStep(
            technique = hardest,
            cells = immutableList(evidence.map(::Cell)),
            explanation = explanation,
            moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
        )
    }

    private fun backwardSlice(
        digits: List<Int?>,
        applied: List<CandidateElimination>,
        forced: ForcedPlacement,
    ): List<CandidateElimination> {
        val requiredCells = hashSetOf<Int>()
        if (forced.unitCells == null) {
            requiredCells += forced.cell
        } else {
            forced.unitCells.filterTo(requiredCells) { digits[it] == null }
        }
        val kept = ArrayList<CandidateElimination>()
        for (index in applied.lastIndex downTo 0) {
            val elimination = applied[index]
            if (elimination.removals.keys.any { it in requiredCells }) {
                kept += elimination
                requiredCells.addAll(elimination.dependsOnCells)
            }
        }
        kept.reverse()
        return kept
    }

    private fun findNextElimination(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
        allowedMaxRank: Int,
    ): EliminationResult {
        val finders: List<Pair<Int, () -> EliminationResult>> = listOf(
            KillerSudokuTechniques.CAGE_COMBINATIONS.difficultyRank to {
                findCageCombinationsElimination(puzzle, digits, candidates)
            },
            KillerSudokuTechniques.RULE_45.difficultyRank to {
                findRule45Elimination(puzzle, digits, candidates)
            },
            KillerSudokuTechniques.LOCKED_CANDIDATES.difficultyRank to {
                findLockedCandidateElimination(digits, candidates)
            },
            KillerSudokuTechniques.NAKED_PAIR.difficultyRank to {
                findNakedPairElimination(digits, candidates)
            },
            KillerSudokuTechniques.INNIE_OUTIE.difficultyRank to {
                findInnieOutieElimination(puzzle, digits, candidates)
            },
            KillerSudokuTechniques.HIDDEN_PAIR.difficultyRank to {
                findHiddenPairElimination(digits, candidates)
            },
            KillerSudokuTechniques.NAKED_TRIPLE.difficultyRank to {
                findNakedTripleElimination(digits, candidates)
            },
            KillerSudokuTechniques.X_WING.difficultyRank to {
                findXWingElimination(digits, candidates)
            },
        )
        for ((rank, finder) in finders) {
            if (rank > allowedMaxRank) break
            when (val result = finder()) {
                is EliminationResult.Found -> return result
                EliminationResult.Contradiction -> return EliminationResult.Contradiction
                EliminationResult.None -> {}
            }
        }
        return EliminationResult.None
    }

    private fun directHint(forced: ForcedPlacement, technique: Technique, cells: List<Int>): HintStep =
        HintStep(
            technique = technique,
            cells = immutableList(cells.distinct().map(::Cell)),
            explanation = forced.directExplanation,
            moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
        )

    private fun candidateMasks(puzzle: KillerSudokuPuzzle, digits: List<Int?>): IntArray =
        IntArray(KILLER_SUDOKU_CELL_COUNT) { index ->
            if (digits[index] != null) {
                0
            } else {
                val row = index / KILLER_SUDOKU_SIZE
                val column = index % KILLER_SUDOKU_SIZE
                val used = usedInRow(digits, row) or
                    usedInColumn(digits, column) or
                    usedInBox(digits, row, column) or
                    usedInCage(puzzle, digits, index)
                ALL_DIGITS_MASK and used.inv()
            }
        }

    private fun forcedPlacement(digits: List<Int?>, candidates: IntArray): ForcedPlacement? {
        for (index in candidates.indices) {
            if (digits[index] != null || Integer.bitCount(candidates[index]) != 1) continue
            val digit = Integer.numberOfTrailingZeros(candidates[index]) + 1
            return ForcedPlacement(
                cell = index,
                digit = digit,
                directExplanation = "Only digit $digit fits ${cellName(index)}.",
                conclusionFragment = "${cellName(index)} with only digit $digit",
                unitCells = null,
            )
        }
        return null
    }

    private fun hiddenPlacement(digits: List<Int?>, candidates: IntArray): ForcedPlacement? {
        for (unit in UNITS) {
            for (digit in KILLER_SUDOKU_DIGITS) {
                val bit = digitBit(digit)
                val possible = unit.cells.filter { digits[it] == null && candidates[it] and bit != 0 }
                if (possible.size != 1) continue
                val index = possible.single()
                return ForcedPlacement(
                    cell = index,
                    digit = digit,
                    directExplanation = "Digit $digit can go in only one cell in ${unit.label}: ${cellName(index)}.",
                    conclusionFragment = "${cellName(index)} as the only place for digit $digit in ${unit.label}",
                    unitCells = unit.cells,
                )
            }
        }
        return null
    }

    private fun findDirectCageCompletion(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): DirectResult {
        for (cage in puzzle.cages) {
            val emptyCells = emptyCellsOfCage(cage, digits)
            if (emptyCells.size != 1) continue
            val target = emptyCells.single()
            val placedSum = cage.cells.sumOf { digits[it.index] ?: 0 }
            val value = cage.sum - placedSum
            if (value !in KILLER_SUDOKU_DIGITS) return DirectResult.Contradiction
            val bit = digitBit(value)
            if (candidates[target] and bit == 0) return DirectResult.Contradiction
            val explanation = if (cage.cells.size == 1) {
                "Cage ${cellName(target)} has sum ${cage.sum}, so ${cellName(target)} must be $value."
            } else {
                "${cageLabel(cage)} sums to ${cage.sum} with $placedSum already placed, so ${cellName(target)} must be $value."
            }
            return DirectResult.Found(
                HintStep(
                    technique = KillerSudokuTechniques.CAGE_COMBINATIONS,
                    cells = immutableList(cage.cells),
                    explanation = explanation,
                    moves = immutableList(listOf(Move.SetDigit(Cell(target), value))),
                ),
            )
        }
        return DirectResult.None
    }

    private fun findDirectSingleHouse45Placement(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): DirectResult {
        for (unit in UNITS) {
            val summary = analyzeHouseCoverage(puzzle, digits, unit.cells, unit.cellSet, 1)
            if (summary.innies.size == 1) {
                val target = summary.innies.single()
                val value = summary.innieSum
                if (value !in KILLER_SUDOKU_DIGITS) return DirectResult.Contradiction
                if (candidates[target] and digitBit(value) == 0) return DirectResult.Contradiction
                val evidence = (unit.cells + summary.whollyInsideCages.flatMap { it.cells.map(Cell::index) }).distinct()
                return DirectResult.Found(
                    HintStep(
                        technique = KillerSudokuTechniques.RULE_45,
                        cells = immutableList(evidence.map(::Cell)),
                        explanation = "By the 45 rule on ${unit.label}, ${cellName(target)} must be $value.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(target), value))),
                    ),
                )
            }
            if (summary.outies.size == 1) {
                val target = summary.outies.single()
                val value = summary.outieSum
                if (value !in KILLER_SUDOKU_DIGITS) return DirectResult.Contradiction
                if (candidates[target] and digitBit(value) == 0) return DirectResult.Contradiction
                val evidence = (unit.cells + summary.touchingCages.flatMap { it.cells.map(Cell::index) }).distinct()
                return DirectResult.Found(
                    HintStep(
                        technique = KillerSudokuTechniques.RULE_45,
                        cells = immutableList(evidence.map(::Cell)),
                        explanation = "By the 45 rule on ${unit.label}, outie ${cellName(target)} must be $value.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(target), value))),
                    ),
                )
            }
        }
        return DirectResult.None
    }

    private fun findDirectMultiHouseInnieOutiePlacement(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): DirectResult {
        for (region in MULTI_HOUSE_REGIONS) {
            val summary = analyzeHouseCoverage(puzzle, digits, region.cells, region.cellSet, region.houseCount)
            if (summary.innies.size == 1) {
                val target = summary.innies.single()
                val value = summary.innieSum
                if (value !in KILLER_SUDOKU_DIGITS) return DirectResult.Contradiction
                if (candidates[target] and digitBit(value) == 0) return DirectResult.Contradiction
                val evidence = (summary.whollyInsideCages.flatMap { it.cells.map(Cell::index) } + target).distinct()
                return DirectResult.Found(
                    HintStep(
                        technique = KillerSudokuTechniques.INNIE_OUTIE,
                        cells = immutableList(evidence.map(::Cell)),
                        explanation = "By the innie/outie rule on ${region.label}, ${cellName(target)} must be $value.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(target), value))),
                    ),
                )
            }
            if (summary.outies.size == 1) {
                val target = summary.outies.single()
                val value = summary.outieSum
                if (value !in KILLER_SUDOKU_DIGITS) return DirectResult.Contradiction
                if (candidates[target] and digitBit(value) == 0) return DirectResult.Contradiction
                val evidence = (summary.touchingCages.flatMap { it.cells.map(Cell::index) } + target).distinct()
                return DirectResult.Found(
                    HintStep(
                        technique = KillerSudokuTechniques.INNIE_OUTIE,
                        cells = immutableList(evidence.map(::Cell)),
                        explanation = "By the innie/outie rule on ${region.label}, outie ${cellName(target)} must be $value.",
                        moves = immutableList(listOf(Move.SetDigit(Cell(target), value))),
                    ),
                )
            }
        }
        return DirectResult.None
    }

    private fun findCageCombinationsElimination(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (cage in puzzle.cages) {
            val emptyCells = emptyCellsOfCage(cage, digits)
            if (emptyCells.isEmpty()) continue
            val placedSum = cage.cells.sumOf { digits[it.index] ?: 0 }
            val remainingSum = cage.sum - placedSum
            val count = emptyCells.size
            if (remainingSum !in 1..KILLER_SUDOKU_MAX_CAGE_SUM) return EliminationResult.Contradiction
            val usedInCage = usedMaskInCage(cage, digits)

            if (count == 1) {
                if (remainingSum !in KILLER_SUDOKU_DIGITS) return EliminationResult.Contradiction
                val bit = digitBit(remainingSum)
                val target = emptyCells.single()
                if (usedInCage and bit != 0 || candidates[target] and bit == 0) {
                    return EliminationResult.Contradiction
                }
                val toRemove = candidates[target] and bit.inv()
                if (toRemove != 0) {
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.CAGE_COMBINATIONS,
                            removals = mapOf(target to toRemove),
                            evidence = cage.cells.map(Cell::index),
                            dependsOnCells = emptyCells,
                            reason = "${cageLabel(cage)} leaves only digit $remainingSum for ${cellName(target)}",
                        ),
                    )
                }
                continue
            }

            val combinations = CAGE_COMBINATIONS[count][remainingSum]
            if (combinations.isEmpty()) return EliminationResult.Contradiction

            var availableUnion = 0
            for (index in emptyCells) {
                availableUnion = availableUnion or candidates[index]
            }
            val perCellValid = IntArray(count)
            var validIntersection = ALL_DIGITS_MASK
            var feasibleCount = 0
            for (combo in combinations) {
                if (combo and usedInCage != 0 || combo and availableUnion != combo) continue
                if (collectFeasibleCageAssignments(emptyCells, candidates, combo, perCellValid)) {
                    feasibleCount++
                    validIntersection = validIntersection and combo
                }
            }
            if (feasibleCount == 0) return EliminationResult.Contradiction

            val cellRemovals = LinkedHashMap<Int, Int>()
            for (offset in 0 until count) {
                val cellIndex = emptyCells[offset]
                val extra = candidates[cellIndex] and perCellValid[offset].inv()
                if (extra != 0) cellRemovals[cellIndex] = extra
            }
            if (cellRemovals.isNotEmpty()) {
                val reason = if (cellRemovals.size == 1) {
                    val target = cellRemovals.keys.single()
                    val offset = emptyCells.indexOf(target)
                    val allowedText = formatDigits(digitsInMask(perCellValid[offset]))
                    "${cageLabel(cage)} with remaining sum $remainingSum restricts ${cellName(target)} to digits $allowedText"
                } else {
                    val removedMask = cellRemovals.values.fold(0) { acc, mask -> acc or mask }
                    val removedDigits = digitsInMask(removedMask)
                    val noun = if (removedDigits.size == 1) "digit" else "digits"
                    val targetsText = cellRemovals.keys.joinToString(" and ", transform = ::cellName)
                    "${cageLabel(cage)} with remaining sum $remainingSum eliminates $noun ${formatDigits(removedDigits)} from $targetsText"
                }
                return EliminationResult.Found(
                    CandidateElimination(
                        technique = KillerSudokuTechniques.CAGE_COMBINATIONS,
                        removals = cellRemovals,
                        evidence = cage.cells.map(Cell::index),
                        dependsOnCells = emptyCells,
                        reason = reason,
                    ),
                )
            }

            if (validIntersection != 0) {
                val emptySet = emptyCells.toSet()
                val unitRemovals = LinkedHashMap<Int, Int>()
                val affectedUnits = ArrayList<KillerUnit>()
                for (unit in sharedUnits(emptyCells)) {
                    var unitTouched = false
                    for (cellIndex in unit.cells) {
                        if (cellIndex in emptySet || digits[cellIndex] != null) continue
                        val overlap = candidates[cellIndex] and validIntersection
                        if (overlap != 0) {
                            unitRemovals[cellIndex] = (unitRemovals[cellIndex] ?: 0) or overlap
                            unitTouched = true
                        }
                    }
                    if (unitTouched) affectedUnits += unit
                }
                if (unitRemovals.isNotEmpty()) {
                    val lockedDigits = digitsInMask(validIntersection)
                    val noun = if (lockedDigits.size == 1) "digit" else "digits"
                    val pronoun = if (lockedDigits.size == 1) "it is" else "they are"
                    val unitNames = affectedUnits.joinToString(" and ") { it.label }
                    val targetArea = if (affectedUnits.size == 1) affectedUnits.single().label else "those units"
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.CAGE_COMBINATIONS,
                            removals = unitRemovals,
                            evidence = (cage.cells.map(Cell::index) + unitRemovals.keys).distinct(),
                            dependsOnCells = emptyCells,
                            reason = "$noun ${formatDigits(lockedDigits)} must appear in ${cageLabel(cage)} within $unitNames, so $pronoun removed from the other cells in $targetArea",
                        ),
                    )
                }
            }
        }
        return EliminationResult.None
    }

    private fun findRule45Elimination(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (unit in UNITS) {
            val summary = analyzeHouseCoverage(puzzle, digits, unit.cells, unit.cellSet, 1)
            if (summary.innies.size == 1) {
                val target = summary.innies.single()
                val value = summary.innieSum
                if (value !in KILLER_SUDOKU_DIGITS) return EliminationResult.Contradiction
                val bit = digitBit(value)
                if (candidates[target] and bit == 0) return EliminationResult.Contradiction
                val toRemove = candidates[target] and bit.inv()
                if (toRemove != 0) {
                    val evidence = (unit.cells + summary.whollyInsideCages.flatMap { it.cells.map(Cell::index) }).distinct()
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.RULE_45,
                            removals = mapOf(target to toRemove),
                            evidence = evidence,
                            dependsOnCells = listOf(target),
                            reason = "the 45 rule on ${unit.label} forces ${cellName(target)} to be $value",
                        ),
                    )
                }
            }
            if (summary.outies.size == 1) {
                val target = summary.outies.single()
                val value = summary.outieSum
                if (value !in KILLER_SUDOKU_DIGITS) return EliminationResult.Contradiction
                val bit = digitBit(value)
                if (candidates[target] and bit == 0) return EliminationResult.Contradiction
                val toRemove = candidates[target] and bit.inv()
                if (toRemove != 0) {
                    val evidence = (unit.cells + summary.touchingCages.flatMap { it.cells.map(Cell::index) }).distinct()
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.RULE_45,
                            removals = mapOf(target to toRemove),
                            evidence = evidence,
                            dependsOnCells = listOf(target),
                            reason = "the 45 rule on ${unit.label} forces outie ${cellName(target)} to be $value",
                        ),
                    )
                }
            }
        }
        return EliminationResult.None
    }

    private fun findLockedCandidateElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (boxRow in 0 until BOX_SIZE) {
            for (boxColumn in 0 until BOX_SIZE) {
                val boxIndexValue = boxRow * BOX_SIZE + boxColumn
                val boxCells = BOX_CELLS[boxIndexValue]
                val boxNumber = boxIndexValue + 1
                val boxEmpty = boxCells.filter { digits[it] == null }
                for (digit in KILLER_SUDOKU_DIGITS) {
                    val bit = digitBit(digit)
                    val possible = boxEmpty.filter { candidates[it] and bit != 0 }
                    if (possible.size < 2) continue
                    val rows = possible.map { it / KILLER_SUDOKU_SIZE }.distinct()
                    if (rows.size == 1) {
                        val row = rows.single()
                        val removals = (0 until KILLER_SUDOKU_SIZE).map { row * KILLER_SUDOKU_SIZE + it }
                            .filter { it !in boxCells && digits[it] == null && candidates[it] and bit != 0 }
                        if (removals.isNotEmpty()) {
                            return EliminationResult.Found(
                                CandidateElimination(
                                    technique = KillerSudokuTechniques.LOCKED_CANDIDATES,
                                    removals = removals.associateWith { bit },
                                    evidence = possible + removals,
                                    dependsOnCells = boxEmpty,
                                    reason = "digit $digit in box $boxNumber is confined to row ${row + 1}, so it is removed from the other cells in that row",
                                ),
                            )
                        }
                    }
                    val columns = possible.map { it % KILLER_SUDOKU_SIZE }.distinct()
                    if (columns.size == 1) {
                        val column = columns.single()
                        val removals = (0 until KILLER_SUDOKU_SIZE).map { it * KILLER_SUDOKU_SIZE + column }
                            .filter { it !in boxCells && digits[it] == null && candidates[it] and bit != 0 }
                        if (removals.isNotEmpty()) {
                            return EliminationResult.Found(
                                CandidateElimination(
                                    technique = KillerSudokuTechniques.LOCKED_CANDIDATES,
                                    removals = removals.associateWith { bit },
                                    evidence = possible + removals,
                                    dependsOnCells = boxEmpty,
                                    reason = "digit $digit in box $boxNumber is confined to column ${column + 1}, so it is removed from the other cells in that column",
                                ),
                            )
                        }
                    }
                }
            }
        }

        for (unit in LINE_UNITS) {
            val unitEmpty = unit.cells.filter { digits[it] == null }
            for (digit in KILLER_SUDOKU_DIGITS) {
                val bit = digitBit(digit)
                val possible = unitEmpty.filter { candidates[it] and bit != 0 }
                if (possible.size < 2) continue
                val boxes = possible.map { boxIndex(it / KILLER_SUDOKU_SIZE, it % KILLER_SUDOKU_SIZE) }.distinct()
                if (boxes.size != 1) continue
                val box = BOX_CELLS[boxes.single()]
                val removals = box.filter { it !in unit.cellSet && digits[it] == null && candidates[it] and bit != 0 }
                if (removals.isEmpty()) continue
                return EliminationResult.Found(
                    CandidateElimination(
                        technique = KillerSudokuTechniques.LOCKED_CANDIDATES,
                        removals = removals.associateWith { bit },
                        evidence = possible + removals,
                        dependsOnCells = unitEmpty,
                        reason = "digit $digit in ${unit.label} is confined to box ${boxes.single() + 1}, so it is removed from the other cells in that box",
                    ),
                )
            }
        }
        return EliminationResult.None
    }

    private fun findNakedPairElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (unit in UNITS) {
            val unitEmpty = unit.cells.filter { digits[it] == null }
            val pairs = unitEmpty.filter { Integer.bitCount(candidates[it]) == 2 }
            for (firstPosition in pairs.indices) {
                val first = pairs[firstPosition]
                val pairMask = candidates[first]
                for (secondPosition in firstPosition + 1 until pairs.size) {
                    val second = pairs[secondPosition]
                    if (candidates[second] != pairMask) continue
                    val matching = pairs.count { candidates[it] == pairMask }
                    if (matching != 2) continue
                    val removals = unitEmpty.filter {
                        it != first && it != second && candidates[it] and pairMask != 0
                    }.associateWith { pairMask }
                    if (removals.isEmpty()) continue
                    val pairDigits = digitsInMask(pairMask).joinToString(" and ")
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.NAKED_PAIR,
                            removals = removals,
                            evidence = listOf(first, second) + removals.keys,
                            dependsOnCells = unitEmpty,
                            reason = "${cellName(first)} and ${cellName(second)} form a naked pair in ${unit.label}, so digits $pairDigits are removed from the other cells in that unit",
                        ),
                    )
                }
            }
        }
        return EliminationResult.None
    }

    private fun findInnieOutieElimination(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        // 1. Multi-house 45*k single innie / single outie.
        for (region in MULTI_HOUSE_REGIONS) {
            val summary = analyzeHouseCoverage(puzzle, digits, region.cells, region.cellSet, region.houseCount)
            if (summary.innies.size == 1) {
                val target = summary.innies.single()
                val value = summary.innieSum
                if (value !in KILLER_SUDOKU_DIGITS) return EliminationResult.Contradiction
                val bit = digitBit(value)
                if (candidates[target] and bit == 0) return EliminationResult.Contradiction
                val toRemove = candidates[target] and bit.inv()
                if (toRemove != 0) {
                    val evidence = (summary.whollyInsideCages.flatMap { it.cells.map(Cell::index) } + target).distinct()
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.INNIE_OUTIE,
                            removals = mapOf(target to toRemove),
                            evidence = evidence,
                            dependsOnCells = listOf(target),
                            reason = "the innie/outie rule on ${region.label} forces ${cellName(target)} to be $value",
                        ),
                    )
                }
            }
            if (summary.outies.size == 1) {
                val target = summary.outies.single()
                val value = summary.outieSum
                if (value !in KILLER_SUDOKU_DIGITS) return EliminationResult.Contradiction
                val bit = digitBit(value)
                if (candidates[target] and bit == 0) return EliminationResult.Contradiction
                val toRemove = candidates[target] and bit.inv()
                if (toRemove != 0) {
                    val evidence = (summary.touchingCages.flatMap { it.cells.map(Cell::index) } + target).distinct()
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.INNIE_OUTIE,
                            removals = mapOf(target to toRemove),
                            evidence = evidence,
                            dependsOnCells = listOf(target),
                            reason = "the innie/outie rule on ${region.label} forces outie ${cellName(target)} to be $value",
                        ),
                    )
                }
            }
        }

        // 2. Multi-cell virtual cages and 1-innie / 1-outie differences across single and multi-house regions.
        val allRegions = buildList(UNITS.size + MULTI_HOUSE_REGIONS.size) {
            UNITS.forEach { add(KillerHouseRegion(1, it.label, it.cells, it.cellSet)) }
            addAll(MULTI_HOUSE_REGIONS)
        }
        for (region in allRegions) {
            val summary = analyzeHouseCoverage(puzzle, digits, region.cells, region.cellSet, region.houseCount)

            if (summary.innies.size in 2..4 && areCellsDistinctByRules(puzzle, summary.innies)) {
                val evidence = (summary.whollyInsideCages.flatMap { it.cells.map(Cell::index) } + summary.innies).distinct()
                when (
                    val result = virtualCageElimination(
                        digits = digits,
                        candidates = candidates,
                        cells = summary.innies,
                        targetSum = summary.innieSum,
                        evidence = evidence,
                        description = "innies ${summary.innies.joinToString("-", transform = ::cellName)} in ${region.label}",
                    )
                ) {
                    is EliminationResult.Found -> return result
                    EliminationResult.Contradiction -> return EliminationResult.Contradiction
                    EliminationResult.None -> {}
                }
            }

            if (summary.outies.size in 2..4 && areCellsDistinctByRules(puzzle, summary.outies)) {
                val evidence = (summary.touchingCages.flatMap { it.cells.map(Cell::index) } + summary.outies).distinct()
                when (
                    val result = virtualCageElimination(
                        digits = digits,
                        candidates = candidates,
                        cells = summary.outies,
                        targetSum = summary.outieSum,
                        evidence = evidence,
                        description = "outies ${summary.outies.joinToString("-", transform = ::cellName)} of ${region.label}",
                    )
                ) {
                    is EliminationResult.Found -> return result
                    EliminationResult.Contradiction -> return EliminationResult.Contradiction
                    EliminationResult.None -> {}
                }
            }

            if (summary.crossingCages.size == 2) {
                val first = summary.crossingCages[0]
                val second = summary.crossingCages[1]
                checkInnieOutieDifference(digits, candidates, region, summary, first, second)?.let { return it }
                checkInnieOutieDifference(digits, candidates, region, summary, second, first)?.let { return it }
            }
        }
        return EliminationResult.None
    }

    private fun virtualCageElimination(
        digits: List<Int?>,
        candidates: IntArray,
        cells: List<Int>,
        targetSum: Int,
        evidence: List<Int>,
        description: String,
    ): EliminationResult {
        val count = cells.size
        if (targetSum !in 1..KILLER_SUDOKU_MAX_CAGE_SUM) return EliminationResult.Contradiction
        val combinations = CAGE_COMBINATIONS[count][targetSum]
        if (combinations.isEmpty()) return EliminationResult.Contradiction

        var availableUnion = 0
        for (index in cells) availableUnion = availableUnion or candidates[index]

        val perCellValid = IntArray(count)
        var validIntersection = ALL_DIGITS_MASK
        var feasibleCount = 0
        for (combo in combinations) {
            if (combo and availableUnion != combo) continue
            if (collectFeasibleCageAssignments(cells, candidates, combo, perCellValid)) {
                feasibleCount++
                validIntersection = validIntersection and combo
            }
        }
        if (feasibleCount == 0) return EliminationResult.Contradiction

        val removals = LinkedHashMap<Int, Int>()
        for (offset in 0 until count) {
            val cellIndex = cells[offset]
            val extra = candidates[cellIndex] and perCellValid[offset].inv()
            if (extra != 0) removals[cellIndex] = extra
        }
        if (removals.isNotEmpty()) {
            val reason = if (removals.size == 1) {
                val target = removals.keys.single()
                val offset = cells.indexOf(target)
                val allowedText = formatDigits(digitsInMask(perCellValid[offset]))
                "the $description sum to $targetSum and restrict ${cellName(target)} to digits $allowedText"
            } else {
                val removedMask = removals.values.fold(0) { acc, mask -> acc or mask }
                val removedDigits = digitsInMask(removedMask)
                val noun = if (removedDigits.size == 1) "digit" else "digits"
                val targetsText = removals.keys.joinToString(" and ", transform = ::cellName)
                "the $description sum to $targetSum and eliminate $noun ${formatDigits(removedDigits)} from $targetsText"
            }
            return EliminationResult.Found(
                CandidateElimination(
                    technique = KillerSudokuTechniques.INNIE_OUTIE,
                    removals = removals,
                    evidence = evidence,
                    dependsOnCells = cells,
                    reason = reason,
                ),
            )
        }

        if (validIntersection != 0) {
            val cellSet = cells.toSet()
            val unitRemovals = LinkedHashMap<Int, Int>()
            val affectedUnits = ArrayList<KillerUnit>()
            for (unit in sharedUnits(cells)) {
                var unitTouched = false
                for (cellIndex in unit.cells) {
                    if (cellIndex in cellSet || digits[cellIndex] != null) continue
                    val overlap = candidates[cellIndex] and validIntersection
                    if (overlap != 0) {
                        unitRemovals[cellIndex] = (unitRemovals[cellIndex] ?: 0) or overlap
                        unitTouched = true
                    }
                }
                if (unitTouched) affectedUnits += unit
            }
            if (unitRemovals.isNotEmpty()) {
                val lockedDigits = digitsInMask(validIntersection)
                val noun = if (lockedDigits.size == 1) "digit" else "digits"
                val pronoun = if (lockedDigits.size == 1) "it is" else "they are"
                val unitNames = affectedUnits.joinToString(" and ") { it.label }
                val targetArea = if (affectedUnits.size == 1) affectedUnits.single().label else "those units"
                return EliminationResult.Found(
                    CandidateElimination(
                        technique = KillerSudokuTechniques.INNIE_OUTIE,
                        removals = unitRemovals,
                        evidence = (evidence + unitRemovals.keys).distinct(),
                        dependsOnCells = cells,
                        reason = "the $description sum to $targetSum so $noun ${formatDigits(lockedDigits)} must appear within $unitNames and $pronoun removed from the other cells in $targetArea",
                    ),
                )
            }
        }
        return EliminationResult.None
    }

    private fun checkInnieOutieDifference(
        digits: List<Int?>,
        candidates: IntArray,
        region: KillerHouseRegion,
        summary: HouseCoverageSummary,
        innieCage: KillerCage,
        outieCage: KillerCage,
    ): EliminationResult? {
        val inCells = emptyCellsOfCage(innieCage, digits).filter { it in region.cellSet }
        val outCells = emptyCellsOfCage(outieCage, digits).filter { it !in region.cellSet }
        if (inCells.size != 1 || outCells.size != 1) return null

        val inCell = inCells.single()
        val outCell = outCells.single()
        val outieCageRemaining = outieCage.sum - outieCage.cells.sumOf { digits[it.index] ?: 0 }
        val diff = summary.targetEmptySum - summary.whollySum - outieCageRemaining

        var validInMask = 0
        var validOutMask = 0
        val sameHouse = shareAnyHouse(inCell, outCell)
        for (inDigit in KILLER_SUDOKU_DIGITS) {
            val inBit = digitBit(inDigit)
            if (candidates[inCell] and inBit == 0) continue
            val outDigit = inDigit - diff
            if (outDigit !in KILLER_SUDOKU_DIGITS) continue
            if (sameHouse && inDigit == outDigit) continue
            val outBit = digitBit(outDigit)
            if (candidates[outCell] and outBit == 0) continue
            validInMask = validInMask or inBit
            validOutMask = validOutMask or outBit
        }
        if (validInMask == 0 || validOutMask == 0) return EliminationResult.Contradiction

        val removals = LinkedHashMap<Int, Int>()
        val inExtra = candidates[inCell] and validInMask.inv()
        val outExtra = candidates[outCell] and validOutMask.inv()
        if (inExtra != 0) removals[inCell] = inExtra
        if (outExtra != 0) removals[outCell] = outExtra
        if (removals.isEmpty()) return null

        val evidence = (
            listOf(inCell, outCell) +
                innieCage.cells.map(Cell::index) +
                outieCage.cells.map(Cell::index)
            ).distinct()
        return EliminationResult.Found(
            CandidateElimination(
                technique = KillerSudokuTechniques.INNIE_OUTIE,
                removals = removals,
                evidence = evidence,
                dependsOnCells = listOf(inCell, outCell),
                reason = "in ${region.label}, ${cellName(inCell)} minus ${cellName(outCell)} must equal $diff",
            ),
        )
    }

    private fun findHiddenPairElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (unit in UNITS) {
            val empty = unit.cells.filter { digits[it] == null }
            for (firstDigit in KILLER_SUDOKU_DIGITS) {
                val firstBit = digitBit(firstDigit)
                val firstCells = empty.filter { candidates[it] and firstBit != 0 }
                if (firstCells.size != 2) continue
                for (secondDigit in firstDigit + 1..KILLER_SUDOKU_SIZE) {
                    val secondBit = digitBit(secondDigit)
                    val secondCells = empty.filter { candidates[it] and secondBit != 0 }
                    if (secondCells != firstCells) continue
                    val pairMask = firstBit or secondBit
                    val extra = pairMask.inv()
                    val removals = firstCells.filter { candidates[it] and extra != 0 }
                        .associateWith { candidates[it] and extra }
                    if (removals.isEmpty()) continue
                    return EliminationResult.Found(
                        CandidateElimination(
                            technique = KillerSudokuTechniques.HIDDEN_PAIR,
                            removals = removals,
                            evidence = firstCells,
                            dependsOnCells = empty,
                            reason = "digits $firstDigit and $secondDigit form a hidden pair in ${unit.label} at ${cellName(firstCells[0])} and ${cellName(firstCells[1])}, so other candidates are removed from those cells",
                        ),
                    )
                }
            }
        }
        return EliminationResult.None
    }

    private fun findNakedTripleElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (unit in UNITS) {
            val unitEmpty = unit.cells.filter { digits[it] == null }
            val tripleCandidates = unitEmpty.filter { Integer.bitCount(candidates[it]) in 2..3 }
            for (firstPosition in tripleCandidates.indices) {
                val first = tripleCandidates[firstPosition]
                for (secondPosition in firstPosition + 1 until tripleCandidates.size) {
                    val second = tripleCandidates[secondPosition]
                    for (thirdPosition in secondPosition + 1 until tripleCandidates.size) {
                        val third = tripleCandidates[thirdPosition]
                        val tripleMask = candidates[first] or candidates[second] or candidates[third]
                        if (Integer.bitCount(tripleMask) != 3) continue
                        val triple = setOf(first, second, third)
                        val removals = unitEmpty.filter {
                            it !in triple && candidates[it] and tripleMask != 0
                        }.associateWith { tripleMask }
                        if (removals.isEmpty()) continue
                        val tripleDigits = digitsInMask(tripleMask).joinToString(" and ")
                        return EliminationResult.Found(
                            CandidateElimination(
                                technique = KillerSudokuTechniques.NAKED_TRIPLE,
                                removals = removals,
                                evidence = listOf(first, second, third) + removals.keys,
                                dependsOnCells = unitEmpty,
                                reason = "${cellName(first)}, ${cellName(second)} and ${cellName(third)} form a naked triple in ${unit.label}, so digits $tripleDigits are removed from the other cells in that unit",
                            ),
                        )
                    }
                }
            }
        }
        return EliminationResult.None
    }

    private fun findXWingElimination(
        digits: List<Int?>,
        candidates: IntArray,
    ): EliminationResult {
        for (digit in KILLER_SUDOKU_DIGITS) {
            val bit = digitBit(digit)
            findLineXWing(
                digit = digit,
                bit = bit,
                digits = digits,
                candidates = candidates,
                cellAt = { row, column -> row * KILLER_SUDOKU_SIZE + column },
                primaryName = "rows",
                secondaryName = "columns",
            )?.let { return EliminationResult.Found(it) }
            findLineXWing(
                digit = digit,
                bit = bit,
                digits = digits,
                candidates = candidates,
                cellAt = { column, row -> row * KILLER_SUDOKU_SIZE + column },
                primaryName = "columns",
                secondaryName = "rows",
            )?.let { return EliminationResult.Found(it) }
        }
        return EliminationResult.None
    }

    private fun findLineXWing(
        digit: Int,
        bit: Int,
        digits: List<Int?>,
        candidates: IntArray,
        cellAt: (primary: Int, secondary: Int) -> Int,
        primaryName: String,
        secondaryName: String,
    ): CandidateElimination? {
        val lines = ArrayList<Pair<Int, List<Int>>>(KILLER_SUDOKU_SIZE)
        for (primary in 0 until KILLER_SUDOKU_SIZE) {
            val positions = (0 until KILLER_SUDOKU_SIZE).filter { secondary ->
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
                    for (primary in 0 until KILLER_SUDOKU_SIZE) {
                        if (primary == firstLine || primary == secondLine) continue
                        val index = cellAt(primary, slot)
                        if (digits[index] == null && candidates[index] and bit != 0) {
                            removals += index
                        }
                    }
                }
                if (removals.isEmpty()) continue
                val dependsOn = buildList {
                    for (secondary in 0 until KILLER_SUDOKU_SIZE) {
                        val firstIndex = cellAt(firstLine, secondary)
                        if (digits[firstIndex] == null) add(firstIndex)
                        val secondIndex = cellAt(secondLine, secondary)
                        if (digits[secondIndex] == null) add(secondIndex)
                    }
                }
                return CandidateElimination(
                    technique = KillerSudokuTechniques.X_WING,
                    removals = removals.associateWith { bit },
                    evidence = corners + removals,
                    dependsOnCells = dependsOn,
                    reason = "digit $digit forms an X-Wing on $primaryName ${firstLine + 1} and ${secondLine + 1} in $secondaryName ${slotA + 1} and ${slotB + 1}, so it is removed from the other cells in those $secondaryName",
                )
            }
        }
        return null
    }

    private fun analyzeHouseCoverage(
        puzzle: KillerSudokuPuzzle,
        digits: List<Int?>,
        regionCells: List<Int>,
        regionCellSet: Set<Int>,
        houseCount: Int,
    ): HouseCoverageSummary {
        var placedSum = 0
        val emptyInRegion = ArrayList<Int>(regionCells.size)
        for (cellIndex in regionCells) {
            val digit = digits[cellIndex]
            if (digit == null) {
                emptyInRegion += cellIndex
            } else {
                placedSum += digit
            }
        }
        val targetEmptySum = houseCount * HOUSE_SUM - placedSum
        val seenCages = HashSet<Int>()
        val whollyInsideCages = ArrayList<KillerCage>()
        val touchingCages = ArrayList<KillerCage>()
        val crossingCages = ArrayList<KillerCage>()
        var whollySum = 0
        var touchingSum = 0
        val innies = ArrayList<Int>()
        val outies = ArrayList<Int>()

        for (cellIndex in emptyInRegion) {
            val cageIndex = puzzle.cageIndexByCell[cellIndex]
            if (!seenCages.add(cageIndex)) continue
            val cage = puzzle.cages[cageIndex]
            touchingCages += cage
            val cageRemainingSum = cage.sum - cage.cells.sumOf { digits[it.index] ?: 0 }
            touchingSum += cageRemainingSum

            val emptyCells = emptyCellsOfCage(cage, digits)
            val outsideCells = emptyCells.filter { it !in regionCellSet }
            if (outsideCells.isEmpty()) {
                whollyInsideCages += cage
                whollySum += cageRemainingSum
            } else {
                crossingCages += cage
                for (emptyCell in emptyCells) {
                    if (emptyCell in regionCellSet) {
                        innies += emptyCell
                    } else {
                        outies += emptyCell
                    }
                }
            }
        }

        return HouseCoverageSummary(
            targetEmptySum = targetEmptySum,
            whollySum = whollySum,
            innieSum = targetEmptySum - whollySum,
            outieSum = touchingSum - targetEmptySum,
            whollyInsideCages = whollyInsideCages,
            touchingCages = touchingCages,
            crossingCages = crossingCages,
            innies = innies,
            outies = outies,
        )
    }

    private fun emptyCellsOfCage(cage: KillerCage, digits: List<Int?>): List<Int> =
        cage.cells.mapNotNull { cell -> if (digits[cell.index] == null) cell.index else null }

    private fun usedMaskInCage(cage: KillerCage, digits: List<Int?>): Int =
        cage.cells.fold(0) { mask, cell ->
            val digit = digits[cell.index]
            if (digit == null) mask else mask or digitBit(digit)
        }

    private fun shareAnyHouse(first: Int, second: Int): Boolean {
        val r1 = first / KILLER_SUDOKU_SIZE
        val c1 = first % KILLER_SUDOKU_SIZE
        val r2 = second / KILLER_SUDOKU_SIZE
        val c2 = second % KILLER_SUDOKU_SIZE
        return r1 == r2 || c1 == c2 || boxIndex(r1, c1) == boxIndex(r2, c2)
    }

    private fun formatDigits(digits: List<Int>): String = when (digits.size) {
        0 -> ""
        1 -> digits.single().toString()
        2 -> "${digits[0]} and ${digits[1]}"
        else -> digits.dropLast(1).joinToString(", ") + " and ${digits.last()}"
    }

    private data class ForcedPlacement(
        val cell: Int,
        val digit: Int,
        val directExplanation: String,
        val conclusionFragment: String,
        val unitCells: List<Int>?,
    )

    private data class CandidateElimination(
        val technique: Technique,
        val removals: Map<Int, Int>,
        val evidence: List<Int>,
        val dependsOnCells: List<Int>,
        val reason: String,
    )

    private data class HouseCoverageSummary(
        val targetEmptySum: Int,
        val whollySum: Int,
        val innieSum: Int,
        val outieSum: Int,
        val whollyInsideCages: List<KillerCage>,
        val touchingCages: List<KillerCage>,
        val crossingCages: List<KillerCage>,
        val innies: List<Int>,
        val outies: List<Int>,
    )

    private sealed interface DirectResult {
        data class Found(val step: HintStep) : DirectResult
        data object None : DirectResult
        data object Contradiction : DirectResult
    }

    private sealed interface EliminationResult {
        data class Found(val elimination: CandidateElimination) : EliminationResult
        data object None : EliminationResult
        data object Contradiction : EliminationResult
    }
}
