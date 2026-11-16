package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NonogramGeneratorTest {
    @Test
    fun seedOneGeneratesTenUniqueSolvableDeterministicPuzzlesForEveryPack() {
        for (pack in Pack.entries) {
            val firstRun = generateBatch(pack)
            val secondRun = generateBatch(pack)
            val firstLines = firstRun.map(NonogramEngine::encodePuzzle)

            assertEquals(firstLines, secondRun.map(NonogramEngine::encodePuzzle))
            assertEquals(10, firstRun.size)
            assertEquals(10, firstRun.map { it.id }.toSet().size)
            assertEquals(10, firstRun.map { it.solution }.toSet().size)
            assertEquals(10, firstRun.map { it.rowClues to it.columnClues }.toSet().size)
            firstRun.forEach { puzzle ->
                assertEquals(NonogramPackBands.sizeForPack(pack), puzzle.size)
                assertTrue(NonogramRules.isValidSolution(puzzle))
                assertUnique(NonogramEngine, puzzle)
                assertTechniqueSolves(NonogramEngine, puzzle)
                assertEquals(puzzle, NonogramEngine.decodePuzzle(NonogramEngine.encodePuzzle(puzzle)))
                val grade = NonogramEngine.grade(puzzle)
                assertEquals(pack, grade.pack)
                assertTrue(NonogramPackBands.isGradeInPackBand(pack, grade))
                assertEquals(NonogramGradeCodec.encode(grade), puzzle.storedGrade)
                NonogramGradeCodec.validate(puzzle.storedGrade)
            }
        }
    }

    @Test
    fun boundarySeedsProduceUniqueDeterministicPuzzleContentForEveryPack() {
        val seeds = listOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)
        for (pack in Pack.entries) {
            val puzzles = seeds.map { seed -> NonogramEngine.generate(SplitMix64(seed), pack) }

            assertEquals(seeds.size, puzzles.map { it.solution to it.rowClues }.toSet().size)
            puzzles.forEach { puzzle ->
                assertUnique(NonogramEngine, puzzle)
                assertTechniqueSolves(NonogramEngine, puzzle)
                assertTrue(NonogramPackBands.isGradeInPackBand(pack, NonogramEngine.grade(puzzle)))
            }
        }
    }

    private fun generateBatch(pack: Pack): List<NonogramPuzzle> {
        val rng = SplitMix64(1L)
        return List(10) { NonogramEngine.generate(rng, pack) }
    }
}
