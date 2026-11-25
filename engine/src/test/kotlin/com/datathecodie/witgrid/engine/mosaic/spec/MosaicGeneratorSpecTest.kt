package com.datathecodie.witgrid.engine.mosaic.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.mosaic.MosaicState
import com.datathecodie.witgrid.engine.mosaic.MosaicEngine
import com.datathecodie.witgrid.engine.mosaic.MosaicPackBands
import com.datathecodie.witgrid.engine.mosaic.MosaicRules
import com.datathecodie.witgrid.engine.mosaic.MosaicTechniqueSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent plan checks for Mosaic pack sizes, uniqueness, guess-free solves and seed stability. */
class MosaicGeneratorSpecTest {

    @Test
    fun sameSeedProducesTheSamePuzzle() {
        val first = MosaicEngine.generate(SplitMix64(2026L), Pack.EASY)
        val replay = MosaicEngine.generate(SplitMix64(2026L), Pack.EASY)

        assertEquals(first, replay)
        assertEquals(first.id, replay.id)
    }

    @Test
    fun everyPackGeneratesAUniqueGuessFreePuzzleAtItsPlanSize() {
        val expectedSizes = mapOf(
            Pack.EASY to 8,
            Pack.MEDIUM to 10,
            Pack.HARD to 12,
            Pack.EXPERT to 15,
        )

        expectedSizes.forEach { (pack, expectedSize) ->
            val puzzle = MosaicEngine.generate(SplitMix64(4100L + expectedSize), pack)

            assertEquals(expectedSize, puzzle.size, "$pack board size")
            assertEquals(expectedSize, MosaicPackBands.sizeForPack(pack), "$pack configured size")
            assertEquals(pack, puzzle.pack)
            assertTrue(MosaicRules.isValidSolution(puzzle), "$pack stored solution")
            assertEquals(1, MosaicRules.countSolutions(puzzle, limit = 2), "$pack uniqueness")

            var state = MosaicState.initial(puzzle)
            var stepCount = 0
            while (!MosaicRules.isSolved(puzzle, state)) {
                val step = MosaicTechniqueSolver.nextStep(puzzle, state)
                assertTrue(step != null, "$pack must remain solvable by techniques after $stepCount steps")
                state = step!!.moves.fold(state, MosaicState::withMove)
                stepCount++
            }
            assertTrue(stepCount > 0, "$pack must contain solve steps")
            assertEquals(pack, MosaicTechniqueSolver.grade(puzzle).pack)
        }
    }
}
