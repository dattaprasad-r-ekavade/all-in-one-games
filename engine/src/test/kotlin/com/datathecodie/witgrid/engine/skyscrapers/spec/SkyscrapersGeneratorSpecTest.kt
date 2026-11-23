package com.datathecodie.witgrid.engine.skyscrapers.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersGenerator
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersTechniqueSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Skyscrapers generator cases: pack sizes, uniqueness, guess-free solving and seed stability. */
class SkyscrapersGeneratorSpecTest {

    @Test
    fun sameSeedGivesTheSamePuzzle() {
        val first = SkyscrapersGenerator.generate(SplitMix64(2026L), Pack.EASY)
        val second = SkyscrapersGenerator.generate(SplitMix64(2026L), Pack.EASY)

        assertEquals(first, second)
        assertEquals(first.id, second.id)
    }

    @Test
    fun differentSeedsGiveDifferentPuzzles() {
        val first = SkyscrapersGenerator.generate(SplitMix64(1L), Pack.EASY)
        val second = SkyscrapersGenerator.generate(SplitMix64(2L), Pack.EASY)

        assertNotEquals(first, second)
    }

    @Test
    fun easyPackMakesAUniqueGuessFreeFourByFour() {
        assertPlanPuzzle(Pack.EASY, expectedSize = 4, seed = 11L)
    }

    @Test
    fun mediumPackMakesAUniqueGuessFreeFiveByFive() {
        assertPlanPuzzle(Pack.MEDIUM, expectedSize = 5, seed = 12L)
    }

    @Test
    fun hardPackMakesAUniqueGuessFreeSixBySix() {
        assertPlanPuzzle(Pack.HARD, expectedSize = 6, seed = 13L)
    }

    @Test
    fun expertPackMakesAUniqueGuessFreeSevenBySeven() {
        assertPlanPuzzle(Pack.EXPERT, expectedSize = 7, seed = 14L)
    }

    /** Checks the plan rules for one generated puzzle: size, Latin square, one solution and a guess-free solve. */
    private fun assertPlanPuzzle(pack: Pack, expectedSize: Int, seed: Long) {
        val puzzle = SkyscrapersGenerator.generate(SplitMix64(seed), pack)

        assertEquals(expectedSize, puzzle.size)
        assertEquals(pack, puzzle.pack)
        assertTrue(SkyscrapersRules.isValidSolution(puzzle))
        assertEquals(1, SkyscrapersRules.countSolutions(puzzle, limit = 2))

        val (solved, steps) = SkyscrapersTechniqueSolver.solveWithHints(puzzle)
        assertTrue(SkyscrapersRules.isSolved(puzzle, solved))
        assertTrue(steps.isNotEmpty())

        assertEquals(pack, SkyscrapersTechniqueSolver.grade(puzzle).pack)
    }
}
