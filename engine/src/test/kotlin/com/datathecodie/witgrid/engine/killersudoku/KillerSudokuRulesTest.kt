package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillerSudokuRulesTest {
    @Test
    fun cageAndPuzzleModelsEnforceGeometryOrderingAndImmutability() {
        val unsortedCage = KillerCage(listOf(Cell(12), Cell(2), Cell(3)), sum = 11)
        assertEquals(listOf(Cell(2), Cell(3), Cell(12)), unsortedCage.cells)
        assertEquals(11, unsortedCage.sum)

        assertThrows(IllegalArgumentException::class.java) {
            KillerCage(emptyList(), sum = 5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerCage((0 until 10).map(::Cell), sum = 45)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerCage(listOf(Cell(81)), sum = 5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerCage(listOf(Cell(0), Cell(0)), sum = 3)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerCage(listOf(Cell(0)), sum = 0)
        }

        val reversedCages = rowCages.reversed()
        val puzzle = puzzle(cages = reversedCages)
        val samePuzzle = puzzle(cages = rowCages)
        assertEquals(samePuzzle, puzzle)
        assertEquals(samePuzzle.hashCode(), puzzle.hashCode())
        assertTrue(unsortedCage.toString().contains("sum=11"))
        assertEquals(rowCages, puzzle.cages)
        assertEquals(0, puzzle.cageIndexOf(Cell(0)))
        assertEquals(rowCages[0], puzzle.cageOf(Cell(8)))
        assertEquals(0, puzzle.cageIndexByCell[8])
        assertThrows(IllegalArgumentException::class.java) { puzzle.cageIndexOf(Cell(81)) }
        assertThrows(IllegalArgumentException::class.java) { puzzle.cageOf(Cell(81)) }

        // Overlapping or incomplete cages must fail construction.
        val overlapping = rowCages.dropLast(1) + KillerCage(listOf(Cell(0), Cell(72)), sum = 8)
        assertThrows(IllegalArgumentException::class.java) { puzzle(cages = overlapping) }
        val incomplete = rowCages.dropLast(1)
        assertThrows(IllegalArgumentException::class.java) { puzzle(cages = incomplete) }

        assertThrows(UnsupportedOperationException::class.java) {
            (puzzle.cages as MutableList<KillerCage>)[0] = rowCages[1]
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (puzzle.givens as MutableList<Int?>)[0] = 5
        }
    }

    @Test
    fun validatesRowsColumnsBoxesGivensCageUniquenessAndCageSums() {
        val crossBoxCages = buildCrossBoxCages()
        val puzzle = puzzle(cages = crossBoxCages)

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

        // Cage containing (0,2) [index 2] and (1,3) [index 12] spans distinct rows, columns, and boxes.
        // Swapping digits to repeat a digit inside that cage while preserving its sum must still fail.
        val repeatedCageDigitPuzzle = puzzle(
            cages = buildCrossBoxCages(
                overrideFirstCrossCageSum = solution[2] + solution[3] + solution[2],
            ),
        )
        val wrongSumPuzzle = puzzle(
            cages = crossBoxCages.mapIndexed { index, cage ->
                if (index == 0) KillerCage(cage.cells, cage.sum + 1) else cage
            },
        )
        val givens = List(CELL_COUNT) { index -> if (index == 0) solution[0] else null }
        val puzzleWithGiven = puzzle(givens = givens, cages = crossBoxCages)
        val wrongGivenSolution = solution.toMutableList().also {
            // Swap columns 0 and 1 across all rows to stay a valid Sudoku grid, breaking given at cell 0.
            for (row in 0 until BOARD_SIZE) {
                val left = row * BOARD_SIZE
                val right = left + 1
                val value = it[left]
                it[left] = it[right]
                it[right] = value
            }
        }

        assertTrue(KillerSudokuRules.isValidSolution(puzzle))
        assertFalse(KillerSudokuRules.isValidSolution(puzzle, repeatedRowDigit))
        assertFalse(KillerSudokuRules.isValidSolution(puzzle, repeatedColumnDigit))
        assertFalse(KillerSudokuRules.isValidSolution(puzzle, repeatedBoxDigit))
        assertFalse(KillerSudokuRules.isValidSolution(repeatedCageDigitPuzzle))
        assertFalse(KillerSudokuRules.isValidSolution(wrongSumPuzzle))
        assertFalse(KillerSudokuRules.isValidSolution(puzzleWithGiven, wrongGivenSolution))
        assertFalse(KillerSudokuRules.isValidSolution(puzzle, solution.dropLast(1)))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoOnHandFixturesAndHonorsCap() {
        // 0 solutions: conflicting givens in top-left box.
        val boxConflict = puzzle(
            givens = List(CELL_COUNT) { index -> if (index == 0 || index == 10) 1 else null },
        )
        // 0 solutions: conflicting givens in the same cage across different row, column, and box.
        val cageConflict = puzzle(
            givens = List(CELL_COUNT) { index -> if (index == 2 || index == 12) 4 else null },
            cages = buildCrossBoxCages(),
        )
        // 0 solutions: impossible 2-cell cage sum 2 (cannot repeat digit 1).
        val impossibleCageSum = puzzle(
            cages = listOf(KillerCage(listOf(Cell(0), Cell(1)), sum = 2)) +
                rowCagesRestAfterFirstTwoCells(),
        )
        // 0 solutions: globally inconsistent zero-given cages preserving total board sum 405.
        val zeroGivensCages = uniqueZeroGivensCages()
        val shiftedSumCages = zeroGivensCages.mapIndexed { index, cage ->
            when (index) {
                0 -> KillerCage(cage.cells, cage.sum + 1)
                1 -> KillerCage(cage.cells, cage.sum - 1)
                else -> cage
            }
        }
        val globallyInconsistent = puzzle(cages = shiftedSumCages)

        // 1 solution: zero-given Killer Sudoku puzzle verified to have a single solution.
        val uniqueZeroGivens = puzzle(cages = zeroGivensCages)

        // Cage constraints narrow a multi-solution Sudoku down to a single Killer Sudoku solution.
        // Cells (0,3)=6, (0,4)=7, (3,3)=7, (3,4)=6 form a valid 6/7 swap across rows, columns, and boxes.
        val partialGivens = solution.mapIndexed { index, digit ->
            if (index in setOf(3, 4, 30, 31)) null else digit
        }
        val ambiguousSudoku = puzzle(givens = partialGivens, cages = rowCages)
        val disambiguatedByCages = puzzle(givens = partialGivens, cages = buildCrossBoxCages())

        // Multiple solutions with open row cages.
        val multiple = puzzle(cages = rowCages)

        assertEquals(0, KillerSudokuRules.countSolutions(boxConflict))
        assertEquals(0, KillerSudokuRules.countSolutions(cageConflict))
        assertEquals(0, KillerSudokuRules.countSolutions(impossibleCageSum))
        assertEquals(0, KillerSudokuRules.countSolutions(globallyInconsistent))
        assertEquals(1, KillerSudokuRules.countSolutions(uniqueZeroGivens))
        assertEquals(2, KillerSudokuRules.countSolutions(ambiguousSudoku, limit = 2))
        assertEquals(1, KillerSudokuRules.countSolutions(disambiguatedByCages, limit = 2))
        assertEquals(2, KillerSudokuRules.countSolutions(multiple, limit = 2))
        assertEquals(1, KillerSudokuRules.countSolutions(multiple, limit = 1))
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuRules.countSolutions(multiple, limit = 0)
        }
    }

    @Test
    fun initialStateLocksGivensAndDigitMovesClearCellNotes() {
        val givens = List(CELL_COUNT) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens)
        val initial = KillerSudokuState.initial(puzzle)
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
        assertNotEquals(initial, entered)

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
    fun mistakesAndSolvedStateUseEnteredDigitsAndIgnoreNotes() {
        val givens = List(CELL_COUNT) { index -> if (index == 0) solution[index] else null }
        val puzzle = puzzle(givens = givens, cages = buildCrossBoxCages())
        val initial = KillerSudokuState.initial(puzzle)
        val wrong = initial
            .withMove(Move.SetDigit(Cell(5), 1))
            .withMove(Move.SetDigit(Cell(1), 7))
            .withMove(Move.SetNotes(Cell(2), 1 shl 0))

        // Mistakes must be returned in row-major order.
        assertEquals(listOf(Cell(1), Cell(5)), KillerSudokuRules.mistakes(puzzle, wrong))
        assertFalse(KillerSudokuRules.isSolved(puzzle, wrong))

        val solved = solution.indices.filter { givens[it] == null }.fold(initial) { state, index ->
            state.withMove(Move.SetDigit(Cell(index), solution[index]))
        }
        val incorrect = solved.withMove(Move.SetDigit(Cell(1), 1))
        val incomplete = solved.withMove(Move.SetDigit(Cell(2), 0))

        assertTrue(KillerSudokuRules.mistakes(puzzle, solved).isEmpty())
        assertTrue(KillerSudokuRules.isSolved(puzzle, solved))
        assertFalse(KillerSudokuRules.isSolved(puzzle, incorrect))
        assertFalse(KillerSudokuRules.isSolved(puzzle, incomplete))

        val otherPuzzle = puzzle(cages = uniqueZeroGivensCages())
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuRules.isSolved(otherPuzzle, solved)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuRules.mistakes(otherPuzzle, solved)
        }
    }

    @Test
    fun stateConstructorRejectsInconsistentDigitsNotesAndGivens() {
        val givens = List(CELL_COUNT) { index -> if (index == 0) solution[0] else null }
        val puzzle = puzzle(givens = givens)
        val validDigits = List(CELL_COUNT) { index -> givens[index] }
        val validNotes = List(CELL_COUNT) { 0 }

        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuState(puzzle, validDigits.dropLast(1), validNotes)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuState(puzzle, validDigits, validNotes.dropLast(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuState(
                puzzle,
                validDigits.toMutableList().also { it[1] = 3 },
                validNotes.toMutableList().also { it[1] = 1 },
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuState(
                puzzle,
                validDigits.toMutableList().also { it[0] = null },
                validNotes,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            KillerSudokuState(
                puzzle,
                validDigits,
                validNotes.toMutableList().also { it[0] = 1 },
            )
        }
    }

    @Test
    fun codecRoundTripsCanonicalLinesAndRejectsMalformedPayloads() {
        val givens = solution.mapIndexed { index, digit -> if (index % 11 == 0) digit else null }
        val puzzle = puzzle(givens = givens, cages = buildCrossBoxCages())
        val line = KillerSudokuCodec.encode(puzzle)
        val fields = line.split('|')

        assertEquals(puzzle, KillerSudokuCodec.decode(line))

        // Also round-trips a puzzle with zero givens and unsorted input order.
        val zeroGivensPuzzle = puzzle(cages = uniqueZeroGivensCages().reversed())
        val zeroLine = KillerSudokuCodec.encode(zeroGivensPuzzle)
        assertEquals(zeroGivensPuzzle, KillerSudokuCodec.decode(zeroLine))

        val invalidGivensChar = fields.toMutableList().also {
            val parts = it[2].split('/')
            it[2] = "z" + parts[0].drop(1) + "/" + parts[1]
        }.joinToString("|")
        val missingSlash = fields.toMutableList().also {
            it[2] = it[2].replace("/", "")
        }.joinToString("|")
        val leadingZeroSum = fields.toMutableList().also {
            val parts = it[2].split('/')
            it[2] = parts[0] + "/0" + parts[1]
        }.joinToString("|")
        val unsortedCells = fields.toMutableList().also {
            val parts = it[2].split('/')
            val cages = parts[1].split(';').toMutableList()
            val first = cages[0].split(':')
            val swappedCells = first[1].split(',').reversed().joinToString(",")
            cages[0] = "${first[0]}:$swappedCells"
            it[2] = "${parts[0]}/${cages.joinToString(";")}"
        }.joinToString("|")
        val unsortedCages = fields.toMutableList().also {
            val parts = it[2].split('/')
            val cages = parts[1].split(';').reversed().joinToString(";")
            it[2] = "${parts[0]}/$cages"
        }.joinToString("|")
        val badSize = fields.toMutableList().also { it[1] = "6" }.joinToString("|")
        val badSolution = fields.toMutableList().also { it[3] = "0" + it[3].drop(1) }.joinToString("|")
        val mismatchedGiven = fields.toMutableList().also {
            val parts = it[2].split('/')
            it[2] = "9" + parts[0].drop(1) + "/" + parts[1]
        }.joinToString("|")

        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(invalidGivensChar) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(missingSlash) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(leadingZeroSum) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(unsortedCells) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(unsortedCages) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(badSize) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(badSolution) }
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(mismatchedGiven) }

        val invalidForEncode = KillerSudokuPuzzle(
            id = "KS-INVALID",
            givens = List(CELL_COUNT) { null },
            cages = rowCages.mapIndexed { index, cage ->
                if (index == 0) KillerCage(cage.cells, 44) else cage
            },
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "pending",
        )
        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.encode(invalidForEncode) }
    }

    @Test
    fun engineExposesKillerSudokuRulesAndCounter() {
        val puzzle = puzzle(cages = uniqueZeroGivensCages())
        val line = KillerSudokuEngine.encodePuzzle(puzzle)
        val decoded = KillerSudokuEngine.decodePuzzle(line)
        val state = KillerSudokuEngine.applyMove(
            KillerSudokuEngine.initialState(decoded),
            Move.SetDigit(Cell(0), 2),
        )

        assertEquals("killersudoku", KillerSudokuEngine.id)
        assertEquals(line, KillerSudokuEngine.encodePuzzle(decoded))
        assertEquals(1, KillerSudokuEngine.countSolutions(decoded, limit = 2))
        assertEquals(listOf(Cell(0)), KillerSudokuEngine.mistakes(decoded, state))
        assertFalse(KillerSudokuEngine.isSolved(decoded, state))
    }

    private fun puzzle(
        givens: List<Int?> = List(CELL_COUNT) { null },
        cages: List<KillerCage> = rowCages,
    ): KillerSudokuPuzzle = KillerSudokuPuzzle(
        id = "KS-FIXTURE-9",
        givens = givens,
        cages = cages,
        solution = solution,
        pack = Pack.EASY,
        storedGrade = "pending",
    )

    private fun cagesFromGroups(
        groups: List<List<Int>>,
        overrideFirstSum: Int? = null,
    ): List<KillerCage> = groups.mapIndexed { index, indices ->
        val sum = if (index == 0 && overrideFirstSum != null) {
            overrideFirstSum
        } else {
            indices.sumOf { solution[it] }
        }
        KillerCage(indices.map(::Cell), sum)
    }

    private fun buildCrossBoxCages(overrideFirstCrossCageSum: Int? = null): List<KillerCage> {
        val groups = listOf(
            listOf(2, 3, 12), // Spans row 0/1, col 2/3, box 0/1: cell 2 and 12 share no house.
            listOf(0, 1),
            listOf(4, 5),
            listOf(6, 7),
            listOf(8, 17),
            listOf(9, 10),
            listOf(11, 20),
            listOf(13, 22),
            listOf(14, 23),
            listOf(15, 16),
            listOf(18, 19, 27),
            listOf(21, 30),
            listOf(24, 25, 26),
        ) + (3 until 9).map { row ->
            val start = row * 9
            if (row == 3) {
                listOf(28, 29, 31, 32, 33, 34, 35)
            } else {
                (0 until 9).map { start + it }
            }
        }
        return groups.mapIndexed { index, indices ->
            val sum = if (index == 0 && overrideFirstCrossCageSum != null) {
                overrideFirstCrossCageSum
            } else {
                indices.sumOf { solution[it] }
            }
            KillerCage(indices.map(::Cell), sum)
        }
    }

    private fun rowCagesRestAfterFirstTwoCells(): List<KillerCage> =
        listOf(KillerCage((2 until 9).map(::Cell), (2 until 9).sumOf { solution[it] })) +
            rowCages.drop(1)

    private fun uniqueZeroGivensCages(): List<KillerCage> = cagesFromGroups(
        listOf(
            listOf(0, 9),
            listOf(1, 2, 10),
            listOf(3, 12),
            listOf(4, 13, 14, 23),
            listOf(5, 6),
            listOf(7, 16),
            listOf(8, 17),
            listOf(11, 20, 21, 22),
            listOf(15, 24),
            listOf(18, 19, 27, 28),
            listOf(25, 26, 35),
            listOf(29, 38),
            listOf(30, 31, 39),
            listOf(32, 40, 41),
            listOf(33, 34, 42),
            listOf(36, 45, 54),
            listOf(37, 46),
            listOf(43, 44, 52, 53),
            listOf(47, 56),
            listOf(48, 49, 57),
            listOf(50, 51),
            listOf(55, 64, 65),
            listOf(58, 66, 67),
            listOf(59, 68, 69),
            listOf(60, 61, 70),
            listOf(62, 71, 79, 80),
            listOf(63, 72, 73),
            listOf(74, 75, 76),
            listOf(77, 78),
        ),
    )

    private companion object {
        const val BOARD_SIZE = 9
        const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE
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

        val rowCages: List<KillerCage> = (0 until BOARD_SIZE).map { row ->
            KillerCage((0 until BOARD_SIZE).map { column -> Cell(row * BOARD_SIZE + column) }, sum = 45)
        }
    }
}
