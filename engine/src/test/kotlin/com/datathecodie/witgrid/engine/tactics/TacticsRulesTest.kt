package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun countSolutionsCountsDistinctWinningFirstMoves() {
        val puzzle = mate2FirstMoveUniqueSecondMoveDoublePuzzle()
        val opener = puzzle.solutionLine[0]
        val reply = puzzle.solutionLine[1]
        assertEquals(listOf(opener), TacticsRules.winningMoves(puzzle.initialBoard, 2))
        val afterOpener = puzzle.initialBoard.applyMove(opener)
        assertEquals(listOf(reply), afterOpener.legalMoves())
        assertEquals(3, TacticsRules.winningMoves(afterOpener.applyMove(reply), 1).size)
        // Distinct winning first moves is 1, even though later positions have multiple mates.
        assertEquals(1, TacticsRules.countSolutions(puzzle))
    }

    @Test
    fun countSolutionsReturnsTwoWhenMultipleWinningFirstMovesExist() {
        val puzzle = mate2DoubleOpenerPuzzle()
        assertEquals(2, TacticsRules.countSolutions(puzzle))
        assertEquals(1, TacticsRules.countSolutions(puzzle, limit = 1))
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
        // Black auto-replied with forced Nc6xd8
        assertEquals(ChessMove(squareToCell("c6"), squareToCell("d8")), s2.lastOpponentMove)
        assertEquals(Piece(PieceColor.BLACK, PieceType.KNIGHT), s2.currentBoard.pieceAt(squareToCell("d8")))
        assertFalse(TacticsEngine.isSolved(puzzle, s2))
        assertEquals(emptyList<Cell>(), TacticsEngine.mistakes(puzzle, s2))

        // Second move: White plays Re1-e8#
        val s3 = TacticsEngine.applyMove(s2, Move.SetDigit(squareToCell("e1"), 1))
        val s4 = TacticsEngine.applyMove(s3, Move.SetDigit(squareToCell("e8"), 1))
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

        // Switch to Rook at e1 (another friendly piece, not a legal capture)
        val s2 = TacticsEngine.applyMove(s1, Move.SetMark(squareToCell("e1"), Mark.FILLED))
        assertEquals(squareToCell("e1"), s2.pendingFrom)
    }

    @Test
    fun legalMistakeDoesNotHandControlToDefendingSide() {
        val puzzle = mate2UniquePuzzle()
        val s0 = TacticsEngine.initialState(puzzle)
        val mistake = ChessMove.fromUci("c4a2")

        val s1 = TacticsEngine.applyMove(s0, Move.SetMark(mistake.from, Mark.FILLED))
        val s2 = TacticsEngine.applyMove(s1, Move.SetMark(mistake.to, Mark.FILLED))
        assertEquals(puzzle.initialBoard.applyMove(mistake), s2.currentBoard)
        assertNull(s2.lastOpponentMove)
        assertEquals(listOf(mistake.to), TacticsEngine.mistakes(puzzle, s2))

        // Next tap: attempting to tap a black piece (e.g. King on g8 or Rook on d8) must be ignored
        val s3 = TacticsEngine.applyMove(s2, Move.SetMark(squareToCell("d8"), Mark.FILLED))
        assertNull(s3.pendingFrom)
        assertEquals(s2, s3)

        // Attempting to complete a move for Black must not be permitted
        val s4 = TacticsEngine.applyMove(s3, Move.SetMark(squareToCell("d7"), Mark.FILLED))
        assertEquals(s2, s4)
        assertFalse(s4.completedPlayerMoves.any { it.from == squareToCell("d8") })
    }

    @Test
    fun counterRejectsCooperativeNonWinningStoredLine() {
        val start = TacticsBoard.fromFen("3r2k1/4bppp/8/2B5/8/3Q4/8/3R2K1 w - -")
        val puzzle = TacticsPuzzle(
            "cooperative-mate", start, PieceColor.WHITE, 2,
            listOf("c5e7", "g8h8", "d3d8").map(ChessMove.Companion::fromUci),
            Pack.MEDIUM, "checkmate:2",
        )
        assertTrue(puzzle.solutionLine.fold(start, TacticsBoard::applyMove).isCheckmate())
        assertFalse(TacticsRules.isValidSolution(puzzle))
        assertEquals(0, TacticsRules.countSolutions(puzzle))
        assertThrows(IllegalArgumentException::class.java) { TacticsEngine.grade(puzzle) }
        assertThrows(IllegalArgumentException::class.java) { TacticsCodec.encode(puzzle) }
    }

    @Test
    fun counterRejectsTiedDefencesToAWinningMove() {
        val start = TacticsBoard.fromFen("3r2k1/4bppp/8/2B5/8/3Q4/8/3R2K1 w - -")
        val puzzle = TacticsPuzzle(
            "tied-defences", start, PieceColor.WHITE, 2,
            listOf("d3d8", "e7d8", "d1d8").map(ChessMove.Companion::fromUci),
            Pack.MEDIUM, "back-rank-mate:2",
        )
        assertEquals(listOf(puzzle.solutionLine.first()), TacticsRules.winningMoves(start, 2))
        assertEquals(2, start.applyMove(puzzle.solutionLine.first()).legalMoves().size)
        assertTrue(puzzle.solutionLine.fold(start, TacticsBoard::applyMove).isCheckmate())
        assertEquals(0, TacticsRules.countSolutions(puzzle))
    }

    @Test
    fun counterAcceptsUniqueBestDefenceWhenOtherRepliesAllowFasterMate() {
        val puzzle = TacticsTechniqueSolverTest.expertMateInThreeFixture()
        val afterOpener = puzzle.initialBoard.applyMove(puzzle.solutionLine[0])
        val bestReply = puzzle.solutionLine[1]
        val replies = afterOpener.legalMoves()
        assertTrue(replies.size > 1)
        val repliesThatAvoidMateInOne = replies.filter { reply ->
            TacticsRules.winningMoves(afterOpener.applyMove(reply), 1).isEmpty()
        }
        assertEquals(listOf(bestReply), repliesThatAvoidMateInOne)
        assertEquals(1, TacticsRules.countSolutions(puzzle))
    }

    @Test
    fun counterRejectsAFullLengthLineWhenMateIsAvailableSooner() {
        val start = TacticsBoard.fromFen("6k1/1p3ppp/8/8/8/8/8/R5K1 w - -")
        val puzzle = TacticsPuzzle(
            "slower-cooperative-mate", start, PieceColor.WHITE, 3,
            listOf("a1a2", "b7b6", "a2a3", "b6b5", "a3a8").map(ChessMove.Companion::fromUci),
            Pack.EXPERT, "back-rank-mate:3",
        )
        assertTrue(puzzle.solutionLine.fold(start, TacticsBoard::applyMove).isCheckmate())
        assertTrue(start.applyMove(ChessMove.fromUci("a1a8")).isCheckmate())
        assertEquals(0, TacticsRules.countSolutions(puzzle))
        assertThrows(IllegalArgumentException::class.java) { TacticsEngine.grade(puzzle) }
    }

    @Test
    fun bestReplyRejectsReplyWhenAttackerCannotForceMateAtDeclaredDepth() {
        // Black has only one legal move (a8-b8), but White has no mating force.
        // bestReply must verify that the defender reply still allows mate (canForceMate == true),
        // returning null instead of selecting a reply where mate cannot be forced.
        val board = TacticsBoard.fromFen("k7/8/K7/8/8/8/8/8 b - -")
        assertEquals(listOf(ChessMove.fromUci("a8b8")), board.legalMoves())
        assertNull(TacticsRules.bestReply(board, remaining = 1))
    }

    @Test
    fun bestReplyAcceptsForcedReplyWhenMateIsPossible() {
        val puzzle = mate2UniquePuzzle()
        val afterOpener = puzzle.initialBoard.applyMove(puzzle.solutionLine[0])
        val forcedReply = puzzle.solutionLine[1]
        assertEquals(listOf(forcedReply), afterOpener.legalMoves())
        assertEquals(forcedReply, TacticsRules.bestReply(afterOpener, remaining = 1))
    }

    @Test
    fun malformedFenMetadataIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("7k/8/8/4P3/8/8/8/K7 w - d6")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("7k/8/8/3pP3/8/8/8/K7 w - d3")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("7k/3r4/8/3pP3/8/8/8/K7 w - d6")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("7k/8/3N4/3pP3/8/8/8/K7 w - d6")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("r3k2r/8/8/8/8/8/4K3/R6R w K - 0 1")
        }

        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("7k/8/8/8/8/8/8/4K3 w K -")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("7k/8/8/8/8/8/8/4K2R w KK -")
        }
        // A FEN target is valid after a double push even without a pawn that can take it.
        val noCapturer = "7k/8/8/3p4/8/8/8/K7 w - d6"
        assertEquals(noCapturer, TacticsBoard.fromFen(noCapturer).toFen())

        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("P6k/8/8/8/8/8/8/7K w - - 0 1")
        }

        // En passant claimed double push is illegal when king was already in check prior to the push
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("r6k/8/8/3pP3/8/8/8/K7 w - d6")
        }
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("k7/8/8/8/3Pp3/8/8/R6K b - d3")
        }
        // Inactive player's king cannot be in check
        assertThrows(IllegalArgumentException::class.java) {
            TacticsBoard.fromFen("R6k/8/8/8/8/8/8/4K3 w - -")
        }
        val legalCheckPush = "8/8/8/3pP3/2k5/8/8/K7 w - d6"
        assertEquals(legalCheckPush, TacticsBoard.fromFen(legalCheckPush).toFen())
    }

    @Test
    fun uciMoveValidation() {
        // Valid 4-character move
        assertEquals(ChessMove(squareToCell("e2"), squareToCell("e4")), ChessMove.fromUci("e2e4"))

        // Valid queen promotion move to rank 8
        assertEquals(ChessMove(squareToCell("e7"), squareToCell("e8")), ChessMove.fromUci("e7e8q"))
        assertEquals(ChessMove(squareToCell("e7"), squareToCell("e8")), ChessMove.fromUci("e7e8Q"))

        // Valid queen promotion move to rank 1
        assertEquals(ChessMove(squareToCell("e2"), squareToCell("e1")), ChessMove.fromUci("e2e1q"))

        // Underpromotions are rejected
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e7e8r") }
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e7e8b") }
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e7e8n") }

        // Garbage fifth character is rejected
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e7e8x") }
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e7e81") }

        // Promotion suffix on non-promotion square is rejected
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e2e4q") }
        assertThrows(IllegalArgumentException::class.java) { ChessMove.fromUci("e7d6q") }
    }

    @Test
    fun enPassantAndPromotionRules() {
        // En passant position: White pawn at e5, Black plays d7-d5 -> White captures e5xd6 e.p.
        val fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w - d6 0 2"
        val board = TacticsBoard.fromFen(fen)
        assertEquals(squareToCell("d6"), board.enPassant)

        val epMove = ChessMove(squareToCell("e5"), squareToCell("d6"))
        assertTrue(board.legalMoves().contains(epMove))

        val afterEp = board.applyMove(epMove)
        assertNull(afterEp.pieceAt(squareToCell("d5")))
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
            initialBoard = TacticsBoard.fromFen("7k/3Q4/5K2/8/8/8/8/8 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("d7"), squareToCell("g7"))),
            pack = Pack.EASY,
            storedGrade = "checkmate:1",
        )

        fun mate1DoublePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate1-double",
            initialBoard = TacticsBoard.fromFen("7k/4Q3/6K1/8/8/8/8/8 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("e7"), squareToCell("g7"))),
            pack = Pack.EASY,
            storedGrade = "checkmate:1",
        )

        fun zeroMatePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "zero-mate-test",
            initialBoard = TacticsBoard.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 1,
            solutionLine = listOf(ChessMove(squareToCell("e2"), squareToCell("e4"))),
            pack = Pack.EASY,
            storedGrade = "none:1",
        )

        fun mate2UniquePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate2-unique",
            initialBoard = TacticsBoard.fromFen("3r2k1/5ppp/2n5/8/2B5/3Q4/8/4R1K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("d3"), squareToCell("d8")),
                ChessMove(squareToCell("c6"), squareToCell("d8")),
                ChessMove(squareToCell("e1"), squareToCell("e8")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "back-rank-mate:2",
        )

        fun mate2FirstMoveUniqueSecondMoveDoublePuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate2-first-unique-second-double",
            initialBoard = TacticsBoard.fromFen("7k/5K2/7p/8/8/Q7/2r5/4n1R1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("a3"), squareToCell("f8")),
                ChessMove(squareToCell("h8"), squareToCell("h7")),
                ChessMove(squareToCell("f8"), squareToCell("g7")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "checkmate:2",
        )

        fun mate2DoubleOpenerPuzzle(): TacticsPuzzle = TacticsPuzzle(
            id = "mate2-double-opener",
            initialBoard = TacticsBoard.fromFen("3r2k1/5ppp/2n5/Q7/8/8/8/3Q2K1 w - - 0 1"),
            sideToMove = PieceColor.WHITE,
            mateIn = 2,
            solutionLine = listOf(
                ChessMove(squareToCell("d1"), squareToCell("d8")),
                ChessMove(squareToCell("c6"), squareToCell("d8")),
                ChessMove(squareToCell("a5"), squareToCell("d8")),
            ),
            pack = Pack.MEDIUM,
            storedGrade = "back-rank-mate:2",
        )
    }
}
