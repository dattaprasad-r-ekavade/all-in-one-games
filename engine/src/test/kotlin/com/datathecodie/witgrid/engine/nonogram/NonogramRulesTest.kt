package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NonogramRulesTest {
    @Test
    fun derivesRowAndColumnRunCluesFromHandBuiltPattern() {
        val clues = NonogramRules.cluesForSolution(5, stripedSolution)

        assertEquals(listOf(listOf(5), emptyList(), listOf(5), emptyList(), listOf(5)), clues.first)
        assertEquals(List(5) { listOf(1, 1, 1) }, clues.second)
        assertThrows(IllegalArgumentException::class.java) {
            NonogramRules.cluesForSolution(4, List(16) { 0 })
        }
        assertThrows(IllegalArgumentException::class.java) {
            NonogramRules.cluesForSolution(5, List(24) { 0 })
        }
        assertThrows(IllegalArgumentException::class.java) {
            NonogramRules.cluesForSolution(5, List(25) { if (it == 0) 2 else 0 })
        }
    }

    @Test
    fun validatesPlanSizesPackBandsAndClueFit() {
        for (pack in Pack.entries) {
            val size = NonogramPackBands.sizeForPack(pack)
            val solution = List(size * size) { 0 }
            val clues = NonogramRules.cluesForSolution(size, solution)
            val puzzle = NonogramPuzzle(
                "NO-EMPTY-${pack.name}",
                size,
                clues.first,
                clues.second,
                solution,
                pack,
                "pending",
            )

            assertEquals(size, puzzle.size)
            assertTrue(NonogramRules.isValidSolution(puzzle))
        }
        assertEquals(5, NonogramPackBands.sizeForPack(Pack.EASY))
        assertEquals(10, NonogramPackBands.sizeForPack(Pack.MEDIUM))
        assertEquals(10, NonogramPackBands.sizeForPack(Pack.HARD))
        assertEquals(15, NonogramPackBands.sizeForPack(Pack.EXPERT))

        assertThrows(IllegalArgumentException::class.java) {
            NonogramPuzzle("NO-BAD-RUN", 5, List(5) { listOf(3, 3) }, List(5) { emptyList() },
                List(25) { 0 }, Pack.EASY, "pending")
        }
        assertThrows(IllegalArgumentException::class.java) {
            NonogramPuzzle("NO-BAD-PACK", 5, List(5) { emptyList() }, List(5) { emptyList() },
                List(25) { 0 }, Pack.MEDIUM, "pending")
        }
    }

    @Test
    fun ruleCounterReturnsZeroOneAndTwoAndHonorsLimit() {
        val impossible = puzzle(
            rowClues = listOf(listOf(5), emptyList(), listOf(5), emptyList(), listOf(5)),
            columnClues = List(5) { emptyList() },
            solution = List(25) { 0 },
        )
        val unique = stripedPuzzle()
        val multiple = puzzle(
            rowClues = List(5) { listOf(1) },
            columnClues = List(5) { listOf(1) },
            solution = diagonalSolution,
        )

        assertEquals(0, NonogramRules.countSolutions(impossible))
        assertEquals(1, NonogramRules.countSolutions(unique))
        assertEquals(2, NonogramRules.countSolutions(multiple, limit = 2))
        assertEquals(1, NonogramRules.countSolutions(multiple, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            NonogramRules.countSolutions(multiple, limit = 0)
        }
    }

    @Test
    fun mistakesIncludeXOnFilledAndFillOnEmptyButIgnoreEmptyCells() {
        val puzzle = stripedPuzzle()
        val state = NonogramState.initial(puzzle)
            .withMove(Move.SetMark(Cell(0), Mark.CROSSED))
            .withMove(Move.SetMark(Cell(5), Mark.FILLED))
            .withMove(Move.SetMark(Cell(6), Mark.CROSSED))

        assertEquals(listOf(Cell(0), Cell(5)), NonogramRules.mistakes(puzzle, state))
        assertEquals(Mark.EMPTY, state.marks[7])
    }

    @Test
    fun solvedBoardRequiresExplicitCorrectFillAndCrossMarks() {
        val puzzle = stripedPuzzle()
        val completed = puzzle.solution.indices.fold(NonogramState.initial(puzzle)) { state, index ->
            state.withMove(
                Move.SetMark(
                    Cell(index),
                    if (puzzle.solution[index] == 1) Mark.FILLED else Mark.CROSSED,
                ),
            )
        }
        val wrongX = completed.withMove(Move.SetMark(Cell(0), Mark.CROSSED))
        val missingMark = completed.withMove(Move.SetMark(Cell(5), Mark.EMPTY))

        assertTrue(NonogramRules.isSolved(puzzle, completed))
        assertFalse(NonogramRules.isSolved(puzzle, NonogramState.initial(puzzle)))
        assertFalse(NonogramRules.isSolved(puzzle, wrongX))
        assertFalse(NonogramRules.isSolved(puzzle, missingMark))
    }

    @Test
    fun stateIsImmutableSupportsClearingAndRejectsOtherMoveTypes() {
        val puzzle = stripedPuzzle()
        val initial = NonogramState.initial(puzzle)
        val filled = initial.withMove(Move.SetMark(Cell(5), Mark.FILLED))
        val cleared = filled.withMove(Move.SetMark(Cell(5), Mark.EMPTY))

        assertEquals(Mark.EMPTY, initial.marks[5])
        assertEquals(Mark.FILLED, filled.marks[5])
        assertEquals(Mark.EMPTY, cleared.marks[5])
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(5), 1))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (filled.marks as MutableList<Mark>)[5] = Mark.CROSSED
        }
    }

    @Test
    fun codecRoundTripsCluesAndRejectsMalformedOrInconsistentData() {
        val puzzle = stripedPuzzle()
        val line = NonogramCodec.encode(puzzle)
        val fields = line.split('|')
        val invalidPayload = fields.toMutableList().also { it[2] = "4;-;5;-;5/1,1,1;1,1,1;1,1,1;1,1,1;1,1,1" }
            .joinToString("|")
        val invalidSolution = fields.toMutableList().also { it[3] = "x" + it[3].drop(1) }.joinToString("|")
        val invalidPack = fields.toMutableList().also { it[4] = Pack.MEDIUM.name }.joinToString("|")

        assertEquals(puzzle, NonogramCodec.decode(line))
        assertThrows(IllegalArgumentException::class.java) { NonogramCodec.decode(invalidPayload) }
        assertThrows(IllegalArgumentException::class.java) { NonogramCodec.decode(invalidSolution) }
        assertThrows(IllegalArgumentException::class.java) { NonogramCodec.decode(invalidPack) }
    }

    @Test
    fun engineExposesTheNonogramModelAndIndependentCounter() {
        val puzzle = stripedPuzzle()
        val line = NonogramEngine.encodePuzzle(puzzle)
        val decoded = NonogramEngine.decodePuzzle(line)
        val state = NonogramEngine.applyMove(
            NonogramEngine.initialState(decoded),
            Move.SetMark(Cell(0), Mark.CROSSED),
        )

        assertEquals("nonogram", NonogramEngine.id)
        assertEquals(line, NonogramEngine.encodePuzzle(decoded))
        assertEquals(1, NonogramEngine.countSolutions(decoded))
        assertEquals(listOf(Cell(0)), NonogramEngine.mistakes(decoded, state))
        assertFalse(NonogramEngine.isSolved(decoded, state))
    }

    private fun stripedPuzzle(): NonogramPuzzle = puzzle(
        rowClues = listOf(listOf(5), emptyList(), listOf(5), emptyList(), listOf(5)),
        columnClues = List(5) { listOf(1, 1, 1) },
        solution = stripedSolution,
    )

    private fun puzzle(
        rowClues: List<List<Int>>,
        columnClues: List<List<Int>>,
        solution: List<Int>,
    ): NonogramPuzzle = NonogramPuzzle(
        id = "NO-FIXTURE-5",
        size = 5,
        rowClues = rowClues,
        columnClues = columnClues,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private companion object {
        val stripedSolution = listOf("11111", "00000", "11111", "00000", "11111")
            .flatMap { row -> row.map(Char::digitToInt) }
        val diagonalSolution = (0 until 25).map { index -> if (index / 5 == index % 5) 1 else 0 }
    }
}
