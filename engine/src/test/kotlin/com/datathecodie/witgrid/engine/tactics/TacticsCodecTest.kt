package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsCodecTest {

    @Test
    fun roundTripsMateInOnePuzzle() {
        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val encoded = TacticsCodec.encode(puzzle)
        val decoded = TacticsCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(puzzle.id, decoded.id)
        assertEquals(puzzle.mateIn, decoded.mateIn)
        assertEquals(puzzle.solutionLine, decoded.solutionLine)
        assertEquals(puzzle.pack, decoded.pack)
        assertEquals(puzzle.storedGrade, decoded.storedGrade)
    }

    @Test
    fun roundTripsMateInTwoPuzzle() {
        val puzzle = TacticsRulesTest.mate2UniquePuzzle()
        val encoded = TacticsCodec.encode(puzzle)
        val decoded = TacticsCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(puzzle.mateIn, decoded.mateIn)
        assertEquals(puzzle.solutionLine, decoded.solutionLine)
    }

    @Test
    fun rejectsInvalidDeclaredMateHorizons() {
        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val line = TacticsCodec.encode(puzzle)
        val fen = puzzle.initialBoard.toFen()
        for (horizon in listOf("3", "4", "invalid", "0 1 invalid")) {
            val invalid = line.replace("$fen 1|", "$fen $horizon|")
            assertThrows(IllegalArgumentException::class.java) { TacticsCodec.decode(invalid) }
        }
    }

    @Test
    fun rejectsMalformedBankLine() {
        // Size not 8
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-size|6|7k/3Q4/5K2/8/8/8/8/8 w - - 1|d7g7|EASY|MATE_1")
        }

        // Invalid FEN payload
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-fen|8|not-a-fen|d7g7|EASY|MATE_1")
        }

        // Invalid solution move
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-move|8|7k/3Q4/5K2/8/8/8/8/8 w - - 1|z9z9|EASY|MATE_1")
        }

        // Solution does not deliver checkmate
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("no-mate|8|7k/3Q4/5K2/8/8/8/8/8 w - - 1|d7d8|EASY|MATE_1")
        }
    }

    @Test
    fun rejectsUnderpromotionAndGarbageUciSuffixes() {
        val puzzle = TacticsRulesTest.mate1UniquePuzzle()
        val line = TacticsCodec.encode(puzzle)
        for (badSuffix in listOf("d7g7r", "d7g7b", "d7g7n", "d7g7x", "d7g71", "d7g7q")) {
            val invalid = line.replace("|d7g7|", "|$badSuffix|")
            assertThrows(IllegalArgumentException::class.java) { TacticsCodec.decode(invalid) }
        }
    }

    @Test
    fun rejectsNonUniqueAttackerMoveInStoredLine() {
        // mate2FirstMoveUniqueSecondMoveDoublePuzzle has Qf8+ Kh7, then Qg7#, Qg8#, Rg7# all mate
        val puzzle = TacticsRulesTest.mate2FirstMoveUniqueSecondMoveDoublePuzzle()
        val bankLine = "CT-TEST|8|${puzzle.initialBoard.toFen()} 2|a3f8 h8h7 f8g7|MEDIUM|checkmate:2"
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode(bankLine)
        }
    }

    @Test
    fun rejectsPromotionSuffixOnNonPawnMoveToLastRank() {
        val puzzle = TacticsRulesTest.mate2UniquePuzzle()
        // d3d8 is Queen move to rank 8 (d3d8q). Queen cannot have promotion suffix!
        val bankLineWithQueenPromo = "CT-TEST|8|${puzzle.initialBoard.toFen()} 2|d3d8q c6d8 e1e8|MEDIUM|back-rank-mate:2"
        val ex = assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode(bankLineWithQueenPromo)
        }
        assertTrue(ex.message!!.contains("Promotion suffix 'q' only allowed for pawn"))
    }

    @Test
    fun codecRoundTripsAndValidatesPawnPromotion() {
        // White pawn on a7 mates with a7a8q#
        val fen = "7k/P7/6K1/8/8/8/8/8 w - - 0 1"
        val board = TacticsBoard.fromFen(fen)
        val move = ChessMove(squareToCell("a7"), squareToCell("a8"))
        val puzzle = TacticsPuzzle(
            id = "pawn-promo-mate",
            initialBoard = board,
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(move),
            pack = Pack.EASY,
            storedGrade = "checkmate:1",
        )
        val encoded = TacticsCodec.encode(puzzle)
        assertTrue(encoded.contains("|a7a8q|"), "Encoded solution line must contain 'q' for pawn promotion: $encoded")
        val decoded = TacticsCodec.decode(encoded)
        assertEquals(puzzle, decoded)

        // Suffix missing for pawn promotion to last rank is rejected
        val missingSuffix = encoded.replace("|a7a8q|", "|a7a8|")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode(missingSuffix)
        }
        assertTrue(ex.message!!.contains("requires promotion suffix 'q'"))
    }
}
