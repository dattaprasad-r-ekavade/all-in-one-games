package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
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
        val steps = mutableListOf<HintStep>()
        while (!CrownsEngine.isSolved(puzzle, state)) {
            val step = requireNotNull(CrownsEngine.nextStep(puzzle, state))
            assertEquals(step, CrownsEngine.nextStep(puzzle, state))
            assertTrue(step.explanation.trim().endsWith('.'))
            assertEquals(1, step.explanation.count { it == '.' || it == '!' || it == '?' })
            techniques += step.technique.id
            steps += step
            state = step.moves.fold(state, CrownsEngine::applyMove)
            assertTrue(CrownsEngine.mistakes(puzzle, state).isEmpty())
        }

        assertTrue(CrownsEngine.isSolved(puzzle, solveWithHints(CrownsEngine, puzzle)))
        assertTrue(techniques.contains("region-line"))
        assertTrue(techniques.contains("touch-exclusion"))
        val grade = CrownsEngine.grade(puzzle)
        assertEquals(steps.size, grade.stepCount)
        assertEquals(steps.maxBy { it.technique.difficultyRank }.technique, grade.hardestTechnique)
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
    fun kRegionsInKLinesReserveThoseRowsOrColumns() {
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

        val transposedPuzzle = CrownsPuzzle(
            puzzle.id,
            puzzle.size,
            puzzle.regions.indices.map { index -> puzzle.regions[transpose(index, puzzle.size)] },
            (0 until puzzle.size).map { column -> puzzle.solutionColumns.indexOf(column) },
            puzzle.pack,
            puzzle.storedGrade,
        )
        val transposedState = CrownsState(
            transposedPuzzle,
            state.marks.indices.map { index -> state.marks[transpose(index, puzzle.size)] },
        )
        val columnStep = requireNotNull(CrownsEngine.nextStep(transposedPuzzle, transposedState))
        assertEquals("k-regions-k-lines", columnStep.technique.id)
        assertEquals(
            step.moves.map { transpose(it.cell.index, puzzle.size) }.sorted(),
            columnStep.moves.map { it.cell.index },
        )
        assertTrue(CrownsEngine.mistakes(
            transposedPuzzle,
            columnStep.moves.fold(transposedState, CrownsEngine::applyMove),
        ).isEmpty())
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
    fun completedRegionCannotReserveAdditionalLinesInASubset() {
        val puzzle = CrownsPuzzle(
            id = "CR-COMPLETED-REGION",
            size = 6,
            regions = listOf(
                "100112",
                "524123",
                "451222",
                "300054",
                "414313",
                "055353",
            ).flatMap { row -> row.map(Char::digitToInt) },
            solutionColumns = listOf(1, 3, 5, 0, 2, 4),
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        val crossedCells = setOf(11, 13, 14, 19, 20, 21, 25, 27, 31, 32, 33)
        val state = crossedCells.fold(CrownsState.initial(puzzle)) { current, index ->
            current.withMove(Move.SetMark(Cell(index), Mark.CROSSED))
        }.withMove(Move.SetMark(Cell(26), Mark.FILLED))

        assertTrue(CrownsEngine.mistakes(puzzle, state).isEmpty())
        // Region E is complete; combining it with region A must not reserve A's two rows.
        val step = requireNotNull(CrownsEngine.nextStep(puzzle, state))
        assertEquals(listOf(5, 7, 10), step.moves.map { it.cell.index })
        val nextState = step.moves.fold(state, CrownsEngine::applyMove)
        assertTrue(CrownsEngine.mistakes(puzzle, nextState).isEmpty())
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
    fun gradeRejectsTheCounterOnlyUniqueFixtureWhenNoTechniqueCanStart() {
        val puzzle = CrownsPuzzle(
            id = "CR-COUNTER-ONLY",
            size = 6,
            regions = listOf("100230", "024124", "253422", "350542", "444502", "242354")
                .flatMap { row -> row.map(Char::digitToInt) },
            solutionColumns = listOf(1, 3, 5, 0, 2, 4),
            pack = Pack.EASY,
            storedGrade = "pending",
        )

        assertUnique(CrownsEngine, puzzle)
        assertNull(CrownsEngine.nextStep(puzzle, CrownsState.initial(puzzle)))
        assertThrows(IllegalArgumentException::class.java) { CrownsEngine.grade(puzzle) }
    }

    @Test
    fun gradeMetadataRejectsMalformedValuesAndReservedTechniqueId() {
        listOf("", "none", "none:1", "region-line:0", "none:-1", "none:bad", "none:0:1").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { CrownsGradeCodec.validate(value) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            CrownsGradeCodec.encode(Grade(Technique("none", "Reserved", 0), 1, Pack.EASY))
        }
        assertEquals("none:0", CrownsGradeCodec.encode(Grade(null, 0, Pack.EASY)))
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

    private fun transpose(index: Int, size: Int): Int = index % size * size + index / size
}
