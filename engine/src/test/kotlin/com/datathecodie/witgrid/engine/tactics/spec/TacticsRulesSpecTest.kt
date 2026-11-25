package com.datathecodie.witgrid.engine.tactics.spec

import com.datathecodie.witgrid.engine.tactics.ChessMove
import com.datathecodie.witgrid.engine.tactics.Piece
import com.datathecodie.witgrid.engine.tactics.PieceColor
import com.datathecodie.witgrid.engine.tactics.PieceType
import com.datathecodie.witgrid.engine.tactics.squareToCell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsRulesSpecTest {

    @Test
    fun kingsideCastlingMovesKingAndRook() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.CASTLING_FEN)
        val castle = ChessMove(squareToCell("e1"), squareToCell("g1"))

        assertTrue(board.legalMoves().contains(castle))
        val after = board.applyMove(castle)
        assertEquals(Piece(PieceColor.WHITE, PieceType.KING), after.pieceAt(squareToCell("g1")))
        assertEquals(Piece(PieceColor.WHITE, PieceType.ROOK), after.pieceAt(squareToCell("f1")))
        assertNull(after.pieceAt(squareToCell("h1")))
        assertNull(after.pieceAt(squareToCell("e1")))
    }

    @Test
    fun queensideCastlingMovesKingAndRook() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.CASTLING_FEN)
        val castle = ChessMove(squareToCell("e1"), squareToCell("c1"))

        assertTrue(board.legalMoves().contains(castle))
        val after = board.applyMove(castle)
        assertEquals(Piece(PieceColor.WHITE, PieceType.KING), after.pieceAt(squareToCell("c1")))
        assertEquals(Piece(PieceColor.WHITE, PieceType.ROOK), after.pieceAt(squareToCell("d1")))
        assertNull(after.pieceAt(squareToCell("a1")))
    }

    @Test
    fun castlingThroughAnAttackedSquareIsIllegal() {
        // The king would pass f1, which the black rook on f2 attacks.
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.CASTLING_THROUGH_ATTACK_FEN)
        val kingside = ChessMove(squareToCell("e1"), squareToCell("g1"))
        val queenside = ChessMove(squareToCell("e1"), squareToCell("c1"))

        assertFalse(board.legalMoves().contains(kingside))
        assertTrue(board.legalMoves().contains(queenside))
    }

    @Test
    fun enPassantCaptureIsLegalRightAfterTheDoublePush() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.EN_PASSANT_FEN)
        val capture = ChessMove(squareToCell("e5"), squareToCell("d6"))

        assertTrue(board.legalMoves().contains(capture))
        val after = board.applyMove(capture)
        assertNull(after.pieceAt(squareToCell("d5")))
        assertEquals(Piece(PieceColor.WHITE, PieceType.PAWN), after.pieceAt(squareToCell("d6")))
    }

    @Test
    fun enPassantCaptureIsIllegalWithoutATarget() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.NO_EN_PASSANT_FEN)
        val capture = ChessMove(squareToCell("e5"), squareToCell("d6"))

        assertFalse(board.legalMoves().contains(capture))
    }

    @Test
    fun doublePushSetsTheEnPassantTarget() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.START_FEN)
        val push = ChessMove(squareToCell("e2"), squareToCell("e4"))

        val after = board.applyMove(push)
        assertEquals(squareToCell("e3"), after.enPassant)
    }

    @Test
    fun pawnPromotesToQueenOnTheLastRank() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.PROMOTION_FEN)
        val pawnMoves = board.legalMoves().filter { it.from == squareToCell("a7") }
        assertEquals(listOf(ChessMove(squareToCell("a7"), squareToCell("a8"))), pawnMoves)

        val after = board.applyMove(pawnMoves.single())
        assertEquals(Piece(PieceColor.WHITE, PieceType.QUEEN), after.pieceAt(squareToCell("a8")))
    }

    @Test
    fun checkIsDetectedAndEveryLegalMoveEscapesIt() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.CHECK_FEN)

        assertTrue(board.isCheck())
        assertFalse(board.isCheckmate())
        assertFalse(board.isStalemate())
        val replies = board.legalMoves()
        assertTrue(replies.isNotEmpty())
        for (reply in replies) {
            assertFalse(board.applyMove(reply).isCheck(PieceColor.BLACK), "Move ${reply.toUci()} leaves the king in check")
        }
    }

    @Test
    fun backRankCheckmateHasNoLegalMoves() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.BACK_RANK_MATE_FEN)

        assertTrue(board.isCheck())
        assertTrue(board.isCheckmate())
        assertFalse(board.isStalemate())
        assertTrue(board.legalMoves().isEmpty())
    }

    @Test
    fun stalemateHasNoLegalMovesAndNoCheck() {
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.STALEMATE_FEN)

        assertFalse(board.isCheck())
        assertTrue(board.isStalemate())
        assertFalse(board.isCheckmate())
        assertTrue(board.legalMoves().isEmpty())
    }

    @Test
    fun illegalMoveIsRejectedWhenApplied() {
        // A king cannot move two squares.
        val board = TacticsSpecFixtures.board(TacticsSpecFixtures.MATE_IN_ONE_FEN)
        assertThrows(IllegalArgumentException::class.java) {
            board.applyMove(ChessMove(squareToCell("f6"), squareToCell("f8")))
        }
    }
}
