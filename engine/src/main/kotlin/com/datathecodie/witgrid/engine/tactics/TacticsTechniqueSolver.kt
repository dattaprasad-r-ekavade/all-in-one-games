package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique
import kotlin.math.abs

/** Finds forcing checks and captures and explains the resulting mating patterns. */
object TacticsTechniqueSolver {

    /** Deduces a hint from the current position, or returns null if the techniques get stuck. */
    fun nextStep(puzzle: TacticsPuzzle, state: TacticsState): HintStep? {
        require(state.puzzle == puzzle) { "Tactics state belongs to a different puzzle" }
        if (TacticsRules.mistakes(puzzle, state).isNotEmpty()) return null
        val remaining = puzzle.mateIn - state.completedPlayerMoves.size
        if (remaining <= 0 || state.currentBoard.sideToMove != puzzle.sideToMove) return null
        val deduction = deduce(state.currentBoard, remaining, requireUnique = true) ?: return null
        val move = deduction.move
        val from = Move.SetMark(move.from, Mark.FILLED)
        val to = Move.SetMark(move.to, Mark.FILLED)
        val pending = state.pendingFrom
        return HintStep(
            technique = deduction.technique,
            cells = listOf(move.from, move.to),
            explanation = deduction.explanation,
            moves = when (pending) {
                null -> listOf(from, to)
                move.from -> listOf(to)
                else -> listOf(Move.SetMark(pending, Mark.FILLED), from, to)
            },
        )
    }

    /** Requires an exact unique mate followed by a complete solve with the supported techniques. */
    fun grade(puzzle: TacticsPuzzle): Grade {
        require(TacticsRules.countSolutions(puzzle) == 1) {
            "Tactics grading requires exactly one solution"
        }
        val pieceCount = puzzle.initialBoard.pieces.count { it != null }
        val expectedPack = TacticsPackBands.packFor(puzzle.mateIn, pieceCount)
        require(puzzle.pack == expectedPack) {
            "Tactics puzzle pack ${puzzle.pack} does not match expected pack $expectedPack for mate-in-${puzzle.mateIn} with $pieceCount pieces"
        }
        return requireNotNull(gradeLine(puzzle.initialBoard, puzzle.solutionLine, expectedPack)) {
            "Tactics puzzle is stuck with the supported techniques"
        }
    }

    internal fun gradeLine(start: TacticsBoard, line: List<ChessMove>, pack: Pack): Grade? {
        var board = start
        var hardest: Technique? = null
        val turns = (line.size + 1) / 2
        for (turn in 0 until turns) {
            val deduction = deduce(board, turns - turn, requireUnique = true) ?: return null
            if (deduction.move != line[turn * 2]) return null
            if (hardest == null || deduction.technique.difficultyRank > hardest.difficultyRank) {
                hardest = deduction.technique
            }
            board = board.applyMoveInternal(deduction.move)
            if (turn + 1 < turns) board = board.applyMoveInternal(line[turn * 2 + 1])
        }
        return Grade(hardest, turns, pack)
    }

    private data class Deduction(val move: ChessMove, val technique: Technique, val explanation: String)

    private fun deduce(board: TacticsBoard, remaining: Int, requireUnique: Boolean): Deduction? {
        var first: Deduction? = null
        for (move in board.legalMoves()) {
            val deduction = prove(board, move, remaining) ?: continue
            if (!requireUnique) return deduction
            if (first != null) return null
            first = deduction
        }
        return first
    }

    private fun prove(board: TacticsBoard, move: ChessMove, remaining: Int): Deduction? {
        val movedPiece = requireNotNull(board.pieceAt(move.from))
        val capture = board.pieceAt(move.to) != null ||
            (movedPiece.type == PieceType.PAWN && move.to == board.enPassant)
        val after = board.applyMoveInternal(move)
        val check = after.isCheck()
        if (!check && (!capture || remaining == 1)) return null
        if (check && after.isCheckmate()) return describeMate(after, move)
        if (remaining == 1) return null

        val replies = after.legalMoves()
        if (replies.isEmpty()) return null
        var continuation: Deduction? = null
        for (reply in replies) {
            val next = deduce(after.applyMoveInternal(reply), remaining - 1, requireUnique = false) ?: return null
            if (continuation == null) continuation = next
        }

        val square = cellToSquare(move.to)
        val technique = if (capture) TacticsTechniques.CAPTURE else TacticsTechniques.CHECK
        val action = if (capture) "The capture on $square" else "The checking move to $square"
        val reply = replies.first()
        val replyMove = "${cellToSquare(reply.from)} to ${cellToSquare(reply.to)}"
        val next = requireNotNull(continuation)
        val nextSquare = cellToSquare(next.move.to)
        val followUp = when (next.technique) {
            TacticsTechniques.CHECK -> "a check on $nextSquare"
            TacticsTechniques.CAPTURE -> "a capture on $nextSquare"
            else -> "${next.technique.displayName.replaceFirstChar { it.lowercaseChar() }} on $nextSquare"
        }
        val explanation = if (replies.size == 1) {
            "$action forces $replyMove and sets up $followUp."
        } else {
            "$action leaves a mating continuation against every reply, including $followUp after $replyMove."
        }
        return Deduction(move, technique, explanation)
    }

    private fun describeMate(board: TacticsBoard, move: ChessMove): Deduction {
        val piece = requireNotNull(board.pieceAt(move.to))
        val king = board.kingCell(board.sideToMove)
        val kingRow = king.index / 8
        val kingCol = king.index % 8
        val row = move.to.index / 8
        val col = move.to.index % 8
        val rankDistance = abs(row - kingRow)
        val fileDistance = abs(col - kingCol)
        val square = cellToSquare(move.to)
        val kingSquare = cellToSquare(king)
        val slider = piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN
        val backRank = if (board.sideToMove == PieceColor.BLACK) 0 else 7
        val rankStep = if (kingRow == 0) 1 else -1
        val fileStep = if (kingCol == 0) 1 else -1
        val knight = Piece(piece.color, PieceType.KNIGHT)

        return when {
            piece.type == PieceType.KNIGHT && rankDistance * fileDistance == 2 &&
                neighboursOccupied(board, kingRow, kingCol) ->
                Deduction(move, TacticsTechniques.SMOTHERED_MATE,
                    "The knight on $square delivers smothered mate because the king's own pieces block every escape square.")
            slider && row == kingRow && kingRow == backRank &&
                board.pieceAt(kingRow + rankStep, kingCol)?.color == board.sideToMove &&
                clearLine(board, row, col, kingRow, kingCol) ->
                Deduction(move, TacticsTechniques.BACK_RANK_MATE,
                    "The move to $square delivers back-rank mate because the king's own pieces block escape from the rank.")
            piece.type == PieceType.ROOK && kingCol in listOf(0, 7) && col == kingCol && kingRow in 1..6 &&
                board.pieceAt(kingRow, kingCol + fileStep) == Piece(board.sideToMove, PieceType.PAWN) &&
                board.pieceAt(kingRow, kingCol + 3 * fileStep) == knight &&
                clearLine(board, row, col, kingRow, kingCol) ->
                Deduction(move, TacticsTechniques.ANASTASIA_MATE,
                    "The rook on $square delivers Anastasia's mate because the knight and pawn seal the neighbouring file.")
            piece.type == PieceType.ROOK && kingRow in listOf(0, 7) && kingCol in listOf(0, 7) &&
                rankDistance + fileDistance == 1 &&
                board.pieceAt(kingRow + 2 * rankStep, kingCol + 2 * fileStep) == knight ->
                Deduction(move, TacticsTechniques.ARABIAN_MATE,
                    "The rook on $square delivers Arabian mate, protected by the knight that also cuts off the corner escape.")
            else -> Deduction(move, TacticsTechniques.CHECKMATE,
                "The move to $square delivers checkmate because the king on $kingSquare has no legal defence.")
        }
    }

    private fun neighboursOccupied(board: TacticsBoard, kingRow: Int, kingCol: Int): Boolean {
        for (row in maxOf(0, kingRow - 1)..minOf(7, kingRow + 1)) {
            for (col in maxOf(0, kingCol - 1)..minOf(7, kingCol + 1)) {
                if (board.pieceAt(row, col)?.color != board.sideToMove) return false
            }
        }
        return true
    }

    private fun clearLine(board: TacticsBoard, row: Int, col: Int, kingRow: Int, kingCol: Int): Boolean =
        if (row == kingRow) {
            (minOf(col, kingCol) + 1 until maxOf(col, kingCol)).all { board.pieceAt(row, it) == null }
        } else {
            (minOf(row, kingRow) + 1 until maxOf(row, kingRow)).all { board.pieceAt(it, col) == null }
        }
}
