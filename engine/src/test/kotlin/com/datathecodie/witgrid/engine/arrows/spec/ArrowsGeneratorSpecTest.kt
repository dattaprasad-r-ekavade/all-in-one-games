package com.datathecodie.witgrid.engine.arrows.spec

import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsPuzzle
import com.datathecodie.witgrid.engine.arrows.ArrowsState
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.isBoardEmpty
import com.datathecodie.witgrid.engine.arrows.spec.ArrowsSpecFixtures.pathToEdgeIsEmpty
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Plan 12.4 generator: at least one clear-all sequence.
 * Same seed must give the same layout. Every generated level must be clearable.
 */
class ArrowsGeneratorSpecTest {

    @Test
    fun sameSeedProducesTheSamePuzzle() {
        val first = ArrowsEngine.generate(SplitMix64(1L), Pack.EASY)
        val replay = ArrowsEngine.generate(SplitMix64(1L), Pack.EASY)

        assertEquals(first, replay)
        assertEquals(first.bankId, replay.bankId)
        assertEquals(first.initialArrows, replay.initialArrows)
        assertEquals(
            ArrowsEngine.encodePuzzle(first),
            ArrowsEngine.encodePuzzle(replay),
        )
    }

    @Test
    fun generatedLevelsMatchPlanSizesAndStartNotLost() {
        val expectedSizes = mapOf(
            Pack.EASY to 5,
            Pack.MEDIUM to 6,
            Pack.HARD to 8,
            Pack.EXPERT to 10,
        )

        expectedSizes.forEach { (pack, expectedSize) ->
            val puzzle = ArrowsEngine.generate(SplitMix64(1L), pack)
            val start = ArrowsEngine.initialState(puzzle)

            assertEquals(expectedSize, puzzle.size, "$pack board size")
            assertEquals(pack, puzzle.pack)
            assertEquals(expectedSize * expectedSize, start.arrows.size)
            assertFalse(ArrowsEngine.isLost(puzzle, start), "$pack deal must not be lost")
            assertFalse(isBoardEmpty(start.arrows), "$pack deal should have arrows to slide")
        }
    }

    @Test
    fun everyGeneratedLevelIsClearableByFollowingHints() {
        val packs = listOf(Pack.EASY, Pack.MEDIUM, Pack.HARD, Pack.EXPERT)
        for (pack in packs) {
            val rng = SplitMix64(1L)
            val puzzle = ArrowsEngine.generate(rng, pack)
            val won = clearWithHints(puzzle)

            assertTrue(ArrowsEngine.isWon(puzzle, won), "$pack must clear by hints")
            assertEquals(0, won.mistakes, "$pack hint path must make zero mistakes")
            assertTrue(isBoardEmpty(won.arrows), "$pack board must be empty at win")
        }
    }

    @Test
    fun sequentialEasyPuzzlesFromOneSeedAreEachClearableAndDeterministic() {
        val firstRun = generateEasy(count = 3, seed = 2026L)
        val secondRun = generateEasy(count = 3, seed = 2026L)

        assertEquals(firstRun.map { it.bankId }, secondRun.map { it.bankId })
        assertEquals(firstRun.map { it.initialArrows }, secondRun.map { it.initialArrows })
        for (puzzle in firstRun) {
            val won = clearWithHints(puzzle)
            assertTrue(ArrowsEngine.isWon(puzzle, won), "${puzzle.bankId} must clear")
            assertEquals(0, won.mistakes)
        }
    }

    private fun generateEasy(count: Int, seed: Long): List<ArrowsPuzzle> {
        val rng = SplitMix64(seed)
        return List(count) { ArrowsEngine.generate(rng, Pack.EASY) }
    }

    private fun clearWithHints(puzzle: ArrowsPuzzle): ArrowsState {
        var state = ArrowsEngine.initialState(puzzle)
        var steps = 0
        while (!ArrowsEngine.isWon(puzzle, state)) {
            assertTrue(steps < puzzle.cellCount, "${puzzle.bankId} hint loop exceeded cell count")
            val hint = ArrowsEngine.hintAction(puzzle, state)
            assertNotNull(hint, "${puzzle.bankId} hintAction returned null before the board was empty")
            assertTrue(
                pathToEdgeIsEmpty(puzzle.size, state.arrows, hint!!.cell.index),
                "${puzzle.bankId} hint cell ${hint.cell.index} must be able to slide now",
            )
            state = ArrowsEngine.applyAction(state, hint)
            assertEquals(0, state.mistakes, "${puzzle.bankId} hint must not be a mistake")
            steps++
        }
        return state
    }
}
