package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique
import java.util.ArrayList

/**
 * Deterministic, guess-free Skyscrapers deductions, hint selection and technique grading.
 */
object SkyscrapersTechniqueSolver {

    /**
     * Returns the easiest next deduction, or null when solved, mistaken or technique-stuck.
     */
    fun nextStep(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): HintStep? {
        require(state.puzzle == puzzle) { "Skyscrapers state belongs to a different puzzle" }
        if (SkyscrapersRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (SkyscrapersRules.isSolved(puzzle, state)) return null

        val size = puzzle.size

        // 1. Edge clues 1 and N direct placement
        findEdgeClueStep(puzzle, state)?.let { return it }

        // 2. Base candidate masks from Latin square exclusions
        val allDigitsMask = (1 shl size) - 1
        var candidates = IntArray(puzzle.grid.cellCount) { index ->
            if (state.digits[index] != null) {
                0
            } else {
                val row = index / size
                val col = index % size
                val used = usedInRow(state.digits, size, row) or usedInCol(state.digits, size, col)
                allDigitsMask and used.inv()
            }
        }
        if (candidates.indices.any { state.digits[it] == null && candidates[it] == 0 }) return null

        // 3. Direct singles from Latin square candidates
        forcedPlacement(puzzle, state.digits, candidates)?.let { forced ->
            return HintStep(
                technique = SkyscrapersTechniques.NAKED_SINGLE,
                cells = immutableList(listOf(Cell(forced.cell))),
                explanation = forced.directExplanation,
                moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
            )
        }
        hiddenPlacement(puzzle, state.digits, candidates)?.let { forced ->
            return HintStep(
                technique = SkyscrapersTechniques.HIDDEN_SINGLE,
                cells = immutableList(forced.evidenceCells.map(::Cell)),
                explanation = forced.directExplanation,
                moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
            )
        }

        // 4. Height bounds eliminations
        val deductions = ArrayList<String>()
        val evidence = linkedSetOf<Int>()
        while (true) {
            val elimination = findHeightBoundsElimination(puzzle, state.digits, candidates)
                ?: return null
            val reduced = candidates.copyOf()
            elimination.removals.forEach { (index, mask) ->
                reduced[index] = reduced[index] and mask.inv()
            }
            if (reduced.contentEquals(candidates) ||
                reduced.indices.any { state.digits[it] == null && reduced[it] == 0 }
            ) {
                return null
            }
            candidates = reduced
            deductions += elimination.reason
            evidence += elimination.evidence

            val forced = forcedPlacement(puzzle, state.digits, candidates)
                ?: hiddenPlacement(puzzle, state.digits, candidates)
            if (forced != null) {
                val firstReason = deductions.first().replaceFirstChar { it.uppercaseChar() }
                val allReasons = if (deductions.size == 1) {
                    firstReason
                } else {
                    firstReason + "; and " + deductions.drop(1).joinToString("; and ")
                }
                val explanation = "$allReasons; this leaves ${forced.conclusionFragment.removePrefix("this leaves ")}."
                return HintStep(
                    technique = SkyscrapersTechniques.HEIGHT_BOUNDS,
                    cells = immutableList((evidence + forced.cell).distinct().map(::Cell)),
                    explanation = explanation,
                    moves = immutableList(listOf(Move.SetDigit(Cell(forced.cell), forced.digit))),
                )
            }
        }
    }

    /** Solves from the givens and returns the hardest technique and number of hint steps. */
    fun grade(puzzle: SkyscrapersPuzzle): Grade {
        require(SkyscrapersRules.isValidSolution(puzzle)) {
            "Skyscrapers grading requires a valid stored solution"
        }
        require(SkyscrapersRules.countSolutions(puzzle, limit = 2) == 1) {
            "Skyscrapers grading requires exactly one solution"
        }
        return requireNotNull(gradeUniquePuzzleOrNull(puzzle)) {
            "Skyscrapers puzzle is stuck with the supported techniques"
        }
    }

    /** Solves using hints until the puzzle is solved, returning the solved state and ordered steps. */
    fun solveWithHints(puzzle: SkyscrapersPuzzle): Pair<SkyscrapersState, List<HintStep>> {
        var state = SkyscrapersState.initial(puzzle)
        val steps = ArrayList<HintStep>()
        val visited = hashSetOf(state)
        while (!SkyscrapersRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Skyscrapers puzzle is stuck before solving"
            }
            steps.add(step)
            val nextState = step.moves.fold(state, SkyscrapersState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Skyscrapers hint sequence did not make progress"
            }
            state = nextState
        }
        return state to steps
    }

    /** Grades a puzzle whose stored solution and uniqueness have already been verified. */
    internal fun gradeUniquePuzzleOrNull(puzzle: SkyscrapersPuzzle): Grade? {
        var state = SkyscrapersState.initial(puzzle)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        val visited = hashSetOf(state)
        while (!SkyscrapersRules.isSolved(puzzle, state)) {
            val step = nextStep(puzzle, state) ?: return null
            val nextState = step.moves.fold(state, SkyscrapersState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Skyscrapers hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }
        return Grade(hardestTechnique, stepCount, SkyscrapersPackBands.packForSize(puzzle.size))
    }

    private fun findEdgeClueStep(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): HintStep? {
        val size = puzzle.size

        // Top edge
        for (col in 0 until size) {
            val clue = puzzle.topClues[col] ?: continue
            if (clue == 1) {
                val index = col
                if (state.digits[index] == null) {
                    return directHint(
                        Cell(index),
                        size,
                        SkyscrapersTechniques.CLUE_1_AND_N,
                        "Clue 1 on the top edge of column ${col + 1} means ${cellName(index, size)} must be building $size.",
                    )
                }
            } else if (clue == size) {
                for (row in 0 until size) {
                    val index = row * size + col
                    val digit = row + 1
                    if (state.digits[index] == null) {
                        return directHint(
                            Cell(index),
                            digit,
                            SkyscrapersTechniques.CLUE_1_AND_N,
                            "Clue $size on the top edge of column ${col + 1} means buildings must increase in order, so ${cellName(index, size)} must be $digit.",
                        )
                    }
                }
            }
        }

        // Bottom edge
        for (col in 0 until size) {
            val clue = puzzle.bottomClues[col] ?: continue
            if (clue == 1) {
                val index = (size - 1) * size + col
                if (state.digits[index] == null) {
                    return directHint(
                        Cell(index),
                        size,
                        SkyscrapersTechniques.CLUE_1_AND_N,
                        "Clue 1 on the bottom edge of column ${col + 1} means ${cellName(index, size)} must be building $size.",
                    )
                }
            } else if (clue == size) {
                for (dist in 0 until size) {
                    val row = size - 1 - dist
                    val index = row * size + col
                    val digit = dist + 1
                    if (state.digits[index] == null) {
                        return directHint(
                            Cell(index),
                            digit,
                            SkyscrapersTechniques.CLUE_1_AND_N,
                            "Clue $size on the bottom edge of column ${col + 1} means buildings must increase in order, so ${cellName(index, size)} must be $digit.",
                        )
                    }
                }
            }
        }

        // Left edge
        for (row in 0 until size) {
            val clue = puzzle.leftClues[row] ?: continue
            if (clue == 1) {
                val index = row * size
                if (state.digits[index] == null) {
                    return directHint(
                        Cell(index),
                        size,
                        SkyscrapersTechniques.CLUE_1_AND_N,
                        "Clue 1 on the left edge of row ${row + 1} means ${cellName(index, size)} must be building $size.",
                    )
                }
            } else if (clue == size) {
                for (col in 0 until size) {
                    val index = row * size + col
                    val digit = col + 1
                    if (state.digits[index] == null) {
                        return directHint(
                            Cell(index),
                            digit,
                            SkyscrapersTechniques.CLUE_1_AND_N,
                            "Clue $size on the left edge of row ${row + 1} means buildings must increase in order, so ${cellName(index, size)} must be $digit.",
                        )
                    }
                }
            }
        }

        // Right edge
        for (row in 0 until size) {
            val clue = puzzle.rightClues[row] ?: continue
            if (clue == 1) {
                val index = row * size + size - 1
                if (state.digits[index] == null) {
                    return directHint(
                        Cell(index),
                        size,
                        SkyscrapersTechniques.CLUE_1_AND_N,
                        "Clue 1 on the right edge of row ${row + 1} means ${cellName(index, size)} must be building $size.",
                    )
                }
            } else if (clue == size) {
                for (dist in 0 until size) {
                    val col = size - 1 - dist
                    val index = row * size + col
                    val digit = dist + 1
                    if (state.digits[index] == null) {
                        return directHint(
                            Cell(index),
                            digit,
                            SkyscrapersTechniques.CLUE_1_AND_N,
                            "Clue $size on the right edge of row ${row + 1} means buildings must increase in order, so ${cellName(index, size)} must be $digit.",
                        )
                    }
                }
            }
        }

        return null
    }

    private fun directHint(
        cell: Cell,
        digit: Int,
        technique: Technique,
        explanation: String,
    ): HintStep = HintStep(
        technique = technique,
        cells = immutableList(listOf(cell)),
        explanation = explanation,
        moves = immutableList(listOf(Move.SetDigit(cell, digit))),
    )

    private fun forcedPlacement(
        puzzle: SkyscrapersPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): ForcedPlacement? {
        val size = puzzle.size
        for (index in candidates.indices) {
            if (digits[index] != null || Integer.bitCount(candidates[index]) != 1) continue
            val digit = Integer.numberOfTrailingZeros(candidates[index]) + 1
            return ForcedPlacement(
                cell = index,
                digit = digit,
                directExplanation = "Only building $digit fits ${cellName(index, size)}.",
                conclusionFragment = "this leaves ${cellName(index, size)} with only building $digit",
                evidenceCells = listOf(index),
            )
        }
        return null
    }

    private fun hiddenPlacement(
        puzzle: SkyscrapersPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): ForcedPlacement? {
        val size = puzzle.size
        for (unit in units(size)) {
            for (digit in 1..size) {
                val bit = 1 shl (digit - 1)
                val possible = unit.cells.filter { digits[it] == null && candidates[it] and bit != 0 }
                if (possible.size != 1) continue
                val index = possible.single()
                return ForcedPlacement(
                    cell = index,
                    digit = digit,
                    directExplanation = "Building $digit can go in only one cell in ${unit.label}: ${cellName(index, size)}.",
                    conclusionFragment = "this leaves ${cellName(index, size)} as the only place for building $digit in ${unit.label}",
                    evidenceCells = unit.cells,
                )
            }
        }
        return null
    }

    private fun findHeightBoundsElimination(
        puzzle: SkyscrapersPuzzle,
        digits: List<Int?>,
        candidates: IntArray,
    ): CandidateElimination? {
        val size = puzzle.size
        val allDigitsMask = (1 shl size) - 1

        // 1. Static height bounds from outside clues: cell at distance i cannot exceed size - clue + i + 1
        for (col in 0 until size) {
            val clue = puzzle.topClues[col] ?: continue
            for (row in 0 until size) {
                val maxHeight = size - clue + row + 1
                if (maxHeight < size) {
                    val cell = row * size + col
                    if (digits[cell] == null) {
                        val maskAbove = allDigitsMask and ((1 shl maxHeight) - 1).inv()
                        val toRemove = candidates[cell] and maskAbove
                        if (toRemove != 0) {
                            return CandidateElimination(
                                removals = mapOf(cell to toRemove),
                                evidence = (0..row).map { it * size + col },
                                reason = "clue $clue on the top edge of column ${col + 1} requires at least $clue visible buildings, so ${cellName(cell, size)} cannot exceed $maxHeight",
                            )
                        }
                    }
                }
            }
        }

        for (col in 0 until size) {
            val clue = puzzle.bottomClues[col] ?: continue
            for (dist in 0 until size) {
                val row = size - 1 - dist
                val maxHeight = size - clue + dist + 1
                if (maxHeight < size) {
                    val cell = row * size + col
                    if (digits[cell] == null) {
                        val maskAbove = allDigitsMask and ((1 shl maxHeight) - 1).inv()
                        val toRemove = candidates[cell] and maskAbove
                        if (toRemove != 0) {
                            return CandidateElimination(
                                removals = mapOf(cell to toRemove),
                                evidence = (row until size).map { it * size + col },
                                reason = "clue $clue on the bottom edge of column ${col + 1} requires at least $clue visible buildings, so ${cellName(cell, size)} cannot exceed $maxHeight",
                            )
                        }
                    }
                }
            }
        }

        for (row in 0 until size) {
            val clue = puzzle.leftClues[row] ?: continue
            for (col in 0 until size) {
                val maxHeight = size - clue + col + 1
                if (maxHeight < size) {
                    val cell = row * size + col
                    if (digits[cell] == null) {
                        val maskAbove = allDigitsMask and ((1 shl maxHeight) - 1).inv()
                        val toRemove = candidates[cell] and maskAbove
                        if (toRemove != 0) {
                            return CandidateElimination(
                                removals = mapOf(cell to toRemove),
                                evidence = (0..col).map { row * size + it },
                                reason = "clue $clue on the left edge of row ${row + 1} requires at least $clue visible buildings, so ${cellName(cell, size)} cannot exceed $maxHeight",
                            )
                        }
                    }
                }
            }
        }

        for (row in 0 until size) {
            val clue = puzzle.rightClues[row] ?: continue
            for (dist in 0 until size) {
                val col = size - 1 - dist
                val maxHeight = size - clue + dist + 1
                if (maxHeight < size) {
                    val cell = row * size + col
                    if (digits[cell] == null) {
                        val maskAbove = allDigitsMask and ((1 shl maxHeight) - 1).inv()
                        val toRemove = candidates[cell] and maskAbove
                        if (toRemove != 0) {
                            return CandidateElimination(
                                removals = mapOf(cell to toRemove),
                                evidence = (col until size).map { row * size + it },
                                reason = "clue $clue on the right edge of row ${row + 1} requires at least $clue visible buildings, so ${cellName(cell, size)} cannot exceed $maxHeight",
                            )
                        }
                    }
                }
            }
        }

        // 2. Line visibility consistency across permutations
        val perms = PERMUTATIONS_BY_SIZE.getValue(size)

        // Rows
        for (r in 0 until size) {
            val leftClue = puzzle.leftClues[r]
            val rightClue = puzzle.rightClues[r]
            if (leftClue == null && rightClue == null) continue

            val combCell = IntArray(size)
            var anyMatch = false
            for (perm in perms) {
                var ok = true
                for (c in 0 until size) {
                    val cell = r * size + c
                    val d = perm[c]
                    val existing = digits[cell]
                    if (existing != null) {
                        if (existing != d) { ok = false; break }
                    } else {
                        if (candidates[cell] and (1 shl (d - 1)) == 0) { ok = false; break }
                    }
                }
                if (!ok) continue
                if (leftClue != null && countVisible(perm) != leftClue) continue
                if (rightClue != null && countVisibleReversed(perm) != rightClue) continue

                anyMatch = true
                for (c in 0 until size) {
                    combCell[c] = combCell[c] or (1 shl (perm[c] - 1))
                }
            }
            if (!anyMatch) return null

            val removals = mutableMapOf<Int, Int>()
            for (c in 0 until size) {
                val cell = r * size + c
                if (digits[cell] == null) {
                    val rem = candidates[cell] and combCell[c].inv()
                    if (rem != 0) {
                        removals[cell] = rem
                    }
                }
            }
            if (removals.isNotEmpty()) {
                return CandidateElimination(
                    removals = removals,
                    evidence = (0 until size).map { r * size + it },
                    reason = permutationReason(
                        removals, size, "row ${r + 1}", "left", leftClue, "right", rightClue,
                    ),
                )
            }
        }

        // Columns
        for (c in 0 until size) {
            val topClue = puzzle.topClues[c]
            val bottomClue = puzzle.bottomClues[c]
            if (topClue == null && bottomClue == null) continue

            val combCell = IntArray(size)
            var anyMatch = false
            for (perm in perms) {
                var ok = true
                for (r in 0 until size) {
                    val cell = r * size + c
                    val d = perm[r]
                    val existing = digits[cell]
                    if (existing != null) {
                        if (existing != d) { ok = false; break }
                    } else {
                        if (candidates[cell] and (1 shl (d - 1)) == 0) { ok = false; break }
                    }
                }
                if (!ok) continue
                if (topClue != null && countVisible(perm) != topClue) continue
                if (bottomClue != null && countVisibleReversed(perm) != bottomClue) continue

                anyMatch = true
                for (r in 0 until size) {
                    combCell[r] = combCell[r] or (1 shl (perm[r] - 1))
                }
            }
            if (!anyMatch) return null

            val removals = mutableMapOf<Int, Int>()
            for (r in 0 until size) {
                val cell = r * size + c
                if (digits[cell] == null) {
                    val rem = candidates[cell] and combCell[r].inv()
                    if (rem != 0) {
                        removals[cell] = rem
                    }
                }
            }
            if (removals.isNotEmpty()) {
                return CandidateElimination(
                    removals = removals,
                    evidence = (0 until size).map { it * size + c },
                    reason = permutationReason(
                        removals, size, "column ${c + 1}", "top", topClue, "bottom", bottomClue,
                    ),
                )
            }
        }

        return null
    }

    private fun permutationReason(
        removals: Map<Int, Int>,
        size: Int,
        line: String,
        firstEdge: String,
        firstClue: Int?,
        secondEdge: String,
        secondClue: Int?,
    ): String {
        val clues = listOfNotNull(
            firstClue?.let { "the $firstEdge clue $it" },
            secondClue?.let { "the $secondEdge clue $it" },
        ).joinToString(" and ")
        val ruledOut = removals.entries.joinToString(", ") { (cell, mask) ->
            val heights = (1..size).filter { mask and (1 shl (it - 1)) != 0 }
            "${heights.joinToString(" or ")} at ${cellName(cell, size)}"
        }
        return "checking $clues on $line against the remaining candidates rules out $ruledOut"
    }

    private fun units(size: Int): List<SkyscrapersUnit> = buildList(2 * size) {
        for (row in 0 until size) {
            add(SkyscrapersUnit("row ${row + 1}", (0 until size).map { row * size + it }))
        }
        for (col in 0 until size) {
            add(SkyscrapersUnit("column ${col + 1}", (0 until size).map { it * size + col }))
        }
    }

    private fun usedInRow(digits: List<Int?>, size: Int, row: Int): Int =
        (0 until size).fold(0) { mask, col ->
            val digit = digits[row * size + col]
            if (digit == null) mask else mask or (1 shl (digit - 1))
        }

    private fun usedInCol(digits: List<Int?>, size: Int, col: Int): Int =
        (0 until size).fold(0) { mask, row ->
            val digit = digits[row * size + col]
            if (digit == null) mask else mask or (1 shl (digit - 1))
        }

    private fun countVisible(heights: IntArray): Int {
        var visibleCount = 0
        var maxHeight = 0
        for (height in heights) {
            if (height > maxHeight) {
                visibleCount++
                maxHeight = height
            }
        }
        return visibleCount
    }

    private fun countVisibleReversed(heights: IntArray): Int {
        var visibleCount = 0
        var maxHeight = 0
        for (i in heights.indices.reversed()) {
            val height = heights[i]
            if (height > maxHeight) {
                visibleCount++
                maxHeight = height
            }
        }
        return visibleCount
    }

    private fun cellName(index: Int, size: Int): String =
        "r${index / size + 1}c${index % size + 1}"

    private val PERMUTATIONS_BY_SIZE: Map<Int, List<IntArray>> =
        (4..7).associateWith { buildPermutations(it) }

    private fun buildPermutations(n: Int): List<IntArray> {
        val results = ArrayList<IntArray>()
        val current = IntArray(n)
        val used = BooleanArray(n + 1)
        fun backtrack(index: Int) {
            if (index == n) {
                results.add(current.clone())
                return
            }
            for (v in 1..n) {
                if (!used[v]) {
                    used[v] = true
                    current[index] = v
                    backtrack(index + 1)
                    used[v] = false
                }
            }
        }
        backtrack(0)
        return results
    }

    private data class ForcedPlacement(
        val cell: Int,
        val digit: Int,
        val directExplanation: String,
        val conclusionFragment: String,
        val evidenceCells: List<Int>,
    )

    private data class CandidateElimination(
        val removals: Map<Int, Int>,
        val evidence: List<Int>,
        val reason: String,
    )

    private data class SkyscrapersUnit(val label: String, val cells: List<Int>)
}
