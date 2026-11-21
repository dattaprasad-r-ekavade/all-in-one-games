
package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell

object TacticsRules {

    /** Checks that the stored line forces mate at the declared depth against best defence. */
    fun isValidSolution(puzzle: TacticsPuzzle): Boolean = countSolutions(puzzle, limit = 1) == 1

    /** Returns true once the player completes the unique mate sequence. */
    fun isSolved(puzzle: TacticsPuzzle, state: TacticsState): Boolean {
        require(state.puzzle == puzzle) { "Tactics state belongs to a different puzzle" }
        return state.completedPlayerMoves.size == puzzle.mateIn &&
            mistakes(puzzle, state).isEmpty() &&
            state.currentBoard.isCheckmate() &&
            countSolutions(puzzle) == 1
    }

    /**
     * Returns the destination squares of committed moves that differ from the stored line,
     * in row-major order. A source-square selection alone is never a mistake.
     */
    fun mistakes(puzzle: TacticsPuzzle, state: TacticsState): List<Cell> {
        require(state.puzzle == puzzle) { "Tactics state belongs to a different puzzle" }
        return state.completedPlayerMoves.mapIndexedNotNull { index, move ->
            move.to.takeIf { move != puzzle.solutionLine.getOrNull(index * 2) }
        }.distinct().sortedBy { it.index }
    }

    /**
     * Returns zero for an invalid stored line. Otherwise returns the largest number of winning
     * moves at any player turn along that line, capped at [limit].
     */
    fun countSolutions(puzzle: TacticsPuzzle, limit: Int = 2): Int {
        require(limit > 0) { "Solution limit must be positive" }
        var board = puzzle.initialBoard
        if (canForceMate(board, puzzle.mateIn - 1)) return 0

        var largestCount = 1
        for (turn in 0 until puzzle.mateIn) {
            val remaining = puzzle.mateIn - turn
            val move = puzzle.solutionLine[2 * turn]
            val legalMoves = board.legalMoves()
            if (move !in legalMoves) return 0
            val afterMove = board.applyMoveInternal(move)
            if (!forcesMate(afterMove, remaining)) return 0

            var winningCount = 1
            if (largestCount < limit) {
                for (alternative in legalMoves) {
                    if (alternative != move && forcesMate(board.applyMoveInternal(alternative), remaining)) {
                        if (++winningCount == limit) break
                    }
                }
            }
            largestCount = maxOf(largestCount, winningCount)

            board = afterMove
            if (remaining > 1) {
                val reply = puzzle.solutionLine[2 * turn + 1]
                if (bestReply(board, remaining - 1) != reply) return 0
                board = board.applyMoveInternal(reply)
            }
        }
        return largestCount
    }

    /** Finds an exact-length mate with one winning move per player turn and one best defence. */
    internal fun findUniqueLine(start: TacticsBoard, mateIn: Int): List<ChessMove>? {
        val line = ArrayList<ChessMove>(2 * mateIn - 1)
        var board = start
        for (remaining in mateIn downTo 1) {
            val move = winningMoves(board, remaining, limit = 2).singleOrNull() ?: return null
            board = board.applyMoveInternal(move)
            line.add(move)
            if (remaining > 1) {
                val reply = bestReply(board, remaining - 1) ?: return null
                board = board.applyMoveInternal(reply)
                line.add(reply)
            }
        }
        return line
    }

    internal fun winningMoves(board: TacticsBoard, remaining: Int, limit: Int = Int.MAX_VALUE): List<ChessMove> =
        board.legalMoves().asSequence()
            .filter { forcesMate(board.applyMoveInternal(it), remaining) }
            .take(limit)
            .toList()

    // The attacker has already proved mate in 'remaining' after every legal reply.
    private fun bestReply(board: TacticsBoard, remaining: Int): ChessMove? =
        board.legalMoves().singleOrNull { reply ->
            !canForceMate(board.applyMoveInternal(reply), remaining - 1)
        }

    // At a defender node every legal reply must still allow mate, and stalemate is a refutation.
    private fun forcesMate(defender: TacticsBoard, remaining: Int): Boolean {
        if (remaining == 1) return defender.isCheckmate()
        val replies = defender.legalMoves()
        if (replies.isEmpty()) return defender.isCheck()
        return replies.all { canForceMate(defender.applyMoveInternal(it), remaining - 1) }
    }

    private fun canForceMate(attacker: TacticsBoard, remaining: Int): Boolean =
        remaining > 0 && attacker.legalMoves().any { forcesMate(attacker.applyMoveInternal(it), remaining) }
}
