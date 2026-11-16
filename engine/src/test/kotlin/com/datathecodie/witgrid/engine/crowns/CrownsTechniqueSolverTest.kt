package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.testkit.assertUnique
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrownsTechniqueSolverTest {
    @Test
    fun deterministicHintsSolveUniqueFixtureWithOneSentenceExplanations() {
        val puzzle = uniquePuzzle()
        assertUnique(CrownsEngine, puzzle)

        var state = CrownsEngine.initialState(puzzle)
        val techniques = mutableSetOf<String>()
        while (!CrownsEngine.isSolved(puzzle, state)) {
            val step = requireNotNull(CrownsEngine.nextStep(puzzle, state))
            assertTrue(step.explanation.trim().endsWith('.'))
            assertEquals(1, step.explanation.count { it == '.' || it == '!' || it == '?' })
            techniques += step.technique.id
            state = step.moves.fold(state, CrownsEngine::applyMove)
        }

        assertTrue(CrownsEngine.isSolved(puzzle, solveWithHints(CrownsEngine, puzzle)))
        assertTrue(techniques.contains("region-line"))
        assertTrue(techniques.contains("touch-exclusion"))
    }

    @Test
    fun placedCrownProducesTouchExclusionMovesForItsNeighbours() {
        val puzzle = uniquePuzzle()
        val crown = puzzle.grid.cell(0, puzzle.solutionColumns[0])
        val state = CrownsState.initial(puzzle).withMove(Move.SetMark(crown, Mark.FILLED))

        val step = requireNotNull(CrownsEngine.nextStep(puzzle, state))

        assertEquals("touch-exclusion", step.technique.id)
        assertEquals(
            puzzle.grid.neighbours(crown).map { Move.SetMark(it, Mark.CROSSED) },
            step.moves,
        )
        assertEquals(1, step.explanation.count { it == '.' || it == '!' || it == '?' })
    }

    @Test
    fun kRegionsInKRowsReserveThoseRows() {
        val puzzle = CrownsPuzzle(
            id = "CR-K-ROWS",
            size = 6,
            regions = listOf(
                "001123",
                "001123",
                "444442",
                "344554",
                "444444",
                "555555",
            ).flatMap { row -> row.map(Char::digitToInt) },
            solutionColumns = listOf(1, 3, 5, 0, 2, 4),
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        val solutionCells = puzzle.solutionColumns.mapIndexed { row, column -> row * puzzle.size + column }.toSet()
        val candidates = setOf(
            0, 1, 6, 7,
            2, 3, 8, 9,
            4, 10, 17,
            5, 11, 18,
            12, 26,
            21, 34,
        )
        val state = CrownsState(
            puzzle,
            puzzle.regions.indices.map { index ->
                if (index in candidates || index in solutionCells) Mark.EMPTY else Mark.CROSSED
            },
        )

        val step = requireNotNull(CrownsEngine.nextStep(puzzle, state))

        assertEquals("k-regions-k-lines", step.technique.id)
        assertTrue(step.moves.isNotEmpty())
        assertTrue(step.moves.all { it.cell.index / puzzle.size in 0..1 })
        assertEquals(1, step.explanation.count { it == '.' || it == '!' || it == '?' })
    }

    @Test
    fun wrongCommittedMarkHasNoHintAndSolvedBoardHasNoNextStep() {
        val puzzle = uniquePuzzle()
        val wrongCell = puzzle.grid.cell(0, 0)
        val wrongState = CrownsState.initial(puzzle).withMove(Move.SetMark(wrongCell, Mark.FILLED))
        val solvedState = puzzle.solutionColumns.mapIndexed { row, column ->
            Move.SetMark(puzzle.grid.cell(row, column), Mark.FILLED)
        }.fold(CrownsState.initial(puzzle), CrownsState::withMove)

        assertNull(CrownsEngine.nextStep(puzzle, wrongState))
        assertNull(CrownsEngine.nextStep(puzzle, solvedState))
    }

    @Test
    fun gradeUsesThePackBandDefinedByBoardSize() {
        val puzzle = uniquePuzzle()

        val grade = CrownsEngine.grade(puzzle)

        assertTrue(grade.stepCount > 0)
        assertEquals(Pack.EASY, grade.pack)
        assertEquals(Pack.EASY, CrownsPackBands.packForSize(6))
        assertEquals(Pack.MEDIUM, CrownsPackBands.packForSize(7))
        assertEquals(Pack.HARD, CrownsPackBands.packForSize(8))
        assertEquals(Pack.EXPERT, CrownsPackBands.packForSize(9))
        assertEquals(Pack.EXPERT, CrownsPackBands.packForSize(10))
        assertTrue(CrownsGradeCodec.encode(grade).startsWith("${grade.hardestTechnique?.id}:"))
        CrownsGradeCodec.validate(CrownsGradeCodec.encode(grade))
        CrownsGradeCodec.validate("none:0")
        assertThrows(IllegalArgumentException::class.java) { CrownsPackBands.packForSize(5) }
        assertThrows(IllegalArgumentException::class.java) { CrownsGradeCodec.validate("unknown:1") }
    }

    @Test
    fun gradeRejectsNonUniquePuzzlesAndWrongPack() {
        val ambiguous = CrownsPuzzle(
            id = "CR-AMBIGUOUS",
            size = 6,
            regions = (0 until 6).flatMap { row -> List(6) { row } },
            solutionColumns = listOf(1, 3, 5, 0, 2, 4),
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        val wrongPack = uniquePuzzle().let { puzzle ->
            CrownsPuzzle(
                puzzle.id,
                puzzle.size,
                puzzle.regions,
                puzzle.solutionColumns,
                Pack.MEDIUM,
                puzzle.storedGrade,
            )
        }

        assertThrows(IllegalArgumentException::class.java) { CrownsEngine.grade(ambiguous) }
        assertThrows(IllegalArgumentException::class.java) { CrownsEngine.grade(wrongPack) }
    }

    @Test
    fun uniquePuzzleFixtureMatchesStoredSolution() {
        val puzzle = uniquePuzzle()
        val solved = solveWithHints(CrownsEngine, puzzle)

        assertTrue(CrownsEngine.isSolved(puzzle, solved))
        assertTrue(CrownsRules.mistakes(puzzle, solved).isEmpty())
    }

    private fun uniquePuzzle(): CrownsPuzzle = CrownsPuzzle(
        id = "CR-UNIQUE-6",
        size = 6,
        regions = listOf(
            "505555",
            "555155",
            "555552",
            "355555",
            "554555",
            "555555",
        ).flatMap { row -> row.map(Char::digitToInt) },
        solutionColumns = listOf(1, 3, 5, 0, 2, 4),
        pack = Pack.EASY,
        storedGrade = "pending",
    )
}
