package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkyscrapersGeneratorTest {

    @Test
    fun seedOneCreatesTenUniqueSolvedAndCorrectlyGradedPuzzlesPerPackDeterministically() {
        val firstRun = generateTenPerPack()
        val secondRun = generateTenPerPack()

        Pack.values().forEach { pack ->
            val firstPuzzles = requireNotNull(firstRun[pack])
            assertEquals(firstPuzzles, secondRun[pack], "$pack generation must be deterministic")
            assertEquals(10, firstPuzzles.size)

            val clueSets = firstPuzzles.map { puzzle ->
                listOf(puzzle.topClues, puzzle.bottomClues, puzzle.leftClues, puzzle.rightClues)
            }.toSet()
            assertEquals(firstPuzzles.size, clueSets.size, "$pack generation should vary clue layouts")

            assertTrue(
                firstPuzzles.map { puzzle ->
                    puzzle.topClues.count { it != null } +
                        puzzle.bottomClues.count { it != null } +
                        puzzle.leftClues.count { it != null } +
                        puzzle.rightClues.count { it != null }
                }.toSet().size > 1,
                "$pack generation should vary clue counts",
            )

            firstPuzzles.forEach { puzzle ->
                assertEquals(pack, puzzle.pack)
                assertTrue(SkyscrapersRules.isValidSolution(puzzle))
                assertEquals(1, SkyscrapersRules.countSolutions(puzzle, limit = 2))

                val grade = SkyscrapersTechniqueSolver.grade(puzzle)
                assertTrue(SkyscrapersPackBands.isGradeInPackBand(pack, grade))
                assertEquals(SkyscrapersGradeCodec.encode(grade), puzzle.storedGrade)
                assertTrue(solveWithHints(puzzle))

                val totalClues = puzzle.topClues.count { it != null } +
                    puzzle.bottomClues.count { it != null } +
                    puzzle.leftClues.count { it != null } +
                    puzzle.rightClues.count { it != null }
                assertTrue(totalClues < 4 * puzzle.size, "Clue removal should have pruned outside clues")
            }
        }
    }

    @Test
    fun generatedIdsAreUnsignedSixteenDigitHex() {
        val idPattern = Regex("^SK-[A-Z]+-[0-9a-f]{16}$")

        generateTenPerPack().values.flatten().forEach { puzzle ->
            assertTrue(idPattern.matches(puzzle.id), "ID ${puzzle.id} does not match $idPattern")
        }
    }

    @Test
    fun engineDelegatesGenerationUsingTheSuppliedRandomStream() {
        val enginePuzzle = SkyscrapersEngine.generate(SplitMix64(42), Pack.MEDIUM)
        val directPuzzle = SkyscrapersGenerator.generate(SplitMix64(42), Pack.MEDIUM)

        assertEquals(directPuzzle, enginePuzzle)
    }

    private fun generateTenPerPack(): Map<Pack, List<SkyscrapersPuzzle>> =
        Pack.values().associateWith { pack ->
            val rng = SplitMix64(1)
            List(10) { SkyscrapersGenerator.generate(rng, pack) }
        }

    private fun solveWithHints(puzzle: SkyscrapersPuzzle): Boolean {
        var state = SkyscrapersState.initial(puzzle)
        var stepCount = 0
        while (!SkyscrapersRules.isSolved(puzzle, state)) {
            val step = SkyscrapersEngine.nextStep(puzzle, state) ?: return false
            state = step.moves.fold(state, SkyscrapersState::withMove)
            stepCount++
            if (stepCount > puzzle.grid.cellCount) return false
        }
        return true
    }
}
