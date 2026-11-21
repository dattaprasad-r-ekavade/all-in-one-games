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
        require(mateIn in 1..4) { "Tactics mate-in horizon must be between 1 and 4" }
        require(this.solutionLine.isNotEmpty()) { "Tactics solution line must not be empty" }
        require(initialBoard.sideToMove == sideToMove) { "Puzzle initial board side must match sideToMove" }
        require(!initialBoard.isCheck(sideToMove.opponent())) {
            "Opponent cannot be in check in the initial position"
        }
        require(isSingleLineField(id)) { "Tactics puzzle ID must be non-blank single-line text" }
        require(isSingleLineField(storedGrade)) { "Tactics grade must be non-blank single-line text" }
    }

    /** Formats the puzzle payload as FEN-like text with mateIn appended. */
    fun toPayload(): String = "${initialBoard.toFen()} $mateIn"

    /** Formats the solution line as space-separated UCI move strings. */
    fun toSolutionString(): String = solutionLine.joinToString(" ") { it.toUci() }

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
