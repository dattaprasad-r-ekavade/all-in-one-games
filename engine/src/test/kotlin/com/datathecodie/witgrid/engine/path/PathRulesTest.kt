package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathRulesTest {

    // 5x5 unique puzzle with walls at (2,1) and (2,2)
    // Row 0: 1 1 1 1 1
    // Row 1: 2 3 3 3 1
    // Row 2: 2 # # 3 1
    // Row 3: 2 3 3 3 1
    // Row 4: 2 2 2 2 1
    private val unique5x5Line = "path-5-u|5|1....23....##...3......21|11111233312##312333122221|EASY|easy:1"

    // 4x4 puzzle with 2 solutions (walls at (1,2) and (2,1); paths 1 and 2 can swap sides)
    private val twoSolutions4x4Line = "path-4-2sol|4|1....2#..#1....2|122212#21#121112|EASY|easy:1"

    // 6x6 unique puzzle with walls
    private val unique6x6Line = "path-6-u|6|1.....2#4#5..3.#...34#..25....1.....|1111112#4#51234#51234#51255551111111|EASY|easy:1"

    @Test
    fun validatesRuleValidSolutionAndRejectsInvalidSolutions() {
        val puzzle = PathCodec.decode(unique5x5Line)
        assertTrue(PathRules.isValidSolution(puzzle))

        // Reject if wrong cell count
        assertFalse(PathRules.isValidSolution(puzzle, puzzle.solution.drop(1)))

        // Reject if non-wall cell has 0
        val withZero = puzzle.solution.toMutableList()
        withZero[1] = 0
        assertFalse(PathRules.isValidSolution(puzzle, withZero))

        // Reject if wall cell has non-zero
        val wallNonZero = puzzle.solution.toMutableList()
        wallNonZero[12] = 1
        assertFalse(PathRules.isValidSolution(puzzle, wallNonZero))

        // Reject if endpoint color doesn't match
        val wrongEndpoint = puzzle.solution.toMutableList()
        wrongEndpoint[0] = 2
        assertFalse(PathRules.isValidSolution(puzzle, wrongEndpoint))

        // Reject branching (degree 3)
        val branching = puzzle.solution.toMutableList()
        branching[2] = 2
        assertFalse(PathRules.isValidSolution(puzzle, branching))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoAndHonorsLimit() {
        val unique = PathCodec.decode(unique5x5Line)
        val twoSolutions = PathCodec.decode(twoSolutions4x4Line)

        // Create an impossible 5x5 puzzle: endpoint (0,0) is trapped by walls on all available sides
        val impossibleWalls = BooleanArray(25) { it == 1 || it == 5 }
        val impossibleEndpoints = ArrayList<Int?>(25)
        for (i in 0 until 25) {
            impossibleEndpoints.add(when (i) {
                0, 24 -> 1
                6, 23 -> 2
                else -> null
            })
        }
        val impossibleSolution = ArrayList<Int>(25)
        for (i in 0 until 25) {
            val endpoint = impossibleEndpoints[i]
            val value = when {
                impossibleWalls[i] -> 0
                endpoint != null -> endpoint
                else -> 1
            }
            impossibleSolution.add(value)
        }
        val impossible = PathPuzzle(
            id = "impossible",
            size = 5,
            endpoints = impossibleEndpoints,
            walls = impossibleWalls.toList(),
            solution = impossibleSolution,
            pack = Pack.EASY,
            storedGrade = "easy:1",
        )

        assertEquals(1, PathRules.countSolutions(unique))
        assertEquals(2, PathRules.countSolutions(twoSolutions, limit = 2))
        assertEquals(1, PathRules.countSolutions(twoSolutions, limit = 1))
        assertEquals(0, PathRules.countSolutions(impossible))

        assertThrows(IllegalArgumentException::class.java) {
            PathRules.countSolutions(unique, limit = 0)
        }
    }

    @Test
    fun solutionCounterSolvesUnique6x6WithWalls() {
        val puzzle6 = PathCodec.decode(unique6x6Line)
        assertEquals(1, PathRules.countSolutions(puzzle6))
    }

    @Test
    fun deadCellWithSharedAdjacentHeadAndTargetCorrectlyIdentifiedAsAlive() {
        // 4x4 fixture where cell (1,1) has zero unassigned neighbors.
        // Cell (1,1) is adjacent to head of pair 2 (row 0, col 1), target of pair 2 (row 2, col 1),
        // head of pair 1 (row 1, col 0), and a wall (row 1, col 2).
        // The old bug stored adjacentHead = 1 (overwriting 2) and adjacentTarget = 2,
        // wrongfully pruning cell (1,1) as dead because adjacentHead != adjacentTarget.
        // With the fix, pair 2 has both head and target adjacent, so cell (1,1) is alive.
        val fixture = "path-4-deadcell-shared-adj|4|#2#31.#.12#.3...|#2#312#312#33333|EASY|easy:1"
        val puzzle = PathCodec.decode(fixture)
        assertTrue(PathRules.isValidSolution(puzzle))
        assertEquals(1, PathRules.countSolutions(puzzle, limit = 2))
    }

    @Test
    fun pathCannotTouchItselfEnforcedInValidationAndSolutionCounting() {
        val puzzle = PathCodec.decode(unique5x5Line)
        assertTrue(PathRules.isValidSolution(puzzle))

        // Reject 2x2 block of one color
        // In unique5x5Line, change cells (0,1), (0,2), (1,1), (1,2) to color 1 (forming a 2x2 block of 1s)
        val with2x2 = puzzle.solution.toMutableList()
        with2x2[1] = 1
        with2x2[2] = 1
        with2x2[6] = 1
        with2x2[7] = 1
        assertFalse(PathRules.isValidSolution(puzzle, with2x2))

        // Reject self-touching path (cell orthogonally adjacent to another cell of same color
        // that is neither its previous nor its next cell on the path)
        val selfTouching = puzzle.solution.toMutableList()
        selfTouching[8] = 1
        assertFalse(PathRules.isValidSolution(puzzle, selfTouching))

        // Puzzle where a path forms a 2x2 or touches itself is rejected by decode
        val invalidSelfTouchingLine = "path-5-invalid-selftouch|5|1....23.........3......21|1111123331233312333122221|EASY|easy:1"
        assertThrows(IllegalArgumentException::class.java) {
            PathCodec.decode(invalidSelfTouchingLine)
        }

        // Direct construction confirms isValidSolution rejects self-touching solutions
        val invalidEndpoints = ArrayList<Int?>(25)
        val invalidWalls = ArrayList<Boolean>(25)
        val invalidSolution = ArrayList<Int>(25)
        for (i in 0 until 25) {
            invalidEndpoints.add(when (i) {
                0, 24 -> 1
                5, 23 -> 2
                6, 17 -> 3
                else -> null
            })
            invalidWalls.add(false)
        }
        val solChars = "1111123331233312333122221"
        for (i in 0 until 25) {
            invalidSolution.add(charToPair(solChars[i]))
        }
        val invalidPuzzle = PathPuzzle(
            id = "path-5-invalid-selftouch",
            size = 5,
            endpoints = invalidEndpoints,
            walls = invalidWalls,
            solution = invalidSolution,
            pack = Pack.EASY,
            storedGrade = "easy:1",
        )
        assertFalse(PathRules.isValidSolution(invalidPuzzle))

        // Isolated 2x2 area with adjacent endpoints: solution counter must return 0
        // because filling all four cells forces the path to touch itself.
        val isolated2x2Endpoints = ArrayList<Int?>(16)
        val isolated2x2Walls = ArrayList<Boolean>(16)
        val isolated2x2Solution = ArrayList<Int>(16)
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val is2x2 = r < 2 && c < 2
                isolated2x2Walls.add(!is2x2)
                isolated2x2Endpoints.add(if (r == 0 && (c == 0 || c == 1)) 1 else null)
                isolated2x2Solution.add(if (is2x2) 1 else 0)
            }
        }
        val isolated2x2Puzzle = PathPuzzle(
            id = "isolated-2x2",
            size = 4,
            endpoints = isolated2x2Endpoints,
            walls = isolated2x2Walls,
            solution = isolated2x2Solution,
            pack = Pack.EASY,
            storedGrade = "easy:1",
        )
        assertFalse(PathRules.isValidSolution(isolated2x2Puzzle))
        assertEquals(0, PathRules.countSolutions(isolated2x2Puzzle))
    }

    @Test
    fun initialStatePlacesGivensAndLocksThem() {
        val puzzle = PathCodec.decode(unique5x5Line)
        val initial = PathState.initial(puzzle)

        assertEquals(1, initial.digits[0])
        assertEquals(2, initial.digits[5])
        assertEquals(null, initial.digits[1])
        assertEquals(null, initial.digits[12])

        // Editing empty cell succeeds
        val entered = initial.withMove(Move.SetDigit(Cell(1), 1))
        assertEquals(1, entered.digits[1])

        // Clearing with 0 succeeds
        val cleared = entered.withMove(Move.SetDigit(Cell(1), 0))
        assertEquals(null, cleared.digits[1])

        // Editing given endpoint throws
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(0), 2))
        }

        // Editing wall throws
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(12), 1))
        }

        // Out of bounds cell throws
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(25), 1))
        }

        // Invalid digit throws
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(1), 4))
        }

        // Unsupported move types throw
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(1), Mark.FILLED))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(1), 1))
        }

        // Defensive copy check
        assertThrows(UnsupportedOperationException::class.java) {
            (initial.digits as MutableList<Int?>)[1] = 1
        }
    }

    @Test
    fun mistakesAndSolvedStateDetection() {
        val puzzle = PathCodec.decode(unique5x5Line)
        var state = PathState.initial(puzzle)

        assertFalse(PathRules.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), PathRules.mistakes(puzzle, state))

        // Enter correct digit at cell 1
        state = state.withMove(Move.SetDigit(Cell(1), 1))
        assertEquals(emptyList<Cell>(), PathRules.mistakes(puzzle, state))
        assertFalse(PathRules.isSolved(puzzle, state))

        // Enter wrong digit at cell 2 (should be 1, set to 2)
        state = state.withMove(Move.SetDigit(Cell(2), 2))
        assertEquals(listOf(Cell(2)), PathRules.mistakes(puzzle, state))
        assertFalse(PathRules.isSolved(puzzle, state))

        // Fix cell 2 and fill all cells with correct solution
        for (i in 0 until 25) {
            if (puzzle.walls[i] || puzzle.endpoints[i] != null) continue
            state = state.withMove(Move.SetDigit(Cell(i), puzzle.solution[i]))
        }

        assertTrue(PathRules.isSolved(puzzle, state))
        assertEquals(emptyList<Cell>(), PathRules.mistakes(puzzle, state))
    }
}
