package com.datathecodie.witgrid.engine.path.spec

import com.datathecodie.witgrid.engine.path.PathRules
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.corridorPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.corridorSolution
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.deadEndPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.rowsPuzzle
import com.datathecodie.witgrid.engine.path.spec.PathSpecFixtures.rowsSolution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Path plan-rule cases: path shape, walls, endpoints, and solution counting. */
class PathRulesSpecTest {

    @Test
    fun fullBoardWithWallAndFourPairsIsRuleValid() {
        assertTrue(PathRules.isValidSolution(rowsPuzzle, rowsSolution))
    }

    @Test
    fun emptyNonWallCellIsRejected() {
        val withGap = rowsSolution.toMutableList().also { it[0] = 0 }
        assertFalse(PathRules.isValidSolution(rowsPuzzle, withGap))
    }

    @Test
    fun wallCellWithPairColourIsRejected() {
        val wallFilled = rowsSolution.toMutableList().also { it[15] = 4 }
        assertFalse(PathRules.isValidSolution(rowsPuzzle, wallFilled))
    }

    @Test
    fun changedEndpointColourIsRejected() {
        val movedEndpoint = rowsSolution.toMutableList().also { it[0] = 2 }
        assertFalse(PathRules.isValidSolution(rowsPuzzle, movedEndpoint))
    }

    @Test
    fun twoByTwoBlockOfOneColourIsRejected() {
        // Cells 1, 2, 5 and 6 form a 2x2 block. None of them is an endpoint, so only the block rule fails.
        val block = rowsSolution.toMutableList().also {
            it[5] = 1
            it[6] = 1
        }
        assertFalse(PathRules.isValidSolution(rowsPuzzle, block))
    }

    @Test
    fun branchingPathIsRejected() {
        // Cell 6 touches cell 2 and cell 7 of the corridor, which makes a branch.
        val branch = corridorSolution.toMutableList().also { it[6] = 1 }
        assertFalse(PathRules.isValidSolution(corridorPuzzle, branch))
    }

    @Test
    fun pathThatTouchesItselfIsRejected() {
        // Cell 5 joins pair 1 below cell 1. Cell 5 is next to cell 1, which is not its path neighbour.
        val touching = rowsSolution.toMutableList().also { it[5] = 1 }
        assertFalse(PathRules.isValidSolution(rowsPuzzle, touching))
    }

    @Test
    fun corridorSolutionIsRuleValid() {
        assertTrue(PathRules.isValidSolution(corridorPuzzle))
    }

    @Test
    fun wrongCellCountIsRejected() {
        assertFalse(PathRules.isValidSolution(corridorPuzzle, corridorSolution.dropLast(1)))
    }

    @Test
    fun corridorHasExactlyOneColouring() {
        assertEquals(1, PathRules.countSolutions(corridorPuzzle, limit = 2))
        assertEquals(1, PathRules.countSolutions(corridorPuzzle, limit = 1))
    }

    @Test
    fun boardWhereEndpointCannotLeaveHasNoSolution() {
        assertEquals(0, PathRules.countSolutions(deadEndPuzzle, limit = 2))
    }

    @Test
    fun countSolutionsRejectsNonPositiveLimit() {
        val thrown = runCatching { PathRules.countSolutions(corridorPuzzle, limit = 0) }
        assertTrue(thrown.isFailure)
    }
}
