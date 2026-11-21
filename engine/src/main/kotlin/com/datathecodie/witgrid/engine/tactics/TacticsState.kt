package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import java.util.ArrayList
import java.util.Collections

/**
 * Immutable player state in Chess Tactics.
 *
 * Tracks the player's interaction as a sequence of [picks] (cell taps).
 * An odd number of picks indicates an in-progress move where [pendingFrom] is selected.
 * Even pairs of picks represent completed player moves.
 */
class TacticsState(
    val puzzle: TacticsPuzzle,
    picks: List<Cell>,
) {
    /** Defensively copied sequence of raw cell picks. */
    val picks: List<Cell> = immutableList(picks)

    /** Currently selected source square, or null if awaiting a piece selection. */
    val pendingFrom: Cell? get() = if (picks.size % 2 == 1) picks.last() else null

    /** All completed player moves made so far. */
    val completedPlayerMoves: List<ChessMove>

    /** Current position on the board reflecting played moves and opponent responses. */
    val currentBoard: TacticsBoard

    /** The last opponent response move played, for display in the UI. */
    val lastOpponentMove: ChessMove?

    /** The most recent move applied on the board (player or opponent). */
    val lastMove: ChessMove?

    init {
        require(this.picks.all { it.index in 0 until 64 }) { "Cell picks outside 8x8 Tactics board" }

        var board = puzzle.initialBoard
        var lastOpponent: ChessMove? = null
        var lastAny: ChessMove? = null
        val playerMoves = ArrayList<ChessMove>(this.picks.size / 2)
        var mistakeOccurred = false

        for (i in 0 until (this.picks.size / 2)) {
            val from = this.picks[2 * i]
            val to = this.picks[2 * i + 1]
            val playerMove = ChessMove(from, to)
            playerMoves.add(playerMove)

            val stepIndex = i * 2
            val expectedMove = puzzle.solutionLine.getOrNull(stepIndex)

            if (!mistakeOccurred && playerMove == expectedMove) {
                board = board.applyMoveInternal(playerMove)
                lastAny = playerMove

                val opponentStep = stepIndex + 1
                if (opponentStep < puzzle.solutionLine.size) {
                    val opponentMove = puzzle.solutionLine[opponentStep]
                    board = board.applyMoveInternal(opponentMove)
                    lastOpponent = opponentMove
                    lastAny = opponentMove
                }
            } else {
                mistakeOccurred = true
                if (board.legalMoves().contains(playerMove)) {
                    board = board.applyMoveInternal(playerMove)
                }
                lastAny = playerMove
            }
        }

        this.completedPlayerMoves = Collections.unmodifiableList(playerMoves)
        this.currentBoard = board
        this.lastOpponentMove = lastOpponent
        this.lastMove = lastAny
    }

    /**
     * Applies one cell pick using existing Move types.
     *
     * Supports [Move.SetMark] and [Move.SetDigit].
     * If [pendingFrom] is null, sets the from-square.
     * If tapped again on [pendingFrom], deselects it.
     * If tapped on another piece of the player's color, switches selection to that piece.
     * Otherwise completes the move.
     */
    fun withMove(move: Move): TacticsState {
        require(move.cell.index in 0 until 64) { "Move cell outside 8x8 Tactics board" }
        require(move is Move.SetMark || move is Move.SetDigit) {
            "Tactics supports cell picks via SetMark or SetDigit only"
        }

        val cell = move.cell
        val currentFrom = pendingFrom

        return when {
            currentFrom == null -> {
                TacticsState(puzzle, picks + cell)
            }
            cell == currentFrom -> {
                // Deselect tapped piece
                TacticsState(puzzle, picks.dropLast(1))
            }
            currentBoard.pieceAt(cell)?.color == puzzle.sideToMove &&
                !currentBoard.legalMoves().contains(ChessMove(currentFrom, cell)) -> {
                // Switch selected piece
                TacticsState(puzzle, picks.dropLast(1) + cell)
            }
            else -> {
                // Complete move
                TacticsState(puzzle, picks + cell)
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is TacticsState &&
        puzzle == other.puzzle && picks == other.picks

    override fun hashCode(): Int = 31 * puzzle.hashCode() + picks.hashCode()

    companion object {
        /** Creates an initial state with no picks. */
        fun initial(puzzle: TacticsPuzzle): TacticsState = TacticsState(puzzle, emptyList())
    }
}
