package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NonogramTechniqueSolverTest {
    @Test
    fun overlapFindsCellsSharedByEveryCluePlacement() {
        val puzzle = puzzle(listOf("01110", "00000", "00000", "00000", "00000"))
        val step = requireNotNull(NonogramEngine.nextStep(puzzle, NonogramState.initial(puzzle)))

        assertEquals("overlap", step.technique.id)
        assertEquals(listOf(Move.SetMark(Cell(2), Mark.FILLED)), step.moves)
        assertOneSentence(step.explanation)
    }

    @Test
    fun edgeRuleCrossesTheCellBesideACompletedRun() {
        val puzzle = puzzle(
            listOf(
                "1011000000",
                "0101100000",
                "0010110000",
                "0001011000",
                "0000101100",
                "0000010110",
                "0000001011",
                "1000000101",
                "1100000010",
                "0110000001",
            ),
            Pack.MEDIUM,
        )
        val state = NonogramState.initial(puzzle)
            .withMove(Move.SetMark(Cell(2), Mark.FILLED))
            .withMove(Move.SetMark(Cell(3), Mark.FILLED))
        val step = requireNotNull(NonogramEngine.nextStep(puzzle, state))

        assertEquals("edge-rules", step.technique.id)
        assertEquals(
            listOf(
                Move.SetMark(Cell(1), Mark.CROSSED),
                Move.SetMark(Cell(4), Mark.CROSSED),
            ),
            step.moves,
        )
        assertOneSentence(step.explanation)
    }

    @Test
    fun edgeRuleDoesNotCallAnExtendableRunComplete() {
        val puzzle = puzzle(
            listOf(
                "1011000000",
                "0101100000",
                "0010110000",
                "0001011000",
                "0000101100",
                "0000010110",
                "0000001011",
                "1000000101",
                "1100000010",
                "0110000001",
            ),
            Pack.MEDIUM,
        )
        val state = NonogramState.initial(puzzle)
            .withMove(Move.SetMark(Cell(2), Mark.FILLED))
        val step = requireNotNull(NonogramEngine.nextStep(puzzle, state))

        assertFalse(
            step.technique.id == "edge-rules" &&
                step.moves.any {
                    it is Move.SetMark && it.cell == Cell(1) && it.mark == Mark.CROSSED
                },
        )
    }

    @Test
    fun lineByLineRuleUsesRemainingPlacementsAndExplainsItsMark() {
        val puzzle = puzzle(listOf("00100", "00000", "00000", "00000", "00000"))
        var state = NonogramState.initial(puzzle)
        for (index in puzzle.solution.indices) {
            if (index == 2) continue
            state = state.withMove(
                Move.SetMark(
                    Cell(index),
                    if (puzzle.solution[index] == 1) Mark.FILLED else Mark.CROSSED,
                ),
            )
        }
        val step = requireNotNull(NonogramEngine.nextStep(puzzle, state))

        assertEquals("line-by-line", step.technique.id)
        assertEquals(listOf(Move.SetMark(Cell(2), Mark.FILLED)), step.moves)
        assertOneSentence(step.explanation)
    }

    @Test
    fun hintsSolveUniqueHandFixturesWithoutRepeatingStates() {
        val fixtures = listOf(
            puzzle(listOf("01110", "00000", "00000", "00000", "00000")),
            puzzle(listOf("11100", "00000", "00000", "00000", "00000")),
            puzzle(listOf("00100", "00000", "00000", "00000", "00000")),
        )

        fixtures.forEach { puzzle ->
            var state = NonogramState.initial(puzzle)
            val visited = mutableSetOf(state)
            var stepCount = 0
            while (!NonogramRules.isSolved(puzzle, state)) {
                val step = requireNotNull(NonogramEngine.nextStep(puzzle, state))
                assertOneSentence(step.explanation)
                val nextState = step.moves.fold(state, NonogramState::withMove)
                assertFalse(nextState == state)
                assertTrue(visited.add(nextState))
                state = nextState
                stepCount++
                assertTrue(stepCount <= puzzle.grid.cellCount)
            }
            assertTrue(NonogramRules.isSolved(puzzle, state))
        }
    }

    @Test
    fun gradeCountsFullLinePassesKeepsPackAndEncodesHardestRule() {
        Pack.entries.forEach { pack ->
            val puzzle = puzzle(gradeRows(pack), pack)
            val grade = NonogramEngine.grade(puzzle)

            assertEquals(pack, grade.pack)
            assertEquals(
                when (pack) {
                    Pack.EASY, Pack.MEDIUM -> 1
                    Pack.HARD -> 6
                    Pack.EXPERT -> 7
                },
                grade.stepCount,
            )
            assertEquals(
                if (pack == Pack.EASY || pack == Pack.MEDIUM) "overlap" else "line-by-line",
                grade.hardestTechnique?.id,
            )
            assertTrue(NonogramPackBands.isGradeInPackBand(pack, grade))
            assertEquals(
                "${grade.hardestTechnique?.id}:${grade.stepCount}",
                NonogramGradeCodec.encode(grade),
            )
            NonogramGradeCodec.validate(NonogramGradeCodec.encode(grade))
        }
    }

    @Test
    fun gradingRejectsAmbiguousAndUniqueButTechniqueStuckPuzzles() {
        val ambiguous = puzzle(listOf("10000", "01000", "00100", "00010", "00001"))
        val uniqueButStuck = puzzle(
            listOf("11000", "01100", "00101", "10010", "00010"),
        )

        assertEquals(2, NonogramRules.countSolutions(ambiguous, limit = 2))
        assertThrows(IllegalArgumentException::class.java) {
            NonogramEngine.grade(ambiguous)
        }
        assertEquals(1, NonogramRules.countSolutions(uniqueButStuck, limit = 2))
        assertNull(NonogramEngine.nextStep(uniqueButStuck, NonogramState.initial(uniqueButStuck)))
        assertThrows(IllegalArgumentException::class.java) {
            NonogramEngine.grade(uniqueButStuck)
        }
    }

    @Test
    fun gradingUsesPassCountsToSeparateOnePassAndMultiPassBands() {
        for (pack in listOf(Pack.EASY, Pack.MEDIUM)) {
            val size = NonogramPackBands.sizeForPack(pack)
            val oneFillRows = listOf("1" + "0".repeat(size - 1)) +
                List(size - 1) { "0".repeat(size) }
            val grade = NonogramEngine.grade(puzzle(oneFillRows, pack))

            assertEquals(1, grade.stepCount)
            assertTrue(NonogramPackBands.isGradeInPackBand(pack, grade))
        }

        val size = 10
        val oneFillRows = listOf("1" + "0".repeat(size - 1)) +
            List(size - 1) { "0".repeat(size) }
        val onePassLineByLinePuzzle = puzzle(oneFillRows, Pack.HARD)
        val overlapOnlyRows = listOf("1".repeat(size)) + List(size - 1) { "0".repeat(size) }
        val overlapOnlyPuzzle = puzzle(overlapOnlyRows, Pack.HARD)

        assertEquals(1, NonogramRules.countSolutions(onePassLineByLinePuzzle))
        assertThrows(IllegalArgumentException::class.java) {
            NonogramEngine.grade(onePassLineByLinePuzzle)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NonogramEngine.grade(overlapOnlyPuzzle)
        }
    }

    @Test
    fun solverRejectsForeignStatesAndStopsOnMistakesOrSolvedBoards() {
        val puzzle = puzzle(listOf("11111", "00000", "00000", "00000", "00000"))
        val otherPuzzle = puzzle(listOf("00000", "11111", "00000", "00000", "00000"))
        val solved = puzzle.solution.indices.fold(NonogramState.initial(puzzle)) { state, index ->
            state.withMove(
                Move.SetMark(
                    Cell(index),
                    if (puzzle.solution[index] == 1) Mark.FILLED else Mark.CROSSED,
                ),
            )
        }
        val mistaken = NonogramState.initial(puzzle).withMove(Move.SetMark(Cell(0), Mark.CROSSED))

        assertNull(NonogramEngine.nextStep(puzzle, solved))
        assertNull(NonogramEngine.nextStep(puzzle, mistaken))
        assertThrows(IllegalArgumentException::class.java) {
            NonogramEngine.nextStep(puzzle, NonogramState.initial(otherPuzzle))
        }
        assertThrows(IllegalArgumentException::class.java) { NonogramGradeCodec.validate("unknown:1") }
        assertThrows(IllegalArgumentException::class.java) { NonogramGradeCodec.validate("none:-1") }
        assertThrows(IllegalArgumentException::class.java) { NonogramGradeCodec.validate("overlap:0") }
    }

    private fun puzzle(rows: List<String>, pack: Pack = Pack.EASY): NonogramPuzzle {
        val size = rows.size
        val solution = rows.flatMap { row -> row.map(Char::digitToInt) }
        val clues = NonogramRules.cluesForSolution(size, solution)
        return NonogramPuzzle(
            id = "NO-SOLVER-${pack.name}",
            size = size,
            rowClues = clues.first,
            columnClues = clues.second,
            solution = solution,
            pack = pack,
            storedGrade = "pending",
        )
    }

    private fun gradeRows(pack: Pack): List<String> {
        val size = NonogramPackBands.sizeForPack(pack)
        return when (pack) {
            Pack.EASY, Pack.MEDIUM ->
                listOf("1".repeat(size)) + List(size - 1) { "0".repeat(size) }

            Pack.HARD -> hardGradeRows

            Pack.EXPERT -> hardGradeRows.map { row -> row + "00000" } +
                List(5) { "000000000000000" }
        }
    }

    private fun assertOneSentence(explanation: String) {
        assertTrue(explanation.isNotBlank())
        assertFalse(explanation.contains('\n'))
        assertEquals(1, explanation.count { it == '.' })
    }

    private companion object {
        val hardGradeRows = listOf(
            "1001011000",
            "0100100110",
            "1101000110",
            "1000001100",
            "0101011011",
            "0101011000",
            "0011101111",
            "0110010001",
            "0011000011",
            "0010000000",
        )
    }
}
