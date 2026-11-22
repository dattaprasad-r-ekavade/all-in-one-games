package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell

/** Player colors in FIDE chess. */
enum class PieceColor {
    WHITE,
    BLACK;

    fun opponent(): PieceColor = if (this == WHITE) BLACK else WHITE
}

/** Standard piece types in FIDE chess. */
enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING,
}

/** An immutable chess piece with color and type. */
data class Piece(val color: PieceColor, val type: PieceType) {
    /** Returns standard single-character algebraic representation (uppercase for White, lowercase for Black). */
    fun toChar(): Char {
        val letter = when (type) {
            PieceType.PAWN -> 'P'
            PieceType.KNIGHT -> 'N'
            PieceType.BISHOP -> 'B'
            PieceType.ROOK -> 'R'
            PieceType.QUEEN -> 'Q'
            PieceType.KING -> 'K'
        }
        return if (color == PieceColor.WHITE) letter else letter.lowercaseChar()
    }

    companion object {
        fun fromChar(char: Char): Piece? {
            val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (char.uppercaseChar()) {
                'P' -> PieceType.PAWN
                'N' -> PieceType.KNIGHT
                'B' -> PieceType.BISHOP
                'R' -> PieceType.ROOK
                'Q' -> PieceType.QUEEN
                'K' -> PieceType.KING
                else -> return null
            }
            return Piece(color, type)
        }
    }
}

/** An immutable chess move between two squares on an 8x8 board. */
data class ChessMove(val from: Cell, val to: Cell) {
    init {
        require(from.index in 0 until 64) { "Move from-cell outside 8x8 board" }
        require(to.index in 0 until 64) { "Move to-cell outside 8x8 board" }
        require(from != to) { "Move from-cell and to-cell must be distinct" }
    }

    /** Formats move in UCI coordinate notation, e.g. "e2e4". */
    fun toUci(): String = "${cellToSquare(from)}${cellToSquare(to)}"

    companion object {
        /** Parses a 4- or 5-character UCI move. Queen promotion is the only supported promotion. */
        fun fromUci(uci: String): ChessMove {
            require(uci.length in 4..5) { "Invalid UCI move: $uci" }
            val from = squareToCell(uci.substring(0, 2))
            val to = squareToCell(uci.substring(2, 4))
            if (uci.length == 5) {
                val promotionChar = uci[4]
                require(promotionChar == 'q' || promotionChar == 'Q') {
                    "Only queen promotion is supported in UCI move: $uci"
                }
                val toRow = to.index / 8
                require(toRow == 0 || toRow == 7) {
                    "Promotion suffix on non-promotion square in UCI move: $uci"
                }
            }
            return ChessMove(from, to)
        }
    }
}

/** Converts an algebraic square (e.g. "e4") to a 0-based row-major cell index (0..63). */
fun squareToCell(square: String): Cell {
    require(square.length == 2) { "Invalid square notation: $square" }
    val file = square[0] - 'a'
    val rank = square[1] - '1'
    require(file in 0..7 && rank in 0..7) { "Coordinates outside 8x8 board: $square" }
    val row = 7 - rank
    return Cell(row * 8 + file)
}

/** Converts a 0-based cell (0..63) to algebraic square notation (e.g. "e4"). */
fun cellToSquare(cell: Cell): String {
    require(cell.index in 0 until 64) { "Cell outside 8x8 board: ${cell.index}" }
    val row = cell.index / 8
    val file = cell.index % 8
    val rank = 8 - row
    return "${('a' + file)}$rank"
}

/** FIDE castling availability for both players. */
data class CastlingRights(
    val whiteKingside: Boolean = false,
    val whiteQueenside: Boolean = false,
    val blackKingside: Boolean = false,
    val blackQueenside: Boolean = false,
) {
    fun toFen(): String {
        val builder = StringBuilder()
        if (whiteKingside) builder.append('K')
        if (whiteQueenside) builder.append('Q')
        if (blackKingside) builder.append('k')
        if (blackQueenside) builder.append('q')
        return if (builder.isEmpty()) "-" else builder.toString()
    }

    companion object {
        fun fromFen(text: String): CastlingRights {
            if (text == "-") return CastlingRights()
            require(text.isNotEmpty() && text.all { it in "KQkq" } && text.toSet().size == text.length) {
                "Invalid FEN castling field: $text"
            }
            return CastlingRights(
                whiteKingside = 'K' in text,
                whiteQueenside = 'Q' in text,
                blackKingside = 'k' in text,
                blackQueenside = 'q' in text,
            )
        }
    }
}
