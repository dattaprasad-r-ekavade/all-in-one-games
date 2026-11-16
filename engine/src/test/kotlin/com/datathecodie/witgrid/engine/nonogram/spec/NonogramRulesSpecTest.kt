package com.datathecodie.witgrid.engine.nonogram.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.nonogram.NonogramPuzzle
import com.datathecodie.witgrid.engine.nonogram.NonogramRules
import com.datathecodie.witgrid.engine.nonogram.NonogramState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Nonogram plan-rule cases built from hand-written patterns and clues. */
class NonogramRulesSpecTest {
    @Test
    fun rowAndColumnCluesDescribeFilledRunLengthsInSequence() {
        val puzzle = clueOrderFixture()

        assertTrue(NonogramRules.isValidSolution(puzzle))
        assertFalse(NonogramRules.isValidSolution(puzzle, puzzle.solution.toMutableList().also {
            it[0] = 0
        }))
        assertFalse(NonogramRules.isValidSolution(puzzle, puzzle.solution.dropLast(1)))
    }

    @Test
    fun solutionCounterFindsZeroOneAndMultipleHandBuiltPatterns() {
        val impossible = puzzle(
            rowClues = List(5) { listOf(5) },
            columnClues = List(5) { emptyList() },
            solutionRows = List(5) { "00000" },
        )
        val unique = stripedFixture()
        val multiple = puzzle(
            rowClues = List(5) { listOf(1) },
            columnClues = List(5) { listOf(1) },
            solutionRows = listOf("10000", "01000", "00100", "00010", "00001"),
        )

        assertEquals(0, NonogramRules.countSolutions(impossible, limit = 2))
        assertEquals(1, NonogramRules.countSolutions(unique, limit = 2))
        assertEquals(2, NonogramRules.countSolutions(multiple, limit = 2))
    }

    @Test
    fun fillAndCrossMarksFollowTheStoredPatternForMistakesAndSolvedState() {
        val puzzle = stripedFixture()
        val partial = NonogramState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.CROSSED))
            .withMove(Move.SetMark(Cell(5), Mark.FILLED))
            .withMove(Move.SetMark(Cell(6), Mark.CROSSED))

        assertEquals(listOf(Cell(0), Cell(5)), NonogramRules.mistakes(puzzle, partial))

        val complete = puzzle.solution.indices.fold(NonogramState.initial(puzzle)) { state, index ->
            state.withMove(
                Move.SetMark(
                    Cell(index),
                    if (puzzle.solution[index] == 1) Mark.FILLED else Mark.CROSSED,
                ),
            )
        }
        val wrongCross = complete.withMove(Move.SetMark(Cell(0), Mark.CROSSED))
        val missingMark = complete.withMove(Move.SetMark(Cell(5), Mark.EMPTY))

        assertTrue(NonogramRules.isSolved(puzzle, complete))
        assertFalse(NonogramRules.isSolved(puzzle, NonogramState.initial(puzzle)))
        assertFalse(NonogramRules.isSolved(puzzle, wrongCross))
        assertFalse(NonogramRules.isSolved(puzzle, missingMark))
    }

    private fun clueOrderFixture(): NonogramPuzzle = puzzle(
        rowClues = listOf(listOf(2, 1), listOf(3), emptyList(), listOf(1, 1, 1), listOf(3)),
        columnClues = listOf(
            listOf(1, 2),
            listOf(1, 1),
            listOf(1, 2),
            listOf(2),
            listOf(1, 1),
        ),
        solutionRows = listOf("11010", "00111", "00000", "10101", "11100"),
    )

    private fun stripedFixture(): NonogramPuzzle = puzzle(
        rowClues = listOf(listOf(5), emptyList(), listOf(5), emptyList(), listOf(5)),
        columnClues = List(5) { listOf(1, 1, 1) },
        solutionRows = listOf("11111", "00000", "11111", "00000", "11111"),
    )

    private fun puzzle(
        rowClues: List<List<Int>>,
        columnClues: List<List<Int>>,
        solutionRows: List<String>,
    ): NonogramPuzzle = NonogramPuzzle(
        id = "NO-SPEC-FIXTURE",
        size = 5,
        rowClues = rowClues,
        columnClues = columnClues,
        solution = solutionRows.flatMap { row -> row.map(Char::digitToInt) },
        pack = Pack.EASY,
        storedGrade = "pending",
    )
}
