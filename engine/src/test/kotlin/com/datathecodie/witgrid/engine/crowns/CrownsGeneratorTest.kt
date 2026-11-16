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
            val secondRun = generateBatch(pack)
            val firstLines = firstRun.map(CrownsEngine::encodePuzzle)

            assertEquals(firstLines, secondRun.map(CrownsEngine::encodePuzzle))
            assertEquals(10, firstRun.size)
            assertEquals(10, firstRun.map { it.id }.toSet().size)
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

    private fun generateBatch(pack: Pack): List<CrownsPuzzle> {
        val rng = SplitMix64(1L)
        return List(10) { CrownsEngine.generate(rng, pack) }
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
