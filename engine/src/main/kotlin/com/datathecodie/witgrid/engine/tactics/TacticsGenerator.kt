package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Builds deterministic positions from mating patterns and proves each resulting line. */
object TacticsGenerator {
    private const val MAX_PUZZLE_ATTEMPTS = 5_000

    private val easyPatterns = listOf(
        // Back-rank mate with a pawn shield.
        Pattern(
            "k g8", "p f7", "p g7", "p h7", "R a1 a2 a3 b1 b2 b3 c1 c2 c3 d1 d2 e1",
            "K a1 b1 c1 d1 e1 f1 g1 h1 a2 b2 c2 d2 e2 f2 g2 h2",
        ),
        // Smothered mate and Arabian mate.
        Pattern(
            "k h8", "r g8", "p g7", "p h7", "N d8 d6 e5 g5 h6",
            "K a1 b1 c1 d1 e1 f1 g1 h1 a2 b2 c2 d2 e2 f2 g2 h2",
        ),
        Pattern(
            "k h8", "N f6", "R a7 b7 c7 d7 e7",
            "K a1 b1 c1 d1 e1 f1 g1 h1 a2 b2 c2 d2 e2 f2 g2 h2",
        ),
    )

    private val mediumPatterns = listOf(
        // The queen draws the king onto the rook's mating rank.
        Pattern(
            "k g8", "r f8", "p f7", "p g7", "p h7", "B g5 h4", "Q a3 b4 c5 d6",
            "R a1 a2 b1 b2 c1 c2 d1 d2 e1 e2", "K a1 b1 c1 e1 f1 g1 h1 a2 b2 g2 h2 f3 g3 h3",
        ),
        // The queen draws a rook onto the king's only free square.
        Pattern(
            "k h8", "r f8", "p g7", "p h7", "N g5 e5 d6 h6", "Q d5 e6 f7",
            "B a2 b3 c4 d5 e6", "K a1 b1 c1 d1 e1 f1 g1 h1 a2 b2 c2 d2 e2 f2 g2 h2",
        ),
    )

    private val hardPatterns = listOf(
        // A bishop guards h7 against the tempting rook check, leaving the queen sacrifice.
        Pattern(
            "k h8", "r f8", "p g7", "p h7", "N d6 g5", "Q d5 e6 f7", "B a2 b3 c4 d5 e6",
            "R h1 h2 h3 h4", "b b1 c2 d3 e4 f5 g8", "K a1 b1 c1 d1 e1 f1 g1 a2 b2 c2 d2 e2 f2 g2",
        ),
        // Multiple checks compete, but only the queen sacrifice diverts the back-rank guard.
        Pattern(
            "k g8", "r d8", "p f7", "p g7", "p h7", "n c6 b7 e6 f7", "B c4 b3 a2 d5",
            "Q d1 d2 d3 d4 d5", "R e1 e2 e3 e4", "K a1 b1 c1 f1 g1 h1 a2 b2 c2 f2 g2 h2",
        ),
    )

    private val expertPatterns = listOf(
        // The knight clears the queen's diagonal with double check, setting up smothered mate.
        Pattern(
            "k g8", "r f8", "p g7", "p h7", "N f7", "Q a2 b3 c4 d5 e6",
            "K a1 b1 c1 d1 e1 f1 g1 h1 a2 b2 c2 d2 e2 f2 g2 h2",
        ),
        // Taking h7 opens the file for the other rook; all replies to the capture must be covered.
        Pattern(
            "k g8", "r f8", "p f7", "p h7", "p g6", "B f6", "R h1 h2 h3 h4 h5",
            "R a1 b1 c1 d1 e1 f1 a2 b2 c2 d2 e2 f2 a3 b3 c3 d3 e3 f3",
            "K a1 b1 c1 d1 e1 f1 g1 a2 b2 c2 d2 e2 f2 g2 a3 b3 c3 d3 e3 f3 g3",
        ),
    )

    private val upperPawnSquares = squares("a7 b7 c7 d7 e7 a6 b6 c6 d6 e6 a5 b5 c5 d5 a4 b4 c4")
    private val lowerPawnSquares = squares("a2 b2 c2 d2 e2 f2 g2 h2 a3 b3 c3 d3 e3 f3 g3 h3 a4 b4 c4")

    /** Builds one puzzle in [pack], rejecting any starting board present in [seenPositions]. */
    fun generate(
        rng: SplitMix64,
        pack: Pack,
        seenPositions: Set<TacticsBoard> = emptySet(),
    ): TacticsPuzzle {
        val (mateIn, patterns) = when (pack) {
            Pack.EASY -> 1 to easyPatterns
            Pack.MEDIUM -> 2 to mediumPatterns
            Pack.HARD -> 2 to hardPatterns
            Pack.EXPERT -> 3 to expertPatterns
        }

        repeat(MAX_PUZZLE_ATTEMPTS) {
            val pieces = patterns[rng.nextInt(patterns.size)].place(rng) ?: return@repeat
            varyMaterial(pieces, pack, rng)
            if (rng.nextInt(2) == 0) {
                for (row in 0..7) {
                    pieces.subList(row * 8, row * 8 + 8).reverse()
                }
            }
            val board = TacticsBoard(pieces, PieceColor.WHITE, CastlingRights(), null)
            if (board in seenPositions) return@repeat
            if (board.isCheck(board.sideToMove.opponent())) return@repeat
            if (pack == Pack.HARD || pack == Pack.MEDIUM) {
                val checks = TacticsPackBands.openingChecks(board)
                if ((checks >= TacticsPackBands.HARD_MIN_CHECKS) != (pack == Pack.HARD)) return@repeat
            }

            val line = TacticsRules.findUniqueLine(board, mateIn) ?: return@repeat
            val grade = TacticsTechniqueSolver.gradeLine(board, line, pack) ?: return@repeat
            val id = "CT-${pack.name}-${rng.nextLong().toULong().toString(16).padStart(16, '0')}"
            return TacticsPuzzle(id, board, board.sideToMove, mateIn, line, pack, TacticsGradeCodec.encode(grade))
        }

        error("Could not generate a technique-solvable Tactics puzzle for $pack after $MAX_PUZZLE_ATTEMPTS attempts")
    }

    /**
     * Builds a pack of [count] distinct technique-solvable puzzles for [pack], ensuring
     * no repeated starting positions.
     */
    fun generatePack(
        rng: SplitMix64,
        pack: Pack,
        count: Int = 241,
    ): List<TacticsPuzzle> {
        require(count > 0) { "Pack puzzle count must be positive" }
        val seen = HashSet<TacticsBoard>(count)
        val puzzles = ArrayList<TacticsPuzzle>(count)
        repeat(count) {
            val puzzle = generate(rng, pack, seen)
            seen.add(puzzle.initialBoard)
            puzzles.add(puzzle)
        }
        return puzzles
    }

    private fun varyMaterial(pieces: MutableList<Piece?>, pack: Pack, rng: SplitMix64) {
        val minimumExtra = when (pack) {
            Pack.EASY, Pack.HARD, Pack.EXPERT -> 3
            Pack.MEDIUM -> 1
        }
        val maximumExtra = when (pack) {
            Pack.EASY, Pack.HARD, Pack.EXPERT -> 6
            Pack.MEDIUM -> TacticsPackBands.MAX_MEDIUM_PIECES - pieces.count { it != null }
        }
        val extra = minimumExtra + rng.nextInt(maximumExtra - minimumExtra + 1)
        repeat(extra) { index ->
            val color = if (index % 2 == 0) PieceColor.BLACK else PieceColor.WHITE
            val available = (if (color == PieceColor.BLACK) upperPawnSquares else lowerPawnSquares)
                .filter { pieces[it] == null }
            pieces[available[rng.nextInt(available.size)]] = Piece(color, PieceType.PAWN)
        }
    }

    /** Each slot is a FEN piece followed by the squares from which it can play its role. */
    private class Pattern(vararg placements: String) {
        private val slots = placements.map { placement ->
            requireNotNull(Piece.fromChar(placement.first())) to squares(placement.substring(2))
        }

        fun place(rng: SplitMix64): MutableList<Piece?>? {
            val pieces = MutableList<Piece?>(64) { null }
            for ((piece, options) in slots) {
                val available = options.filter { pieces[it] == null }
                if (available.isEmpty()) return null
                pieces[available[rng.nextInt(available.size)]] = piece
            }
            return pieces
        }
    }

    private fun squares(text: String): List<Int> = text.split(' ').map { squareToCell(it).index }
}
