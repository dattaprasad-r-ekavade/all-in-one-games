package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable 8x8 FIDE chess board position.
 *
 * Cell indices run 0..63 in row-major order:
 * Row 0 corresponds to rank 8 (a8 = 0, b8 = 1, ..., h8 = 7).
 * Row 7 corresponds to rank 1 (a1 = 56, b1 = 57, ..., h1 = 63).
 */
class TacticsBoard(
    pieces: List<Piece?>,
    val sideToMove: PieceColor,
    val castling: CastlingRights,
    val enPassant: Cell?,
) {
    val pieces: List<Piece?> = Collections.unmodifiableList(ArrayList(pieces))

    init {
        require(this.pieces.size == 64) { "Tactics board must have exactly 64 squares" }
        require(enPassant == null || enPassant.index in 0 until 64) { "En passant cell outside board" }
        val whiteKings = this.pieces.count { it?.color == PieceColor.WHITE && it.type == PieceType.KING }
        val blackKings = this.pieces.count { it?.color == PieceColor.BLACK && it.type == PieceType.KING }
        require(whiteKings == 1) { "Tactics board must have exactly one white king" }
        require(blackKings == 1) { "Tactics board must have exactly one black king" }
    }

    fun pieceAt(cell: Cell): Piece? = pieces[cell.index]

    fun pieceAt(row: Int, col: Int): Piece? = pieces[row * 8 + col]

    fun kingCell(color: PieceColor): Cell {
        val index = pieces.indexOfFirst { it?.color == color && it.type == PieceType.KING }
        require(index >= 0) { "King not found on board for $color" }
        return Cell(index)
    }

    /** Returns true if [target] is attacked by any piece of [attackerColor]. */
    fun isSquareAttacked(target: Cell, attackerColor: PieceColor): Boolean {
        val targetRow = target.index / 8
        val targetCol = target.index % 8

        // 1. Attacked by enemy pawns
        val pawnRow = if (attackerColor == PieceColor.WHITE) targetRow + 1 else targetRow - 1
        if (pawnRow in 0..7) {
            for (dc in listOf(-1, 1)) {
                val pawnCol = targetCol + dc
                if (pawnCol in 0..7) {
                    val piece = pieceAt(pawnRow, pawnCol)
                    if (piece?.color == attackerColor && piece.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // 2. Attacked by enemy knights
        val knightOffsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1,
        )
        for ((dr, dc) in knightOffsets) {
            val r = targetRow + dr
            val c = targetCol + dc
            if (r in 0..7 && c in 0..7) {
                val piece = pieceAt(r, c)
                if (piece?.color == attackerColor && piece.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 3. Attacked by enemy king
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = targetRow + dr
                val c = targetCol + dc
                if (r in 0..7 && c in 0..7) {
                    val piece = pieceAt(r, c)
                    if (piece?.color == attackerColor && piece.type == PieceType.KING) {
                        return true
                    }
                }
            }
        }

        // 4. Attacked by diagonal sliders (Bishop, Queen)
        val diagonalDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagonalDirs) {
            var r = targetRow + dr
            var c = targetCol + dc
            while (r in 0..7 && c in 0..7) {
                val piece = pieceAt(r, c)
                if (piece != null) {
                    if (piece.color == attackerColor && (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        // 5. Attacked by orthogonal sliders (Rook, Queen)
        val orthogonalDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in orthogonalDirs) {
            var r = targetRow + dr
            var c = targetCol + dc
            while (r in 0..7 && c in 0..7) {
                val piece = pieceAt(r, c)
                if (piece != null) {
                    if (piece.color == attackerColor && (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        return false
    }

    /** Returns true if [color]'s king is currently in check. */
    fun isCheck(color: PieceColor = sideToMove): Boolean =
        isSquareAttacked(kingCell(color), color.opponent())

    /** Generates all strictly legal FIDE moves for [sideToMove]. */
    fun legalMoves(): List<ChessMove> {
        val pseudoMoves = ArrayList<ChessMove>(64)
        val movingColor = sideToMove
        val opponentColor = movingColor.opponent()

        for (index in 0 until 64) {
            val piece = pieces[index] ?: continue
            if (piece.color != movingColor) continue
            val cell = Cell(index)
            val row = index / 8
            val col = index % 8

            when (piece.type) {
                PieceType.PAWN -> {
                    val dr = if (movingColor == PieceColor.WHITE) -1 else 1
                    val startRow = if (movingColor == PieceColor.WHITE) 6 else 1
                    val nextRow = row + dr
                    if (nextRow in 0..7 && pieceAt(nextRow, col) == null) {
                        pseudoMoves.add(ChessMove(cell, Cell(nextRow * 8 + col)))
                        val doubleRow = nextRow + dr
                        if (row == startRow && pieceAt(doubleRow, col) == null) {
                            pseudoMoves.add(ChessMove(cell, Cell(doubleRow * 8 + col)))
                        }
                    }
                    for (dc in listOf(-1, 1)) {
                        val captureCol = col + dc
                        if (nextRow in 0..7 && captureCol in 0..7) {
                            val targetCell = Cell(nextRow * 8 + captureCol)
                            val targetPiece = pieceAt(targetCell)
                            if (targetPiece != null && targetPiece.color == opponentColor) {
                                pseudoMoves.add(ChessMove(cell, targetCell))
                            } else if (enPassant != null && enPassant == targetCell) {
                                pseudoMoves.add(ChessMove(cell, targetCell))
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    val offsets = listOf(
                        -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                        1 to -2, 1 to 2, 2 to -1, 2 to 1,
                    )
                    for ((dr, dc) in offsets) {
                        val r = row + dr
                        val c = col + dc
                        if (r in 0..7 && c in 0..7) {
                            val targetPiece = pieceAt(r, c)
                            if (targetPiece == null || targetPiece.color == opponentColor) {
                                pseudoMoves.add(ChessMove(cell, Cell(r * 8 + c)))
                            }
                        }
                    }
                }

                PieceType.BISHOP -> addRays(cell, row, col, movingColor, pseudoMoves, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
                PieceType.ROOK -> addRays(cell, row, col, movingColor, pseudoMoves, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
                PieceType.QUEEN -> {
                    addRays(cell, row, col, movingColor, pseudoMoves, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
                    addRays(cell, row, col, movingColor, pseudoMoves, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
                }

                PieceType.KING -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val r = row + dr
                            val c = col + dc
                            if (r in 0..7 && c in 0..7) {
                                val targetPiece = pieceAt(r, c)
                                if (targetPiece == null || targetPiece.color == opponentColor) {
                                    pseudoMoves.add(ChessMove(cell, Cell(r * 8 + c)))
                                }
                            }
                        }
                    }

                    // Castling (only if king is not currently in check)
                    if (!isCheck(movingColor)) {
                        if (movingColor == PieceColor.WHITE) {
                            if (castling.whiteKingside &&
                                pieceAt(7, 5) == null && pieceAt(7, 6) == null &&
                                pieceAt(7, 7) == Piece(PieceColor.WHITE, PieceType.ROOK) &&
                                !isSquareAttacked(Cell(7 * 8 + 5), PieceColor.BLACK) &&
                                !isSquareAttacked(Cell(7 * 8 + 6), PieceColor.BLACK)
                            ) {
                                pseudoMoves.add(ChessMove(cell, Cell(7 * 8 + 6)))
                            }
                            if (castling.whiteQueenside &&
                                pieceAt(7, 3) == null && pieceAt(7, 2) == null && pieceAt(7, 1) == null &&
                                pieceAt(7, 0) == Piece(PieceColor.WHITE, PieceType.ROOK) &&
                                !isSquareAttacked(Cell(7 * 8 + 3), PieceColor.BLACK) &&
                                !isSquareAttacked(Cell(7 * 8 + 2), PieceColor.BLACK)
                            ) {
                                pseudoMoves.add(ChessMove(cell, Cell(7 * 8 + 2)))
                            }
                        } else {
                            if (castling.blackKingside &&
                                pieceAt(0, 5) == null && pieceAt(0, 6) == null &&
                                pieceAt(0, 7) == Piece(PieceColor.BLACK, PieceType.ROOK) &&
                                !isSquareAttacked(Cell(0 * 8 + 5), PieceColor.WHITE) &&
                                !isSquareAttacked(Cell(0 * 8 + 6), PieceColor.WHITE)
                            ) {
                                pseudoMoves.add(ChessMove(cell, Cell(0 * 8 + 6)))
                            }
                            if (castling.blackQueenside &&
                                pieceAt(0, 3) == null && pieceAt(0, 2) == null && pieceAt(0, 1) == null &&
                                pieceAt(0, 0) == Piece(PieceColor.BLACK, PieceType.ROOK) &&
                                !isSquareAttacked(Cell(0 * 8 + 3), PieceColor.WHITE) &&
                                !isSquareAttacked(Cell(0 * 8 + 2), PieceColor.WHITE)
                            ) {
                                pseudoMoves.add(ChessMove(cell, Cell(0 * 8 + 2)))
                            }
                        }
                    }
                }
            }
        }

        // Filter out pseudo-legal moves that leave the king in check
        return pseudoMoves.filter { move ->
            val nextBoard = applyMoveInternal(move)
            !nextBoard.isSquareAttacked(nextBoard.kingCell(movingColor), opponentColor)
        }
    }

    private fun addRays(
        cell: Cell,
        row: Int,
        col: Int,
        movingColor: PieceColor,
        dest: MutableList<ChessMove>,
        directions: List<Pair<Int, Int>>,
    ) {
        val opponentColor = movingColor.opponent()
        for ((dr, dc) in directions) {
            var r = row + dr
            var c = col + dc
            while (r in 0..7 && c in 0..7) {
                val targetPiece = pieceAt(r, c)
                if (targetPiece == null) {
                    dest.add(ChessMove(cell, Cell(r * 8 + c)))
                } else {
                    if (targetPiece.color == opponentColor) {
                        dest.add(ChessMove(cell, Cell(r * 8 + c)))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    /** Applies [move] to the board, returning the resulting board position. */
    fun applyMove(move: ChessMove): TacticsBoard {
        require(legalMoves().contains(move)) { "Illegal chess move: ${move.toUci()}" }
        return applyMoveInternal(move)
    }

    /** Low-level move application without legalMoves check (used for simulation and move execution). */
    fun applyMoveInternal(move: ChessMove): TacticsBoard {
        val movingPiece = requireNotNull(pieceAt(move.from)) { "No piece at ${move.from}" }
        val updated = ArrayList<Piece?>(pieces)
        var newEnPassant: Cell? = null
        var newCastling = castling
        var targetPiece = movingPiece

        val fromRow = move.from.index / 8
        val fromCol = move.from.index % 8
        val toRow = move.to.index / 8
        val toCol = move.to.index % 8

        if (movingPiece.type == PieceType.PAWN) {
            // En passant capture
            if (move.to == enPassant && fromCol != toCol) {
                val capturedPawnIndex = fromRow * 8 + toCol
                updated[capturedPawnIndex] = null
            }
            // Double push sets en passant target square
            if (kotlin.math.abs(toRow - fromRow) == 2) {
                newEnPassant = Cell(((fromRow + toRow) / 2) * 8 + fromCol)
            }
            // Queen-only promotion
            if ((movingPiece.color == PieceColor.WHITE && toRow == 0) ||
                (movingPiece.color == PieceColor.BLACK && toRow == 7)
            ) {
                targetPiece = Piece(movingPiece.color, PieceType.QUEEN)
            }
        }

        if (movingPiece.type == PieceType.KING) {
            // Clear castling rights when king moves
            newCastling = if (movingPiece.color == PieceColor.WHITE) {
                newCastling.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                newCastling.copy(blackKingside = false, blackQueenside = false)
            }
            // Move rook when castling
            if (movingPiece.color == PieceColor.WHITE) {
                if (move.from.index == 60 && move.to.index == 62) { // e1 -> g1
                    updated[63] = null
                    updated[61] = Piece(PieceColor.WHITE, PieceType.ROOK)
                } else if (move.from.index == 60 && move.to.index == 58) { // e1 -> c1
                    updated[56] = null
                    updated[59] = Piece(PieceColor.WHITE, PieceType.ROOK)
                }
            } else {
                if (move.from.index == 4 && move.to.index == 6) { // e8 -> g8
                    updated[7] = null
                    updated[5] = Piece(PieceColor.BLACK, PieceType.ROOK)
                } else if (move.from.index == 4 && move.to.index == 2) { // e8 -> c8
                    updated[0] = null
                    updated[3] = Piece(PieceColor.BLACK, PieceType.ROOK)
                }
            }
        }

        if (movingPiece.type == PieceType.ROOK) {
            newCastling = when (move.from.index) {
                56 -> newCastling.copy(whiteQueenside = false)
                63 -> newCastling.copy(whiteKingside = false)
                0 -> newCastling.copy(blackQueenside = false)
                7 -> newCastling.copy(blackKingside = false)
                else -> newCastling
            }
        }

        // Rook capture updates opponent castling rights
        newCastling = when (move.to.index) {
            56 -> newCastling.copy(whiteQueenside = false)
            63 -> newCastling.copy(whiteKingside = false)
            0 -> newCastling.copy(blackQueenside = false)
            7 -> newCastling.copy(blackKingside = false)
            else -> newCastling
        }

        updated[move.from.index] = null
        updated[move.to.index] = targetPiece

        return TacticsBoard(
            pieces = updated,
            sideToMove = sideToMove.opponent(),
            castling = newCastling,
            enPassant = newEnPassant,
        )
    }

    /** True if the side to move is in check and has no legal moves. */
    fun isCheckmate(): Boolean = isCheck(sideToMove) && legalMoves().isEmpty()

    /** True if the side to move is not in check but has no legal moves. */
    fun isStalemate(): Boolean = !isCheck(sideToMove) && legalMoves().isEmpty()

    /** Formats board as standard FEN string (pieces, side, castling, en passant). */
    fun toFen(): String {
        val builder = StringBuilder()
        for (row in 0..7) {
            var emptyCount = 0
            for (col in 0..7) {
                val piece = pieceAt(row, col)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        builder.append(emptyCount)
                        emptyCount = 0
                    }
                    builder.append(piece.toChar())
                }
            }
            if (emptyCount > 0) builder.append(emptyCount)
            if (row < 7) builder.append('/')
        }
        val side = if (sideToMove == PieceColor.WHITE) "w" else "b"
        val ep = enPassant?.let(::cellToSquare) ?: "-"
        return "$builder $side ${castling.toFen()} $ep"
    }

    override fun equals(other: Any?): Boolean = other is TacticsBoard &&
        pieces == other.pieces &&
        sideToMove == other.sideToMove &&
        castling == other.castling &&
        enPassant == other.enPassant

    override fun hashCode(): Int = listOf(pieces, sideToMove, castling, enPassant).hashCode()

    companion object {
        /** Parses a standard or truncated FEN string into a [TacticsBoard]. */
        fun fromFen(fen: String): TacticsBoard {
            val parts = fen.trim().split(Regex("\\s+"))
            require(parts.size >= 4) { "FEN must contain at least 4 tokens: pieces, side, castling, enPassant" }

            val rankStrings = parts[0].split('/')
            require(rankStrings.size == 8) { "FEN board must have 8 ranks separated by /" }

            val pieces = ArrayList<Piece?>(64)
            for (rankStr in rankStrings) {
                var col = 0
                for (char in rankStr) {
                    if (char.isDigit()) {
                        val emptyCount = char.digitToInt()
                        repeat(emptyCount) { pieces.add(null) }
                        col += emptyCount
                    } else {
                        val piece = requireNotNull(Piece.fromChar(char)) { "Invalid piece char in FEN: $char" }
                        pieces.add(piece)
                        col++
                    }
                }
                require(col == 8) { "Rank in FEN does not total 8 squares: $rankStr" }
            }

            val sideToMove = when (parts[1]) {
                "w" -> PieceColor.WHITE
                "b" -> PieceColor.BLACK
                else -> throw IllegalArgumentException("Invalid side to move in FEN: ${parts[1]}")
            }

            val castling = CastlingRights.fromFen(parts[2])
            val enPassant = if (parts[3] == "-") null else squareToCell(parts[3])

            return TacticsBoard(pieces, sideToMove, castling, enPassant)
        }
    }
}
