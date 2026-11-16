package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.engine.testkit.assertUnique
import com.datathecodie.witgrid.engine.testkit.solveWithHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BinaryTechniqueSolverTest {
    @Test
    fun pairCapSolvesUniqueFixtureAndGradeUsesHardestTechnique() {
        val puzzle = puzzleWithSolutionBlanks(setOf(2))
        assertUnique(BinaryEngine, puzzle)

        var state = BinaryEngine.initialState(puzzle)
        val techniqueIds = mutableListOf<String>()
        while (!BinaryEngine.isSolved(puzzle, state)) {
            val step = requireNotNull(BinaryEngine.nextStep(puzzle, state))
            assertEquals(step, BinaryEngine.nextStep(puzzle, state))
            assertSingleSentence(step.explanation)
            techniqueIds += step.technique.id
            state = step.moves.fold(state, BinaryEngine::applyMove)
            assertTrue(BinaryEngine.mistakes(puzzle, state).isEmpty())
        }

        assertEquals(listOf("pair-cap"), techniqueIds)
        assertTrue(BinaryEngine.isSolved(puzzle, solveWithHints(BinaryEngine, puzzle)))
        val grade = BinaryEngine.grade(puzzle)
        assertEquals(1, grade.stepCount)
        assertEquals(BinaryTechniques.PAIR_CAP, grade.hardestTechnique)
        assertEquals(Pack.EASY, grade.pack)
        assertEquals("pair-cap:1", BinaryGradeCodec.encode(grade))
        BinaryGradeCodec.validate(BinaryGradeCodec.encode(grade))
    }

    @Test
    fun gapFillSolvesUniqueMediumFixture() {
        val solution = validSolution(8)
        val puzzle = BinaryPuzzle(
            id = "BI-GAP-8",
            size = 8,
            givens = solution.mapIndexed { index, value -> if (index == 3) null else value },
            solution = solution,
            pack = Pack.MEDIUM,
            storedGrade = "pending",
        )
        assertUnique(BinaryEngine, puzzle)

        val step = requireNotNull(BinaryEngine.nextStep(puzzle, BinaryEngine.initialState(puzzle)))

        assertEquals("gap-fill", step.technique.id)
        assertEquals(listOf(Move.SetMark(Cell(3), Mark.FILLED)), step.moves)
        assertSingleSentence(step.explanation)
        assertEquals(Pack.MEDIUM, BinaryEngine.grade(puzzle).pack)
    }

    @Test
    fun countCompletionFillsTheRemainingSymbolOnceItsLimitIsReached() {
        val puzzle = puzzleWithSolutionBlanks(setOf(1))

        val step = requireNotNull(BinaryEngine.nextStep(puzzle, BinaryEngine.initialState(puzzle)))

        assertEquals("count-completion", step.technique.id)
        assertEquals(listOf(Move.SetMark(Cell(1), Mark.FILLED)), step.moves)
        assertSingleSentence(step.explanation)
        assertEquals(BinaryTechniques.COUNT_COMPLETION, BinaryEngine.grade(puzzle).hardestTechnique)
    }

    @Test
    fun rowAndColumnComparisonForceTheOppositeOrderOnHardBoards() {
        val solution = comparisonSolution
        val puzzle = BinaryPuzzle(
            id = "BI-COMPARISON-10",
            size = 10,
            givens = List(100) { null },
            solution = solution,
            pack = Pack.HARD,
            storedGrade = "pending",
        )
        assertTrue(BinaryRules.isValidSolution(puzzle))

        val rowState = comparisonState(puzzle)
        val rowStep = requireNotNull(BinaryEngine.nextStep(puzzle, rowState))
        assertEquals("line-comparison", rowStep.technique.id)
        assertEquals(
            "Row 2 matches Row 1 except at columns 1 and 10, so its empty cells must use the opposite order to keep lines distinct.",
            rowStep.explanation,
        )
        assertEquals(
            listOf(
                Move.SetMark(Cell(10), Mark.CROSSED),
                Move.SetMark(Cell(19), Mark.FILLED),
            ),
            rowStep.moves,
        )
        assertSingleSentence(rowStep.explanation)
        assertTrue(BinaryRules.mistakes(puzzle, rowStep.moves.fold(rowState, BinaryState::withMove)).isEmpty())

        val transposedPuzzle = BinaryPuzzle(
            id = "BI-COMPARISON-10-TRANSPOSED",
            size = 10,
            givens = List(100) { null },
            solution = transpose(solution),
            pack = Pack.HARD,
            storedGrade = "pending",
        )
        val transposedState = BinaryState(
            transposedPuzzle,
            rowState.symbols.indices.map { index -> rowState.symbols[transposeIndex(index, 10)] },
        )

        val columnStep = requireNotNull(BinaryEngine.nextStep(transposedPuzzle, transposedState))
        assertEquals("line-comparison", columnStep.technique.id)
        assertEquals(
            "Column 2 matches Column 1 except at rows 1 and 10, so its empty cells must use the opposite order to keep lines distinct.",
            columnStep.explanation,
        )
        assertSingleSentence(columnStep.explanation)
    }

    @Test
    fun comparisonIsUnavailableOnEasyAndMediumBoards() {
        for (size in listOf(6, 8)) {
            val solution = if (size == 6) solution6 else mediumComparisonSolution
            val puzzle = BinaryPuzzle(
                id = "BI-NO-COMPARISON-$size",
                size = size,
                givens = List(size * size) { null },
                solution = solution,
                pack = BinaryPackBands.packForSize(size),
                storedGrade = "pending",
            )
            val state = comparisonState(puzzle, solution)

            val step = BinaryEngine.nextStep(puzzle, state)

            assertTrue(step == null || step.technique.id != "line-comparison")
        }
    }

    @Test
    fun gradeRejectsUniqueButStuckAndMultipleSolutionPuzzles() {
        val stuck = stuckUniquePuzzle()
        assertUnique(BinaryEngine, stuck)
        assertNull(BinaryEngine.nextStep(stuck, BinaryEngine.initialState(stuck)))
        assertThrows(IllegalArgumentException::class.java) { BinaryEngine.grade(stuck) }

        val ambiguous = puzzleWithSolutionBlanks((0 until 36).toSet())
        assertEquals(2, BinaryEngine.countSolutions(ambiguous))
        assertThrows(IllegalArgumentException::class.java) { BinaryEngine.grade(ambiguous) }
    }

    @Test
    fun gradePackBandsAndZeroStepMetadataFollowBoardSize() {
        val expectedPacks = mapOf(
            6 to Pack.EASY,
            8 to Pack.MEDIUM,
            10 to Pack.HARD,
            12 to Pack.EXPERT,
        )
        for ((size, pack) in expectedPacks) {
            val solution = validSolution(size)
            val puzzle = BinaryPuzzle(
                id = "BI-FULL-$size",
                size = size,
                givens = solution,
                solution = solution,
                pack = pack,
                storedGrade = "pending",
            )

            val grade = BinaryEngine.grade(puzzle)

            assertEquals(Grade(null, 0, pack), grade)
            assertEquals("none:0", BinaryGradeCodec.encode(grade))
        }

        val wrongPack = BinaryPuzzle(
            id = "BI-WRONG-PACK",
            size = 6,
            givens = solution6,
            solution = solution6,
            pack = Pack.MEDIUM,
            storedGrade = "pending",
        )
        assertThrows(IllegalArgumentException::class.java) { BinaryEngine.grade(wrongPack) }
    }

    @Test
    fun gradeCodecRejectsMalformedOrUnknownMetadata() {
        listOf("", "none", "none:1", "pair-cap:0", "none:-1", "none:bad", "none:0:1", "unknown:1")
            .forEach { value ->
                assertThrows(IllegalArgumentException::class.java) { BinaryGradeCodec.validate(value) }
            }
        assertThrows(IllegalArgumentException::class.java) {
            BinaryGradeCodec.encode(Grade(Technique("none", "Reserved", 0), 1, Pack.EASY))
        }
        BinaryGradeCodec.validate("none:0")
        BinaryGradeCodec.validate("pair-cap:1")
    }

    private fun puzzleWithSolutionBlanks(blanks: Set<Int>): BinaryPuzzle = BinaryPuzzle(
        id = "BI-UNIQUE-6",
        size = 6,
        givens = solution6.mapIndexed { index, value -> if (index in blanks) null else value },
        solution = solution6,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun stuckUniquePuzzle(): BinaryPuzzle {
        val givens = listOf(
            "..1...",
            "......",
            ".1.0..",
            "0...1.",
            "..1..0",
            "1...0.",
        ).flatMap { row -> row.map { value -> if (value == '.') null else value.digitToInt() } }
        return BinaryPuzzle("BI-STUCK-6", 6, givens, solution6, Pack.EASY, "pending")
    }

    private fun comparisonState(puzzle: BinaryPuzzle, solution: List<Int> = comparisonSolution): BinaryState {
        var state = BinaryState.initial(puzzle)
        val differingColumns = (0 until puzzle.size).filter { column ->
            solution[column] != solution[puzzle.size + column]
        }
        require(differingColumns.size == 2)
        for (column in 0 until puzzle.size) {
            state = state.withMove(moveFor(column, solution[column]))
        }
        for (row in 1..2) {
            for (column in 0 until puzzle.size) {
                if (column in differingColumns) continue
                val index = row * puzzle.size + column
                state = state.withMove(moveFor(index, solution[index]))
            }
        }
        return state
    }

    private fun moveFor(index: Int, value: Int): Move.SetMark = Move.SetMark(
        Cell(index),
        if (value == 0) Mark.FILLED else Mark.CROSSED,
    )

    private fun validSolution(size: Int): List<Int> {
        val base = when (size) {
            6 -> "001011"
            8 -> "00101011"
            10 -> "0010101011"
            12 -> "001010101011"
            else -> throw IllegalArgumentException("Unsupported test size")
        }
        return (0 until size).flatMap { row ->
            (0 until size).map { column -> (base[(row + column) % size] - '0') }
        }
    }

    private fun transpose(values: List<Int>): List<Int> = (0 until 100).map { index ->
        values[transposeIndex(index, 10)]
    }

    private fun transposeIndex(index: Int, size: Int): Int = index % size * size + index / size

    private fun assertSingleSentence(explanation: String) {
        assertTrue(explanation.trim().endsWith('.'))
        assertEquals(1, explanation.count { it == '.' || it == '!' || it == '?' })
    }

    private companion object {
        val solution6 = listOf(
            "001011",
            "001101",
            "110010",
            "010011",
            "101100",
            "110100",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val comparisonSolution = listOf(
            "0010101011",
            "1010101010",
            "1101010100",
            "0110101001",
            "1011010010",
            "1100110100",
            "0101001101",
            "0010011011",
            "1001100110",
            "0101010101",
        ).flatMap { row -> row.map(Char::digitToInt) }

        val mediumComparisonSolution = listOf(
            "00101011",
            "10101010",
            "11010100",
            "01101001",
            "10010110",
            "11001100",
            "00110011",
            "01010101",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
