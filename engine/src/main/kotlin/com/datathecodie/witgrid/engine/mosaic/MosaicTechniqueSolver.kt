package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique

/** Guess-free Mosaic deductions, deterministic hint selection and technique grading. */
object MosaicTechniqueSolver {
    /** Returns the easiest next deduction, or null for a solved, mistaken or technique-stuck board. */
    fun nextStep(puzzle: MosaicPuzzle, state: MosaicState): HintStep? {
        require(state.puzzle == puzzle) { "Mosaic state belongs to a different puzzle" }
        if (MosaicRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (MosaicRules.isSolved(puzzle, state)) return null

        emptyAreaStep(puzzle, state)?.let { return it }
        fullAreaStep(puzzle, state)?.let { return it }
        remainingCountStep(puzzle, state)?.let { return it }
        return clueOverlapStep(puzzle, state)
    }

    /** Solves from clues without guessing and returns the hardest technique and hint step count. */
    fun grade(puzzle: MosaicPuzzle): Grade {
        require(MosaicRules.isValidSolution(puzzle)) {
            "Mosaic grading requires a valid stored solution"
        }
        require(puzzle.size in MosaicPackBands.launchSizes) {
            "Mosaic bank size must be 8, 10, 12 or 15"
        }
        val pack = MosaicPackBands.packForSize(puzzle.size)
        require(puzzle.pack == pack) {
            "Mosaic pack must match its board size"
        }
        require(MosaicRules.countSolutions(puzzle, limit = 2) == 1) {
            "Mosaic grading requires exactly one solution"
        }

        val grade = gradeByTechniques(puzzle)
        require(MosaicPackBands.isGradeInPackBand(pack, grade)) {
            "Mosaic grade does not fit the $pack pack band"
        }
        return grade
    }

    /** Computes a technique grade without solution counting, for generator and validator checks. */
    internal fun gradeByTechniques(puzzle: MosaicPuzzle): Grade {
        var state = MosaicState.initial(puzzle)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        val visited = hashSetOf(state)

        while (!MosaicRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Mosaic puzzle is stuck with the supported techniques"
            }
            val nextState = step.moves.fold(state, MosaicState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Mosaic hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }
        return Grade(hardestTechnique, stepCount, puzzle.pack)
    }

    private fun emptyAreaStep(puzzle: MosaicPuzzle, state: MosaicState): HintStep? {
        for (index in 0 until puzzle.grid.cellCount) {
            val clue = puzzle.clues[index] ?: continue
            if (clue != 0) continue
            val clueCell = Cell(index)
            val area = MosaicRules.areaCells(puzzle.grid, clueCell)
            val unknowns = area.filter { state.marks[it.index] == Mark.EMPTY }
            if (unknowns.isEmpty()) continue

            val moves = unknowns.sortedBy { it.index }.map { Move.SetMark(it, Mark.CROSSED) }
            val cells = area.sortedBy { it.index }
            return HintStep(
                technique = MosaicTechniques.EMPTY_AREA,
                cells = immutableList(cells),
                explanation = "A clue of 0 means every cell in its 3x3 area must be crossed.",
                moves = immutableList(moves),
            )
        }
        return null
    }

    private fun fullAreaStep(puzzle: MosaicPuzzle, state: MosaicState): HintStep? {
        for (index in 0 until puzzle.grid.cellCount) {
            val clue = puzzle.clues[index] ?: continue
            val clueCell = Cell(index)
            val area = MosaicRules.areaCells(puzzle.grid, clueCell)
            if (clue != area.size) continue
            val unknowns = area.filter { state.marks[it.index] == Mark.EMPTY }
            if (unknowns.isEmpty()) continue

            val moves = unknowns.sortedBy { it.index }.map { Move.SetMark(it, Mark.FILLED) }
            val cells = area.sortedBy { it.index }
            return HintStep(
                technique = MosaicTechniques.FULL_AREA,
                cells = immutableList(cells),
                explanation = "A clue of $clue in an area of ${area.size} cells means every cell in its 3x3 area must be filled.",
                moves = immutableList(moves),
            )
        }
        return null
    }

    private fun remainingCountStep(puzzle: MosaicPuzzle, state: MosaicState): HintStep? {
        for (index in 0 until puzzle.grid.cellCount) {
            val clue = puzzle.clues[index] ?: continue
            val clueCell = Cell(index)
            val area = MosaicRules.areaCells(puzzle.grid, clueCell)
            if (clue == 0 || clue == area.size) continue

            val filledCount = area.count { state.marks[it.index] == Mark.FILLED }
            val unknowns = area.filter { state.marks[it.index] == Mark.EMPTY }
            if (unknowns.isEmpty()) continue

            val remainingNeeded = clue - filledCount
            if (remainingNeeded < 0 || remainingNeeded > unknowns.size) continue

            if (remainingNeeded == 0) {
                val moves = unknowns.sortedBy { it.index }.map { Move.SetMark(it, Mark.CROSSED) }
                val cells = area.sortedBy { it.index }
                return HintStep(
                    technique = MosaicTechniques.REMAINING_COUNT,
                    cells = immutableList(cells),
                    explanation = "A clue of $clue already has all its filled cells, so its remaining unknown cells must be crossed.",
                    moves = immutableList(moves),
                )
            }

            if (remainingNeeded == unknowns.size) {
                val moves = unknowns.sortedBy { it.index }.map { Move.SetMark(it, Mark.FILLED) }
                val cells = area.sortedBy { it.index }
                return HintStep(
                    technique = MosaicTechniques.REMAINING_COUNT,
                    cells = immutableList(cells),
                    explanation = "A clue of $clue needs $remainingNeeded more filled cells and has only $remainingNeeded unknown cells left, so they must all be filled.",
                    moves = immutableList(moves),
                )
            }
        }
        return null
    }

    private fun clueOverlapStep(puzzle: MosaicPuzzle, state: MosaicState): HintStep? {
        val clueIndices = (0 until puzzle.grid.cellCount).filter { puzzle.clues[it] != null }

        for (i in clueIndices) {
            val clueA = puzzle.clues[i]!!
            val cellA = Cell(i)
            val areaA = MosaicRules.areaCells(puzzle.grid, cellA)
            val unknownsA = areaA.filter { state.marks[it.index] == Mark.EMPTY }
            if (unknownsA.isEmpty()) continue
            val filledA = areaA.count { state.marks[it.index] == Mark.FILLED }
            val remA = clueA - filledA
            if (remA < 0 || remA > unknownsA.size) continue

            val rowA = puzzle.grid.row(cellA)
            val columnA = puzzle.grid.column(cellA)

            for (j in clueIndices) {
                if (i == j) continue
                val rowB = j / puzzle.size
                val columnB = j % puzzle.size
                if (kotlin.math.abs(rowA - rowB) > 2 || kotlin.math.abs(columnA - columnB) > 2) continue

                val clueB = puzzle.clues[j]!!
                val cellB = Cell(j)
                val areaB = MosaicRules.areaCells(puzzle.grid, cellB)
                val unknownsB = areaB.filter { state.marks[it.index] == Mark.EMPTY }
                if (unknownsB.isEmpty()) continue
                val filledB = areaB.count { state.marks[it.index] == Mark.FILLED }
                val remB = clueB - filledB
                if (remB < 0 || remB > unknownsB.size) continue

                val intersection = unknownsA.filter { cell -> unknownsB.any { it.index == cell.index } }
                if (intersection.isEmpty()) continue

                val onlyA = unknownsA.filter { cell -> unknownsB.none { it.index == cell.index } }
                val onlyB = unknownsB.filter { cell -> unknownsA.none { it.index == cell.index } }

                // Subset rule cross: unknownsA is a subset of unknownsB, and both need the same remaining fills
                if (onlyA.isEmpty() && onlyB.isNotEmpty() && remB == remA) {
                    val moves = onlyB.sortedBy { it.index }.map { Move.SetMark(it, Mark.CROSSED) }
                    val cells = (areaA + areaB).distinct().sortedBy { it.index }
                    val explanation = "The clues at row ${rowA + 1}, column ${columnA + 1} and " +
                        "row ${rowB + 1}, column ${columnB + 1} both have a remaining fill count of $remA, " +
                        "and all unmarked cells in the first area are shared, " +
                        "so cross the unmarked cells exclusive to the second area."
                    return HintStep(
                        technique = MosaicTechniques.CLUE_OVERLAP,
                        cells = immutableList(cells),
                        explanation = explanation,
                        moves = immutableList(moves),
                    )
                }

                // Overlap fill: remaining needed fills difference equals outside cell count
                if (onlyB.isNotEmpty() && remB - remA == onlyB.size) {
                    val fillMoves = onlyB.sortedBy { it.index }.map { Move.SetMark(it, Mark.FILLED) }
                    val crossMoves = onlyA.sortedBy { it.index }.map { Move.SetMark(it, Mark.CROSSED) }
                    val moves = fillMoves + crossMoves
                    val cells = (areaA + areaB).distinct().sortedBy { it.index }
                    val explanation = "The clues at row ${rowA + 1}, column ${columnA + 1} and " +
                        "row ${rowB + 1}, column ${columnB + 1} overlap, and the second clue needs more fills " +
                        "by exactly the number of unmarked cells exclusive to the second area, " +
                        "so fill all unmarked cells exclusive to the second area" +
                        if (onlyA.isEmpty()) "." else " and cross all unmarked cells exclusive to the first area."
                    return HintStep(
                        technique = MosaicTechniques.CLUE_OVERLAP,
                        cells = immutableList(cells),
                        explanation = explanation,
                        moves = immutableList(moves),
                    )
                }
            }
        }
        return null
    }
}
