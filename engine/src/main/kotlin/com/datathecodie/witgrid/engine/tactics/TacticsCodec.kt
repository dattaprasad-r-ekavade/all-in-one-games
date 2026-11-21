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
        require(tokens.size >= 4) { "Tactics payload must have at least 4 tokens for FEN representation" }

        val fenString = "${tokens[0]} ${tokens[1]} ${tokens[2]} ${tokens[3]}"
        val initialBoard = TacticsBoard.fromFen(fenString)

        val solutionMoves = bankLine.solution.trim().split(Regex("\\s+")).map { uci ->
            ChessMove.fromUci(uci)
        }
        require(solutionMoves.isNotEmpty()) { "Tactics solution line must not be empty" }

        val mateIn = when (tokens.size) {
            5 -> tokens[4].toIntOrNull() ?: ((solutionMoves.size + 1) / 2)
            7 -> tokens[6].toIntOrNull() ?: ((solutionMoves.size + 1) / 2)
            else -> (solutionMoves.size + 1) / 2
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
