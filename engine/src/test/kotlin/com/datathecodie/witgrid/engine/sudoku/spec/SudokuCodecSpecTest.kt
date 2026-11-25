package com.datathecodie.witgrid.engine.sudoku.spec

import com.datathecodie.witgrid.engine.sudoku.SudokuEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Independent Sudoku 9x9 bank line checks, using the hand-made fixture line. */
class SudokuCodecSpecTest {
    @Test
    fun bankLineRoundTripsGivensSolutionPackAndGrade() {
        val line = SudokuSpecFixtures.bankLine()
        val puzzle = SudokuEngine.decodePuzzle(line)

        assertEquals(SudokuSpecFixtures.puzzle(), puzzle)
        assertEquals(line, SudokuEngine.encodePuzzle(puzzle))
    }

    @Test
    fun givenThatDisagreesWithTheStoredSolutionIsRejected() {
        // The first payload symbol is 5 in the solution. Writing 1 makes a given disagree with it.
        val wrongGiven = SudokuSpecFixtures.payload.replaceRange(0, 1, "1")

        assertThrows(IllegalArgumentException::class.java) {
            SudokuEngine.decodePuzzle(SudokuSpecFixtures.bankLine(payloadText = wrongGiven))
        }
    }

    @Test
    fun storedSolutionThatBreaksARowRuleIsRejected() {
        // The first solution symbol is 5. Writing 6 repeats 6 in row 1.
        val brokenSolution = SudokuSpecFixtures.solutionText.replaceRange(0, 1, "6")

        assertThrows(IllegalArgumentException::class.java) {
            SudokuEngine.decodePuzzle(SudokuSpecFixtures.bankLine(solutionDigits = brokenSolution))
        }
    }

    @Test
    fun payloadSymbolsOtherThanDotAndDigitsOneToNineAreRejected() {
        val badSymbol = SudokuSpecFixtures.payload.replaceRange(2, 3, "x")

        assertThrows(IllegalArgumentException::class.java) {
            SudokuEngine.decodePuzzle(SudokuSpecFixtures.bankLine(payloadText = badSymbol))
        }
    }
}
