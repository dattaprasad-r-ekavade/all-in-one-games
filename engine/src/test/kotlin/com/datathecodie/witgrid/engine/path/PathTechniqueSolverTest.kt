package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.testkit.assertTechniqueSolves
import com.datathecodie.witgrid.engine.testkit.assertUnique
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathTechniqueSolverTest {

    // 5x5 unique puzzle with walls at (2,1) and (2,2)
    private val unique5x5Line = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|EASY|easy:1"

    // 6x6 unique puzzle (EASY)
    private val unique6x6Line = "path-6-u|6|1.....2#4#5..3.#...34#..25....1.....|1111112#4#51234#51234#51255551111111|EASY|easy:1"

    @Test
    fun solvesHandMadeFixturesAcrossAllPacks() {
        val puzzle5 = PathCodec.decode(unique5x5Line)
        assertUnique(PathEngine, puzzle5)
        assertTechniqueSolves(PathEngine, puzzle5)
        val grade5 = PathTechniqueSolver.grade(puzzle5)
        assertEquals(Pack.EASY, grade5.pack)
        assertTrue(grade5.stepCount > 0)
        assertNotNull(grade5.hardestTechnique)

        val puzzle6 = PathCodec.decode(unique6x6Line)
        assertUnique(PathEngine, puzzle6)
        assertTechniqueSolves(PathEngine, puzzle6)
        val grade6 = PathTechniqueSolver.grade(puzzle6)
        assertEquals(Pack.EASY, grade6.pack)

        val puzzle8 = buildCorridorPuzzle(8, Pack.MEDIUM)
        assertTrue(PathRules.isValidSolution(puzzle8))
        assertUnique(PathEngine, puzzle8)
        assertTechniqueSolves(PathEngine, puzzle8)
        val grade8 = PathTechniqueSolver.grade(puzzle8)
        assertEquals(Pack.MEDIUM, grade8.pack)

        val puzzle10 = buildCorridorPuzzle(10, Pack.HARD)
        assertTrue(PathRules.isValidSolution(puzzle10))
        assertUnique(PathEngine, puzzle10)
        assertTechniqueSolves(PathEngine, puzzle10)
        val grade10 = PathTechniqueSolver.grade(puzzle10)
        assertEquals(Pack.HARD, grade10.pack)

        val puzzle12 = buildCorridorPuzzle(12, Pack.EXPERT)
        assertTrue(PathRules.isValidSolution(puzzle12))
        assertUnique(PathEngine, puzzle12)
        assertTechniqueSolves(PathEngine, puzzle12)
        val grade12 = PathTechniqueSolver.grade(puzzle12)
        assertEquals(Pack.EXPERT, grade12.pack)
    }

    @Test
    fun everyHintStepHasNonEmptyOneSentenceExplanationEndingWithPeriod() {
        val puzzle = PathCodec.decode(unique6x6Line)
        var state = PathEngine.initialState(puzzle)

        while (!PathRules.isSolved(puzzle, state)) {
            val step = PathTechniqueSolver.nextStep(puzzle, state)
            assertNotNull(step)
            val explanation = step!!.explanation
            assertTrue(explanation.isNotBlank())
            assertTrue(explanation.endsWith("."), "Explanation must end with a period: '$explanation'")
            assertFalse(explanation.dropLast(1).contains("."), "Explanation must be a single sentence: '$explanation'")
            state = step.moves.fold(state, PathEngine::applyMove)
        }
    }

    @Test
    fun nextStepReturnsNullWhenSolvedOrMistaken() {
        val puzzle = PathCodec.decode(unique5x5Line)
        val solvedState = solveWithHints(PathEngine, puzzle)
        assertTrue(PathRules.isSolved(puzzle, solvedState))
        assertNull(PathTechniqueSolver.nextStep(puzzle, solvedState))

        // State with a mistake
        val initialState = PathEngine.initialState(puzzle)
        val mistakenState = initialState.withMove(Move.SetDigit(Cell(1), 2))
        assertEquals(1, PathRules.mistakes(puzzle, mistakenState).size)
        assertNull(PathTechniqueSolver.nextStep(puzzle, mistakenState))
    }

    // 4x4 unique puzzle
    // Row 0: 1 1 2 2
    // Row 1: 1 # # 2
    // Row 2: 1 3 3 2
    // Row 3: 1 1 3 2
    private val unique4x4Line = "path-4-u|4|.12..##..3...132|11221##213321132|EASY|easy:1"

    @Test
    fun solvesValid4x4Puzzle() {
        val puzzle4 = PathCodec.decode(unique4x4Line)
        assertTrue(PathRules.isValidSolution(puzzle4))
        assertUnique(PathEngine, puzzle4)
        assertTechniqueSolves(PathEngine, puzzle4)
        val grade4 = PathTechniqueSolver.grade(puzzle4)
        assertEquals(Pack.EASY, grade4.pack)
    }

    @Test
    fun gradeRejectsTechniqueStuckPuzzle() {
        val stuckLine = "path-6-stuck|6|12.3.3.#.2#4167...6..87..9.54.98...5|1223331#22#4167774668874998544988555|EASY|easy:1"
        val puzzle = PathCodec.decode(stuckLine)
        assertTrue(PathRules.isValidSolution(puzzle))
        assertEquals(1, PathRules.countSolutions(puzzle, limit = 2))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            PathTechniqueSolver.grade(puzzle)
        }
        assertTrue(ex.message!!.contains("stuck"))
    }

    @Test
    fun doesNotTreatSharedNeighborAsForcedJoinWhenDetourExists() {
        // Unique 6x6 puzzle with detour:
        // Row 0: #...##
        // Row 1: #.2.##
        // Row 2: #1.1##
        // Row 3: ##2###
        // Row 4: ######
        // Row 5: ######
        val payload =
            "#...##" +
            "#.2.##" +
            "#1.1##" +
            "##2###" +
            "######" +
            "######"
        val solution =
            "#111##" +
            "#121##" +
            "#121##" +
            "##2###" +
            "######" +
            "######"
        val line = "path-6-detour|6|$payload|$solution|EASY|easy:1"
        val puzzle = PathCodec.decode(line)
        assertTrue(PathRules.isValidSolution(puzzle))
        assertUnique(PathEngine, puzzle)

        val initialState = PathEngine.initialState(puzzle)
        val firstStep = PathTechniqueSolver.nextStep(puzzle, initialState)
        assertNotNull(firstStep)
        // Must NOT assign pair 1 to r3c3. Pair 2 at r4c3 must take r3c3.
        val move = firstStep!!.moves.first() as Move.SetDigit
        assertEquals(2, move.digit)
        assertEquals(Cell(14), move.cell) // r3c3 (index 14 = row 2, col 2)
        assertEquals(PathTechniques.ENDPOINT_EXTENSION, firstStep.technique)

        assertTechniqueSolves(PathEngine, puzzle)
        val grade = PathTechniqueSolver.grade(puzzle)
        assertEquals(Pack.EASY, grade.pack)
    }

    @Test
    fun packForSizeRejectsSizesSevenNineAndEleven() {
        assertThrows(IllegalArgumentException::class.java) { PathPackBands.packForSize(7) }
        assertThrows(IllegalArgumentException::class.java) { PathPackBands.packForSize(9) }
        assertThrows(IllegalArgumentException::class.java) { PathPackBands.packForSize(11) }

        assertEquals(Pack.EASY, PathPackBands.packForSize(4))
        assertEquals(Pack.EASY, PathPackBands.packForSize(5))
        assertEquals(Pack.EASY, PathPackBands.packForSize(6))
        assertEquals(Pack.MEDIUM, PathPackBands.packForSize(8))
        assertEquals(Pack.HARD, PathPackBands.packForSize(10))
        assertEquals(Pack.EXPERT, PathPackBands.packForSize(12))
    }

    @Test
    fun gradeRejectsStuckOrNonUniquePuzzles() {
        // Multi-solution 4x4 puzzle
        val twoSolLine = "path-4-2sol|4|1....2#..#1....2|122212#21#121112|EASY|easy:1"
        val twoSolPuzzle = PathCodec.decode(twoSolLine)
        assertEquals(2, PathEngine.countSolutions(twoSolPuzzle, limit = 2))
        assertThrows(IllegalArgumentException::class.java) {
            PathTechniqueSolver.grade(twoSolPuzzle)
        }

        // Pack mismatch
        val packMismatchLine = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|MEDIUM|medium:1"
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode(packMismatchLine)
        }
    }

    @Test
    fun testsSpecificTechniques() {
        // Endpoint extension on 6x6 puzzle: endpoints of pair 3 are at (2,1) and (3,1), or pair 4
        val puzzle = PathCodec.decode(unique6x6Line)
        val state = PathEngine.initialState(puzzle)
        val step = PathTechniqueSolver.nextStep(puzzle, state)
        assertNotNull(step)
        assertTrue(
            step!!.technique == PathTechniques.ENDPOINT_EXTENSION ||
            step.technique == PathTechniques.FORCED_NEIGHBOUR
        )
    }

    private fun buildCorridorPuzzle(size: Int, pack: Pack): PathPuzzle {
        require(size in listOf(6, 8, 10, 12))
        val cellCount = size * size
        val solution = IntArray(cellCount)
        val walls = BooleanArray(cellCount)
        val endpoints = arrayOfNulls<Int>(cellCount)

        // Color 1: outer border (row 0, col size-1, row size-1)
        for (c in 0 until size) {
            solution[c] = 1
            solution[(size - 1) * size + c] = 1
        }
        for (r in 0 until size) {
            solution[r * size + size - 1] = 1
        }
        endpoints[0] = 1
        endpoints[(size - 1) * size] = 1

        // Color 2: Col 0 from row 1 to size - 2
        for (r in 1 until size - 1) {
            solution[r * size] = 2
        }
        endpoints[1 * size] = 2
        endpoints[(size - 2) * size] = 2

        // Inside area: rows 1 until size - 2, cols 1 until size - 2
        var nextPair = 3
        val insideCols = size - 3
        val numColTriplets = insideCols / 3
        val remainder = insideCols % 3

        for (t in 0 until numColTriplets) {
            val col1 = 3 * t + 1
            val col2 = 3 * t + 2
            val col3 = 3 * t + 3

            // col1: wall at row 1, path below
            walls[1 * size + col1] = true
            val p1 = nextPair++
            for (r in 2 until size - 2) {
                solution[r * size + col1] = p1
            }
            endpoints[2 * size + col1] = p1
            endpoints[(size - 3) * size + col1] = p1

            // col2: path from row 1 to size - 3
            val p2 = nextPair++
            for (r in 1 until size - 2) {
                solution[r * size + col2] = p2
            }
            endpoints[1 * size + col2] = p2
            endpoints[(size - 3) * size + col2] = p2

            // col3: all walls from row 1 to size - 3
            for (r in 1 until size - 2) {
                walls[r * size + col3] = true
            }
        }

        if (remainder >= 1) {
            val col1 = 3 * numColTriplets + 1
            walls[1 * size + col1] = true
            val p1 = nextPair++
            for (r in 2 until size - 2) {
                solution[r * size + col1] = p1
            }
            endpoints[2 * size + col1] = p1
            endpoints[(size - 3) * size + col1] = p1
        }
        if (remainder >= 2) {
            val col2 = 3 * numColTriplets + 2
            val p2 = nextPair++
            for (r in 1 until size - 2) {
                solution[r * size + col2] = p2
            }
            endpoints[1 * size + col2] = p2
            endpoints[(size - 3) * size + col2] = p2
        }

        // Turn corridor pair
        val turnPair = nextPair++
        for (r in 1 until size - 1) {
            solution[r * size + size - 2] = turnPair
        }
        for (c in 1 until size - 1) {
            solution[(size - 2) * size + c] = turnPair
        }
        endpoints[1 * size + size - 2] = turnPair
        endpoints[(size - 2) * size + 1] = turnPair

        return PathPuzzle(
            id = "corridor-$size",
            size = size,
            endpoints = endpoints.toList(),
            walls = walls.toList(),
            solution = solution.toList(),
            pack = pack,
            storedGrade = "easy:1",
        )
    }
}
