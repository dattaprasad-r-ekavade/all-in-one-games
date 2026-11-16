package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique

/** Guess-free Crowns deductions, deterministic hint selection and grading. */
object CrownsTechniqueSolver {
    /** Returns the easiest next deduction, or null for a solved, mistaken or stuck board. */
    fun nextStep(puzzle: CrownsPuzzle, state: CrownsState): HintStep? {
        require(state.puzzle == puzzle) { "Crowns state belongs to a different puzzle" }
        if (CrownsRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (CrownsRules.isSolved(puzzle, state)) return null

        touchingCrownsStep(puzzle, state)?.let { return it }
        val candidates = legalCandidates(puzzle, state)
        regionLineStep(puzzle, candidates)?.let { return it }
        return kRegionsKLinesStep(puzzle, candidates)
    }

    /** Solves from the givens with the published techniques and returns the hardest-step grade. */
    fun grade(puzzle: CrownsPuzzle): Grade {
        require(CrownsRules.countSolutions(puzzle, limit = 2) == 1) {
            "Crowns grading requires exactly one solution"
        }
        val pack = CrownsPackBands.packForSize(puzzle.size)
        require(puzzle.pack == pack) { "Crowns pack must match its board size" }

        var state = CrownsState.initial(puzzle)
        val visited = hashSetOf(state)
        var stepCount = 0
        var hardestTechnique: Technique? = null
        while (!CrownsRules.isSolved(puzzle, state)) {
            val step = requireNotNull(nextStep(puzzle, state)) {
                "Crowns puzzle is stuck with the supported techniques"
            }
            val nextState = step.moves.fold(state, CrownsState::withMove)
            require(nextState != state && visited.add(nextState)) {
                "Crowns hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }
        return Grade(hardestTechnique, stepCount, pack)
    }

    private fun touchingCrownsStep(puzzle: CrownsPuzzle, state: CrownsState): HintStep? {
        for (index in state.marks.indices) {
            if (state.marks[index] != Mark.FILLED) continue
            val crown = Cell(index)
            val neighbours = puzzle.grid.neighbours(crown)
            val moves = neighbours
                .filter { state.marks[it.index] == Mark.EMPTY }
                .map { Move.SetMark(it, Mark.CROSSED) }
            if (moves.isNotEmpty()) {
                return HintStep(
                    technique = CrownsTechniques.TOUCH_EXCLUSION,
                    cells = immutableList(listOf(crown) + neighbours),
                    explanation = "A crown excludes every cell that touches it, including diagonals.",
                    moves = immutableList(moves),
                )
            }
        }
        return null
    }

    private fun legalCandidates(puzzle: CrownsPuzzle, state: CrownsState): List<List<Cell>> {
        val size = puzzle.size
        val blocked = BooleanArray(puzzle.grid.cellCount)
        for (index in state.marks.indices) {
            if (state.marks[index] != Mark.FILLED) continue
            val crown = Cell(index)
            val crownRow = index / size
            val crownColumn = index % size
            for (line in 0 until size) {
                blocked[crownRow * size + line] = true
                blocked[line * size + crownColumn] = true
            }
            for (cell in 0 until puzzle.grid.cellCount) {
                if (puzzle.regions[cell] == puzzle.regions[index]) blocked[cell] = true
            }
            puzzle.grid.neighbours(crown).forEach { blocked[it.index] = true }
            blocked[index] = true
        }

        return (0 until size).map { region ->
            (0 until puzzle.grid.cellCount)
                .filter { index ->
                    state.marks[index] == Mark.EMPTY &&
                        !blocked[index] &&
                        puzzle.regions[index] == region
                }
                .map(::Cell)
        }
    }

    private fun regionLineStep(puzzle: CrownsPuzzle, candidates: List<List<Cell>>): HintStep? {
        val size = puzzle.size
        for (region in candidates.indices) {
            val cells = candidates[region]
            if (cells.isEmpty()) continue
            if (cells.size == 1) {
                return HintStep(
                    technique = CrownsTechniques.REGION_LINE,
                    cells = immutableList(cells),
                    explanation = "Region ${regionName(region)} has only one legal cell, so place a crown there.",
                    moves = immutableList(listOf(Move.SetMark(cells.single(), Mark.FILLED))),
                )
            }

            val rows = cells.map { it.index / size }.toSet()
            val columns = cells.map { it.index % size }.toSet()
            val lineCells: List<Cell>
            val lineName: String
            val lineNumber: Int
            if (rows.size == 1) {
                lineNumber = rows.single()
                lineCells = cellsInRow(size, lineNumber)
                lineName = "row"
            } else if (columns.size == 1) {
                lineNumber = columns.single()
                lineCells = cellsInColumn(size, lineNumber)
                lineName = "column"
            } else {
                continue
            }

            val moves = lineCells
                .filter { puzzle.regions[it.index] != region && it in candidates[puzzle.regions[it.index]] }
                .map { Move.SetMark(it, Mark.CROSSED) }
            if (moves.isNotEmpty()) {
                return HintStep(
                    technique = CrownsTechniques.REGION_LINE,
                    cells = immutableList(cells),
                    explanation = "Region ${regionName(region)} is confined to $lineName ${lineNumber + 1}, so other regions cannot use that line.",
                    moves = immutableList(moves),
                )
            }
        }
        return null
    }

    private fun kRegionsKLinesStep(puzzle: CrownsPuzzle, candidates: List<List<Cell>>): HintStep? {
        // A region with a placed crown needs no further line, so it cannot reserve another one.
        val remainingRegions = candidates.indices.filter { candidates[it].isNotEmpty() }
        for (subsetSize in 2 until remainingRegions.size) {
            for (regions in regionSubsets(remainingRegions.size, subsetSize)) {
                val regionIds = regions.map { remainingRegions[it] }
                rowSubsetStep(puzzle, candidates, regionIds)?.let { return it }
                columnSubsetStep(puzzle, candidates, regionIds)?.let { return it }
            }
        }
        return null
    }

    private fun rowSubsetStep(
        puzzle: CrownsPuzzle,
        candidates: List<List<Cell>>,
        regions: List<Int>,
    ): HintStep? {
        val rows = regions.flatMap { region -> candidates[region].map { it.index / puzzle.size } }.toSet()
        if (rows.size != regions.size) return null
        val moves = candidates.indices
            .filter { it !in regions }
            .flatMap { region -> candidates[region].filter { it.index / puzzle.size in rows } }
            .distinct()
            .sortedBy { it.index }
            .map { Move.SetMark(it, Mark.CROSSED) }
        if (moves.isEmpty()) return null
        return HintStep(
            technique = CrownsTechniques.K_REGIONS_K_LINES,
            cells = immutableList(regions.flatMap { candidates[it] }.distinct().sortedBy { it.index }),
            explanation = "Regions ${regionNames(regions)} can place their crowns only in rows ${lineNumbers(rows)}, so other regions cannot use those rows.",
            moves = immutableList(moves),
        )
    }

    private fun columnSubsetStep(
        puzzle: CrownsPuzzle,
        candidates: List<List<Cell>>,
        regions: List<Int>,
    ): HintStep? {
        val columns = regions.flatMap { region -> candidates[region].map { it.index % puzzle.size } }.toSet()
        if (columns.size != regions.size) return null
        val moves = candidates.indices
            .filter { it !in regions }
            .flatMap { region -> candidates[region].filter { it.index % puzzle.size in columns } }
            .distinct()
            .sortedBy { it.index }
            .map { Move.SetMark(it, Mark.CROSSED) }
        if (moves.isEmpty()) return null
        return HintStep(
            technique = CrownsTechniques.K_REGIONS_K_LINES,
            cells = immutableList(regions.flatMap { candidates[it] }.distinct().sortedBy { it.index }),
            explanation = "Regions ${regionNames(regions)} can place their crowns only in columns ${lineNumbers(columns)}, so other regions cannot use those columns.",
            moves = immutableList(moves),
        )
    }

    private fun cellsInRow(size: Int, row: Int): List<Cell> =
        (0 until size).map { column -> Cell(row * size + column) }

    private fun cellsInColumn(size: Int, column: Int): List<Cell> =
        (0 until size).map { row -> Cell(row * size + column) }

    private fun regionSubsets(regionCount: Int, subsetSize: Int): Sequence<List<Int>> = sequence {
        val selected = ArrayList<Int>(subsetSize)
        suspend fun SequenceScope<List<Int>>.choose(start: Int) {
            if (selected.size == subsetSize) {
                yield(selected.toList())
                return
            }
            val remaining = subsetSize - selected.size
            for (region in start..regionCount - remaining) {
                selected.add(region)
                choose(region + 1)
                selected.removeAt(selected.lastIndex)
            }
        }
        choose(0)
    }

    private fun regionName(region: Int): Char = 'A' + region

    private fun regionNames(regions: List<Int>): String = regions
        .map(::regionName)
        .joinToString(separator = " and ")

    private fun lineNumbers(lines: Set<Int>): String = lines.sorted().joinToString(
        separator = " and ",
    ) { (it + 1).toString() }
}
