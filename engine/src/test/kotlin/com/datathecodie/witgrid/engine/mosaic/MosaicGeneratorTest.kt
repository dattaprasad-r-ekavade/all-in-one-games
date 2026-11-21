package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MosaicGeneratorTest {
    private val idPattern = Regex("^[A-Z]{2}-[A-Z]+-[0-9a-f]{16}$")

    @Test
    fun easyPackGeneratesTenUniqueSolvablePuzzles() {
        val puzzles = generatePuzzles(Pack.EASY, 10, seed = 1L)
        assertBatchValid(puzzles, Pack.EASY, 10)
    }

    @Test
    fun mediumPackGeneratesTenUniqueSolvablePuzzles() {
        val puzzles = generatePuzzles(Pack.MEDIUM, 10, seed = 1L)
        assertBatchValid(puzzles, Pack.MEDIUM, 10)
    }

    @Test
    fun hardPackGeneratesTenUniqueSolvablePuzzles() {
        val puzzles = generatePuzzles(Pack.HARD, 10, seed = 1L)
        assertBatchValid(puzzles, Pack.HARD, 10)
    }

    @Test
    fun expertPackGeneratesTenUniqueSolvablePuzzles() {
        val puzzles = generatePuzzles(Pack.EXPERT, 10, seed = 1L)
        assertBatchValid(puzzles, Pack.EXPERT, 10)
    }

    @Test
    fun sameSeedGivesIdenticalPuzzlesOnSecondRun() {
        for (pack in listOf(Pack.EASY, Pack.MEDIUM, Pack.HARD, Pack.EXPERT)) {
            val firstRun = generatePuzzles(pack, 10, seed = 1L).map(MosaicEngine::encodePuzzle)
            val secondRun = generatePuzzles(pack, 10, seed = 1L).map(MosaicEngine::encodePuzzle)
            assertEquals(firstRun, secondRun, "Pack $pack must produce identical output on replay")
        }
    }

    @Test
    fun boundarySeedsProduceUniqueDeterministicPuzzles() {
        val seeds = listOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)
        val puzzles = seeds.map { seed -> MosaicEngine.generate(SplitMix64(seed), Pack.EASY) }
        assertEquals(seeds.size, puzzles.map { it.id }.toSet().size)
        puzzles.forEach { puzzle ->
            assertBatchValid(listOf(puzzle), Pack.EASY, 1)
        }
    }

    @Test
    fun generatedIdsMatchPattern() {
        for (pack in listOf(Pack.EASY, Pack.MEDIUM, Pack.HARD, Pack.EXPERT)) {
            val puzzle = MosaicEngine.generate(SplitMix64(123L), pack)
            assertTrue(idPattern.matches(puzzle.id), "Unexpected ID format: ${puzzle.id}")
            assertEquals("MO-${pack.name}", puzzle.id.substringBeforeLast('-'))
        }
    }

    private fun generatePuzzles(pack: Pack, count: Int, seed: Long): List<MosaicPuzzle> {
        val rng = SplitMix64(seed)
        return List(count) { MosaicEngine.generate(rng, pack) }
    }

    private fun assertBatchValid(puzzles: List<MosaicPuzzle>, pack: Pack, expectedCount: Int) {
        assertEquals(expectedCount, puzzles.size)
        assertEquals(expectedCount, puzzles.map { it.id }.toSet().size)
        puzzles.forEach { puzzle ->
            assertEquals(pack, puzzle.pack)
            assertEquals(MosaicPackBands.sizeForPack(pack), puzzle.size)
            assertTrue(idPattern.matches(puzzle.id), "ID must match pattern: ${puzzle.id}")
            assertTrue(MosaicRules.isValidSolution(puzzle))
            assertUnique(MosaicEngine, puzzle)
            assertTechniqueSolves(MosaicEngine, puzzle)
            val grade = MosaicEngine.grade(puzzle)
            assertEquals(pack, grade.pack)
            assertTrue(MosaicPackBands.isGradeInPackBand(pack, grade))
            assertEquals(MosaicGradeCodec.encode(grade), puzzle.storedGrade)
            MosaicGradeCodec.validate(puzzle.storedGrade)
        }
    }
}
