package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique

/**
 * Deterministic technique deduction, hint generation, and grading for Chess Tactics.
 */
object TacticsTechniqueSolver {

    /**
     * Returns the next deduction from [puzzle]'s stored solution line as a [HintStep].
     *
     * Returns null if the board is already solved, contains mistakes, or if no legal
     * solution move remains.
     */
    fun nextStep(puzzle: TacticsPuzzle, state: TacticsState): HintStep? {
        require(state.puzzle == puzzle) { "Tactics state belongs to a different puzzle" }
        if (TacticsRules.mistakes(puzzle, state).isNotEmpty()) return null
        if (TacticsRules.isSolved(puzzle, state)) return null

        val turnIndex = state.completedPlayerMoves.size
        val stepIndex = turnIndex * 2
        if (stepIndex >= puzzle.solutionLine.size) return null

        val nextMove = puzzle.solutionLine[stepIndex]
        val currentBoard = state.currentBoard
        if (!currentBoard.legalMoves().contains(nextMove)) return null

        val movingPiece = currentBoard.pieceAt(nextMove.from) ?: return null
        val targetPiece = currentBoard.pieceAt(nextMove.to)
        val afterMove = currentBoard.applyMoveInternal(nextMove)
        val isCheckmate = afterMove.isCheckmate()
        val isCheck = afterMove.isCheck(afterMove.sideToMove)
        val isCapture = targetPiece != null || (nextMove.to == currentBoard.enPassant && movingPiece.type == PieceType.PAWN)
        val remainingMateIn = puzzle.mateIn - turnIndex

        val (technique, explanation) = classifyMove(
            afterMove = afterMove,
            move = nextMove,
            movingPiece = movingPiece,
            isCheckmate = isCheckmate,
            isCheck = isCheck,
            isCapture = isCapture,
            remainingMateIn = remainingMateIn,
        )

        val moves = when {
            state.pendingFrom == null -> listOf(
                Move.SetMark(nextMove.from, Mark.FILLED),
                Move.SetMark(nextMove.to, Mark.FILLED),
            )
            state.pendingFrom == nextMove.from -> listOf(
                Move.SetMark(nextMove.to, Mark.FILLED),
            )
            else -> listOf(
                Move.SetMark(state.pendingFrom!!, Mark.FILLED),
                Move.SetMark(nextMove.from, Mark.FILLED),
                Move.SetMark(nextMove.to, Mark.FILLED),
            )
        }

        return HintStep(
            technique = technique,
            cells = listOf(nextMove.from, nextMove.to),
            explanation = explanation,
            moves = moves,
        )
    }

    /**
     * Solves [puzzle] with the technique solver from its initial state and returns its [Grade].
     *
     * Rejects puzzles that are not unique ([TacticsRules.countSolutions] != 1), puzzles whose
     * pack band does not match their mate horizon and piece count, or puzzles where the hint
     * sequence becomes stuck.
     */
    fun grade(puzzle: TacticsPuzzle): Grade {
        require(TacticsRules.countSolutions(puzzle, limit = 2) == 1) {
            "Tactics grading requires exactly one solution"
        }
        require(TacticsRules.isValidSolution(puzzle)) {
            "Tactics grading requires a valid solution line"
        }

        val pieceCount = puzzle.initialBoard.pieces.count { it != null }
        val expectedPack = TacticsPackBands.packFor(puzzle.mateIn, pieceCount)
        require(puzzle.pack == expectedPack) {
            "Tactics puzzle pack ${puzzle.pack} does not match expected pack $expectedPack for mate-in-${puzzle.mateIn} with $pieceCount pieces"
        }

        var state = TacticsState.initial(puzzle)
        val visited = hashSetOf(state)
        var stepCount = 0
        var hardestTechnique: Technique? = null

        while (!TacticsRules.isSolved(puzzle, state)) {
            val step = nextStep(puzzle, state) ?: throw IllegalArgumentException(
                "Tactics puzzle is stuck with the supported techniques",
            )
            val nextState = step.moves.fold(state) { current, move -> current.withMove(move) }
            require(nextState != state && visited.add(nextState)) {
                "Tactics hint sequence did not make unique progress"
            }
            state = nextState
            stepCount++
            if (hardestTechnique == null || step.technique.difficultyRank > hardestTechnique.difficultyRank) {
                hardestTechnique = step.technique
            }
        }

        return Grade(hardestTechnique, stepCount, expectedPack)
    }

    private fun classifyMove(
        afterMove: TacticsBoard,
        move: ChessMove,
        movingPiece: Piece,
        isCheckmate: Boolean,
        isCheck: Boolean,
        isCapture: Boolean,
        remainingMateIn: Int,
    ): Pair<Technique, String> {
        val destSquare = cellToSquare(move.to)
        if (isCheckmate) {
            if (isSmotheredMate(afterMove, movingPiece)) {
                return TacticsTechniques.SMOTHERED_MATE to "Move to $destSquare delivers smothered mate against the trapped king."
            }
            if (isBackRankMate(afterMove, move, movingPiece)) {
                return TacticsTechniques.BACK_RANK_MATE to "Move to $destSquare delivers back-rank mate on the edge rank."
            }
            return if (isCapture) {
                TacticsTechniques.CHECKMATE to "Captures on $destSquare to deliver checkmate."
            } else {
                TacticsTechniques.CHECKMATE to "Move to $destSquare delivers checkmate."
            }
        }

        if (isCheck) {
            return if (isCapture) {
                TacticsTechniques.CAPTURE to "Captures the piece on $destSquare with check."
            } else {
                TacticsTechniques.CHECK to "Checks the king from $destSquare."
            }
        }

        if (isCapture) {
            return TacticsTechniques.CAPTURE to "Captures the piece on $destSquare."
        }

        return TacticsTechniques.FORCES_MATE to "Move to $destSquare forces mate in $remainingMateIn."
    }

    /**
     * Returns true if [afterMove] is a smothered mate delivered by [movingPiece]:
     * The checking piece is a knight and all squares adjacent to the enemy king on the board
     * are occupied by the defending side's own pieces.
     */
    private fun isSmotheredMate(afterMove: TacticsBoard, movingPiece: Piece): Boolean {
        if (movingPiece.type != PieceType.KNIGHT) return false
        val kingColor = afterMove.sideToMove
        val kingCell = afterMove.kingCell(kingColor)
        val kingRow = kingCell.index / 8
        val kingCol = kingCell.index % 8

        var totalAdjacent = 0
        var friendlyOccupied = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = kingRow + dr
                val c = kingCol + dc
                if (r in 0..7 && c in 0..7) {
                    totalAdjacent++
                    val p = afterMove.pieceAt(r, c)
                    if (p != null && p.color == kingColor) {
                        friendlyOccupied++
                    }
                }
            }
        }
        return totalAdjacent > 0 && friendlyOccupied == totalAdjacent
    }

    /**
     * Returns true if [afterMove] is a back-rank mate:
     * A rook or queen delivers checkmate along the defending king's back rank, and the square
     * directly in front of the king is blocked by a friendly piece.
     */
    private fun isBackRankMate(afterMove: TacticsBoard, move: ChessMove, movingPiece: Piece): Boolean {
        if (movingPiece.type != PieceType.ROOK && movingPiece.type != PieceType.QUEEN) return false
        val kingColor = afterMove.sideToMove
        val kingCell = afterMove.kingCell(kingColor)
        val kingRow = kingCell.index / 8
        val kingCol = kingCell.index % 8

        val isBackRank = if (kingColor == PieceColor.BLACK) kingRow == 0 else kingRow == 7
        if (!isBackRank) return false
        if (move.to.index / 8 != kingRow) return false

        val shieldRow = if (kingColor == PieceColor.BLACK) 1 else 6
        val pieceInFront = afterMove.pieceAt(shieldRow, kingCol)
        return pieceInFront != null && pieceInFront.color == kingColor
    }
}
