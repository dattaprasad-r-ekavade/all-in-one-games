package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicRulesTest {
    @Test
    fun solutionCounterReturnsZeroOneAndTwoOnHandFixtures() {
        // Zero solutions: contradictory clues on overlapping area
        // (0,0) area size 4 has clue 0 (all 4 cells must be 0)
        // (0,1) area size 6 has clue 6, but 4 of its cells are forced to 0 by (0,0)
        val noSolution = MosaicPuzzle(
            id = "MO-ZERO",
            size = 3,
            clues = listOf(
                0, 6, null,
                null, null, null,
                null, null, null,
            ),
            solution = List(9) { 0 },
            pack = Pack.EASY,
            storedGrade = "pending",
        )

        // One solution: unique 3x3 pattern with all clues specified
        val solution3x3 = listOf(
            1, 0, 1,
            0, 1, 0,
            1, 0, 1,
        )
        val grid3 = Grid(3)
        val clues3x3 = (0 until 9).map { MosaicRules.clueAt(grid3, solution3x3, Cell(it)) }
        val unique3x3 = MosaicPuzzle(
            id = "MO-UNIQUE-3",
            size = 3,
            clues = clues3x3,
            solution = solution3x3,
            pack = Pack.EASY,
            storedGrade = "pending",
        )

        // Multiple solutions: 3x3 grid with no clues has 2^9 = 512 solutions
        val multipleSolutions = MosaicPuzzle(
            id = "MO-MULTI",
            size = 3,
            clues = List(9) { null },
            solution = List(9) { 0 },
            pack = Pack.EASY,
            storedGrade = "pending",
        )

        assertEquals(0, MosaicRules.countSolutions(noSolution))
        assertEquals(1, MosaicRules.countSolutions(unique3x3))
        assertEquals(2, MosaicRules.countSolutions(multipleSolutions, limit = 2))
        assertEquals(1, MosaicRules.countSolutions(multipleSolutions, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            MosaicRules.countSolutions(multipleSolutions, limit = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MosaicRules.countSolutions(multipleSolutions, limit = -1)
        }
    }

    @Test
    fun launchSizeEightByEightHandFixtureHasUniqueSolution() {
        val solution = listOf(
            1, 0, 0, 1, 1, 0, 0, 1,
            0, 1, 1, 0, 0, 1, 1, 0,
            0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1, 1, 0, 0, 1,
            1, 0, 0, 1, 1, 0, 0, 1,
            0, 1, 1, 0, 0, 1, 1, 0,
            0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1, 1, 0, 0, 1,
        )
        val grid = Grid(8)
        val clues = (0 until 64).map { MosaicRules.clueAt(grid, solution, Cell(it)) }
        val puzzle = MosaicPuzzle(
            id = "MO-EASY-8",
            size = 8,
            clues = clues,
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "none:1",
        )

        assertTrue(MosaicRules.isValidSolution(puzzle))
        assertEquals(1, MosaicRules.countSolutions(puzzle, limit = 2))
    }

    @Test
    fun isValidSolutionChecksEveryClueAndValidatesRange() {
        val solution = listOf(
            1, 0, 1,
            0, 1, 0,
            1, 0, 1,
        )
        val grid = Grid(3)
        val clues = (0 until 9).map { MosaicRules.clueAt(grid, solution, Cell(it)) }
        val puzzle = MosaicPuzzle("MO-TEST", 3, clues, solution, Pack.EASY, "pending")

        assertTrue(MosaicRules.isValidSolution(puzzle))
        // Bit flipped in solution
        val wrongBit = solution.toMutableList().also { it[0] = 0 }
        assertFalse(MosaicRules.isValidSolution(puzzle, wrongBit))
        // Wrong solution size
        assertFalse(MosaicRules.isValidSolution(puzzle, solution.dropLast(1)))
        // Non-binary values
        assertFalse(MosaicRules.isValidSolution(puzzle, solution.toMutableList().also { it[0] = 2 }))
    }

    @Test
    fun isSolvedRequiresFilledForOnesAndCrossedForZeros() {
        val solution = listOf(
            1, 0, 1,
            0, 1, 0,
            1, 0, 1,
        )
        val grid = Grid(3)
        val clues = (0 until 9).map { MosaicRules.clueAt(grid, solution, Cell(it)) }
        val puzzle = MosaicPuzzle("MO-TEST", 3, clues, solution, Pack.EASY, "pending")

        val state = MosaicState.initial(puzzle)
        assertFalse(MosaicRules.isSolved(puzzle, state))

        val solvedState = solution.indices.fold(state) { current, index ->
            val mark = if (solution[index] == 1) Mark.FILLED else Mark.CROSSED
            current.withMove(Move.SetMark(Cell(index), mark))
        }
        assertTrue(MosaicRules.isSolved(puzzle, solvedState))
        assertTrue(MosaicRules.mistakes(puzzle, solvedState).isEmpty())

        // Leaving one zero cell as EMPTY does not count as solved
        val incompleteState = solvedState.withMove(Move.SetMark(Cell(1), Mark.EMPTY))
        assertFalse(MosaicRules.isSolved(puzzle, incompleteState))

        // Inverting one cell is not solved
        val wrongMark = solvedState.withMove(Move.SetMark(Cell(0), Mark.CROSSED))
        assertFalse(MosaicRules.isSolved(puzzle, wrongMark))
    }

    @Test
    fun mistakesReportWrongCommittedMarksInRowMajorOrder() {
        val solution = listOf(
            1, 0, 1,
            0, 1, 0,
            1, 0, 1,
        )
        val grid = Grid(3)
        val clues = (0 until 9).map { MosaicRules.clueAt(grid, solution, Cell(it)) }
        val puzzle = MosaicPuzzle("MO-TEST", 3, clues, solution, Pack.EASY, "pending")

        val state = MosaicState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.CROSSED)) // cell 0 should be 1 (FILLED), mistake!
            .withMove(Move.SetMark(Cell(1), Mark.FILLED))  // cell 1 should be 0 (CROSSED), mistake!
            .withMove(Move.SetMark(Cell(2), Mark.EMPTY))   // cell 2 empty, not a mistake

        val errors = MosaicRules.mistakes(puzzle, state)
        assertEquals(listOf(Cell(0), Cell(1)), errors)
    }

    @Test
    fun puzzleConstructorRejectsClueExceedingAreaSize() {
        // Corner cell 0 in a 3x3 grid has area size 4 (self + 3 neighbours). Clue 5 is invalid.
        val invalidClues = listOf(
            5, null, null,
            null, null, null,
            null, null, null,
        )
        assertThrows(IllegalArgumentException::class.java) {
            MosaicPuzzle("MO-INV", 3, invalidClues, List(9) { 0 }, Pack.EASY, "pending")
        }
    }
}
