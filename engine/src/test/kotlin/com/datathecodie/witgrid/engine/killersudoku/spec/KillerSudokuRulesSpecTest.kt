package com.datathecodie.witgrid.engine.killersudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.killersudoku.KillerCage
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuRules
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.baseCages
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.emptyGivens
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.isSudokuGrid
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.puzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.repeatedDigitCages
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.rowCages
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Independent Killer Sudoku rule cases from plan 12.7, using hand-checked fixtures. */
class KillerSudokuRulesSpecTest {
    @Test
    fun fixtureGridIsValidSudokuAndCagesPartitionTheBoard() {
        assertTrue(isSudokuGrid(solution))
        assertEquals(
            (0 until 81).toList(),
            baseCages.flatMap { cage -> cage.cells.map { it.index } }.sorted(),
        )
    }

    @Test
    fun fixtureCageSumsAreRightAndCageDigitsAreDistinct() {
        for (cage in baseCages) {
            val digits = cage.cells.map { solution[it.index] }
            assertEquals(digits.size, digits.toSet().size)
            assertEquals(digits.sum(), cage.sum)
        }
    }

    @Test
    fun cageSortsItsCellsAndRejectsEmptyOversizedDuplicateOutOfRangeOrZeroSumShapes() {
        assertEquals(
            listOf(Cell(2), Cell(3), Cell(12)),
            KillerCage(listOf(Cell(12), Cell(2), Cell(3)), sum = 11).cells,
        )
        assertThrows(IllegalArgumentException::class.java) { KillerCage(emptyList(), sum = 5) }
        assertThrows(IllegalArgumentException::class.java) { KillerCage((0..9).map(::Cell), sum = 45) }
        assertThrows(IllegalArgumentException::class.java) { KillerCage(listOf(Cell(4), Cell(4)), sum = 8) }
        assertThrows(IllegalArgumentException::class.java) { KillerCage(listOf(Cell(81)), sum = 5) }
        assertThrows(IllegalArgumentException::class.java) { KillerCage(listOf(Cell(0)), sum = 0) }
    }

    @Test
    fun puzzleCagesMustPartitionTheBoardAndGivensMustBeDigits() {
        val overlapping = baseCages + KillerCage(listOf(Cell(0)), sum = 5)
        assertThrows(IllegalArgumentException::class.java) { puzzle(cages = overlapping) }
        assertThrows(IllegalArgumentException::class.java) { puzzle(cages = baseCages.dropLast(1)) }
        assertThrows(IllegalArgumentException::class.java) { puzzle(givens = List(80) { null }) }
        assertThrows(IllegalArgumentException::class.java) {
            puzzle(givens = List(81) { index -> if (index == 0) 0 else null })
        }

        assertEquals(8, puzzle().cageOf(Cell(0)).sum)
        assertEquals(listOf(Cell(27), Cell(28)), puzzle().cageOf(Cell(28)).cells)
    }

    @Test
    fun solutionMustFollowSudokuDigitsGivensAndCageSums() {
        val fixture = puzzle()

        assertTrue(KillerSudokuRules.isValidSolution(fixture, solution))
        assertTrue(KillerSudokuRules.isValidSolution(fixture))
        assertFalse(KillerSudokuRules.isValidSolution(fixture, solution.dropLast(1)))
        assertFalse(
            KillerSudokuRules.isValidSolution(fixture, solution.mapIndexed { index, digit -> if (index == 0) 0 else digit }),
        )
        // Copying cell 0 into cell 1 repeats a digit in row 0.
        assertFalse(KillerSudokuRules.isValidSolution(fixture, solution.toMutableList().also { it[1] = it[0] }))

        // Cage [0, 1] must total 8. A total of 9 is wrong, even though the digits form a Sudoku.
        val wrongSum = baseCages.toMutableList().also {
            it[0] = KillerCage(listOf(Cell(0), Cell(1)), sum = 9)
        }
        assertFalse(KillerSudokuRules.isValidSolution(puzzle(cages = wrongSum)))

        val correctGiven = List(81) { index -> if (index == 0) 5 else null }
        val wrongGiven = List(81) { index -> if (index == 0) 6 else null }
        assertTrue(KillerSudokuRules.isValidSolution(puzzle(givens = correctGiven)))
        assertFalse(KillerSudokuRules.isValidSolution(puzzle(givens = wrongGiven)))
    }

    @Test
    fun cageCannotRepeatADigitEvenWhenItsSumIsRight() {
        // Cells 0 and 28 both hold 5. They share no row, column or box, so the grid stays valid.
        val cage = repeatedDigitCages.last()
        assertEquals(listOf(Cell(0), Cell(28)), cage.cells)
        assertEquals(10, cage.sum)
        assertEquals(cage.sum, cage.cells.sumOf { solution[it.index] })
        assertTrue(isSudokuGrid(solution))

        assertFalse(KillerSudokuRules.isValidSolution(puzzle(cages = repeatedDigitCages)))
    }

    @Test
    fun solutionCounterReturnsZeroOneAndTwoWithinTheCap() {
        // Zero: two 1s in row 0 break the Sudoku rules.
        val conflictingGivens = List(81) { index -> if (index == 0 || index == 1) 1 else null }
        assertEquals(0, KillerSudokuRules.countSolutions(puzzle(givens = conflictingGivens), limit = 2))

        // Zero: two distinct digits cannot total 2.
        val impossibleCage = baseCages.toMutableList().also {
            it[0] = KillerCage(listOf(Cell(0), Cell(1)), sum = 2)
        }
        assertEquals(0, KillerSudokuRules.countSolutions(puzzle(cages = impossibleCage), limit = 2))

        // One: every cell is given, and the givens match the only grid left.
        assertEquals(1, KillerSudokuRules.countSolutions(puzzle(givens = solution), limit = 2))

        // One: the stored solution fits, and the cap stops the search after one.
        assertEquals(1, KillerSudokuRules.countSolutions(puzzle(), limit = 1))

        // Two: many Sudoku grids satisfy nine row cages of sum 45. The cap stops at two.
        assertEquals(2, KillerSudokuRules.countSolutions(puzzle(cages = rowCages), limit = 2))
    }

    @Test
    fun puzzleWithNoGivensHasEmptyGivenList() {
        assertEquals(emptyGivens, puzzle().givens)
    }
}
