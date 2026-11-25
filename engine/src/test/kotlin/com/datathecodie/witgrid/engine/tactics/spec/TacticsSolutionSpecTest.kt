package com.datathecodie.witgrid.engine.tactics.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.tactics.TacticsCodec
import com.datathecodie.witgrid.engine.tactics.TacticsRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsSolutionSpecTest {

    @Test
    fun countSolutionsIsOneForAUniqueMateInOne() {
        val puzzle = TacticsSpecFixtures.mateInOneUnique()

        assertEquals(1, TacticsRules.countSolutions(puzzle))
        assertTrue(TacticsRules.isValidSolution(puzzle))
    }

    @Test
    fun countSolutionsIsTwoForTwoMatesInOneAndTheCapIsRespected() {
        val puzzle = TacticsSpecFixtures.mateInOneWithSecondMate()

        assertEquals(2, TacticsRules.countSolutions(puzzle))
        assertEquals(2, TacticsRules.countSolutions(puzzle, limit = 2))
        assertEquals(1, TacticsRules.countSolutions(puzzle, limit = 1))
    }

    @Test
    fun countSolutionsRejectsAZeroLimit() {
        val puzzle = TacticsSpecFixtures.mateInOneUnique()

        assertThrows(IllegalArgumentException::class.java) { TacticsRules.countSolutions(puzzle, limit = 0) }
    }

    @Test
    fun countSolutionsIsZeroWhenNoForcedMateExists() {
        val puzzle = TacticsSpecFixtures.noMateQuietLine()

        assertEquals(0, TacticsRules.countSolutions(puzzle))
        assertFalse(TacticsRules.isValidSolution(puzzle))
    }

    @Test
    fun countSolutionsIgnoresTheStoredLine() {
        // Same position, three different stored lines. Only the first is a winning move.
        val winning = TacticsSpecFixtures.puzzle(
            "line-winning", TacticsSpecFixtures.TWO_MATES_IN_ONE_FEN, 1,
            listOf("e7g7"), Pack.EASY, "checkmate:1",
        )
        val otherMate = TacticsSpecFixtures.puzzle(
            "line-other-mate", TacticsSpecFixtures.TWO_MATES_IN_ONE_FEN, 1,
            listOf("e7e8"), Pack.EASY, "checkmate:1",
        )
        val nonWinning = TacticsSpecFixtures.puzzle(
            "line-non-winning", TacticsSpecFixtures.TWO_MATES_IN_ONE_FEN, 1,
            listOf("e7d6"), Pack.EASY, "checkmate:1",
        )

        assertEquals(2, TacticsRules.countSolutions(winning))
        assertEquals(2, TacticsRules.countSolutions(otherMate))
        assertEquals(2, TacticsRules.countSolutions(nonWinning))
    }

    @Test
    fun countSolutionsCountsWinningFirstMovesOnlyNotLaterMates() {
        // After Qf8+ Kh7 there are three mates, but only one first move wins.
        val puzzle = TacticsSpecFixtures.mateInTwoSecondMoveDouble()

        assertEquals(1, TacticsRules.countSolutions(puzzle))
    }

    @Test
    fun countSolutionsCountsTwoWinningFirstMoves() {
        val puzzle = TacticsSpecFixtures.mateInTwoTwoOpeners()

        assertEquals(2, TacticsRules.countSolutions(puzzle))
        assertEquals(1, TacticsRules.countSolutions(puzzle, limit = 1))
    }

    @Test
    fun uniqueForcedMateInTwoIsAccepted() {
        val puzzle = TacticsSpecFixtures.mateInTwoForcedDefence()

        assertEquals(1, TacticsRules.countSolutions(puzzle))
        assertTrue(TacticsRules.isValidSolution(puzzle))
    }

    @Test
    fun promotionMateIsAcceptedWithQueenPromotion() {
        val puzzle = TacticsSpecFixtures.promotionMate()

        assertEquals(1, TacticsRules.countSolutions(puzzle))
        assertTrue(TacticsRules.isValidSolution(puzzle))
    }

    @Test
    fun storedLineWithANonUniqueAttackerMoveIsRejected() {
        // The second White move has three mates (Qg7#, Qg8#, Rg7#), so the line is not unique.
        val puzzle = TacticsSpecFixtures.mateInTwoSecondMoveDouble()

        assertFalse(TacticsRules.isValidSolution(puzzle))
        assertThrows(IllegalArgumentException::class.java) { TacticsCodec.encode(puzzle) }
    }

    @Test
    fun storedLineWithAWrongFirstMoveIsRejected() {
        // Qd3-d7 is not a winning move. The only winning move is Qd3-d8.
        val wrong = TacticsSpecFixtures.puzzle(
            "wrong-first-move", TacticsSpecFixtures.MATE_IN_TWO_FEN, 2,
            listOf("d3d7", "c6d8", "e1e8"), Pack.MEDIUM, "back-rank-mate:2",
        )

        assertFalse(TacticsRules.isValidSolution(wrong))
        assertThrows(IllegalArgumentException::class.java) { TacticsCodec.encode(wrong) }
    }
}
