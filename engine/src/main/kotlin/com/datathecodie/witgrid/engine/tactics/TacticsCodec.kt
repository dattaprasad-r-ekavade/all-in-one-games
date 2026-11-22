package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.BankLine

/**
 * BankLine encoder and decoder for Chess Tactics puzzles.
 */
object TacticsCodec {
    /** Decodes a BankLine into a validated [TacticsPuzzle]. */
    fun decode(line: String): TacticsPuzzle {
        val bankLine = BankLine.decode(line)
        require(bankLine.size == 8) { "Tactics board size must be 8" }

        val tokens = bankLine.payload.trim().split(Regex("\\s+"))
        require(tokens.size in 4..7) { "Tactics payload must contain FEN and an optional mate horizon" }

        val fenString = "${tokens[0]} ${tokens[1]} ${tokens[2]} ${tokens[3]}"
        val initialBoard = TacticsBoard.fromFen(fenString)

        var currentBoard = initialBoard
        val rawUciMoves = bankLine.solution.trim().split(Regex("\\s+"))
        require(rawUciMoves.isNotEmpty() && rawUciMoves.none { it.isEmpty() }) {
            "Tactics solution line must not be empty"
        }
        val solutionMoves = rawUciMoves.map { uci ->
            require(uci.length in 4..5) { "Invalid UCI move: $uci" }
            val move = ChessMove.fromUci(uci)
            val movingPiece = currentBoard.pieceAt(move.from)
            requireNotNull(movingPiece) { "No piece at ${cellToSquare(move.from)} for move $uci" }
            val toRow = move.to.index / 8
            val isPawnPromotion = movingPiece.type == PieceType.PAWN &&
                ((movingPiece.color == PieceColor.WHITE && toRow == 0) ||
                 (movingPiece.color == PieceColor.BLACK && toRow == 7))

            if (uci.length == 5) {
                require(isPawnPromotion) {
                    "Promotion suffix 'q' only allowed for pawn move to the last rank, got $movingPiece on ${cellToSquare(move.from)} moving to ${cellToSquare(move.to)}"
                }
            } else {
                require(!isPawnPromotion) {
                    "Pawn move to the last rank requires promotion suffix 'q': $uci"
                }
            }
            require(currentBoard.legalMoves().contains(move)) { "Illegal move in solution line: $uci" }
            currentBoard = currentBoard.applyMoveInternal(move)
            move
        }

        val mateIn = if (tokens.size == 5 || tokens.size == 7) {
            requireNotNull(tokens.last().toIntOrNull()) { "Invalid Tactics mate horizon" }
        } else {
            (solutionMoves.size + 1) / 2
        }

        val puzzle = TacticsPuzzle(
            id = bankLine.id,
            initialBoard = initialBoard,
            sideToMove = initialBoard.sideToMove,
            mateIn = mateIn,
            solutionLine = solutionMoves,
            pack = bankLine.pack,
            storedGrade = bankLine.grade,
        )

        require(TacticsRules.isValidSolution(puzzle)) {
            "Tactics puzzle stored solution line does not force checkmate"
        }

        return puzzle
    }

    /** Encodes a [TacticsPuzzle] into canonical BankLine format. */
    fun encode(puzzle: TacticsPuzzle): String {
        require(TacticsRules.isValidSolution(puzzle)) {
            "Tactics puzzle stored solution line does not force checkmate"
        }
        return BankLine(
            id = puzzle.id,
            size = puzzle.size,
            payload = puzzle.toPayload(),
            solution = puzzle.toSolutionString(),
            pack = puzzle.pack,
            grade = puzzle.storedGrade,
        ).encode()
    }
}
