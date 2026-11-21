package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SudokuRulesTest {
    @Test
    fun validatesHandBuiltRowsColumnsAndThreeByThreeBoxes() {
        val puzzle = puzzle()
        val repeatedRowDigit = solution.toMutableList().also { it[1] = it[0] }
        val repeatedColumnDigit = solution.toMutableList().also { it[9] = it[0] }
        val repeatedBoxDigit = solution.toMutableList().also { values ->
            for (row in 0 until BOARD_SIZE) {
                val first = row * BOARD_SIZE
                val fifth = first + 4
                val value = values[first]
                values[first] = values[fifth]
                values[fifth] = value
            }
        }

        assertTrue(SudokuRules.isValidSolution(puzzle))
        assertFalse(SudokuRules.isValidSolution(puzzle, repeatedRowDigit))
        assertFalse(SudokuRules.isValidSolution(puzzle, repeatedColumnDigit))
        assertTrue(rowsAreValid(repeatedBoxDigit))
        assertTrue(columnsAreValid(repeatedBoxDigit))
        assertFalse(SudokuRules.isValidSolution(puzzle, repeatedBoxDigit))
        assertFalse(SudokuRules.isValidSolution(puzzle, solution.dropLast(1)))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoAndHonorsItsCap() {
        val boxConflict = puzzle(givens = givensConflictingOnlyInTopLeftBox())
        val unique = puzzle(givens = solution.map { it as Int? })
        val multiple = puzzle()

        assertEquals(0, SudokuRules.countSolutions(boxConflict))
        assertEquals(2, countSolutionsIgnoringBoxes(boxConflict, limit = 2))
        assertEquals(1, SudokuRules.countSolutions(unique))
        assertEquals(2, SudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(1, SudokuRules.countSolutions(multiple, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            SudokuRules.countSolutions(multiple, limit = 0)
        }
    }

    @Test
    fun initialStateLocksGivensAndDigitMovesClearCellNotes() {
        val givens = List(81) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens)
        val initial = SudokuState.initial(puzzle)
        val noted = initial.withMove(Move.SetNotes(Cell(1), (1 shl 0) or (1 shl 8)))
        val entered = noted.withMove(Move.SetDigit(Cell(1), 4))
        val cleared = entered.withMove(Move.SetDigit(Cell(1), 0))

        assertEquals(solution[0], initial.digits[0])
        assertEquals(null, initial.digits[1])
        assertEquals(0, initial.notes[1])
        assertEquals((1 shl 0) or (1 shl 8), noted.notes[1])
        assertEquals(4, entered.digits[1])
        assertEquals(0, entered.notes[1])
        assertEquals(null, cleared.digits[1])
        assertEquals(0, cleared.notes[1])
        assertEquals(null, initial.digits[1])

        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(0), 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(0), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            entered.withMove(Move.SetNotes(Cell(1), 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetNotes(Cell(1), 1 shl 9))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetDigit(Cell(1), 10))
        }
        assertThrows(IllegalArgumentException::class.java) {
            initial.withMove(Move.SetMark(Cell(1), Mark.FILLED))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (entered.digits as MutableList<Int?>)[1] = null
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (noted.notes as MutableList<Int>)[1] = 0
        }
    }

    @Test
    fun mistakesAndSolvedStateUseEveryEnteredDigitAndIgnoreNotes() {
        val givens = List(81) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens)
        val initial = SudokuState.initial(puzzle)
        val wrong = initial
            .withMove(Move.SetDigit(Cell(1), 7))
            .withMove(Move.SetNotes(Cell(2), 1 shl 0))

        assertEquals(listOf(Cell(1)), SudokuRules.mistakes(puzzle, wrong))
        assertFalse(SudokuRules.isSolved(puzzle, wrong))

        val solved = solution.indices.filter { givens[it] == null }.fold(initial) { state, index ->
            state.withMove(Move.SetDigit(Cell(index), solution[index]))
        }
        val incorrect = solved.withMove(Move.SetDigit(Cell(1), 1))
        val incomplete = solved.withMove(Move.SetDigit(Cell(2), 0))

        assertTrue(SudokuRules.mistakes(puzzle, solved).isEmpty())
        assertTrue(SudokuRules.isSolved(puzzle, solved))
        assertFalse(SudokuRules.isSolved(puzzle, incorrect))
        assertFalse(SudokuRules.isSolved(puzzle, incomplete))
    }

    @Test
    fun codecRoundTripsGivensAndRejectsMalformedPayloadAndSolutions() {
        val givens = solution.mapIndexed { index, digit -> if (index % 7 == 0) digit else null }
        val puzzle = puzzle(givens)
        val line = SudokuCodec.encode(puzzle)
        val fields = line.split('|')
        val invalidPayload = fields.toMutableList().also { it[2] = "z" + it[2].drop(1) }.joinToString("|")
        val invalidSolution = fields.toMutableList().also { it[3] = "0" + it[3].drop(1) }.joinToString("|")
        val invalidGrid = fields.toMutableList().also { it[3] = "2" + it[3].drop(1) }.joinToString("|")
        val badSize = fields.toMutableList().also { it[1] = "8" }.joinToString("|")
        val mismatchedGiven = fields.toMutableList().also {
            it[2] = "9" + it[2].drop(1)
        }.joinToString("|")

        assertEquals(puzzle, SudokuCodec.decode(line))
        assertThrows(IllegalArgumentException::class.java) { SudokuCodec.decode(invalidPayload) }
        assertThrows(IllegalArgumentException::class.java) { SudokuCodec.decode(invalidSolution) }
        assertThrows(IllegalArgumentException::class.java) { SudokuCodec.decode(invalidGrid) }
        assertThrows(IllegalArgumentException::class.java) { SudokuCodec.decode(badSize) }
        assertThrows(IllegalArgumentException::class.java) { SudokuCodec.decode(mismatchedGiven) }
    }

    @Test
    fun engineExposesSudokuRulesAndCounter() {
        val puzzle = puzzle()
        val line = SudokuEngine.encodePuzzle(puzzle)
        val decoded = SudokuEngine.decodePuzzle(line)
        val state = SudokuEngine.applyMove(
            SudokuEngine.initialState(decoded),
            Move.SetDigit(Cell(0), 2),
        )

        assertEquals("sudoku", SudokuEngine.id)
        assertEquals(line, SudokuEngine.encodePuzzle(decoded))
        assertEquals(2, SudokuEngine.countSolutions(decoded, limit = 2))
        assertEquals(listOf(Cell(0)), SudokuEngine.mistakes(decoded, state))
        assertFalse(SudokuEngine.isSolved(decoded, state))
    }

    private fun puzzle(givens: List<Int?> = List(81) { null }): SudokuPuzzle = SudokuPuzzle(
        id = "SU-FIXTURE-9",
        givens = givens,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun givensConflictingOnlyInTopLeftBox(): List<Int?> = List(CELL_COUNT) { index ->
        when (index) {
            0, 10 -> 1
            else -> null
        }
    }

    private fun rowsAreValid(values: List<Int>): Boolean =
        (0 until BOARD_SIZE).all { row ->
            (0 until BOARD_SIZE).map { column -> values[row * BOARD_SIZE + column] }.toSet() == DIGITS
        }

    private fun columnsAreValid(values: List<Int>): Boolean =
        (0 until BOARD_SIZE).all { column ->
            (0 until BOARD_SIZE).map { row -> values[row * BOARD_SIZE + column] }.toSet() == DIGITS
        }

    /**
     * Reference counter that omits 3×3 boxes so tests can show when box pruning changes the count.
     */
    private fun countSolutionsIgnoringBoxes(puzzle: SudokuPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        val values = IntArray(CELL_COUNT)
        val rowUsed = IntArray(BOARD_SIZE)
        val columnUsed = IntArray(BOARD_SIZE)
        for (index in puzzle.givens.indices) {
            val given = puzzle.givens[index] ?: continue
            val bit = 1 shl (given - 1)
            val row = index / BOARD_SIZE
            val column = index % BOARD_SIZE
            if (rowUsed[row] and bit != 0 || columnUsed[column] and bit != 0) {
                return 0
            }
            values[index] = given
            rowUsed[row] = rowUsed[row] or bit
            columnUsed[column] = columnUsed[column] or bit
        }

        var solutionCount = 0
        fun search() {
            if (solutionCount >= limit) return
            var bestCell = -1
            var bestCandidates = 0
            var bestCount = BOARD_SIZE + 1
            for (index in values.indices) {
                if (values[index] != 0) continue
                val row = index / BOARD_SIZE
                val column = index % BOARD_SIZE
                val used = rowUsed[row] or columnUsed[column]
                val candidates = SUDOKU_NOTE_MASK and used.inv()
                val candidateCount = Integer.bitCount(candidates)
                if (candidateCount == 0) return
                if (candidateCount < bestCount) {
                    bestCell = index
                    bestCandidates = candidates
                    bestCount = candidateCount
                    if (candidateCount == 1) break
                }
            }
            if (bestCell == -1) {
                solutionCount++
                return
            }

            val row = bestCell / BOARD_SIZE
            val column = bestCell % BOARD_SIZE
            var candidates = bestCandidates
            while (candidates != 0 && solutionCount < limit) {
                val bit = Integer.lowestOneBit(candidates)
                val digit = Integer.numberOfTrailingZeros(bit) + 1
                values[bestCell] = digit
                rowUsed[row] = rowUsed[row] or bit
                columnUsed[column] = columnUsed[column] or bit
                search()
                values[bestCell] = 0
                rowUsed[row] = rowUsed[row] and bit.inv()
                columnUsed[column] = columnUsed[column] and bit.inv()
                candidates = candidates and bit.inv()
            }
        }

        search()
        return solutionCount
    }

    private companion object {
        const val BOARD_SIZE = 9
        const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE
        val DIGITS = (1..BOARD_SIZE).toSet()
        val solution = listOf(
            "534678912",
            "672195348",
            "198342567",
            "859761423",
            "426853791",
            "713924856",
            "961537284",
            "287419635",
            "345286179",
        ).flatMap { row -> row.map(Char::digitToInt) }
    }
}