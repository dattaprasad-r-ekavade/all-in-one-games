package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell

/**
 * Rules, solution counting, and state validation for Chess Tactics.
 */
object TacticsRules {
    /** Returns whether [puzzle]'s stored solution line is legally valid and ends in checkmate. */
    fun isValidSolution(puzzle: TacticsPuzzle): Boolean {
        var board = puzzle.initialBoard
        for (move in puzzle.solutionLine) {
            if (!board.legalMoves().contains(move)) return false
            board = board.applyMoveInternal(move)
        }
        return board.isCheckmate()
    }

    /**
     * Returns true if all solution moves have been correctly executed and the final position is checkmate.
     */
    fun isSolved(puzzle: TacticsPuzzle, state: TacticsState): Boolean {
        require(state.puzzle == puzzle) { "Tactics state belongs to a different puzzle" }
        if (!isValidSolution(puzzle)) return false
        if (mistakes(puzzle, state).isNotEmpty()) return false

        val requiredPlayerMoves = (puzzle.solutionLine.size + 1) / 2
        if (state.completedPlayerMoves.size != requiredPlayerMoves) return false

        return state.currentBoard.isCheckmate()
    }

    /**
     * Returns incorrect committed entries in row-major order; empty cells and in-progress picks are ignored.
     *
     * For Chess Tactics, a committed player move (a completed from-and-to square pick) is a mistake if it
     * does not match the unique solution move for that turn in the puzzle's solution line. For each incorrect
     * player move, its destination cell is reported as a mistake (in row-major order). An in-progress move
     * (where only the from-square has been picked) is not a mistake.
     */
    fun mistakes(puzzle: TacticsPuzzle, state: TacticsState): List<Cell> {
        require(state.puzzle == puzzle) { "Tactics state belongs to a different puzzle" }
        val mistakeCells = ArrayList<Cell>()

        for ((index, move) in state.completedPlayerMoves.withIndex()) {
            val solutionStep = index * 2
            val expectedMove = puzzle.solutionLine.getOrNull(solutionStep)
            if (move != expectedMove) {
                mistakeCells.add(move.to)
            }
        }

        return mistakeCells.distinct().sortedBy { it.index }
    }

    /**
     * Counts how many distinct first moves for [puzzle]'s side to move force checkmate in at most
     * [puzzle.mateIn] moves, stopping when [limit] is reached.
     */
    fun countSolutions(puzzle: TacticsPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        var count = 0

        for (move in puzzle.initialBoard.legalMoves()) {
            val nextBoard = puzzle.initialBoard.applyMoveInternal(move)
            if (forcesMate(nextBoard, puzzle.mateIn, puzzle.sideToMove)) {
                count++
                if (count >= limit) return limit
            }
        }

        return count
    }

    /**
     * Evaluates whether [winningColor] can force checkmate against all defenses within [turnsRemaining].
     */
    private fun forcesMate(
        board: TacticsBoard,
        turnsRemaining: Int,
        winningColor: PieceColor,
    ): Boolean {
        if (board.isCheckmate()) {
            return board.sideToMove != winningColor
        }
        if (turnsRemaining <= 1) {
            return false
        }
        if (board.isStalemate()) {
            return false
        }

        return if (board.sideToMove != winningColor) {
            // Defending opponent: winningColor must force mate against EVERY legal reply
            val replies = board.legalMoves()
            if (replies.isEmpty()) return false
            for (reply in replies) {
                val afterReply = board.applyMoveInternal(reply)
                if (!forcesMate(afterReply, turnsRemaining - 1, winningColor)) {
                    return false
                }
            }
            true
        } else {
            // Winning side: at least ONE move must force mate
            val candidates = board.legalMoves()
            for (candidate in candidates) {
                val afterCandidate = board.applyMoveInternal(candidate)
                if (forcesMate(afterCandidate, turnsRemaining, winningColor)) {
                    return true
                }
            }
            false
        }
    }
}
