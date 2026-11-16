package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/** Guess-free Binary deductions, deterministic hint selection and grading. */
object BinaryTechniqueSolver {
    /** Returns the easiest next deduction, or null for a solved, mistaken or stuck board. */
    fun nextStep(puzzle: BinaryPuzzle, state: BinaryState): HintStep? {
        require(state.puzzle == puzzle) { "Binary state belongs to a different puzzle" }
        if (BinaryRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (BinaryRules.isSolved(puzzle, state)) return null

        pairCapStep(puzzle, state)?.let { return it }
        gapFillStep(puzzle, state)?.let { return it }
        countCompletionStep(puzzle, state)?.let { return it }
        if (puzzle.size >= 10) lineComparisonStep(puzzle, state)?.let { return it }
        return null
    }

    /** Solves from the givens with the published techniques and returns the hardest-step grade. */
    fun grade(puzzle: BinaryPuzzle): Grade {
        val pack = validateGradeInput(puzzle)
        require(BinaryRules.countSolutions(puzzle, limit = 2) == 1) {
            "Binary grading requires exactly one solution"
        }
        return gradeByTechniques(puzzle, pack)
    }

    /** Computes the technique grade without a solution-count search; callers validate uniqueness separately. */
    internal fun gradeByTechniques(puzzle: BinaryPuzzle): Grade =
        gradeByTechniques(puzzle, validateGradeInput(puzzle))

    private fun validateGradeInput(puzzle: BinaryPuzzle): Pack {
        require(BinaryRules.isValidSolution(puzzle)) { "Binary grading requires a valid stored solution" }
        require(puzzle.givens.indices.all { index ->
            puzzle.givens[index] == null || puzzle.givens[index] == puzzle.solution[index]
        }) { "Binary givens must agree with the stored solution" }
        val pack = BinaryPackBands.packForSize(puzzle.size)
        require(puzzle.pack == pack) { "Binary pack must match its board size" }
        return pack
    }

    private fun gradeByTechniques(puzzle: BinaryPuzzle, pack: Pack): Grade {
        var state = BinaryState.initial(puzzle)
        val visited = hashSetOf(state)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        while (!BinaryRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Binary puzzle is stuck with the supported techniques"
            }
            val nextState = step.moves.fold(state, BinaryState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Binary hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null ||
                step.technique.difficultyRank > hardestTechnique.difficultyRank
            ) {
                hardestTechnique = step.technique
            }
        }
        return Grade(hardestTechnique, stepCount, pack)
    }

    private fun pairCapStep(puzzle: BinaryPuzzle, state: BinaryState): HintStep? {
        for (line in LineDirection.entries) {
            for (lineNumber in 0 until puzzle.size) {
                val indices = line.indices(puzzle.size, lineNumber)
                for (start in 0 until puzzle.size - 2) {
                    val first = state.symbols[indices[start]]
                    val second = state.symbols[indices[start + 1]]
                    val third = state.symbols[indices[start + 2]]
                    val missingOffset: Int
                    val repeated: BinarySymbol
                    when {
                        first == BinarySymbol.EMPTY && second != BinarySymbol.EMPTY && second == third -> {
                            missingOffset = 0
                            repeated = second
                        }

                        third == BinarySymbol.EMPTY && first != BinarySymbol.EMPTY && first == second -> {
                            missingOffset = 2
                            repeated = first
                        }

                        else -> continue
                    }
                    val target = Cell(indices[start + missingOffset])
                    return hint(
                        BinaryTechniques.PAIR_CAP,
                        listOf(Cell(indices[start]), Cell(indices[start + 1]), Cell(indices[start + 2])),
                        "${line.label(lineNumber)} has a pair of ${repeated.label()} symbols, so the adjacent cell must be ${repeated.opposite().label()}.",
                        listOf(setSymbol(target, repeated.opposite())),
                    )
                }
            }
        }
        return null
    }

    private fun gapFillStep(puzzle: BinaryPuzzle, state: BinaryState): HintStep? {
        for (line in LineDirection.entries) {
            for (lineNumber in 0 until puzzle.size) {
                val indices = line.indices(puzzle.size, lineNumber)
                for (start in 0 until puzzle.size - 2) {
                    val first = state.symbols[indices[start]]
                    val middle = state.symbols[indices[start + 1]]
                    val last = state.symbols[indices[start + 2]]
                    if (first == BinarySymbol.EMPTY || middle != BinarySymbol.EMPTY || first != last) continue
                    val target = Cell(indices[start + 1])
                    return hint(
                        BinaryTechniques.GAP_FILL,
                        listOf(Cell(indices[start]), target, Cell(indices[start + 2])),
                        "${line.label(lineNumber)} has two ${first.label()} symbols around one empty cell, so the middle must be ${first.opposite().label()}.",
                        listOf(setSymbol(target, first.opposite())),
                    )
                }
            }
        }
        return null
    }

    private fun countCompletionStep(puzzle: BinaryPuzzle, state: BinaryState): HintStep? {
        val half = puzzle.size / 2
        for (line in LineDirection.entries) {
            for (lineNumber in 0 until puzzle.size) {
                val indices = line.indices(puzzle.size, lineNumber)
                val aCount = indices.count { state.symbols[it] == BinarySymbol.A }
                val bCount = indices.count { state.symbols[it] == BinarySymbol.B }
                val completed = when {
                    aCount == half -> BinarySymbol.B
                    bCount == half -> BinarySymbol.A
                    else -> continue
                }
                val targets = indices.filter { state.symbols[it] == BinarySymbol.EMPTY }
                if (targets.isEmpty()) continue
                return hint(
                    BinaryTechniques.COUNT_COMPLETION,
                    indices.map(::Cell),
                    "${line.label(lineNumber)} already has ${half} ${completed.opposite().label()} symbols, so every empty cell must be ${completed.label()}.",
                    targets.map { setSymbol(Cell(it), completed) },
                )
            }
        }
        return null
    }

    private fun lineComparisonStep(puzzle: BinaryPuzzle, state: BinaryState): HintStep? {
        for (direction in LineDirection.entries) {
            for (referenceNumber in 0 until puzzle.size) {
                val reference = direction.indices(puzzle.size, referenceNumber)
                if (reference.any { state.symbols[it] == BinarySymbol.EMPTY }) continue
                for (partialNumber in 0 until puzzle.size) {
                    if (partialNumber == referenceNumber) continue
                    val partial = direction.indices(puzzle.size, partialNumber)
                    val holes = partial.filter { state.symbols[it] == BinarySymbol.EMPTY }
                    if (holes.size != 2) continue
                    if (partial.indices.any { offset ->
                        val cell = partial[offset]
                        cell !in holes && state.symbols[cell] != state.symbols[reference[offset]]
                    }) {
                        continue
                    }

                    val firstOffset = partial.indexOf(holes[0])
                    val secondOffset = partial.indexOf(holes[1])
                    val referenceFirst = state.symbols[reference[firstOffset]]
                    val referenceSecond = state.symbols[reference[secondOffset]]
                    if (referenceFirst == referenceSecond) continue

                    val firstCell = Cell(holes[0])
                    val secondCell = Cell(holes[1])
                    return hint(
                        BinaryTechniques.LINE_COMPARISON,
                        listOf(Cell(reference[firstOffset]), firstCell, Cell(reference[secondOffset]), secondCell),
                        "${direction.label(partialNumber)} matches ${direction.label(referenceNumber)} except at ${direction.positionsLabel(firstOffset, secondOffset)}, so its empty cells must use the opposite order to keep lines distinct.",
                        listOf(
                            setSymbol(firstCell, referenceFirst.opposite()),
                            setSymbol(secondCell, referenceSecond.opposite()),
                        ),
                    )
                }
            }
        }
        return null
    }

    private fun hint(
        technique: Technique,
        cells: List<Cell>,
        explanation: String,
        moves: List<Move>,
    ): HintStep = HintStep(
        technique = technique,
        cells = immutableList(cells),
        explanation = explanation,
        moves = immutableList(moves),
    )

    private fun setSymbol(cell: Cell, symbol: BinarySymbol): Move.SetMark = Move.SetMark(
        cell,
        when (symbol) {
            BinarySymbol.EMPTY -> Mark.EMPTY
            BinarySymbol.A -> Mark.FILLED
            BinarySymbol.B -> Mark.CROSSED
        },
    )

    private fun LineDirection.indices(size: Int, lineNumber: Int): List<Int> = when (this) {
        LineDirection.ROW -> (0 until size).map { column -> lineNumber * size + column }
        LineDirection.COLUMN -> (0 until size).map { row -> row * size + lineNumber }
    }

    private fun LineDirection.label(lineNumber: Int): String =
        "${if (this == LineDirection.ROW) "Row" else "Column"} ${lineNumber + 1}"

    private fun LineDirection.positionsLabel(first: Int, second: Int): String =
        if (this == LineDirection.ROW) "columns ${first + 1} and ${second + 1}"
        else "rows ${first + 1} and ${second + 1}"

    private fun BinarySymbol.label(): String = when (this) {
        BinarySymbol.A -> "A"
        BinarySymbol.B -> "B"
        BinarySymbol.EMPTY -> "an empty"
    }

    private fun BinarySymbol.opposite(): BinarySymbol = when (this) {
        BinarySymbol.A -> BinarySymbol.B
        BinarySymbol.B -> BinarySymbol.A
        BinarySymbol.EMPTY -> throw IllegalArgumentException("An empty cell has no opposite symbol")
    }

    private enum class LineDirection {
        ROW,
        COLUMN,
    }
}
