package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique

/** Guess-free Nonogram deductions, deterministic hints and line-pass grading. */
object NonogramTechniqueSolver {
    /** Returns one deterministic deduction, or null for a solved, mistaken or stuck board. */
    fun nextStep(puzzle: NonogramPuzzle, state: NonogramState): HintStep? {
        require(state.puzzle == puzzle) { "Nonogram state belongs to a different puzzle" }
        if (NonogramRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (NonogramRules.isSolved(puzzle, state)) return null

        for (technique in NonogramTechniques.all) {
            for (line in puzzle.lines()) {
                deductionForLine(puzzle, state, line, technique)?.let { return it }
            }
        }
        return null
    }

    /** Solves from clues without guessing and grades by full line passes and hardest rule. */
    fun grade(puzzle: NonogramPuzzle): Grade {
        require(NonogramRules.isValidSolution(puzzle)) {
            "Nonogram grading requires a valid stored solution"
        }
        require(puzzle.size == NonogramPackBands.sizeForPack(puzzle.pack)) {
            "Nonogram pack must match its board size"
        }
        require(NonogramRules.countSolutions(puzzle, limit = 2) == 1) {
            "Nonogram grading requires exactly one solution"
        }

        var state = NonogramState.initial(puzzle)
        var passCount = 0
        var hardestTechnique: Technique? = null
        while (!NonogramRules.isSolved(puzzle, state)) {
            val beforePass = state
            passCount++
            for (technique in NonogramTechniques.all) {
                for (line in puzzle.lines()) {
                    val step = deductionForLine(puzzle, state, line, technique) ?: continue
                    val nextState = step.moves.fold(state, NonogramState::withMove)
                    check(nextState != state) { "Nonogram hint sequence did not make progress" }
                    state = nextState
                    if (hardestTechnique == null ||
                        technique.difficultyRank > hardestTechnique.difficultyRank
                    ) {
                        hardestTechnique = technique
                    }
                    if (NonogramRules.isSolved(puzzle, state)) break
                }
                if (NonogramRules.isSolved(puzzle, state)) break
            }
            require(state != beforePass) {
                "Nonogram puzzle is stuck with the supported techniques"
            }
        }
        val grade = Grade(hardestTechnique, passCount, puzzle.pack)
        require(NonogramPackBands.isGradeInPackBand(puzzle.pack, grade)) {
            "Nonogram grade does not fit the ${puzzle.pack} pack band"
        }
        return grade
    }

    private fun deductionForLine(
        puzzle: NonogramPuzzle,
        state: NonogramState,
        line: ClueLine,
        technique: Technique,
    ): HintStep? {
        val rawPatterns = line.patterns(puzzle.size)
        val candidates = rawPatterns.filter { pattern ->
            line.indices.indices.all { offset ->
                val filled = (pattern ushr offset) and 1 == 1
                when (state.marks[line.indices[offset]]) {
                    Mark.EMPTY -> true
                    Mark.FILLED -> filled
                    Mark.CROSSED -> !filled
                }
            }
        }
        if (candidates.isEmpty()) return null

        val forcedValues = IntArray(puzzle.size) { offset ->
            val first = (candidates.first() ushr offset) and 1
            if (candidates.all { ((it ushr offset) and 1) == first }) first else -1
        }
        val overlapOffsets = line.indices.indices.filter { offset ->
            state.marks[line.indices[offset]] == Mark.EMPTY &&
                if (line.clues.isEmpty()) {
                    forcedValues[offset] == 0
                } else {
                    forcedValues[offset] == 1 &&
                        rawPatterns.all { ((it ushr offset) and 1) == 1 }
                }
        }.toSet()
        val edgeOffsets = edgeCrossOffsets(line, state, candidates)

        val targetOffsets = line.indices.indices.filter { offset ->
            if (state.marks[line.indices[offset]] != Mark.EMPTY || forcedValues[offset] == -1) {
                false
            } else {
                when (technique) {
                    NonogramTechniques.OVERLAP -> offset in overlapOffsets
                    NonogramTechniques.EDGE_RULES -> offset in edgeOffsets
                    NonogramTechniques.LINE_BY_LINE ->
                        offset !in overlapOffsets && offset !in edgeOffsets

                    else -> false
                }
            }
        }
        if (targetOffsets.isEmpty()) return null

        val moves = targetOffsets.map { offset ->
            Move.SetMark(
                Cell(line.indices[offset]),
                if (forcedValues[offset] == 1) Mark.FILLED else Mark.CROSSED,
            )
        }
        val explanation = when (technique) {
            NonogramTechniques.OVERLAP -> if (line.clues.isEmpty()) {
                "${line.label()} has no filled runs, so every cell must be crossed."
            } else {
                "Every placement of the runs in ${line.label()} fills ${line.positions(targetOffsets)}, so the overlap must be filled."
            }

            NonogramTechniques.EDGE_RULES ->
                "A completed run on ${line.label()} fixes its neighboring empty cells as crossed."

            NonogramTechniques.LINE_BY_LINE ->
                "The remaining valid placements on ${line.label()} force the highlighted cells to match their clues."

            else -> error("Unsupported Nonogram technique")
        }
        return HintStep(
            technique = technique,
            cells = line.indices.map(::Cell),
            explanation = explanation,
            moves = moves,
        )
    }

    private fun edgeCrossOffsets(
        line: ClueLine,
        state: NonogramState,
        candidates: List<Int>,
    ): Set<Int> {
        val result = linkedSetOf<Int>()
        var start = 0
        while (start < line.indices.size) {
            if (state.marks[line.indices[start]] != Mark.FILLED) {
                start++
                continue
            }
            var end = start
            while (end + 1 < line.indices.size &&
                state.marks[line.indices[end + 1]] == Mark.FILLED
            ) {
                end++
            }
            val runLength = end - start + 1
            val closedBefore = start == 0 || candidates.all { ((it ushr (start - 1)) and 1) == 0 }
            val closedAfter = end == line.indices.lastIndex ||
                candidates.all { ((it ushr (end + 1)) and 1) == 0 }
            if (runLength in line.clues && closedBefore && closedAfter) {
                if (start > 0 && state.marks[line.indices[start - 1]] == Mark.EMPTY &&
                    candidates.all { ((it ushr (start - 1)) and 1) == 0 }
                ) {
                    result += start - 1
                }
                if (end + 1 < line.indices.size &&
                    state.marks[line.indices[end + 1]] == Mark.EMPTY &&
                    candidates.all { ((it ushr (end + 1)) and 1) == 0 }
                ) {
                    result += end + 1
                }
            }
            start = end + 1
        }
        return result
    }

    private fun NonogramPuzzle.lines(): List<ClueLine> {
        val puzzle = this
        val boardSize = puzzle.size
        return buildList(boardSize * 2) {
            for (row in 0 until boardSize) {
                add(
                    ClueLine(
                        RowOrColumn.ROW,
                        row,
                        (0 until boardSize).map { row * boardSize + it },
                        puzzle.rowClues[row],
                    ),
                )
            }
            for (column in 0 until boardSize) {
                add(
                    ClueLine(
                        RowOrColumn.COLUMN,
                        column,
                        (0 until boardSize).map { it * boardSize + column },
                        puzzle.columnClues[column],
                    ),
                )
            }
        }
    }

    private data class ClueLine(
        val direction: RowOrColumn,
        val number: Int,
        val indices: List<Int>,
        val clues: List<Int>,
    ) {
        fun patterns(size: Int): List<Int> {
            val results = ArrayList<Int>()
            fun place(clueIndex: Int, nextStart: Int, mask: Int) {
                if (clueIndex == clues.size) {
                    results += mask
                    return
                }
                val remainingRuns = clues.size - clueIndex
                val minimumCells = clues.drop(clueIndex).sum() + remainingRuns - 1
                val lastStart = size - minimumCells
                for (start in nextStart..lastStart) {
                    val runMask = ((1 shl clues[clueIndex]) - 1) shl start
                    place(clueIndex + 1, start + clues[clueIndex] + 1, mask or runMask)
                }
            }
            place(0, 0, 0)
            return results
        }

        fun label(): String = "${direction.label} ${number + 1}"

        fun positions(offsets: List<Int>): String = offsets.joinToString(
            prefix = if (direction == RowOrColumn.ROW) "columns " else "rows ",
            separator = " and ",
        ) { (it + 1).toString() }
    }

    private enum class RowOrColumn(val label: String) {
        ROW("Row"),
        COLUMN("Column"),
    }
}
