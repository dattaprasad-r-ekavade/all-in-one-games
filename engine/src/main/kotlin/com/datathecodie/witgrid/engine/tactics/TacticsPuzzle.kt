package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Grid
import com.datathecodie.witgrid.engine.core.Pack
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable Chess Tactics puzzle data.
 *
 * Defines the initial [initialBoard], the side to play [sideToMove], the mating horizon [mateIn],
 * and the canonical forced [solutionLine] leading to checkmate.
 */
class TacticsPuzzle(
    val id: String,
    val initialBoard: TacticsBoard,
    val sideToMove: PieceColor,
    val mateIn: Int,
    solutionLine: List<ChessMove>,
    val pack: Pack,
    val storedGrade: String,
) {
    /** Fixed 8x8 chessboard size. */
    val size: Int = 8

    /** Board geometry. */
    val grid: Grid = Grid(size)

    /** Defensively copied sequence of solution moves (UCI coordinate pairs). */
    val solutionLine: List<ChessMove> = immutableList(solutionLine)

    init {
        require(mateIn in 1..3) { "Tactics mate-in horizon must be between 1 and 3" }
        require(this.solutionLine.size == 2 * mateIn - 1) {
            "Tactics solution line size (${this.solutionLine.size}) must match mate-in-$mateIn horizon (expected ${2 * mateIn - 1} plies)"
        }
        require(initialBoard.sideToMove == sideToMove) { "Puzzle initial board side must match sideToMove" }
        require(!initialBoard.isCheck(sideToMove.opponent())) {
            "Opponent cannot be in check in the initial position"
        }
        require(isSingleLineField(id)) { "Tactics puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Tactics grade must be non-blank single-line text" }
    }

    /** Formats the puzzle payload as FEN-like text with mateIn appended. */
    fun toPayload(): String = "${initialBoard.toFen()} $mateIn"

    /** Formats the solution line as space-separated UCI move strings, appending 'q' for pawn promotions. */
    fun toSolutionString(): String {
        var board = initialBoard
        return solutionLine.joinToString(" ") { move ->
            val piece = board.pieceAt(move.from)
            val toRow = move.to.index / 8
            val isPromo = piece?.type == PieceType.PAWN &&
                ((piece.color == PieceColor.WHITE && toRow == 0) ||
                 (piece.color == PieceColor.BLACK && toRow == 7))
            board = board.applyMoveInternal(move)
            if (isPromo) "${move.toUci()}q" else move.toUci()
        }
    }

    override fun equals(other: Any?): Boolean = other is TacticsPuzzle &&
        id == other.id &&
        initialBoard == other.initialBoard &&
        sideToMove == other.sideToMove &&
        mateIn == other.mateIn &&
        solutionLine == other.solutionLine &&
        pack == other.pack &&
        storedGrade == other.storedGrade

    override fun hashCode(): Int = listOf(id, initialBoard, sideToMove, mateIn, solutionLine, pack, storedGrade).hashCode()

    private fun isSingleLineField(value: String): Boolean =
        value.isNotBlank() && value.none { it == '|' || it == '\n' || it == '\r' }
}

internal fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
