package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FutoshikiGeneratorTest {
    @Test
    fun seedOneCreatesTenUniqueSolvedAndCorrectlyGradedPuzzlesPerPackDeterministically() {
        val firstRun = generateTenPerPack()
        val secondRun = generateTenPerPack()

        Pack.values().forEach { pack ->
            val firstPuzzles = requireNotNull(firstRun[pack])
            assertEquals(firstPuzzles, secondRun[pack], "$pack generation must be deterministic")
            assertEquals(10, firstPuzzles.size)
            assertEquals(
                firstPuzzles.size,
                firstPuzzles.map { it.givens to (it.horizontalSigns to it.verticalSigns) }.toSet().size,
                "$pack generation should vary puzzle layouts",
            )
            assertTrue(
                firstPuzzles.map { puzzle -> puzzle.givens.map { it != null } }.toSet().size > 1,
                "$pack generation should vary which cells are clues",
            )
            assertTrue(
                firstPuzzles.map { puzzle ->
                    puzzle.horizontalSigns.count { it != FutoshikiSign.NONE } +
                        puzzle.verticalSigns.count { it != FutoshikiSign.NONE }
                }.toSet().size > 1,
                "$pack generation should vary sign counts",
            )
            firstPuzzles.forEach { puzzle ->
                assertEquals(pack, puzzle.pack)
                assertTrue(FutoshikiRules.isValidSolution(puzzle))
                assertEquals(1, FutoshikiRules.countSolutions(puzzle, limit = 2))

                val grade = FutoshikiTechniqueSolver.grade(puzzle)
                assertTrue(FutoshikiPackBands.isGradeInPackBand(pack, grade))
                val signCount = puzzle.horizontalSigns.count { it != FutoshikiSign.NONE } +
                    puzzle.verticalSigns.count { it != FutoshikiSign.NONE }
                assertTrue(
                    FutoshikiPackBands.isSignCountInPackBand(pack, signCount),
                    "$pack puzzle ${puzzle.id} has invalid sign count: $signCount",
                )
                assertEquals(FutoshikiGradeCodec.encode(grade), puzzle.storedGrade)
                assertTrue(solveWithHints(puzzle))
            }
        }

        val expertTotalSigns = requireNotNull(firstRun[Pack.EXPERT]).sumOf {
            it.horizontalSigns.count { s -> s != FutoshikiSign.NONE } +
                it.verticalSigns.count { s -> s != FutoshikiSign.NONE }
        }
        val hardTotalSigns = requireNotNull(firstRun[Pack.HARD]).sumOf {
            it.horizontalSigns.count { s -> s != FutoshikiSign.NONE } +
                it.verticalSigns.count { s -> s != FutoshikiSign.NONE }
        }
        assertTrue(
            expertTotalSigns < hardTotalSigns,
            "Expert generation must produce fewer signs than Hard (Expert: $expertTotalSigns, Hard: $hardTotalSigns)",
        )
    }

    @Test
    fun generatedIdsAreUnsignedSixteenDigitHex() {
        val idPattern = Regex("^FU-[A-Z]+-[0-9a-f]{16}$")

        generateTenPerPack().values.flatten().forEach { puzzle ->
            assertTrue(idPattern.matches(puzzle.id), "ID ${puzzle.id} does not match $idPattern")
        }
    }

    @Test
    fun engineDelegatesGenerationUsingTheSuppliedRandomStream() {
        val enginePuzzle = FutoshikiEngine.generate(SplitMix64(54), Pack.HARD)
        val directPuzzle = FutoshikiGenerator.generate(SplitMix64(54), Pack.HARD)

        assertEquals(directPuzzle, enginePuzzle)
    }

    private fun generateTenPerPack(): Map<Pack, List<FutoshikiPuzzle>> =
        Pack.values().associateWith { pack ->
            val rng = SplitMix64(1)
            List(10) { FutoshikiGenerator.generate(rng, pack) }
        }

    private fun solveWithHints(puzzle: FutoshikiPuzzle): Boolean {
        var state = FutoshikiState.initial(puzzle)
        var stepCount = 0
        while (!FutoshikiRules.isSolved(puzzle, state)) {
            val step = FutoshikiEngine.nextStep(puzzle, state) ?: return false
            state = step.moves.fold(state, FutoshikiState::withMove)
            stepCount++
            if (stepCount > puzzle.grid.cellCount) return false
        }
        return true
    }
}
