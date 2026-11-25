package com.datathecodie.witgrid.engine.killersudoku.spec

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.killersudoku.KillerCage
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuCodec
import com.datathecodie.witgrid.engine.killersudoku.KillerSudokuPuzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.CAGE_TEXT
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.SOLUTION_TEXT
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.baseCages
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.emptyGivens
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.puzzle
import com.datathecodie.witgrid.engine.killersudoku.spec.KillerSudokuSpecFixtures.solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Independent Killer Sudoku bank line cases: canonical text, round trip, and malformed input. */
class KillerSudokuCodecSpecTest {
    private val emptyGivensText = ".".repeat(81)

    @Test
    fun encodesHandFixtureAsTheCanonicalBankLine() {
        assertEquals(bankLine(), KillerSudokuCodec.encode(puzzle()))
    }

    @Test
    fun decodesCanonicalLineBackToTheSamePuzzle() {
        val decoded = KillerSudokuCodec.decode(bankLine())

        assertEquals(puzzle(), decoded)
        assertEquals(baseCages, decoded.cages)
        assertEquals(Pack.EASY, decoded.pack)
        assertEquals("hand", decoded.storedGrade)
    }

    @Test
    fun roundTripKeepsGivensInTheirCells() {
        val givens = List(81) { index ->
            when (index) {
                0 -> 5
                80 -> 9
                else -> null
            }
        }
        val original = puzzle(givens = givens)
        val encoded = KillerSudokuCodec.encode(original)

        assertEquals("5" + ".".repeat(79) + "9", encoded.split('|')[2].substringBefore('/'))
        assertEquals(original, KillerSudokuCodec.decode(encoded))
    }

    @Test
    fun rejectsMalformedBankLines() {
        val malformed = listOf(
            "KS-SPEC-9|9|$emptyGivensText/$CAGE_TEXT|$SOLUTION_TEXT|EASY",
            bankLine(size = "6"),
            bankLine(size = "nine"),
            bankLine(givens = ".".repeat(80)),
            bankLine(givens = "x" + ".".repeat(80)),
            "KS-SPEC-9|9|$emptyGivensText$CAGE_TEXT|$SOLUTION_TEXT|EASY|hand",
            bankLine(cages = CAGE_TEXT.replace("8:0,1", "08:0,1")),
            bankLine(cages = CAGE_TEXT.replace("8:0,1", "0:0,1")),
            bankLine(cages = CAGE_TEXT.replace("17:2,3,4", "17:4,3,2")),
            bankLine(cages = CAGE_TEXT.replace("8:0,1;17:2,3,4", "17:2,3,4;8:0,1")),
            bankLine(cages = CAGE_TEXT.replace("31:76,77,78,79,80", "31:76,77,78,79")),
            bankLine(cages = CAGE_TEXT.replace("31:76,77,78,79,80", "31:76,77,78,79,81")),
            bankLine(cages = CAGE_TEXT.replace("8:0,1", "9:0,1")),
            bankLine(digits = "0" + SOLUTION_TEXT.drop(1)),
            bankLine(digits = "3" + SOLUTION_TEXT.drop(1)),
            bankLine(givens = "9" + emptyGivensText.drop(1)),
            bankLine(pack = "HARDEST"),
        )

        for (text in malformed) {
            assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.decode(text) }
        }
    }

    @Test
    fun encodeRejectsStoredSolutionThatBreaksACageSum() {
        val broken = KillerSudokuPuzzle(
            id = "KS-SPEC-9",
            givens = emptyGivens,
            cages = baseCages.toMutableList().also {
                it[0] = KillerCage(listOf(Cell(0), Cell(1)), sum = 9)
            },
            solution = solution,
            pack = Pack.EASY,
            storedGrade = "hand",
        )

        assertThrows(IllegalArgumentException::class.java) { KillerSudokuCodec.encode(broken) }
    }

    /** Builds the fixture bank line, replacing any field that a malformed case changes. */
    private fun bankLine(
        givens: String = emptyGivensText,
        cages: String = CAGE_TEXT,
        digits: String = SOLUTION_TEXT,
        size: String = "9",
        pack: String = "EASY",
        grade: String = "hand",
    ): String = "KS-SPEC-9|$size|$givens/$cages|$digits|$pack|$grade"
}
