package com.datathecodie.witgrid.engine.crowns.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.crowns.CrownsPuzzle
import com.datathecodie.witgrid.engine.crowns.CrownsRules
import com.datathecodie.witgrid.engine.crowns.CrownsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent plan-rule cases, using hand-built boards rather than generator output. */
class CrownsRulesSpecTest {
    @Test
    fun validLayoutHasOneCrownPerRowColumnAndRegionWithoutTouching() {
        val puzzle = puzzle()

        assertTrue(CrownsRules.isValidSolution(puzzle))
        assertFalse(CrownsRules.isValidSolution(puzzle, listOf(1, 3, 5, 0, 1, 4)))
        assertFalse(CrownsRules.isValidSolution(puzzle, listOf(0, 1, 3, 5, 2, 4)))
        assertFalse(CrownsRules.isValidSolution(puzzle, listOf(1, 3, 5, 0, 2, 4, 6)))
    }

    @Test
    fun eachRegionCanOnlyHoldOneCrown() {
        val puzzle = puzzle()
        val regions = puzzle.regions.toMutableList()
        regions[1 * puzzle.size + puzzle.solutionColumns[1]] = 0
        val repeatedRegion = puzzle(
            regions = regions,
        )

        assertFalse(CrownsRules.isValidSolution(repeatedRegion))
    }

    @Test
    fun solutionCounterDistinguishesNoSingleAndMultipleSolutions() {
        val noSolution = puzzle(
            size = 2,
            solution = listOf(0, 1),
        )
        val oneSolution = puzzle(
            size = 1,
            solution = listOf(0),
        )
        val multipleSolutions = puzzle()

        assertEquals(0, CrownsRules.countSolutions(noSolution, limit = 2))
        assertEquals(1, CrownsRules.countSolutions(oneSolution, limit = 2))
        assertEquals(2, CrownsRules.countSolutions(multipleSolutions, limit = 2))
    }

    @Test
    fun mistakesIncludeWrongCrownAndCrossOnTheStoredCrown() {
        val puzzle = puzzle()
        val state = CrownsState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.FILLED))
            .withMove(Move.SetMark(Cell(puzzle.solutionColumns[0]), Mark.CROSSED))

        assertEquals(listOf(Cell(0), Cell(puzzle.solutionColumns[0])), CrownsRules.mistakes(puzzle, state))
    }

    @Test
    fun solvedBoardMayLeaveNonCrownCellsEmptyOrCrossed() {
        val puzzle = puzzle()
        val solvedState = puzzle.solutionColumns.mapIndexed { row, column ->
            Move.SetMark(puzzle.grid.cell(row, column), Mark.FILLED)
        }.fold(CrownsState.initial(puzzle), CrownsState::withMove)
            .withMove(Move.SetMark(Cell(0), Mark.CROSSED))

        assertTrue(CrownsRules.isSolved(puzzle, solvedState))
        assertTrue(CrownsRules.mistakes(puzzle, solvedState).isEmpty())
    }

    private fun puzzle(
        size: Int = 6,
        solution: List<Int> = listOf(1, 3, 5, 0, 2, 4),
        regions: List<Int> = (0 until size).flatMap { row -> List(size) { row } },
    ): CrownsPuzzle = CrownsPuzzle(
        id = "CR-SPEC-$size",
        size = size,
        regions = regions,
        solutionColumns = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )
}
