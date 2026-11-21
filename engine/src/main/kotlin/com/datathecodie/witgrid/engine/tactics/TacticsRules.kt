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
     * Counts solutions under the 'unique mate sequence' rule:
     * At every player turn along the stored solution line (advancing with the stored opponent replies),
     * exactly one move must keep a forced mate in the remaining moves.
     *
     * Returns the capped count of the worst turn across all player turns:
     * - Returns 0 if any player turn has no move that forces checkmate in the remaining moves.
     * - Returns [limit] (e.g. 2) if any player turn has two or more winning moves.
     * - Returns 1 if every player turn has exactly one winning move.
     */
    fun countSolutions(puzzle: TacticsPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }

        var worstCount = 1
        var currentBoard = puzzle.initialBoard
        val totalMoves = puzzle.solutionLine.size
        val playerTurns = (totalMoves + 1) / 2

        for (turn in 0 until playerTurns) {
            val remainingMateIn = puzzle.mateIn - turn
            var turnWinningMoves = 0

            for (move in currentBoard.legalMoves()) {
                if (isWinningMove(currentBoard, move, remainingMateIn, puzzle.sideToMove)) {
                    turnWinningMoves++
                    if (turnWinningMoves >= limit) break
                }
            }

            if (turnWinningMoves == 0) {
                return 0
            }
            if (turnWinningMoves > worstCount) {
                worstCount = turnWinningMoves
            }
            if (worstCount >= limit) {
                return limit
            }

            val playerStep = turn * 2
            val playerMove = puzzle.solutionLine[playerStep]
            currentBoard = currentBoard.applyMoveInternal(playerMove)

            val opponentStep = playerStep + 1
            if (opponentStep < totalMoves) {
                val opponentMove = puzzle.solutionLine[opponentStep]
                currentBoard = currentBoard.applyMoveInternal(opponentMove)
            }
        }

        return minOf(limit, worstCount)
    }

    /**
     * Evaluates whether [move] from [board] forces checkmate for [winningColor] against all defenses
     * within [movesRemaining] player moves.
     */
    private fun isWinningMove(
        board: TacticsBoard,
        move: ChessMove,
        movesRemaining: Int,
        winningColor: PieceColor,
    ): Boolean {
        val afterMove = board.applyMoveInternal(move)
        if (afterMove.isCheckmate()) {
            return afterMove.sideToMove != winningColor
        }
        if (afterMove.isStalemate() || movesRemaining <= 1) {
            return false
        }
        val replies = afterMove.legalMoves()
        if (replies.isEmpty()) return false
        return replies.all { reply ->
            canForceMate(afterMove.applyMoveInternal(reply), movesRemaining - 1, winningColor)
        }
    }

    /**
     * Evaluates whether [winningColor] can force checkmate from [board] in at most [movesRemaining] moves,
     * where it is [winningColor]'s turn to move.
     */
    private fun canForceMate(
        board: TacticsBoard,
        movesRemaining: Int,
        winningColor: PieceColor,
    ): Boolean {
        if (movesRemaining <= 0) return false
        for (candidate in board.legalMoves()) {
            if (isWinningMove(board, candidate, movesRemaining, winningColor)) {
                return true
            }
        }
        return false
    }
}
