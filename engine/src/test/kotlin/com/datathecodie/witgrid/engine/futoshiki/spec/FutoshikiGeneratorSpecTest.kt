package com.datathecodie.witgrid.engine.futoshiki.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiEngine
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiPackBands
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiRules
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiState
import com.datathecodie.witgrid.engine.futoshiki.FutoshikiTechniqueSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent generated-puzzle checks for the launch sizes, fairness and deterministic seeds. */
class FutoshikiGeneratorSpecTest {
    @Test
    fun sameSeedProducesTheSamePuzzleForEveryPack() {
        for (pack in Pack.values()) {
            assertEquals(
                FutoshikiEngine.generate(SplitMix64(20261125L), pack),
                FutoshikiEngine.generate(SplitMix64(20261125L), pack),
                "$pack seed stability",
            )
        }
    }

    @Test
    fun everyPackUsesItsPlannedBoardSizeAndProducesUniqueGuessFreePuzzles() {
        val expectedSizes = mapOf(Pack.EASY to 5, Pack.MEDIUM to 6, Pack.HARD to 7, Pack.EXPERT to 7)
        for ((pack, size) in expectedSizes) {
            val puzzle = FutoshikiEngine.generate(SplitMix64(4100L + pack.ordinal), pack)
            assertEquals(size, puzzle.size, "$pack size")
            assertTrue(FutoshikiRules.isValidSolution(puzzle), "$pack stored answer obeys rules")
            assertEquals(1, FutoshikiRules.countSolutions(puzzle, limit = 2), "$pack uniqueness")
            assertTrue(FutoshikiPackBands.isGradeInPackBand(pack, FutoshikiTechniqueSolver.grade(puzzle)))

            var state = FutoshikiState.initial(puzzle)
            var steps = 0
            while (!FutoshikiRules.isSolved(puzzle, state)) {
                val hint = FutoshikiEngine.nextStep(puzzle, state)
                    ?: throw AssertionError("$pack puzzle is stuck")
                state = hint.moves.fold(state, FutoshikiState::withMove)
                steps++
                assertTrue(steps <= puzzle.grid.cellCount, "$pack solve must finish without guessing")
            }
        }
    }
}
