package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BinaryRulesTest {
    @Test
    fun validGridHasBalancedDistinctRowsAndColumnsWithoutRunsOfThree() {
        val puzzle = puzzle()

        assertTrue(BinaryRules.isValidSolution(puzzle))
        assertFalse(BinaryRules.isValidSolution(puzzle, puzzle.solution.toMutableList().also { it[2] = 0 }))
        assertFalse(BinaryRules.isValidSolution(puzzle, puzzle.solution.dropLast(1)))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoWithCap() {
        val noSolution = puzzle(givens = List(36) { index -> if (index in 0..2) 0 else null })
        val oneSolution = puzzle(givens = solution)
        val multipleSolutions = puzzle()

        assertEquals(0, BinaryRules.countSolutions(noSolution))
        assertEquals(1, BinaryRules.countSolutions(oneSolution))
        assertEquals(2, BinaryRules.countSolutions(multipleSolutions, limit = 2))
        assertEquals(1, BinaryRules.countSolutions(multipleSolutions, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            BinaryRules.countSolutions(multipleSolutions, limit = 0)
        }
    }

    @Test
    fun completeGivensRejectDuplicateLinesAndColumnTriples() {
        val duplicateRows = listOf("001011", "001101", "110010", "010101", "101100", "110010")
            .flatMap { row -> row.map(Char::digitToInt) }
        val columnTriple = listOf("001011", "001101", "010011", "101100", "110010", "110100")
            .flatMap { row -> row.map(Char::digitToInt) }
        val invalidGrids = listOf(duplicateRows, transpose(duplicateRows), columnTriple, transpose(columnTriple))

        invalidGrids.forEach { values ->
            val puzzle = puzzle(givens = values)
            assertFalse(BinaryRules.isValidSolution(puzzle, values))
            assertEquals(0, BinaryRules.countSolutions(puzzle))
        }
    }

    @Test
    fun emptySixBySixGridHas4140SolutionsAndHonorsLargerCaps() {
        val puzzle = puzzle()

        assertEquals(3, BinaryRules.countSolutions(puzzle, limit = 3))
        assertEquals(40, BinaryRules.countSolutions(puzzle, limit = 40))
        assertEquals(4_140, BinaryRules.countSolutions(puzzle, limit = 4_141))
    }

    @Test
    fun mistakesReturnWrongCommittedSymbolsInRowMajorOrder() {
        val puzzle = puzzle(givens = List(36) { null })
        val state = BinaryState.initial(puzzle)
            .withMove(Move.SetMark(Cell(2), Mark.FILLED))
            .withMove(Move.SetMark(Cell(3), Mark.CROSSED))

        assertEquals(listOf(Cell(2), Cell(3)), BinaryRules.mistakes(puzzle, state))
    }

    @Test
    fun solvedBoardMatchesEveryStoredSymbolAndMayUseEitherMark() {
        val givens = List(36) { index -> if (index % 4 == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens)
        val state = BinaryState.initial(puzzle)
        val solved = solution.indices.fold(state) { current, index ->
            val mark = if (solution[index] == 0) Mark.FILLED else Mark.CROSSED
            current.withMove(Move.SetMark(Cell(index), mark))
        }

        assertTrue(BinaryRules.isSolved(puzzle, solved))
        assertTrue(BinaryRules.mistakes(puzzle, solved).isEmpty())
        assertFalse(BinaryRules.isSolved(puzzle, state))
    }

    @Test
    fun stateKeepsGivensAndRejectsUnsupportedMoves() {
        val givens = List(36) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens)
        val state = BinaryState.initial(puzzle)

        assertEquals(BinarySymbol.A, state.symbols[0])
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetMark(Cell(0), Mark.CROSSED))
        }
        assertThrows(IllegalArgumentException::class.java) {
            state.withMove(Move.SetDigit(Cell(1), 1))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (state.symbols as MutableList<BinarySymbol>)[1] = BinarySymbol.A
        }
    }

    @Test
    fun puzzleCopiesMutableGivensAndSolutionInputs() {
        val givens = solution.map { it as Int? }.toMutableList()
        val solutionInput = solution.toMutableList()
        val puzzle = BinaryPuzzle(
            id = "BI-COPY-6",
            size = 6,
            givens = givens,
            solution = solutionInput,
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        givens[0] = 1
        solutionInput[0] = 1

        assertEquals(0, puzzle.givens[0])
        assertEquals(0, puzzle.solution[0])
    }

    @Test
    fun bankCodecRoundTripsAndRejectsBadData() {
        val givens = List(36) { index -> if (index % 3 == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens)
        val line = BinaryCodec.encode(puzzle)
        val fields = line.split('|')
        val badPayload = fields.toMutableList().also { it[2] = "x" + it[2].drop(1) }.joinToString("|")
        val mismatchedGiven = fields.toMutableList().also { it[2] = "1" + it[2].drop(1) }.joinToString("|")
        val badSolution = fields.toMutableList().also { it[3] = "2" + it[3].drop(1) }.joinToString("|")
        val badPack = fields.toMutableList().also { it[4] = Pack.MEDIUM.name }.joinToString("|")

        assertEquals(puzzle, BinaryCodec.decode(line))
        assertThrows(IllegalArgumentException::class.java) { BinaryCodec.decode(badPayload) }
        assertThrows(IllegalArgumentException::class.java) { BinaryCodec.decode(mismatchedGiven) }
        assertThrows(IllegalArgumentException::class.java) { BinaryCodec.decode(badSolution) }
        assertThrows(IllegalArgumentException::class.java) { BinaryCodec.decode(badPack) }
    }

    @Test
    fun packBandsMatchThePlanSizes() {
        assertEquals(Pack.EASY, BinaryPackBands.packForSize(6))
        assertEquals(Pack.MEDIUM, BinaryPackBands.packForSize(8))
        assertEquals(Pack.HARD, BinaryPackBands.packForSize(10))
        assertEquals(Pack.EXPERT, BinaryPackBands.packForSize(12))
        assertThrows(IllegalArgumentException::class.java) { BinaryPackBands.packForSize(7) }
        assertThrows(IllegalArgumentException::class.java) { BinaryPackBands.packForSize(5) }
    }

    @Test
    fun everyPlanSizeSupportsAValidFullGridAndUniqueGivenPuzzle() {
        for (size in listOf(6, 8, 10, 12)) {
            val solution = solutionFor(size)
            val puzzle = BinaryPuzzle(
                id = "BI-FIXTURE-$size",
                size = size,
                givens = solution,
                solution = solution,
                pack = BinaryPackBands.packForSize(size),
                storedGrade = "pending",
            )
            val openPuzzle = BinaryPuzzle(
                id = "BI-OPEN-$size",
                size = size,
                givens = List(size * size) { null },
                solution = solution,
                pack = BinaryPackBands.packForSize(size),
                storedGrade = "pending",
            )

            assertTrue(BinaryRules.isValidSolution(puzzle), "Size $size stored grid must obey the rules")
            assertEquals(1, BinaryRules.countSolutions(puzzle), "Fully given size $size grid must be unique")
            assertEquals(puzzle, BinaryCodec.decode(BinaryCodec.encode(puzzle)))
            assertEquals(2, BinaryRules.countSolutions(openPuzzle), "Open size $size grid must honor the cap")
        }
    }

    @Test
    fun engineUsesTheBinaryPuzzleAndStateContract() {
        val puzzle = puzzle(givens = List(36) { null })
        val line = BinaryEngine.encodePuzzle(puzzle)
        val decoded = BinaryEngine.decodePuzzle(line)
        val state = BinaryEngine.applyMove(
            BinaryEngine.initialState(decoded),
            Move.SetMark(Cell(2), Mark.FILLED),
        )

        assertEquals("binary", BinaryEngine.id)
        assertEquals(line, BinaryEngine.encodePuzzle(decoded))
        assertEquals(2, BinaryEngine.countSolutions(decoded))
        assertEquals(listOf(Cell(2)), BinaryEngine.mistakes(decoded, state))
        assertFalse(BinaryEngine.isSolved(decoded, state))
    }

    private fun puzzle(givens: List<Int?> = List(36) { null }): BinaryPuzzle = BinaryPuzzle(
        id = "BI-FIXTURE-6",
        size = 6,
        givens = givens,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun solutionFor(size: Int): List<Int> {
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

    private fun transpose(values: List<Int>): List<Int> = (0 until 36).map { index ->
        values[(index % 6) * 6 + index / 6]
    }

    private companion object {
        val solution = listOf(
            "001011",
            "001101",
            "110010",
            "010011",
            "101100",
            "110100",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}
