package com.datathecodie.witgrid.engine.skyscrapers.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersTechniqueSolver
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersTechniques
import com.datathecodie.witgrid.engine.skyscrapers.spec.EdgeClues
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.fullClues4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.puzzle
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.solution4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.topTwoRowGivens4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.nulls
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Skyscrapers technique cases: clue 1 and n, singles, and guess-free full solves. */
class SkyscrapersTechniqueSolverSpecTest {

    @Test
    fun clueNOnLeftEdgeForcesHeightsOneToNInOrder() {
        val clues = EdgeClues.empty(4).copy(left = listOf(4, null, null, null))
        val puzzle = puzzle(4, solution4, clues)

        val step = requireNotNull(SkyscrapersTechniqueSolver.nextStep(puzzle, SkyscrapersState.initial(puzzle)))

        assertEquals(SkyscrapersTechniques.CLUE_1_AND_N, step.technique)
        // Only the first row is forced. Every placed digit must equal its position in that row plus one.
        step.moves.forEach { move ->
            val digitMove = move as Move.SetDigit
            assertTrue(digitMove.cell.index < 4)
            assertEquals(digitMove.cell.index + 1, digitMove.digit)
        }
    }

    @Test
    fun clueOneOnTopEdgePlacesTheTallestBuildingFirst() {
        val clues = EdgeClues.empty(4).copy(top = listOf(1, null, null, null))
        val puzzle = puzzle(4, solution4, clues)

        val step = requireNotNull(SkyscrapersTechniqueSolver.nextStep(puzzle, SkyscrapersState.initial(puzzle)))

        assertEquals(SkyscrapersTechniques.CLUE_1_AND_N, step.technique)
        assertEquals(listOf(Move.SetDigit(Cell(0), 4)), step.moves)
    }

    @Test
    fun nakedSingleIsPlacedWhenOneDigitFitsRowAndColumn() {
        // Rows 1 to 3 are given, so each empty cell in row 4 has exactly one digit left.
        val givens = List(16) { index -> if (index < 12) solution4[index] else null }
        val puzzle = puzzle(4, solution4, EdgeClues.empty(4), givens)

        val step = requireNotNull(SkyscrapersTechniqueSolver.nextStep(puzzle, SkyscrapersState.initial(puzzle)))

        assertEquals(SkyscrapersTechniques.NAKED_SINGLE, step.technique)
        val digitMove = step.moves.single() as Move.SetDigit
        assertTrue(digitMove.cell.index >= 12)
        assertEquals(solution4[digitMove.cell.index], digitMove.digit)
    }

    @Test
    fun handMadeFourByFourSolvesWithoutGuessingAndEveryPlacementIsTrue() {
        val puzzle = puzzle(4, solution4, fullClues4, topTwoRowGivens4)

        val (solved, steps) = SkyscrapersTechniqueSolver.solveWithHints(puzzle)

        assertTrue(SkyscrapersRules.isSolved(puzzle, solved))
        assertTrue(steps.isNotEmpty())
        steps.forEach { step ->
            assertTrue(step.technique in SkyscrapersTechniques.all)
            step.moves.forEach { move ->
                val digitMove = move as Move.SetDigit
                assertEquals(solution4[digitMove.cell.index], digitMove.digit)
            }
        }
    }

    @Test
    fun gradeReportsAHardestTechniqueForAUniquePuzzle() {
        val puzzle = puzzle(4, solution4, fullClues4, topTwoRowGivens4)

        val grade = SkyscrapersTechniqueSolver.grade(puzzle)

        assertNotNull(grade.hardestTechnique)
        assertTrue(grade.stepCount > 0)
        assertTrue(grade.hardestTechnique in SkyscrapersTechniques.all)
    }

    @Test
    fun gradeRefusesAPuzzleWithTwoSolutions() {
        val clues = fullClues4.copy(
            bottom = listOf(1, 2, null, null),
            right = listOf(1, 2, null, null),
        )
        val puzzle = puzzle(4, solution4, clues, topTwoRowGivens4)

        assertEquals(2, SkyscrapersRules.countSolutions(puzzle, limit = 2))
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersTechniqueSolver.grade(puzzle)
        }
    }

    @Test
    fun emptyCluesAndGivensLeaveTheSolverWithNoDeduction() {
        val puzzle = puzzle(4, solution4, EdgeClues.empty(4), nulls(16))

        assertNull(SkyscrapersTechniqueSolver.nextStep(puzzle, SkyscrapersState.initial(puzzle)))
    }
}
