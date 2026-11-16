package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BinaryGeneratorTest {
    @Test
    fun seedOneGeneratesTenUniqueSolvableDeterministicPuzzlesForEveryPack() {
        for (pack in Pack.entries) {
            val firstRun = generateBatch(pack)
            val secondRun = generateBatch(pack)
            val firstLines = firstRun.map(BinaryEngine::encodePuzzle)

            assertEquals(firstLines, secondRun.map(BinaryEngine::encodePuzzle))
            assertEquals(10, firstRun.size)
            assertEquals(10, firstRun.map { it.id }.toSet().size)
            assertEquals(10, firstRun.map { it.solution }.toSet().size)
            assertEquals(10, firstRun.map { it.solution to it.givens }.toSet().size)
            firstRun.forEach { puzzle ->
                assertEquals(sizeForPack(pack), puzzle.size)
                assertTrue(BinaryRules.isValidSolution(puzzle))
                assertTrue(puzzle.givens.any { it == null }, "Generated puzzle must remove some givens")
                assertUnique(BinaryEngine, puzzle)
                assertTechniqueSolves(BinaryEngine, puzzle)
                assertEquals(puzzle, BinaryEngine.decodePuzzle(BinaryEngine.encodePuzzle(puzzle)))
                assertEquals(pack, BinaryEngine.grade(puzzle).pack)
                assertEquals(BinaryGradeCodec.encode(BinaryEngine.grade(puzzle)), puzzle.storedGrade)
                BinaryGradeCodec.validate(puzzle.storedGrade)
            }
        }
    }

    @Test
    fun boundarySeedsGenerateValidDifferentPuzzleContentForEveryPack() {
        val seeds = listOf(0L, -1L, 2L, Long.MIN_VALUE, Long.MAX_VALUE)
        for (pack in Pack.entries) {
            val puzzles = seeds.map { seed -> BinaryEngine.generate(SplitMix64(seed), pack) }

            assertEquals(seeds.size, puzzles.map { it.solution to it.givens }.toSet().size)
            puzzles.forEach { puzzle ->
                assertUnique(BinaryEngine, puzzle)
                assertTechniqueSolves(BinaryEngine, puzzle)
                assertEquals(pack, BinaryEngine.grade(puzzle).pack)
            }
        }
    }

    private fun generateBatch(pack: Pack): List<BinaryPuzzle> {
        val rng = SplitMix64(1L)
        return List(10) { BinaryEngine.generate(rng, pack) }
    }

    private fun sizeForPack(pack: Pack): Int = when (pack) {
        Pack.EASY -> 6
        Pack.MEDIUM -> 8
        Pack.HARD -> 10
        Pack.EXPERT -> 12
    }
}
