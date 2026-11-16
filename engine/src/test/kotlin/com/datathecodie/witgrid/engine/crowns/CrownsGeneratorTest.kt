package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrownsGeneratorTest {
    @Test
    fun seedOneGeneratesTenUniqueSolvableDeterministicPuzzlesForEveryPack() {
        for (pack in Pack.entries) {
            val firstRun = generateBatch(pack)
            val secondRun = generateFreshBatch(pack)
            val firstLines = firstRun.map(CrownsEngine::encodePuzzle)

            assertEquals(firstLines, secondRun.map(CrownsEngine::encodePuzzle))
            assertEquals(10, firstRun.size)
            assertEquals(10, firstRun.map { it.id }.toSet().size)
            assertEquals(10, firstRun.map { it.regions to it.solutionColumns }.toSet().size)
            if (pack == Pack.EXPERT) {
                assertEquals(setOf(9, 10), firstRun.map { it.size }.toSet())
            }
            firstRun.forEach { puzzle ->
                assertUnique(CrownsEngine, puzzle)
                assertTechniqueSolves(CrownsEngine, puzzle)
                assertEquals(puzzle, CrownsEngine.decodePuzzle(CrownsEngine.encodePuzzle(puzzle)))
                assertEquals(pack, CrownsEngine.grade(puzzle).pack)
                assertEquals(CrownsGradeCodec.encode(CrownsEngine.grade(puzzle)), puzzle.storedGrade)
                CrownsGradeCodec.validate(puzzle.storedGrade)
                assertConnectedRegions(puzzle)
            }
        }
    }

    @Test
    fun everyPackUsesThePlanBoardSizes() {
        for (pack in Pack.entries) {
            val puzzle = CrownsEngine.generate(SplitMix64(1L), pack)
            val validSizes = when (pack) {
                Pack.EASY -> setOf(6)
                Pack.MEDIUM -> setOf(7)
                Pack.HARD -> setOf(8)
                Pack.EXPERT -> setOf(9, 10)
            }
            assertTrue(puzzle.size in validSizes)
        }
    }

    @Test
    fun boundarySeedsGenerateValidDistinctContentForEveryPack() {
        val seeds = listOf(0L, -1L, 2L, Long.MIN_VALUE, Long.MAX_VALUE)
        for (pack in Pack.entries) {
            val puzzles = seeds.map { CrownsEngine.generate(SplitMix64(it), pack) }

            assertEquals(seeds.size, puzzles.map { it.regions to it.solutionColumns }.toSet().size)
            puzzles.forEach { puzzle ->
                assertUnique(CrownsEngine, puzzle)
                assertTechniqueSolves(CrownsEngine, puzzle)
                assertEquals(pack, CrownsEngine.grade(puzzle).pack)
                assertConnectedRegions(puzzle)
            }
        }
    }

    @Test
    fun everyRegionHasAtLeastThreeCellsForSeedOne() {
        for (pack in Pack.entries) {
            generateBatch(pack).forEach { puzzle ->
                val smallest = (0 until puzzle.size).minOf { region ->
                    puzzle.regions.count { it == region }
                }
                assertTrue(smallest >= 3, "Smallest region in ${puzzle.id} has $smallest cells")
            }
        }
    }

    @Test
    fun regionSizesStayBetweenMinimumAndMaximumForSeedOne() {
        for (pack in Pack.entries) {
            generateBatch(pack).forEach { puzzle ->
                val sizes = (0 until puzzle.size).map { region -> puzzle.regions.count { it == region } }
                assertTrue(sizes.min() >= 3, "Region under 3 cells in ${puzzle.id}: $sizes")
                assertTrue(sizes.max() <= puzzle.size * 5 / 2, "Region over size*5/2 in ${puzzle.id}: $sizes")
            }
        }
    }

    @Test
    fun techniqueStepCapsHoldForSeedOne() {
        val kRegionsCaps = mapOf(Pack.EASY to 0, Pack.MEDIUM to 1, Pack.HARD to 2)
        for ((pack, cap) in kRegionsCaps) {
            generateBatch(pack).forEach { puzzle ->
                val steps = CrownsStepCounter.countSteps(puzzle)
                assertTrue(steps != null, "Techniques stuck on ${puzzle.id}")
                val kRegionSteps = steps!![CrownsTechniques.K_REGIONS_K_LINES.id] ?: 0
                assertTrue(kRegionSteps <= cap, "${puzzle.id} has $kRegionSteps k-regions steps, cap $cap")
            }
        }
    }

    @Test
    fun puzzleIdsAreUnsignedHexWithoutMinusSign() {
        val idPattern = Regex("^CR-[A-Z]+-[0-9a-f]{16}$")
        for (pack in Pack.entries) {
            generateBatch(pack).forEach { puzzle ->
                assertTrue(idPattern.matches(puzzle.id), "Bad Crowns ID ${puzzle.id}")
            }
        }
    }

    private fun generateBatch(pack: Pack): List<CrownsPuzzle> = seedOneBatches.getValue(pack)

    private fun generateFreshBatch(pack: Pack): List<CrownsPuzzle> {
        val rng = SplitMix64(1L)
        return List(10) { CrownsEngine.generate(rng, pack) }
    }

    /** Expert generation is slow, so the seed-1 batches are built once and shared by the tests. */
    private companion object {
        val seedOneBatches: Map<Pack, List<CrownsPuzzle>> by lazy {
            Pack.entries.associateWith { pack ->
                val rng = SplitMix64(1L)
                List(10) { CrownsEngine.generate(rng, pack) }
            }
        }
    }

    private fun assertConnectedRegions(puzzle: CrownsPuzzle) {
        for (region in 0 until puzzle.size) {
            val regionCells = puzzle.regions.indices.filter { puzzle.regions[it] == region }.toSet()
            assertTrue(regionCells.isNotEmpty())
            val visited = mutableSetOf(regionCells.first())
            val queue = mutableListOf(regionCells.first())
            var nextIndex = 0
            while (nextIndex < queue.size) {
                val cell = queue[nextIndex++]
                orthogonalNeighbours(cell, puzzle.size)
                    .filter { it in regionCells && visited.add(it) }
                    .forEach(queue::add)
            }
            assertEquals(regionCells, visited, "Region $region must be one flood-filled component")
        }
    }

    private fun orthogonalNeighbours(cell: Int, size: Int): List<Int> {
        val row = cell / size
        val column = cell % size
        return buildList {
            if (row > 0) add(cell - size)
            if (column > 0) add(cell - 1)
            if (column + 1 < size) add(cell + 1)
            if (row + 1 < size) add(cell + size)
        }
    }
}
