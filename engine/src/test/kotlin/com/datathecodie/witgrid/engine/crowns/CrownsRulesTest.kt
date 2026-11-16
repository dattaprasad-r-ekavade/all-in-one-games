package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrownsRulesTest {
    @Test
    fun solutionCounterReturnsZeroOneAndTwoWithCap() {
        assertEquals(0, CrownsRules.countSolutions(puzzle(size = 2, solution = listOf(0, 1))))
        assertEquals(1, CrownsRules.countSolutions(puzzle(size = 1, solution = listOf(0))))
        assertEquals(2, CrownsRules.countSolutions(puzzle(size = 4, solution = listOf(1, 3, 0, 2))))
        assertEquals(1, CrownsRules.countSolutions(puzzle(size = 4, solution = listOf(1, 3, 0, 2)), limit = 1))
    }

    @Test
    fun regionsReduceLaunchSizePuzzleFromMultipleSolutionsToOne() {
        val solution = listOf(1, 3, 5, 0, 2, 4)
        val rowRegions = (0 until 6).flatMap { row -> List(6) { row } }
        val uniqueRegions = listOf(
            "100230",
            "024124",
            "253422",
            "350542",
            "444502",
            "242354",
        ).flatMap { row -> row.map { it.digitToInt() } }

        assertEquals(2, CrownsRules.countSolutions(puzzle(size = 6, solution = solution, regions = rowRegions)))
        val uniquePuzzle = puzzle(size = 6, solution = solution, regions = uniqueRegions)
        assertTrue(CrownsRules.isValidSolution(uniquePuzzle))
        assertEquals(1, CrownsRules.countSolutions(uniquePuzzle))
    }

    @Test
    fun bankCodecRoundTripsLaunchPuzzle() {
        val puzzle = launchPuzzle()

        assertEquals(puzzle, CrownsCodec.decode(CrownsCodec.encode(puzzle)))
    }

    @Test
    fun engineExposesCrownsModelThroughSharedContract() {
        val puzzle = launchPuzzle()
        val line = CrownsEngine.encodePuzzle(puzzle)
        val decoded = CrownsEngine.decodePuzzle(line)
        var state = CrownsEngine.initialState(decoded)
        state = CrownsEngine.applyMove(
            state,
            Move.SetMark(decoded.grid.cell(0, decoded.solutionColumns[0]), Mark.FILLED),
        )

        assertEquals(line, CrownsEngine.encodePuzzle(decoded))
        assertEquals(2, CrownsEngine.countSolutions(decoded))
        assertEquals(emptyList<Cell>(), CrownsEngine.mistakes(decoded, state))
        assertFalse(CrownsEngine.isSolved(decoded, state))
    }

    @Test
    fun bankCodecRejectsInvalidStoredSolutionAndPayload() {
        val puzzle = launchPuzzle()
        val line = CrownsCodec.encode(puzzle)
        val fields = line.split('|')
        val invalidSolution = fields.toMutableList().also { it[3] = "000000" }.joinToString("|")
        val invalidPayload = fields.toMutableList().also { it[2] = "0" }.joinToString("|")

        assertThrows(IllegalArgumentException::class.java) { CrownsCodec.decode(invalidSolution) }
        assertThrows(IllegalArgumentException::class.java) { CrownsCodec.decode(invalidPayload) }
    }

    @Test
    fun movesAndMistakesUseRowMajorCellsAndXOnCrownIsWrong() {
        val puzzle = launchPuzzle()
        var state = CrownsState.initial(puzzle)
        state = state.withMove(Move.SetMark(Cell(0), Mark.FILLED))
        state = state.withMove(Move.SetMark(Cell(puzzle.solutionColumns[0]), Mark.CROSSED))

        assertEquals(listOf(Cell(0), Cell(puzzle.solutionColumns[0])), CrownsRules.mistakes(puzzle, state))
        assertFalse(CrownsRules.isSolved(puzzle, state))
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetMark(Cell(puzzle.grid.cellCount), Mark.FILLED))
        }
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(0), 1))
        }
    }

    @Test
    fun correctCrownsSolveWithoutRequiringXsInEveryOtherCell() {
        val puzzle = launchPuzzle()
        var state = CrownsState.initial(puzzle)
        puzzle.solutionColumns.forEachIndexed { row, column ->
            state = state.withMove(Move.SetMark(puzzle.grid.cell(row, column), Mark.FILLED))
        }

        assertTrue(CrownsRules.isSolved(puzzle, state))
        assertTrue(CrownsRules.mistakes(puzzle, state).isEmpty())
    }

    @Test
    fun stateCopiesMarksAndPuzzleCopiesRegions() {
        val regions = (0 until 6).flatMap { row -> List(6) { row } }.toMutableList()
        val solution = listOf(1, 3, 5, 0, 2, 4).toMutableList()
        val puzzle = CrownsPuzzle("CR-COPY", 6, regions, solution, Pack.EASY, "pending")
        regions[0] = 5
        solution[0] = 0
        val state = CrownsState.initial(puzzle)
        val marks = state.marks as MutableList<Mark>

        assertThrows(UnsupportedOperationException::class.java) { marks[0] = Mark.FILLED }
        assertEquals(0, puzzle.regions[0])
        assertEquals(1, puzzle.solutionColumns[0])
    }

    private fun launchPuzzle(): CrownsPuzzle = puzzle(
        size = 6,
        solution = listOf(1, 3, 5, 0, 2, 4),
        regions = (0 until 6).flatMap { row -> List(6) { row } },
    )

    private fun puzzle(
        size: Int,
        solution: List<Int>,
        regions: List<Int> = (0 until size).flatMap { row -> List(size) { row } },
    ): CrownsPuzzle = CrownsPuzzle(
        id = "CR-FIXTURE-$size",
        size = size,
        regions = regions,
        solutionColumns = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )
}
