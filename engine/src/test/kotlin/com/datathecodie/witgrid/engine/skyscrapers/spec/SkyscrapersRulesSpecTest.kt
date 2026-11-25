package com.datathecodie.witgrid.engine.skyscrapers.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.cyclicSquare
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.fullClues
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.fullClues4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.nulls
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.puzzle
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.solution4
import com.datathecodie.witgrid.engine.skyscrapers.spec.SkyscrapersSpecFixtures.topTwoRowGivens4
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Skyscrapers plan-rule cases: visibility counting, Latin squares and uniqueness. */
class SkyscrapersRulesSpecTest {

    @Test
    fun visibilityCountsOnlyBuildingsTallerThanEveryBuildingBeforeThem() {
        // From the left, 1, 3 and 4 are seen. From the right, only 4 is seen.
        assertEquals(3, SkyscrapersRules.countVisible(listOf(1, 3, 2, 4)))
        assertEquals(1, SkyscrapersRules.countVisible(listOf(1, 3, 2, 4).asReversed()))

        // A short building behind a tall one is hidden. Only 3 and 4 are seen.
        assertEquals(2, SkyscrapersRules.countVisible(listOf(3, 1, 2, 4)))
        assertEquals(1, SkyscrapersRules.countVisible(listOf(3, 1, 2, 4).asReversed()))

        assertEquals(4, SkyscrapersRules.countVisible(listOf(1, 2, 3, 4)))
        assertEquals(1, SkyscrapersRules.countVisible(listOf(1, 2, 3, 4).asReversed()))
        assertEquals(1, SkyscrapersRules.countVisible(listOf(4, 3, 2, 1)))
    }

    @Test
    fun leftRightAndTopBottomCluesCountFromTheirOwnSide() {
        // The first row reads 1 2 3 4 from the left, so its left clue is 4 and its right clue is 1.
        val wrongLeft = fullClues4.copy(left = listOf(1, 2, 2, 1))
        val wrongRight = fullClues4.copy(right = listOf(4, 2, 2, 4))
        val wrongTop = fullClues4.copy(top = listOf(1, 2, 2, 4))

        assertTrue(SkyscrapersRules.isValidSolution(puzzle(4, solution4, fullClues4)))
        assertFalse(SkyscrapersRules.isValidSolution(puzzle(4, solution4, wrongLeft)))
        assertFalse(SkyscrapersRules.isValidSolution(puzzle(4, solution4, wrongRight)))
        assertFalse(SkyscrapersRules.isValidSolution(puzzle(4, solution4, wrongTop)))
    }

    @Test
    fun latinSquaresOfSizeFourToSevenAreValidWithTheirClues() {
        for (size in 4..7) {
            val solution = cyclicSquare(size)
            val valid = puzzle(size, solution, fullClues(size, solution))

            // Copying the first digit into the next cell repeats it in a row. Copying it one row down repeats it in a column.
            val repeatedInRow = solution.toMutableList().also { it[1] = it[0] }
            val repeatedInColumn = solution.toMutableList().also { it[size] = it[0] }

            assertTrue(SkyscrapersRules.isValidSolution(valid), "size $size")
            assertFalse(SkyscrapersRules.isValidSolution(valid, repeatedInRow), "size $size row")
            assertFalse(SkyscrapersRules.isValidSolution(valid, repeatedInColumn), "size $size column")
            assertFalse(SkyscrapersRules.isValidSolution(valid, solution.dropLast(1)), "size $size short")
        }
    }

    @Test
    fun boardsOutsideFourToSevenAreRejected() {
        for (size in listOf(3, 8)) {
            assertThrows(IllegalArgumentException::class.java) {
                SkyscrapersPuzzle(
                    id = "SK-BAD-$size",
                    size = size,
                    topClues = nulls(size),
                    bottomClues = nulls(size),
                    leftClues = nulls(size),
                    rightClues = nulls(size),
                    solution = cyclicSquare(size),
                    pack = Pack.EASY,
                    storedGrade = "none:0",
                )
            }
        }
    }

    @Test
    fun handMadeCluesWithTopTwoRowsGivenHaveExactlyOneSolution() {
        val puzzle = puzzle(4, solution4, fullClues4, topTwoRowGivens4)

        assertEquals(1, SkyscrapersRules.countSolutions(puzzle, limit = 2))
    }

    @Test
    fun removingFourClueSidesLetsASecondSolutionFit() {
        // Without these four clues, the grid whose third row is 3421 and fourth row is 4312 also fits.
        val clues = fullClues4.copy(
            bottom = listOf(1, 2, null, null),
            right = listOf(1, 2, null, null),
        )
        val puzzle = puzzle(4, solution4, clues, topTwoRowGivens4)

        assertEquals(2, SkyscrapersRules.countSolutions(puzzle, limit = 2))
        assertEquals(1, SkyscrapersRules.countSolutions(puzzle, limit = 1))
    }

    @Test
    fun emptyFourByFourBoardHasManySolutionsButTheCapStopsAtTwo() {
        val puzzle = puzzle(4, solution4)

        assertEquals(2, SkyscrapersRules.countSolutions(puzzle, limit = 2))
        assertThrows(IllegalArgumentException::class.java) {
            SkyscrapersRules.countSolutions(puzzle, limit = 0)
        }
    }
}
