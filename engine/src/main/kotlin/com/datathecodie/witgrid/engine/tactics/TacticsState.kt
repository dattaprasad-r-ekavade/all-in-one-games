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
    val pendingFrom: Cell?
        get() = picks.lastOrNull()?.takeIf {
            picks.size == 2 * completedPlayerMoves.size + 1 &&
                !hasMistake && currentBoard.sideToMove == puzzle.sideToMove &&
                currentBoard.pieceAt(it)?.color == puzzle.sideToMove
        }

    private val hasMistake: Boolean

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

            if (board.sideToMove != puzzle.sideToMove || board.pieceAt(from)?.color != puzzle.sideToMove) {
                break
            }

            val stepIndex = i * 2
            val legal = playerMove in board.legalMoves()
            playerMoves.add(playerMove)
            lastAny = playerMove
            if (legal) board = board.applyMoveInternal(playerMove)
            if (!legal || playerMove != puzzle.solutionLine.getOrNull(stepIndex)) {
                // Keep the mistake visible until undo without letting the player move the defender.
                mistakeOccurred = true
                break
            }

            val reply = puzzle.solutionLine.getOrNull(stepIndex + 1) ?: break
            if (reply !in board.legalMoves()) break
            board = board.applyMoveInternal(reply)
            lastOpponent = reply
            lastAny = reply
        }

        this.hasMistake = mistakeOccurred
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

        if (hasMistake || currentBoard.sideToMove != puzzle.sideToMove ||
            completedPlayerMoves.size == puzzle.mateIn
        ) {
            return this
        }

        val cell = move.cell
        val currentFrom = pendingFrom

        return when {
            currentFrom == null -> {
                if (currentBoard.pieceAt(cell)?.color == puzzle.sideToMove) {
                    TacticsState(puzzle, picks + cell)
                } else {
                    this
                }
            }
            cell == currentFrom -> {
                // Deselect tapped piece
                TacticsState(puzzle, picks.dropLast(1))
            }
            currentBoard.pieceAt(cell)?.color == puzzle.sideToMove -> {
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
