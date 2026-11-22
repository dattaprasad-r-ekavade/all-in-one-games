package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathGeneratorTest {

    @Test
    fun testGenerateEasyPuzzles() {
        val rng = SplitMix64(1)
        val puzzles = (1..10).map {
            PathEngine.generate(rng, Pack.EASY)
        }

        for (puzzle in puzzles) {
            assertEquals(6, puzzle.size)
            assertEquals(Pack.EASY, puzzle.pack)
            assertTrue(PathRules.isValidSolution(puzzle))
            assertEquals(1, PathRules.countSolutions(puzzle, limit = 2))
            val grade = PathTechniqueSolver.grade(puzzle)
            assertEquals(Pack.EASY, grade.pack)
            assertTrue(puzzle.id.matches(Regex("""^PA-[A-Z]+-[0-9a-f]{16}$""")))
        }
    }

    @Test
    fun testGenerateMediumPuzzles() {
        val rng = SplitMix64(1)
        val puzzles = (1..10).map {
            PathEngine.generate(rng, Pack.MEDIUM)
        }

        for (puzzle in puzzles) {
            assertEquals(8, puzzle.size)
            assertEquals(Pack.MEDIUM, puzzle.pack)
            assertTrue(PathRules.isValidSolution(puzzle))
            assertEquals(1, PathRules.countSolutions(puzzle, limit = 2))
            val grade = PathTechniqueSolver.grade(puzzle)
            assertEquals(Pack.MEDIUM, grade.pack)
            assertTrue(puzzle.id.matches(Regex("""^PA-[A-Z]+-[0-9a-f]{16}$""")))
        }
    }

    @Test
    fun testGenerateHardPuzzles() {
        val rng = SplitMix64(1)
        val puzzles = (1..10).map {
            PathEngine.generate(rng, Pack.HARD)
        }

        for (puzzle in puzzles) {
            assertEquals(10, puzzle.size)
            assertEquals(Pack.HARD, puzzle.pack)
            assertTrue(PathRules.isValidSolution(puzzle))
            assertEquals(1, PathRules.countSolutions(puzzle, limit = 2))
            val grade = PathTechniqueSolver.grade(puzzle)
            assertEquals(Pack.HARD, grade.pack)
            assertTrue(puzzle.id.matches(Regex("""^PA-[A-Z]+-[0-9a-f]{16}$""")))
        }
    }

    @Test
    fun testGenerateExpertPuzzles() {
        val rng = SplitMix64(1)
        val puzzles = (1..10).map {
            PathEngine.generate(rng, Pack.EXPERT)
        }

        for (puzzle in puzzles) {
            assertEquals(12, puzzle.size)
            assertEquals(Pack.EXPERT, puzzle.pack)
            assertTrue(PathRules.isValidSolution(puzzle))
            assertEquals(1, PathRules.countSolutions(puzzle, limit = 2))
            val grade = PathTechniqueSolver.grade(puzzle)
            assertEquals(Pack.EXPERT, grade.pack)
            assertTrue(puzzle.id.matches(Regex("""^PA-[A-Z]+-[0-9a-f]{16}$""")))
        }
    }

    @Test
    fun testDeterminismAcrossRuns() {
        for (pack in Pack.entries) {
            val p1 = PathEngine.generate(SplitMix64(42), pack)
            val p2 = PathEngine.generate(SplitMix64(42), pack)
            assertEquals(p1.id, p2.id)
            assertEquals(p1.size, p2.size)
            assertEquals(p1.endpoints, p2.endpoints)
            assertEquals(p1.walls, p2.walls)
            assertEquals(p1.solution, p2.solution)
            assertEquals(p1.pack, p2.pack)
            assertEquals(p1.storedGrade, p2.storedGrade)
        }
    }
}
