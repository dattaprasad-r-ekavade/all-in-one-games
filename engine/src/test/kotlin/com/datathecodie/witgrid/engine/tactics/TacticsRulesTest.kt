package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsRulesTest {

    @Test
    fun solutionCounterReturnsZeroOneAndTwoWithCap() {
        val mate1Unique = mate1UniquePuzzle()
        val mate1Double = mate1DoublePuzzle()
        val zeroMate = zeroMatePuzzle()

        assertEquals(1, TacticsRules.countSolutions(mate1Unique))
        assertEquals(2, TacticsRules.countSolutions(mate1Double))
        assertEquals(1, TacticsRules.countSolutions(mate1Double, limit = 1))
        assertEquals(0, TacticsRules.countSolutions(zeroMate))
    }

    @Test
    fun mateInTwoCounterAndPlaythrough() {
        val puzzle = mate2UniquePuzzle()
        assertEquals(1, TacticsRules.countSolutions(puzzle))

        val s0 = TacticsEngine.initialState(puzzle)
        assertFalse(TacticsEngine.isSolved(puzzle, s0))
        assertEquals(emptyList<Cell>(), TacticsEngine.mistakes(puzzle, s0))

        // First move: White plays Qd3xd8+
        val s1 = TacticsEngine.applyMove(s0, Move.SetMark(squareToCell("d3"), Mark.FILLED))
        assertEquals(squareToCell("d3"), s1.pendingFrom)

        val s2 = TacticsEngine.applyMove(s1, Move.SetMark(squareToCell("d8"), Mark.FILLED))
        assertNull(s2.pendingFrom)
        // Black auto-replied with Be7xd8
        assertEquals(ChessMove(squareToCell("e7"), squareToCell("d8")), s2.lastOpponentMove)
        assertEquals(Piece(PieceColor.BLACK, PieceType.BISHOP), s2.currentBoard.pieceAt(squareToCell("d8")))
        assertFalse(TacticsEngine.isSolved(puzzle, s2))
        assertEquals(emptyList<Cell>(), TacticsEngine.mistakes(puzzle, s2))

        // Second move: White plays Rd1xd8#
        val s3 = TacticsEngine.applyMove(s2, Move.SetDigit(squareToCell("d1"), 1))
        val s4 = TacticsEngine.applyMove(s3, Move.SetDigit(squareToCell("d8"), 1))
        assertTrue(TacticsEngine.isSolved(puzzle, s4))
        assertEquals(emptyList<Cell>(), TacticsEngine.mistakes(puzzle, s4))
        assertTrue(s4.currentBoard.isCheckmate())
    }

    @Test
    fun mistakeReportingAndDeselection() {
        val puzzle = mate1UniquePuzzle()
        val s0 = TacticsEngine.initialState(puzzle)

        // Select and deselect
        val s1 = TacticsEngine.applyMove(s0, Move.SetMark(squareToCell("d7"), Mark.FILLED))
        assertEquals(squareToCell("d7"), s1.pendingFrom)
        val s2 = TacticsEngine.applyMove(s1, Move.SetMark(squareToCell("d7"), Mark.EMPTY))
        assertNull(s2.pendingFrom)

        // Make wrong move (d7d8 is not the mating move d7g7)
        val s3 = TacticsEngine.applyMove(s2, Move.SetMark(squareToCell("d7"), Mark.FILLED))
        val s4 = TacticsEngine.applyMove(s3, Move.SetMark(squareToCell("d8"), Mark.FILLED))
        assertFalse(TacticsEngine.isSolved(puzzle, s4))
        assertEquals(listOf(squareToCell("d8")), TacticsEngine.mistakes(puzzle, s4))
    }

    @Test
    fun selectionSwitchToAnotherFriendlyPiece() {
        val puzzle = mate2UniquePuzzle()
        val s0 = TacticsEngine.initialState(puzzle)

        // Pick Queen at d3
        val s1 = TacticsEngine.applyMove(s0, Move.SetMark(squareToCell("d3"), Mark.FILLED))
        assertEquals(squareToCell("d3"), s1.pendingFrom)

        // Switch to Rook at d1 (another friendly piece, not a legal capture)
        val s2 = TacticsEngine.applyMove(s1, Move.SetMark(squareToCell("d1"), Mark.FILLED))
        assertEquals(squareToCell("d1"), s2.pendingFrom)
    }

    @Test
    fun enPassantAndPromotionRules() {
        // En passant position: White pawn at e5, Black plays d7-d5 -> White captures e5xd6 e.p.
        val fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        val board = TacticsBoard.fromFen(fen)
        assertEquals(squareToCell("d6"), board.enPassant)

        val epMove = ChessMove(squareToCell("e5"), squareToCell("d6"))
        assertTrue(board.legalMoves().contains(epMove))

        val afterEp = board.applyMove(epMove)
        assertNull(afterEp.pieceAt(squareToCell("d5"))) // captured Black pawn removed
        assertEquals(Piece(PieceColor.WHITE, PieceType.PAWN), afterEp.pieceAt(squareToCell("d6")))

        // Promotion position: White pawn at a7, moves to a8 and promotes to Queen
        val promoFen = "8/P7/5k2/8/8/5K2/8/8 w - - 0 1"
        val promoBoard = TacticsBoard.fromFen(promoFen)
        val promoMove = ChessMove(squareToCell("a7"), squareToCell("a8"))
        val afterPromo = promoBoard.applyMove(promoMove)
        assertEquals(Piece(PieceColor.WHITE, PieceType.QUEEN), afterPromo.pieceAt(squareToCell("a8")))
    }

    @Test
    fun castlingRules() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        val board = TacticsBoard.fromFen(fen)

        // White kingside O-O (e1 -> g1)
        val oo = ChessMove(squareToCell("e1"), squareToCell("g1"))
        assertTrue(board.legalMoves().contains(oo))
        val afterOo = board.applyMove(oo)
        assertEquals(Piece(PieceColor.WHITE, PieceType.KING), afterOo.pieceAt(squareToCell("g1")))
        assertEquals(Piece(PieceColor.WHITE, PieceType.ROOK), afterOo.pieceAt(squareToCell("f1")))
        assertNull(afterOo.pieceAt(squareToCell("h1")))

        // White queenside O-O-O (e1 -> c1)
        val ooo = ChessMove(squareToCell("e1"), squareToCell("c1"))
        assertTrue(board.legalMoves().contains(ooo))
        val afterOoo = board.applyMove(ooo)
        assertEquals(Piece(PieceColor.WHITE, PieceType.KING), afterOoo.pieceAt(squareToCell("c1")))
        assertEquals(Piece(PieceColor.WHITE, PieceType.ROOK), afterOoo.pieceAt(squareToCell("d1")))
        assertNull(afterOoo.pieceAt(squareToCell("a1")))
    }

    companion object {
        fun mate1UniquePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate1-unique",
            initialBoard = TacticsBoard.fromFen("8/3Q4/5K2/8/8/8/8/7k w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("d7"), squareToCell("g7"))),
            pack = Pack.EASY,
            storedGrade = "MATE_1",
        )

        fun mate1DoublePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate1-double",
            initialBoard = TacticsBoard.fromFen("8/8/6KQ/8/8/8/8/7k w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("g6"), squareToCell("g7"))),
            pack = Pack.EASY,
            storedGrade = "MATE_1",
        )

        fun zeroMatePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "zero-mate-test",
            initialBoard = TacticsBoard.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("e2"), squareToCell("e4"))),
            pack = Pack.EASY,
            storedGrade = "NONE",
        )

        fun mate2UniquePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate2-unique",
            initialBoard = TacticsBoard.fromFen("3r2k1/4bppp/8/8/8/3Q4/8/3R2K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("d3"), squareToCell("d8")),
                ChessMove(squareToCell("e7"), squareToCell("d8")),
                ChessMove(squareToCell("d1"), squareToCell("d8")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "MATE_2",
        )
    }
}
