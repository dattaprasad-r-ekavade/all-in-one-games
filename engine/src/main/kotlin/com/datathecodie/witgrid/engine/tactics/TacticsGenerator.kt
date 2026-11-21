package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Deterministic procedural generator for unique, technique-solvable Chess Tactics puzzles.
 *
 * Generates positions from tactical mating pattern templates across four difficulty packs:
 * - Easy: Mate in 1
 * - Medium: Mate in 2 with fewer pieces (pieceCount <= 10)
 * - Hard: Mate in 2 with more pieces (pieceCount > 10)
 * - Expert: Mate in 3
 */
object TacticsGenerator {
    private const val MAX_PUZZLE_ATTEMPTS = 5_000
    private const val PLACEHOLDER_ID = "CT-SEARCH"

    /** Builds one puzzle in [pack], consuming randomness only from [rng]. */
    fun generate(rng: SplitMix64, pack: Pack): TacticsPuzzle {
        repeat(MAX_PUZZLE_ATTEMPTS) {
            val candidate = generateCandidate(rng, pack) ?: return@repeat
            if (!isLegalTacticsPosition(candidate.first, PieceColor.WHITE)) return@repeat

            val placeholder = TacticsPuzzle(
                id = PLACEHOLDER_ID,
                initialBoard = candidate.first,
                sideToMove = PieceColor.WHITE,
                mateIn = candidate.second,
                solutionLine = candidate.third,
                pack = pack,
                storedGrade = "none:0",
            )

            if (!TacticsRules.isValidSolution(placeholder)) return@repeat
            if (TacticsRules.countSolutions(placeholder, limit = 2) != 1) return@repeat
            if (!TacticsPackBands.isPuzzleInPackBand(placeholder)) return@repeat

            val grade = try {
                TacticsTechniqueSolver.grade(placeholder)
            } catch (_: IllegalArgumentException) {
                null
            } ?: return@repeat

            if (grade.pack != pack) return@repeat

            val id = "CT-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}"
            val finalGradeString = TacticsGradeCodec.encode(grade)
            return TacticsPuzzle(
                id = id,
                initialBoard = candidate.first,
                sideToMove = PieceColor.WHITE,
                mateIn = candidate.second,
                solutionLine = candidate.third,
                pack = pack,
                storedGrade = finalGradeString,
            )
        }

        throw IllegalStateException("Could not generate a technique-solvable Tactics puzzle for $pack after $MAX_PUZZLE_ATTEMPTS attempts")
    }

    private fun generateCandidate(
        rng: SplitMix64,
        pack: Pack,
    ): Triple<TacticsBoard, Int, List<ChessMove>>? = when (pack) {
        Pack.EASY -> generateEasy(rng)
        Pack.MEDIUM -> generateMedium(rng)
        Pack.HARD -> generateHard(rng)
        Pack.EXPERT -> generateExpert(rng)
    }

    // -------------------------------------------------------------------------
    // Pack.EASY: Mate in 1
    // -------------------------------------------------------------------------

    private fun generateEasy(rng: SplitMix64): Triple<TacticsBoard, Int, List<ChessMove>> {
        val pattern = rng.nextInt(4)
        val (board, solution) = when (pattern) {
            0 -> generateEasyBackRank(rng)
            1 -> generateEasySmothered(rng)
            2 -> generateEasyQueenAndKing(rng)
            else -> generateEasyArabian(rng)
        }

        // Add 0 to 2 harmless passive pawns on distant squares to increase variation
        val pawnsToAdd = rng.nextInt(3)
        if (pawnsToAdd > 0) {
            val pieces = ArrayList(board.pieces)
            val candidatePawns = listOf(
                Cell(1 * 8 + 0) to Piece(PieceColor.BLACK, PieceType.PAWN),
                Cell(6 * 8 + 0) to Piece(PieceColor.WHITE, PieceType.PAWN),
                Cell(1 * 8 + 1) to Piece(PieceColor.BLACK, PieceType.PAWN),
                Cell(6 * 8 + 1) to Piece(PieceColor.WHITE, PieceType.PAWN),
                Cell(2 * 8 + 0) to Piece(PieceColor.BLACK, PieceType.PAWN),
                Cell(5 * 8 + 0) to Piece(PieceColor.WHITE, PieceType.PAWN),
                Cell(1 * 8 + 7) to Piece(PieceColor.BLACK, PieceType.PAWN),
                Cell(6 * 8 + 7) to Piece(PieceColor.WHITE, PieceType.PAWN),
            )
            var added = 0
            for ((cell, p) in candidatePawns.shuffled(rng)) {
                if (pieces[cell.index] == null) {
                    pieces[cell.index] = p
                    added++
                    if (added >= pawnsToAdd) break
                }
            }
            val enrichedBoard = TacticsBoard(pieces, PieceColor.WHITE, CastlingRights(), null)
            return Triple(enrichedBoard, 1, solution)
        }

        return Triple(board, 1, solution)
    }

    private fun generateEasyBackRank(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val variant = rng.nextInt(6)
        val (kCol, rCols) = when (variant) {
            0 -> 6 to listOf(0, 1, 2, 3) // g8
            1 -> 1 to listOf(4, 5, 6, 7) // b8
            2 -> 7 to listOf(0, 1, 2, 3) // h8
            3 -> 2 to listOf(4, 5, 6, 7) // c8
            4 -> 5 to listOf(0, 1, 2, 3) // f8
            else -> 0 to listOf(3, 4, 5, 6, 7) // a8
        }

        pieces[kCol] = Piece(PieceColor.BLACK, PieceType.KING)
        when (kCol) {
            6 -> {
                pieces[1 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)
            }
            1 -> {
                pieces[1 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.PAWN)
            }
            7 -> {
                pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)
            }
            2 -> {
                pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 3] = Piece(PieceColor.BLACK, PieceType.PAWN)
            }
            5 -> {
                pieces[1 * 8 + 4] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
            }
            else -> {
                pieces[1 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)
            }
        }

        val rCol = rCols[rng.nextInt(rCols.size)]
        val rRow = 2 + rng.nextInt(6) // rows 2..7
        val fromCell = Cell(rRow * 8 + rCol)
        val toCell = Cell(rCol)
        pieces[fromCell.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

        val wkCol = if (kCol >= 4) rng.nextInt(4) else 4 + rng.nextInt(4)
        val wkRow = 5 + rng.nextInt(3) // rows 5..7
        val wkIndex = wkRow * 8 + wkCol
        if (pieces[wkIndex] == null) {
            pieces[wkIndex] = Piece(PieceColor.WHITE, PieceType.KING)
        } else {
            pieces[7 * 8 + (if (kCol >= 4) 0 else 7)] = Piece(PieceColor.WHITE, PieceType.KING)
        }

        val board = TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        return board to listOf(ChessMove(fromCell, toCell))
    }

    private fun generateEasySmothered(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val corner = rng.nextInt(4)

        val (fromCell, toCell) = when (corner) {
            0 -> {
                // h8 corner
                pieces[7] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[6] = Piece(PieceColor.BLACK, PieceType.ROOK)
                pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)

                val candidateStarts = listOf(
                    Cell(3 * 8 + 4), Cell(3 * 8 + 6), Cell(2 * 8 + 3), Cell(0 * 8 + 3),
                    Cell(2 * 8 + 7), Cell(3 * 8 + 5),
                )
                val from = candidateStarts[rng.nextInt(candidateStarts.size)]
                pieces[from.index] = Piece(PieceColor.WHITE, PieceType.KNIGHT)
                from to Cell(1 * 8 + 5)
            }
            1 -> {
                // a8 corner
                pieces[0] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[1] = Piece(PieceColor.BLACK, PieceType.ROOK)
                pieces[1 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)

                val candidateStarts = listOf(
                    Cell(3 * 8 + 3), Cell(3 * 8 + 1), Cell(2 * 8 + 4), Cell(0 * 8 + 4),
                    Cell(2 * 8 + 0), Cell(3 * 8 + 2),
                )
                val from = candidateStarts[rng.nextInt(candidateStarts.size)]
                pieces[from.index] = Piece(PieceColor.WHITE, PieceType.KNIGHT)
                from to Cell(1 * 8 + 2)
            }
            2 -> {
                // h1 corner
                pieces[7 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[7 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.ROOK)
                pieces[6 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[6 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)

                val candidateStarts = listOf(
                    Cell(4 * 8 + 4), Cell(4 * 8 + 6), Cell(5 * 8 + 3), Cell(7 * 8 + 3),
                )
                val from = candidateStarts[rng.nextInt(candidateStarts.size)]
                pieces[from.index] = Piece(PieceColor.WHITE, PieceType.KNIGHT)
                from to Cell(6 * 8 + 5)
            }
            else -> {
                // a1 corner
                pieces[7 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[7 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.ROOK)
                pieces[6 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[6 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)

                val candidateStarts = listOf(
                    Cell(4 * 8 + 3), Cell(4 * 8 + 1), Cell(5 * 8 + 4), Cell(7 * 8 + 4),
                )
                val from = candidateStarts[rng.nextInt(candidateStarts.size)]
                pieces[from.index] = Piece(PieceColor.WHITE, PieceType.KNIGHT)
                from to Cell(6 * 8 + 2)
            }
        }

        // White king on safe square
        val wkCol = if (toCell.index % 8 >= 4) rng.nextInt(3) else 5 + rng.nextInt(3)
        val wkRow = if (toCell.index / 8 <= 3) 5 + rng.nextInt(3) else rng.nextInt(3)
        pieces[wkRow * 8 + wkCol] = Piece(PieceColor.WHITE, PieceType.KING)

        val board = TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        return board to listOf(ChessMove(fromCell, toCell))
    }

    private fun generateEasyQueenAndKing(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val (fromCell, toCell) = if (isKingside) {
            pieces[7] = Piece(PieceColor.BLACK, PieceType.KING)
            val wkRow = 1 + rng.nextInt(2)
            pieces[wkRow * 8 + 5] = Piece(PieceColor.WHITE, PieceType.KING)

            val qStarts = listOf(
                Cell(1 * 8 + 0), Cell(1 * 8 + 1), Cell(1 * 8 + 2), Cell(1 * 8 + 3),
                Cell(4 * 8 + 3), Cell(5 * 8 + 2), Cell(6 * 8 + 1), Cell(3 * 8 + 4),
            )
            val from = qStarts[rng.nextInt(qStarts.size)]
            pieces[from.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)
            from to Cell(1 * 8 + 6)
        } else {
            pieces[0] = Piece(PieceColor.BLACK, PieceType.KING)
            val wkRow = 1 + rng.nextInt(2)
            pieces[wkRow * 8 + 2] = Piece(PieceColor.WHITE, PieceType.KING)

            val qStarts = listOf(
                Cell(1 * 8 + 7), Cell(1 * 8 + 6), Cell(1 * 8 + 5), Cell(1 * 8 + 4),
                Cell(4 * 8 + 4), Cell(5 * 8 + 5), Cell(6 * 8 + 6), Cell(3 * 8 + 3),
            )
            val from = qStarts[rng.nextInt(qStarts.size)]
            pieces[from.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)
            from to Cell(1 * 8 + 1)
        }

        val board = TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        return board to listOf(ChessMove(fromCell, toCell))
    }

    private fun generateEasyArabian(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val (fromCell, toCell) = if (isKingside) {
            pieces[7] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[2 * 8 + 5] = Piece(PieceColor.WHITE, PieceType.KNIGHT)
            val rStarts = listOf(
                Cell(1 * 8 + 0), Cell(1 * 8 + 1), Cell(1 * 8 + 2), Cell(1 * 8 + 3),
                Cell(1 * 8 + 4), Cell(4 * 8 + 7), Cell(5 * 8 + 7), Cell(6 * 8 + 7),
            )
            val from = rStarts[rng.nextInt(rStarts.size)]
            pieces[from.index] = Piece(PieceColor.WHITE, PieceType.ROOK)
            val wkCol = rng.nextInt(4)
            val wkRow = 5 + rng.nextInt(3)
            pieces[wkRow * 8 + wkCol] = Piece(PieceColor.WHITE, PieceType.KING)
            from to Cell(1 * 8 + 7)
        } else {
            pieces[0] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[2 * 8 + 2] = Piece(PieceColor.WHITE, PieceType.KNIGHT)
            val rStarts = listOf(
                Cell(1 * 8 + 3), Cell(1 * 8 + 4), Cell(1 * 8 + 5), Cell(1 * 8 + 6),
                Cell(1 * 8 + 7), Cell(4 * 8 + 0), Cell(5 * 8 + 0), Cell(6 * 8 + 0),
            )
            val from = rStarts[rng.nextInt(rStarts.size)]
            pieces[from.index] = Piece(PieceColor.WHITE, PieceType.ROOK)
            val wkCol = 4 + rng.nextInt(4)
            val wkRow = 5 + rng.nextInt(3)
            pieces[wkRow * 8 + wkCol] = Piece(PieceColor.WHITE, PieceType.KING)
            from to Cell(1 * 8 + 0)
        }

        val board = TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        return board to listOf(ChessMove(fromCell, toCell))
    }

    // -------------------------------------------------------------------------
    // Pack.MEDIUM: Mate in 2, pieceCount <= 10
    // -------------------------------------------------------------------------

    private data class DeflectionMoves(val qCell: Cell, val defenderCell: Cell, val rCell: Cell, val targetCell: Cell)

    private fun generateMedium(rng: SplitMix64): Triple<TacticsBoard, Int, List<ChessMove>> {
        val pattern = rng.nextInt(4)
        val (board, solution) = when (pattern) {
            0 -> generateMediumBackRankDeflection(rng)
            1 -> generateMediumSmothered(rng)
            2 -> generateMediumAnastasia(rng)
            else -> generateMediumGreco(rng)
        }
        return Triple(board, 2, solution)
    }

    private fun generateMediumBackRankDeflection(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val moves = if (isKingside) {
            pieces[6] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[1 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)

            val f = 2 + rng.nextInt(2) // c-file (2) or d-file (3)
            pieces[f] = Piece(PieceColor.BLACK, PieceType.ROOK)

            val bCol = f + 1
            pieces[1 * 8 + bCol] = Piece(PieceColor.BLACK, PieceType.BISHOP)

            val wCol = f - 1
            val wRow = 2 + rng.nextInt(3)
            pieces[wRow * 8 + wCol] = Piece(PieceColor.WHITE, PieceType.BISHOP)

            val qRows = listOf(3, 4, 5)
            val qRow = qRows[rng.nextInt(qRows.size)]
            val q = Cell(qRow * 8 + f)
            pieces[q.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val r = Cell(7 * 8 + f)
            pieces[r.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(Cell(7 * 8 + 6), Cell(7 * 8 + 7), Cell(6 * 8 + 6), Cell(6 * 8 + 7), Cell(7 * 8 + 0), Cell(7 * 8 + 1), Cell(6 * 8 + 0))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            DeflectionMoves(q, Cell(1 * 8 + bCol), r, Cell(f))
        } else {
            // Mirrored queenside castle
            pieces[1] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[1 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.PAWN)

            val f = 4 + rng.nextInt(2)
            pieces[f] = Piece(PieceColor.BLACK, PieceType.ROOK)

            val bCol = f - 1
            pieces[1 * 8 + bCol] = Piece(PieceColor.BLACK, PieceType.BISHOP)

            val wCol = f + 1
            val wRow = 2 + rng.nextInt(3)
            pieces[wRow * 8 + wCol] = Piece(PieceColor.WHITE, PieceType.BISHOP)

            val qRows = listOf(3, 4, 5)
            val qRow = qRows[rng.nextInt(qRows.size)]
            val q = Cell(qRow * 8 + f)
            pieces[q.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val r = Cell(7 * 8 + f)
            pieces[r.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(Cell(7 * 8 + 0), Cell(7 * 8 + 1), Cell(6 * 8 + 0), Cell(6 * 8 + 1), Cell(7 * 8 + 7), Cell(7 * 8 + 6))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            DeflectionMoves(q, Cell(1 * 8 + bCol), r, Cell(f))
        }

        val board = TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        val solution = listOf(
            ChessMove(moves.qCell, moves.targetCell),
            ChessMove(moves.defenderCell, moves.targetCell),
            ChessMove(moves.rCell, moves.targetCell),
        )
        return board to solution
    }

    private fun generateMediumSmothered(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val (solution, board) = if (isKingside) {
            pieces[7] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[5] = Piece(PieceColor.BLACK, PieceType.ROOK)
            pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)

            pieces[3 * 8 + 6] = Piece(PieceColor.WHITE, PieceType.KNIGHT)

            val qStarts = listOf(
                Cell(6 * 8 + 0), Cell(5 * 8 + 1), Cell(4 * 8 + 2), Cell(3 * 8 + 3),
            )
            val qFrom = qStarts[rng.nextInt(qStarts.size)]
            pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val wkSquares = listOf(Cell(7 * 8 + 0), Cell(7 * 8 + 1), Cell(6 * 8 + 0), Cell(6 * 8 + 1), Cell(7 * 8 + 2), Cell(6 * 8 + 2), Cell(5 * 8 + 0))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val sol = listOf(
                ChessMove(qFrom, Cell(0 * 8 + 6)),
                ChessMove(Cell(0 * 8 + 5), Cell(0 * 8 + 6)),
                ChessMove(Cell(3 * 8 + 6), Cell(1 * 8 + 5)),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        } else {
            pieces[0] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[2] = Piece(PieceColor.BLACK, PieceType.ROOK)
            pieces[1 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)

            pieces[3 * 8 + 1] = Piece(PieceColor.WHITE, PieceType.KNIGHT)

            val qStarts = listOf(
                Cell(6 * 8 + 7), Cell(5 * 8 + 6), Cell(4 * 8 + 5), Cell(3 * 8 + 4),
            )
            val qFrom = qStarts[rng.nextInt(qStarts.size)]
            pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val wkSquares = listOf(Cell(7 * 8 + 7), Cell(7 * 8 + 6), Cell(6 * 8 + 7), Cell(6 * 8 + 6), Cell(7 * 8 + 5), Cell(6 * 8 + 5), Cell(5 * 8 + 7))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val sol = listOf(
                ChessMove(qFrom, Cell(0 * 8 + 1)),
                ChessMove(Cell(0 * 8 + 2), Cell(0 * 8 + 1)),
                ChessMove(Cell(3 * 8 + 1), Cell(1 * 8 + 2)),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        }

        return board to solution
    }

    private fun generateMediumAnastasia(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val (solution, board) = if (isKingside) {
            pieces[7] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)

            pieces[1 * 8 + 4] = Piece(PieceColor.WHITE, PieceType.KNIGHT)

            val qStarts = listOf(
                Cell(5 * 8 + 3), Cell(6 * 8 + 2), Cell(4 * 8 + 4), Cell(3 * 8 + 5),
            )
            val qFrom = qStarts[rng.nextInt(qStarts.size)]
            pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val rCols = listOf(0, 1, 2, 3)
            val rCol = rCols[rng.nextInt(rCols.size)]
            val rFrom = Cell(7 * 8 + rCol)
            pieces[rFrom.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(Cell(7 * 8 + 4), Cell(7 * 8 + 5), Cell(6 * 8 + 4), Cell(6 * 8 + 5), Cell(6 * 8 + 0), Cell(7 * 8 + 0))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val h7Cell = Cell(1 * 8 + 7)
            val kCell = Cell(7)
            val h1Cell = Cell(7 * 8 + 7)
            val sol = listOf(
                ChessMove(qFrom, h7Cell),
                ChessMove(kCell, h7Cell),
                ChessMove(rFrom, h1Cell),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        } else {
            pieces[0] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)

            pieces[1 * 8 + 3] = Piece(PieceColor.WHITE, PieceType.KNIGHT)

            val qStarts = listOf(
                Cell(5 * 8 + 4), Cell(6 * 8 + 5), Cell(4 * 8 + 3), Cell(3 * 8 + 2),
            )
            val qFrom = qStarts[rng.nextInt(qStarts.size)]
            pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val rCols = listOf(4, 5, 6, 7)
            val rCol = rCols[rng.nextInt(rCols.size)]
            val rFrom = Cell(7 * 8 + rCol)
            pieces[rFrom.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(Cell(7 * 8 + 2), Cell(7 * 8 + 3), Cell(6 * 8 + 2), Cell(6 * 8 + 3), Cell(6 * 8 + 7), Cell(7 * 8 + 7))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val a7Cell = Cell(1 * 8 + 0)
            val kCell = Cell(0)
            val a1Cell = Cell(7 * 8 + 0)
            val sol = listOf(
                ChessMove(qFrom, a7Cell),
                ChessMove(kCell, a7Cell),
                ChessMove(rFrom, a1Cell),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        }

        return board to solution
    }

    private fun generateMediumGreco(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        pieces[7] = Piece(PieceColor.BLACK, PieceType.KING)
        pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)

        val bStarts = listOf(Cell(4 * 8 + 2), Cell(5 * 8 + 1), Cell(6 * 8 + 0), Cell(3 * 8 + 3))
        val bFrom = bStarts[rng.nextInt(bStarts.size)]
        pieces[bFrom.index] = Piece(PieceColor.WHITE, PieceType.BISHOP)

        val qStarts = listOf(Cell(5 * 8 + 3), Cell(6 * 8 + 2), Cell(4 * 8 + 4))
        val qFrom = qStarts[rng.nextInt(qStarts.size)]
        pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

        val rStarts = listOf(Cell(7 * 8 + 5), Cell(7 * 8 + 4), Cell(7 * 8 + 3))
        val rFrom = rStarts[rng.nextInt(rStarts.size)]
        pieces[rFrom.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

        val wkSquares = listOf(Cell(7 * 8 + 0), Cell(7 * 8 + 1), Cell(6 * 8 + 0), Cell(6 * 8 + 1), Cell(5 * 8 + 0))
        val wk = wkSquares[rng.nextInt(wkSquares.size)]
        pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

        val board = TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        val h7Cell = Cell(1 * 8 + 7)
        val kCell = Cell(7)
        val h1Cell = Cell(7 * 8 + 7)
        val solution = listOf(
            ChessMove(qFrom, h7Cell),
            ChessMove(kCell, h7Cell),
            ChessMove(rFrom, h1Cell),
        )
        return board to solution
    }

    // -------------------------------------------------------------------------
    // Pack.HARD: Mate in 2, pieceCount > 10
    // -------------------------------------------------------------------------

    private fun generateHard(rng: SplitMix64): Triple<TacticsBoard, Int, List<ChessMove>> {
        val pattern = rng.nextInt(4)
        val (baseBoard, solution) = when (pattern) {
            0 -> generateMediumBackRankDeflection(rng)
            1 -> generateMediumSmothered(rng)
            2 -> generateMediumAnastasia(rng)
            else -> generateMediumGreco(rng)
        }

        val pieces = ArrayList(baseBoard.pieces)

        val candidatePawns = listOf(
            Cell(1 * 8 + 0) to Piece(PieceColor.BLACK, PieceType.PAWN),
            Cell(1 * 8 + 1) to Piece(PieceColor.BLACK, PieceType.PAWN),
            Cell(6 * 8 + 0) to Piece(PieceColor.WHITE, PieceType.PAWN),
            Cell(6 * 8 + 1) to Piece(PieceColor.WHITE, PieceType.PAWN),
            Cell(2 * 8 + 0) to Piece(PieceColor.BLACK, PieceType.PAWN),
            Cell(5 * 8 + 0) to Piece(PieceColor.WHITE, PieceType.PAWN),
            Cell(2 * 8 + 1) to Piece(PieceColor.BLACK, PieceType.PAWN),
            Cell(5 * 8 + 1) to Piece(PieceColor.WHITE, PieceType.PAWN),
            Cell(3 * 8 + 0) to Piece(PieceColor.BLACK, PieceType.PAWN),
            Cell(4 * 8 + 0) to Piece(PieceColor.WHITE, PieceType.PAWN),
        )

        val targetPieceCount = 11 + rng.nextInt(3) // 11 to 13 pieces
        val shuffledPawns = candidatePawns.shuffled(rng)
        for ((cell, piece) in shuffledPawns) {
            if (pieces[cell.index] == null) {
                pieces[cell.index] = piece
            }
            if (pieces.count { it != null } >= targetPieceCount) break
        }

        val board = TacticsBoard(pieces, PieceColor.WHITE, CastlingRights(), null)
        return Triple(board, 2, solution)
    }

    // -------------------------------------------------------------------------
    // Pack.EXPERT: Mate in 3
    // -------------------------------------------------------------------------

    private fun generateExpert(rng: SplitMix64): Triple<TacticsBoard, Int, List<ChessMove>> {
        val pattern = rng.nextInt(3)
        val (board, solution) = when (pattern) {
            0 -> generateExpertRookLadder(rng)
            1 -> generateExpertCornerRookSac(rng)
            else -> generateExpertAnastasia(rng)
        }
        return Triple(board, 3, solution)
    }

    private fun generateExpertRookLadder(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val variant = rng.nextInt(4)

        val (board, solution) = when (variant) {
            0 -> {
                pieces[2 * 8 + 4] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[1 * 8 + 3] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.PAWN)

                pieces[3 * 8 + 0] = Piece(PieceColor.WHITE, PieceType.ROOK)
                val r2Rows = listOf(4, 5, 6, 7)
                val r2Row = r2Rows[rng.nextInt(r2Rows.size)]
                pieces[r2Row * 8 + 1] = Piece(PieceColor.WHITE, PieceType.ROOK)

                val wkSquares = listOf(Cell(7 * 8 + 7), Cell(7 * 8 + 6), Cell(6 * 8 + 7), Cell(6 * 8 + 6), Cell(5 * 8 + 7))
                val wk = wkSquares[rng.nextInt(wkSquares.size)]
                pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

                val sol = listOf(
                    ChessMove(Cell(r2Row * 8 + 1), Cell(2 * 8 + 1)),
                    ChessMove(Cell(2 * 8 + 4), Cell(1 * 8 + 4)),
                    ChessMove(Cell(3 * 8 + 0), Cell(1 * 8 + 0)),
                    ChessMove(Cell(1 * 8 + 4), Cell(0 * 8 + 4)),
                    ChessMove(Cell(2 * 8 + 1), Cell(0 * 8 + 1)),
                )
                TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null) to sol
            }
            1 -> {
                pieces[2 * 8 + 3] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[1 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 4] = Piece(PieceColor.BLACK, PieceType.PAWN)

                pieces[3 * 8 + 7] = Piece(PieceColor.WHITE, PieceType.ROOK)
                val r2Rows = listOf(4, 5, 6, 7)
                val r2Row = r2Rows[rng.nextInt(r2Rows.size)]
                pieces[r2Row * 8 + 6] = Piece(PieceColor.WHITE, PieceType.ROOK)

                val wkSquares = listOf(Cell(7 * 8 + 0), Cell(7 * 8 + 1), Cell(6 * 8 + 0), Cell(6 * 8 + 1), Cell(5 * 8 + 0))
                val wk = wkSquares[rng.nextInt(wkSquares.size)]
                pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

                val sol = listOf(
                    ChessMove(Cell(r2Row * 8 + 6), Cell(2 * 8 + 6)),
                    ChessMove(Cell(2 * 8 + 3), Cell(1 * 8 + 3)),
                    ChessMove(Cell(3 * 8 + 7), Cell(1 * 8 + 7)),
                    ChessMove(Cell(1 * 8 + 3), Cell(0 * 8 + 3)),
                    ChessMove(Cell(2 * 8 + 6), Cell(0 * 8 + 6)),
                )
                TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null) to sol
            }
            2 -> {
                pieces[2 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 3] = Piece(PieceColor.BLACK, PieceType.PAWN)

                pieces[3 * 8 + 7] = Piece(PieceColor.WHITE, PieceType.ROOK)
                val r2Rows = listOf(4, 5, 6, 7)
                val r2Row = r2Rows[rng.nextInt(r2Rows.size)]
                pieces[r2Row * 8 + 6] = Piece(PieceColor.WHITE, PieceType.ROOK)

                val wkSquares = listOf(Cell(7 * 8 + 0), Cell(6 * 8 + 0), Cell(5 * 8 + 0))
                val wk = wkSquares[rng.nextInt(wkSquares.size)]
                pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

                val sol = listOf(
                    ChessMove(Cell(r2Row * 8 + 6), Cell(2 * 8 + 6)),
                    ChessMove(Cell(2 * 8 + 2), Cell(1 * 8 + 2)),
                    ChessMove(Cell(3 * 8 + 7), Cell(1 * 8 + 7)),
                    ChessMove(Cell(1 * 8 + 2), Cell(0 * 8 + 2)),
                    ChessMove(Cell(2 * 8 + 6), Cell(0 * 8 + 6)),
                )
                TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null) to sol
            }
            else -> {
                pieces[2 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.KING)
                pieces[1 * 8 + 4] = Piece(PieceColor.BLACK, PieceType.PAWN)
                pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)

                pieces[3 * 8 + 0] = Piece(PieceColor.WHITE, PieceType.ROOK)
                val r2Rows = listOf(4, 5, 6, 7)
                val r2Row = r2Rows[rng.nextInt(r2Rows.size)]
                pieces[r2Row * 8 + 1] = Piece(PieceColor.WHITE, PieceType.ROOK)

                val wkSquares = listOf(Cell(7 * 8 + 7), Cell(6 * 8 + 7), Cell(5 * 8 + 7))
                val wk = wkSquares[rng.nextInt(wkSquares.size)]
                pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

                val sol = listOf(
                    ChessMove(Cell(r2Row * 8 + 1), Cell(2 * 8 + 1)),
                    ChessMove(Cell(2 * 8 + 5), Cell(1 * 8 + 5)),
                    ChessMove(Cell(3 * 8 + 0), Cell(1 * 8 + 0)),
                    ChessMove(Cell(1 * 8 + 5), Cell(0 * 8 + 5)),
                    ChessMove(Cell(2 * 8 + 1), Cell(0 * 8 + 1)),
                )
                TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null) to sol
            }
        }
        return board to solution
    }

    private fun generateExpertCornerRookSac(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val (solution, board) = if (isKingside) {
            pieces[6] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[5] = Piece(PieceColor.BLACK, PieceType.ROOK)
            pieces[1 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 7] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[2 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)

            pieces[2 * 8 + 5] = Piece(PieceColor.WHITE, PieceType.BISHOP)
            pieces[7 * 8 + 7] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val r2Cols = listOf(0, 1, 2, 3)
            val r2Col = r2Cols[rng.nextInt(r2Cols.size)]
            pieces[7 * 8 + r2Col] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(
                Cell(5 * 8 + 6),
                Cell(5 * 8 + 5),
                Cell(5 * 8 + 4),
                Cell(6 * 8 + 6),
                Cell(6 * 8 + 5),
                Cell(6 * 8 + 4),
                Cell(4 * 8 + 6),
                Cell(5 * 8 + 3),
                Cell(6 * 8 + 3),
                Cell(7 * 8 + 4),
            )
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val sol = listOf(
                ChessMove(Cell(7 * 8 + 7), Cell(1 * 8 + 7)),
                ChessMove(Cell(0 * 8 + 6), Cell(1 * 8 + 7)),
                ChessMove(Cell(7 * 8 + r2Col), Cell(7 * 8 + 7)),
                ChessMove(Cell(1 * 8 + 7), Cell(0 * 8 + 6)),
                ChessMove(Cell(7 * 8 + 7), Cell(0 * 8 + 7)),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        } else {
            pieces[1] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[2] = Piece(PieceColor.BLACK, PieceType.ROOK)
            pieces[1 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 0] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[2 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)

            pieces[2 * 8 + 2] = Piece(PieceColor.WHITE, PieceType.BISHOP)
            pieces[7 * 8 + 0] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val r2Cols = listOf(4, 5, 6, 7)
            val r2Col = r2Cols[rng.nextInt(r2Cols.size)]
            pieces[7 * 8 + r2Col] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(
                Cell(5 * 8 + 1),
                Cell(5 * 8 + 2),
                Cell(5 * 8 + 3),
                Cell(6 * 8 + 1),
                Cell(6 * 8 + 2),
                Cell(6 * 8 + 3),
                Cell(4 * 8 + 1),
                Cell(5 * 8 + 4),
                Cell(6 * 8 + 4),
                Cell(7 * 8 + 3),
            )
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val sol = listOf(
                ChessMove(Cell(7 * 8 + 0), Cell(1 * 8 + 0)),
                ChessMove(Cell(0 * 8 + 1), Cell(1 * 8 + 0)),
                ChessMove(Cell(7 * 8 + r2Col), Cell(7 * 8 + 0)),
                ChessMove(Cell(1 * 8 + 0), Cell(0 * 8 + 1)),
                ChessMove(Cell(7 * 8 + 0), Cell(0 * 8 + 0)),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        }

        return board to solution
    }

    private fun generateExpertAnastasia(rng: SplitMix64): Pair<TacticsBoard, List<ChessMove>> {
        val pieces = arrayOfNulls<Piece>(64)
        val isKingside = rng.nextInt(2) == 0

        val (solution, board) = if (isKingside) {
            pieces[6] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[5] = Piece(PieceColor.BLACK, PieceType.ROOK)
            pieces[1 * 8 + 5] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 6] = Piece(PieceColor.BLACK, PieceType.PAWN)

            val nStarts = listOf(Cell(4 * 8 + 4), Cell(3 * 8 + 3), Cell(3 * 8 + 5))
            val nFrom = nStarts[rng.nextInt(nStarts.size)]
            pieces[nFrom.index] = Piece(PieceColor.WHITE, PieceType.KNIGHT)

            val qStarts = listOf(Cell(5 * 8 + 3), Cell(6 * 8 + 2), Cell(4 * 8 + 4), Cell(6 * 8 + 3))
            val qFrom = qStarts[rng.nextInt(qStarts.size)]
            pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val rStarts = listOf(Cell(7 * 8 + 0), Cell(7 * 8 + 1), Cell(7 * 8 + 2))
            val rFrom = rStarts[rng.nextInt(rStarts.size)]
            pieces[rFrom.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(Cell(7 * 8 + 4), Cell(6 * 8 + 4), Cell(6 * 8 + 5))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val sol = listOf(
                ChessMove(nFrom, Cell(1 * 8 + 4)),
                ChessMove(Cell(0 * 8 + 6), Cell(0 * 8 + 7)),
                ChessMove(qFrom, Cell(1 * 8 + 7)),
                ChessMove(Cell(0 * 8 + 7), Cell(1 * 8 + 7)),
                ChessMove(rFrom, Cell(7 * 8 + 7)),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        } else {
            pieces[1] = Piece(PieceColor.BLACK, PieceType.KING)
            pieces[2] = Piece(PieceColor.BLACK, PieceType.ROOK)
            pieces[1 * 8 + 2] = Piece(PieceColor.BLACK, PieceType.PAWN)
            pieces[1 * 8 + 1] = Piece(PieceColor.BLACK, PieceType.PAWN)

            val nStarts = listOf(Cell(4 * 8 + 3), Cell(3 * 8 + 4), Cell(3 * 8 + 2))
            val nFrom = nStarts[rng.nextInt(nStarts.size)]
            pieces[nFrom.index] = Piece(PieceColor.WHITE, PieceType.KNIGHT)

            val qStarts = listOf(Cell(5 * 8 + 4), Cell(6 * 8 + 5), Cell(4 * 8 + 3), Cell(6 * 8 + 4))
            val qFrom = qStarts[rng.nextInt(qStarts.size)]
            pieces[qFrom.index] = Piece(PieceColor.WHITE, PieceType.QUEEN)

            val rStarts = listOf(Cell(7 * 8 + 7), Cell(7 * 8 + 6), Cell(7 * 8 + 5))
            val rFrom = rStarts[rng.nextInt(rStarts.size)]
            pieces[rFrom.index] = Piece(PieceColor.WHITE, PieceType.ROOK)

            val wkSquares = listOf(Cell(7 * 8 + 3), Cell(6 * 8 + 3), Cell(6 * 8 + 2))
            val wk = wkSquares[rng.nextInt(wkSquares.size)]
            pieces[wk.index] = Piece(PieceColor.WHITE, PieceType.KING)

            val sol = listOf(
                ChessMove(nFrom, Cell(1 * 8 + 3)),
                ChessMove(Cell(0 * 8 + 1), Cell(0 * 8 + 0)),
                ChessMove(qFrom, Cell(1 * 8 + 0)),
                ChessMove(Cell(0 * 8 + 0), Cell(1 * 8 + 0)),
                ChessMove(rFrom, Cell(7 * 8 + 0)),
            )
            sol to TacticsBoard(pieces.toList(), PieceColor.WHITE, CastlingRights(), null)
        }

        return board to solution
    }

    // -------------------------------------------------------------------------
    // Legality Checks
    // -------------------------------------------------------------------------

    private fun isLegalTacticsPosition(board: TacticsBoard, sideToMove: PieceColor): Boolean {
        if (board.isCheck(sideToMove.opponent())) return false

        // Pawns cannot be placed on the first or last rank
        for (col in 0..7) {
            val p0 = board.pieceAt(0, col)
            if (p0 != null && p0.type == PieceType.PAWN) return false
            val p7 = board.pieceAt(7, col)
            if (p7 != null && p7.type == PieceType.PAWN) return false
        }

        // Kings cannot touch
        val wk = board.kingCell(PieceColor.WHITE)
        val bk = board.kingCell(PieceColor.BLACK)
        val dr = kotlin.math.abs(wk.index / 8 - bk.index / 8)
        val dc = kotlin.math.abs(wk.index % 8 - bk.index % 8)
        if (dr <= 1 && dc <= 1) return false

        return true
    }

    private fun <T> List<T>.shuffled(rng: SplitMix64): List<T> {
        val list = ArrayList(this)
        for (i in list.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}
