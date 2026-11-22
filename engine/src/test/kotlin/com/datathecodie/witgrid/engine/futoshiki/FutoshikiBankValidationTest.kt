package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FutoshikiBankValidationTest {
    private val expertSolution = listOf(
        1, 2, 3, 4, 5, 6, 7,
        2, 1, 4, 3, 7, 5, 6,
        3, 4, 1, 2, 6, 7, 5,
        4, 5, 6, 7, 1, 2, 3,
        5, 3, 7, 6, 2, 1, 4,
        6, 7, 2, 5, 3, 4, 1,
        7, 6, 5, 1, 4, 3, 2,
    )

    private val expertGivens = listOf<Int?>(
        null, null, null, null, null, null, null,
        null, null, null, null, null, null, null,
        null, null, null, 2, 6, null, null,
        null, null, null, null, null, null, 3,
        5, 3, 7, 6, 2, 1, 4,
        6, 7, 2, 5, 3, 4, 1,
        7, 6, 5, 1, 4, 3, 2,
    )

    private val hardSolution = (0 until 49).map { (it / 7 + it % 7) % 7 + 1 }

    private val hardGivens = listOf<Int?>(
        null, null, 3, null, null, null, 7,
        null, 3, null, null, null, null, 1,
        null, null, 5, null, 7, 1, null,
        null, 5, null, 7, 1, 2, null,
        null, null, 7, 1, 2, null, null,
        6, null, 1, null, null, null, null,
        null, null, 2, null, null, null, null,
    )

    @Test
    fun literalSignLimitsMatchSpecIndependently() {
        // Plan §12.8 and review note: Hard at least 10 signs, Expert at most 8 signs
        assertEquals(8, FutoshikiPackBands.MAX_EXPERT_SIGNS)
        assertEquals(10, FutoshikiPackBands.MIN_HARD_SIGNS)

        // Literal independent assertions without relying on helper internals
        val literalMaxExpert = 8
        val literalMinHard = 10

        assertTrue(FutoshikiPackBands.isSignCountInPackBand(Pack.EXPERT, literalMaxExpert))
        assertFalse(FutoshikiPackBands.isSignCountInPackBand(Pack.EXPERT, literalMaxExpert + 1))
        assertFalse(FutoshikiPackBands.isSignCountInPackBand(Pack.EXPERT, 9))

        assertTrue(FutoshikiPackBands.isSignCountInPackBand(Pack.HARD, literalMinHard))
        assertFalse(FutoshikiPackBands.isSignCountInPackBand(Pack.HARD, literalMinHard - 1))
        assertFalse(FutoshikiPackBands.isSignCountInPackBand(Pack.HARD, 9))
    }

    @Test
    fun expertEightSignBoundaryPassesBankValidationAndNineSignsIsRejected() {
        val h8 = "-------------------<----------------------".map { FutoshikiSign.fromHorizontalChar(it) }
        val v8 = "^--v-v--^---^---^--------^----------------".map { FutoshikiSign.fromVerticalChar(it) }

        val validExpert8 = FutoshikiPuzzle(
            id = "FU-EXPERT-8",
            size = 7,
            givens = expertGivens,
            horizontalSigns = h8,
            verticalSigns = v8,
            solution = expertSolution,
            pack = Pack.EXPERT,
            storedGrade = "naked-pair:25",
        )

        assertEquals(8, validExpert8.signCount)
        assertEquals(1, FutoshikiRules.countSolutions(validExpert8, limit = 2))

        val canonicalLine = FutoshikiCodec.encode(validExpert8)
        val decoded = FutoshikiCodec.decode(canonicalLine)
        assertEquals(validExpert8, decoded)

        val grade = FutoshikiTechniqueSolver.grade(validExpert8)
        assertEquals(Grade(FutoshikiTechniques.NAKED_PAIR, 25, Pack.EXPERT), grade)
        assertEquals(grade, FutoshikiEngine.grade(validExpert8))

        // Boundary: 9 signs on Expert must be rejected
        val h9 = "-------->----------<----------------------".map { FutoshikiSign.fromHorizontalChar(it) }
        val invalidExpert9 = FutoshikiPuzzle(
            id = "FU-EXPERT-9",
            size = 7,
            givens = expertGivens,
            horizontalSigns = h9,
            verticalSigns = v8,
            solution = expertSolution,
            pack = Pack.EXPERT,
            storedGrade = "naked-pair:25",
        )

        assertEquals(9, invalidExpert9.signCount)
        assertEquals(1, FutoshikiRules.countSolutions(invalidExpert9, limit = 2))

        val gradeException = assertThrows(IllegalArgumentException::class.java) {
            FutoshikiTechniqueSolver.grade(invalidExpert9)
        }
        assertTrue(gradeException.message!!.contains("sign count (9)"))

        val engineGradeException = assertThrows(IllegalArgumentException::class.java) {
            FutoshikiEngine.grade(invalidExpert9)
        }
        assertTrue(engineGradeException.message!!.contains("sign count (9)"))

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiCodec.encode(invalidExpert9)
        }

        val rawLine9 = BankLine(
            id = invalidExpert9.id,
            size = 7,
            payload = expertGivens.joinToString("") { it?.toString() ?: "." } + "/" +
                h9.joinToString("") { it.horizontalSymbol.toString() } + "/" +
                v8.joinToString("") { it.verticalSymbol.toString() },
            solution = expertSolution.joinToString(""),
            pack = Pack.EXPERT,
            grade = "naked-pair:25",
        ).encode()

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiCodec.decode(rawLine9)
        }
    }

    @Test
    fun hardTenSignBoundaryPassesBankValidationAndNineSignsIsRejected() {
        val h10 = List(42) { FutoshikiSign.NONE }
        val v10 = "--------------------------------^^^^v^^^^^".map { FutoshikiSign.fromVerticalChar(it) }

        val validHard10 = FutoshikiPuzzle(
            id = "FU-HARD-10",
            size = 7,
            givens = hardGivens,
            horizontalSigns = h10,
            verticalSigns = v10,
            solution = hardSolution,
            pack = Pack.HARD,
            storedGrade = "sign-chains:32",
        )

        assertEquals(10, validHard10.signCount)
        assertEquals(1, FutoshikiRules.countSolutions(validHard10, limit = 2))

        val canonicalLine = FutoshikiCodec.encode(validHard10)
        val decoded = FutoshikiCodec.decode(canonicalLine)
        assertEquals(validHard10, decoded)

        val grade = FutoshikiTechniqueSolver.grade(validHard10)
        assertEquals(Grade(FutoshikiTechniques.SIGN_CHAINS, 32, Pack.HARD), grade)
        assertEquals(grade, FutoshikiEngine.grade(validHard10))

        // Boundary: 9 signs on Hard must be rejected
        val v9 = "--------------------------------^^^^v^^^^-".map { FutoshikiSign.fromVerticalChar(it) }
        val invalidHard9 = FutoshikiPuzzle(
            id = "FU-HARD-9",
            size = 7,
            givens = hardGivens,
            horizontalSigns = h10,
            verticalSigns = v9,
            solution = hardSolution,
            pack = Pack.HARD,
            storedGrade = "sign-chains:32",
        )

        assertEquals(9, invalidHard9.signCount)

        val gradeException = assertThrows(IllegalArgumentException::class.java) {
            FutoshikiTechniqueSolver.grade(invalidHard9)
        }
        assertTrue(gradeException.message!!.contains("sign count (9)"))

        val engineGradeException = assertThrows(IllegalArgumentException::class.java) {
            FutoshikiEngine.grade(invalidHard9)
        }
        assertTrue(engineGradeException.message!!.contains("sign count (9)"))

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiCodec.encode(invalidHard9)
        }

        val rawLine9 = BankLine(
            id = invalidHard9.id,
            size = 7,
            payload = hardGivens.joinToString("") { it?.toString() ?: "." } + "/" +
                h10.joinToString("") { it.horizontalSymbol.toString() } + "/" +
                v9.joinToString("") { it.verticalSymbol.toString() },
            solution = hardSolution.joinToString(""),
            pack = Pack.HARD,
            grade = "sign-chains:32",
        ).encode()

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiCodec.decode(rawLine9)
        }
    }

    @Test
    fun nineteenSignExpertFixtureIsRejectedByBankValidation() {
        val h19 = "-------->------<---<->--------->----->-->-".map {
            FutoshikiSign.fromHorizontalChar(it)
        }
        val v19 = "^v-v-v--^---^---^--------^---^v----^v-----".map {
            FutoshikiSign.fromVerticalChar(it)
        }
        val puzzle19 = FutoshikiPuzzle(
            id = "FU-EXPERT-19-SIGNS",
            size = 7,
            givens = expertGivens,
            horizontalSigns = h19,
            verticalSigns = v19,
            solution = expertSolution,
            pack = Pack.EXPERT,
            storedGrade = "naked-pair:25",
        )

        assertEquals(19, puzzle19.signCount)
        assertEquals(1, FutoshikiRules.countSolutions(puzzle19, limit = 2))

        val gradeException = assertThrows(IllegalArgumentException::class.java) {
            FutoshikiTechniqueSolver.grade(puzzle19)
        }
        assertTrue(gradeException.message!!.contains("sign count (19)"))

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiEngine.grade(puzzle19)
        }

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiCodec.encode(puzzle19)
        }

        val rawLine19 = BankLine(
            id = puzzle19.id,
            size = 7,
            payload = expertGivens.joinToString("") { it?.toString() ?: "." } + "/" +
                h19.joinToString("") { it.horizontalSymbol.toString() } + "/" +
                v19.joinToString("") { it.verticalSymbol.toString() },
            solution = expertSolution.joinToString(""),
            pack = Pack.EXPERT,
            grade = "naked-pair:25",
        ).encode()

        assertThrows(IllegalArgumentException::class.java) {
            FutoshikiCodec.decode(rawLine19)
        }
    }
}
