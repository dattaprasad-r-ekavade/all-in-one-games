package com.datathecodie.witgrid.engine.futoshiki.spec

import com.datathecodie.witgrid.engine.futoshiki.FutoshikiRules
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiSign
import com.datathecodie.witgrid.engine.futoshiki.spec.FutoshikiSpecFixtures.cyclicSquare
import com.datathecodie.witgrid.engine.futoshiki.spec.FutoshikiSpecFixtures.puzzle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent checks of the Latin-square and adjacent inequality rules. */
class FutoshikiRulesSpecTest {
    @Test
    fun sizesFiveThroughSevenAcceptLatinSquaresAndRejectRowOrColumnRepeats() {
        for (size in 5..7) {
            val board = puzzle(size)
            val answer = cyclicSquare(size)
            assertTrue(FutoshikiRules.isValidSolution(board, answer), "size $size Latin square")
            assertFalse(FutoshikiRules.isValidSolution(board, answer.toMutableList().also { it[1] = it[0] }), "size $size row")
            assertFalse(FutoshikiRules.isValidSolution(board, answer.toMutableList().also { it[size] = it[0] }), "size $size column")
            assertFalse(FutoshikiRules.isValidSolution(board, answer.dropLast(1)), "size $size short board")
        }
    }

    @Test
    fun horizontalAndVerticalSignsAreEnforcedInTheirOwnDirections() {
        val answer = cyclicSquare(5)
        val horizontal = List(20) { FutoshikiSign.NONE }.toMutableList().also { it[0] = FutoshikiSign.LESS }
        val vertical = List(20) { FutoshikiSign.NONE }.toMutableList().also { it[0] = FutoshikiSign.LESS }
        val board = puzzle(5, horizontal = horizontal, vertical = vertical)

        assertTrue(FutoshikiRules.isValidSolution(board, answer))
        val wrongHorizontal = horizontal.toMutableList().also { it[0] = FutoshikiSign.GREATER }
        val wrongVertical = vertical.toMutableList().also { it[0] = FutoshikiSign.GREATER }
        assertFalse(FutoshikiRules.isValidSolution(puzzle(5, horizontal = wrongHorizontal, vertical = vertical), answer))
        assertFalse(FutoshikiRules.isValidSolution(puzzle(5, horizontal = horizontal, vertical = wrongVertical), answer))
        assertEquals(2, board.signCount)
    }

    @Test
    fun solutionCounterStopsAtItsCapAndRejectsContradictoryGivens() {
        val open = puzzle(5)
        assertEquals(2, FutoshikiRules.countSolutions(open, limit = 2))
        val givens = List<Int?>(25) { null }.toMutableList().also { it[0] = 1; it[1] = 1 }
        assertEquals(0, FutoshikiRules.countSolutions(puzzle(5, givens = givens)))
    }
}
