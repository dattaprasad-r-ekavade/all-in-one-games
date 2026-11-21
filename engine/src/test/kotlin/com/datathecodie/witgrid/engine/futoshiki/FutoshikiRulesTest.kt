package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FutoshikiRulesTest {
    private val solution = listOf(
        1, 2, 3, 4, 5,
        2, 3, 4, 5, 1,
        3, 4, 5, 1, 2,
        4, 5, 1, 2, 3,
        5, 1, 2, 3, 4,
    )

    private val horizontalSigns = ("<<<<" + "<<<>" + "<<><" + "<><<" + "><<<").map {
        FutoshikiSign.fromHorizontalChar(it)
    }
    private val verticalSigns = ("^^^^v" + "^^^v^" + "^^v^^" + "^v^^^").map {
        FutoshikiSign.fromVerticalChar(it)
    }

    private fun puzzle(
        id: String = "test-fu-1",
        size: Int = 5,
        givens: List<Int?> = List(size * size) { null },
        hSigns: List<FutoshikiSign> = horizontalSigns,
        vSigns: List<FutoshikiSign> = verticalSigns,
        sol: List<Int> = solution,
        pack: Pack = Pack.EASY,
        grade: String = "GRADE_A",
    ): FutoshikiPuzzle = FutoshikiPuzzle(
        id = id,
        size = size,
        givens = givens,
        horizontalSigns = hSigns,
        verticalSigns = vSigns,
        solution = sol,
        pack = pack,
        storedGrade = grade,
    )

    @Test
    fun validatesHandBuiltRowsColumnsAndSigns() {
        val puzzle = puzzle()

        assertTrue(FutoshikiRules.isValidSolution(puzzle))

        // Row duplicate
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = it[1]
        }))

        // Column duplicate
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = it[5]
        }))

        // Wrong size
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.dropLast(1)))

        // Value out of bounds
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = 6
        }))
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = 0
        }))

        // Violating horizontal inequality: (0,0) < (0,1), but change to (0,0)=3, (0,2)=1
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = 3
            it[2] = 1
        }))

        // Violating vertical inequality: (0,0) < (1,0), but change to (0,0)=4, (3,0)=1
        assertFalse(FutoshikiRules.isValidSolution(puzzle, solution.toMutableList().also {
            it[0] = 4
            it[15] = 1
        }))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoAndHonorsItsCap() {
        // Conflicting givens in row
        val rowConflictGivens = List(25) { index -> if (index == 0 || index == 1) 1 else null }
        val impossibleRow = puzzle(givens = rowConflictGivens)

        // Conflicting givens in column
        val colConflictGivens = List(25) { index -> if (index == 0 || index == 5) 2 else null }
        val impossibleCol = puzzle(givens = colConflictGivens)

        // Given violating inequality: (0,0) is 5, (0,1) is 1, but sign is '<'
        val ineqConflictGivens = List(25) { index ->
            when (index) {
                0 -> 5
                1 -> 1
                else -> null
            }
        }
        val impossibleIneq = puzzle(givens = ineqConflictGivens)

        assertEquals(0, FutoshikiRules.countSolutions(impossibleRow))
        assertEquals(0, FutoshikiRules.countSolutions(impossibleCol))
        assertEquals(0, FutoshikiRules.countSolutions(impossibleIneq))

        // Unique solution (all givens set to solution)
        val unique = puzzle(givens = solution)
        assertEquals(1, FutoshikiRules.countSolutions(unique))

        // Multiple solutions: no givens and no signs
        val noSignsH = List(20) { FutoshikiSign.NONE }
        val noSignsV = List(20) { FutoshikiSign.NONE }
        val multiple = puzzle(hSigns = noSignsH, vSigns = noSignsV)

        assertEquals(2, FutoshikiRules.countSolutions(multiple, limit = 2))
        assertEquals(1, FutoshikiRules.countSolutions(multiple, limit = 1))

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiRules.countSolutions(multiple, limit = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiRules.countSolutions(multiple, limit = -1)
        }
    }

    @Test
    fun initialStateLocksGivensAndDigitMovesClearCellNotes() {
        val givens = List(25) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens)
        val initial = FutoshikiState.initial(puzzle)
        val noted = initial.withMove(Move.SetNotes(Cell(1), (1 shl 0) or (1 shl 3)))
        val entered = noted.withMove(Move.SetDigit(Cell(1), 4))
        val cleared = entered.withMove(Move.SetDigit(Cell(1), 0))

        assertEquals(solution[0], initial.digits[0])
        assertEquals(null, initial.digits[1])
        assertEquals(0, initial.notes[1])
        assertEquals((1 shl 0) or (1 shl 3), noted.notes[1])
        assertEquals(4, entered.digits[1])
        assertEquals(0, entered.notes[1])
        assertEquals(null, cleared.digits[1])
        assertEquals(0, cleared.notes[1])
        assertEquals(null, initial.digits[1])

        // Givens cannot be changed
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(0), 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(0), 1))
        }

        // Cannot set notes in a filled cell
        assertThrows(IllegalArgumentException::class.java) {
            entered.withMove(Move.SetNotes(Cell(1), 1))
        }

        // Notes outside range 1..5
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(1), 1 shl 5))
        }

        // Digit outside 0..5
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(1), 6))
        }

        // Move.SetMark is unsupported
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(1), Mark.FILLED))
        }

        // Immutability: defensive lists throw on mutation
        assertThrows(UnsupportedOperationException::class.java) {
            (entered.digits as MutableList<Int?>)[1] = null
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (noted.notes as MutableList<Int>)[1] = 0
        }
    }

    @Test
    fun mistakesAndSolvedStateUseEveryEnteredDigitAndIgnoreNotes() {
        val givens = List(25) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens)
        val initial = FutoshikiState.initial(puzzle)
        val wrong = initial
            .withMove(Move.SetDigit(Cell(1), 5)) // solution[1] is 2
            .withMove(Move.SetNotes(Cell(2), 1 shl 0))

        assertEquals(listOf(Cell(1)), FutoshikiRules.mistakes(puzzle, wrong))
        assertFalse(FutoshikiRules.isSolved(puzzle, wrong))

        val solved = solution.indices.filter { givens[it] == null }.fold(initial) { state, index ->
            state.withMove(Move.SetDigit(Cell(index), solution[index]))
        }
        val incorrect = solved.withMove(Move.SetDigit(Cell(1), 1))
        val incomplete = solved.withMove(Move.SetDigit(Cell(2), 0))

        assertTrue(FutoshikiRules.mistakes(puzzle, solved).isEmpty())
        assertTrue(FutoshikiRules.isSolved(puzzle, solved))
        assertFalse(FutoshikiRules.isSolved(puzzle, incorrect))
        assertFalse(FutoshikiRules.isSolved(puzzle, incomplete))
    }

    @Test
    fun codecRoundTripsGivensAndSignsAndRejectsMalformed() {
        val givens = solution.mapIndexed { index, digit -> if (index % 4 == 0) digit else null }
        val puzzle = puzzle(givens = givens)
        val line = FutoshikiCodec.encode(puzzle)

        assertEquals(puzzle, FutoshikiCodec.decode(line))

        val fields = line.split('|')

        // Payload with bad character in givens
        val invalidPayloadGivens = fields.toMutableList().also {
            val parts = it[2].split('/')
            it[2] = ("9" + parts[0].drop(1)) + "/" + parts[1] + "/" + parts[2]
        }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) { FutoshikiCodec.decode(invalidPayloadGivens) }

        // Payload with bad character in hSigns
        val invalidPayloadHSigns = fields.toMutableList().also {
            val parts = it[2].split('/')
            it[2] = parts[0] + "/" + ("x" + parts[1].drop(1)) + "/" + parts[2]
        }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) { FutoshikiCodec.decode(invalidPayloadHSigns) }

        // Payload with missing parts
        val invalidPayloadParts = fields.toMutableList().also {
            it[2] = "not/enough"
        }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) { FutoshikiCodec.decode(invalidPayloadParts) }

        // Invalid solution digit
        val invalidSolution = fields.toMutableList().also {
            it[3] = "6" + it[3].drop(1)
        }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) { FutoshikiCodec.decode(invalidSolution) }

        // Invalid board size
        val badSize = fields.toMutableList().also { it[1] = "4" }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) { FutoshikiCodec.decode(badSize) }

        // Mismatched given
        val mismatchedGiven = fields.toMutableList().also {
            val parts = it[2].split('/')
            it[2] = ("5" + parts[0].drop(1)) + "/" + parts[1] + "/" + parts[2]
        }.joinToString("|")
        assertThrows(IllegalArgumentException::class.java) { FutoshikiCodec.decode(mismatchedGiven) }
    }

    @Test
    fun engineExposesFutoshikiRulesAndCounter() {
        val puzzle = puzzle()
        val line = FutoshikiEngine.encodePuzzle(puzzle)
        val decoded = FutoshikiEngine.decodePuzzle(line)
        val state = FutoshikiEngine.applyMove(
            FutoshikiEngine.initialState(decoded),
            Move.SetDigit(Cell(0), 4),
        )

        assertEquals("futoshiki", FutoshikiEngine.id)
        assertEquals(line, FutoshikiEngine.encodePuzzle(decoded))
        assertEquals(1, FutoshikiEngine.countSolutions(decoded, limit = 2))
        assertEquals(listOf(Cell(0)), FutoshikiEngine.mistakes(decoded, state))
        assertFalse(FutoshikiEngine.isSolved(decoded, state))

        // Unimplemented methods throw NotImplementedError
        assertThrows(NotImplementedError::class.java) {
            FutoshikiEngine.generate(SplitMix64(1L), Pack.EASY)
        }
    }
}
