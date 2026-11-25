package com.datathecodie.witgrid.engine.tactics.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.tactics.ChessMove
import com.datathecodie.witgrid.engine.tactics.TacticsBoard
import com.datathecodie.witgrid.engine.tactics.TacticsPuzzle

/**
 * Hand-made Chess Tactics positions for the spec tests.
 *
 * Each position was checked by hand against FIDE movement. The comments give the
 * mating line, so a reader can verify the expected results without running the engine.
 */
object TacticsSpecFixtures {

    /** Mate in 1. Qg7 is mate, protected by Kf6. No other first move mates. */
    const val MATE_IN_ONE_FEN = "7k/3Q4/5K2/8/8/8/8/8 w - - 0 1"

    /** Two mates in 1: Qg7# (protected by Kg6) and Qe8# (the rank covers g8, Kg6 covers g7 and h7). */
    const val TWO_MATES_IN_ONE_FEN = "7k/4Q3/6K1/8/8/8/8/8 w - - 0 1"

    /** Mate in 2 with one defence. After Qd8+ only Nxd8 is legal, then Re8 is mate. */
    const val MATE_IN_TWO_FEN = "3r2k1/5ppp/2n5/8/2B5/3Q4/8/4R1K1 w - - 0 1"

    /** Mate in 2 with two winning first moves: Qxd8+ and Qd8+ from a5 both lead to Qxd8 mate. */
    const val MATE_IN_TWO_TWO_OPENERS_FEN = "3r2k1/5ppp/2n5/Q7/8/8/8/3Q2K1 w - - 0 1"

    /**
     * Mate in 2 where the first move is unique but the second move has three mates:
     * after Qf8+ Kh7, Qg7#, Qg8# and Rg7# all mate.
     */
    const val MATE_IN_TWO_SECOND_MOVE_DOUBLE_FEN = "7k/5K2/7p/8/8/Q7/2r5/4n1R1 w - - 0 1"

    /** Promotion mate: a8=Q is mate on the back rank, with Kg6 covering g7 and h7. */
    const val PROMOTION_MATE_FEN = "7k/P7/6K1/8/8/8/8/8 w - - 0 1"

    /** No mate in 1 exists. A quiet pawn push is the only line in the test. */
    const val NO_MATE_FEN = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"

    /** Castling position. Both sides keep all castling rights. */
    const val CASTLING_FEN = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"

    /** Black rook on f2 attacks f1, so White may not castle kingside. Queenside is still legal. */
    const val CASTLING_THROUGH_ATTACK_FEN = "r3k2r/8/8/8/8/8/5r2/R3K2R w KQkq - 0 1"

    /** Black has just played d7-d5, so e5xd6 en passant is legal. */
    const val EN_PASSANT_FEN = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3"

    /** Same position with no en passant target. e5xd6 is then illegal. */
    const val NO_EN_PASSANT_FEN = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3"

    /** Pawn on a7 can promote on a8. */
    const val PROMOTION_FEN = "8/P7/5k2/8/8/5K2/8/8 w - - 0 1"

    /** Black is in check from Re1 along the e-file. Black has legal king moves. */
    const val CHECK_FEN = "4k3/8/8/8/8/8/8/4R1K1 b - - 0 1"

    /** Back-rank checkmate. Ra8 mates Kg8 because the pawns on f7, g7 and h7 block the king. */
    const val BACK_RANK_MATE_FEN = "R5k1/5ppp/8/8/8/8/8/6K1 b - - 0 1"

    /** Stalemate. Black king a8 has no legal move, and Qb6 does not check it. */
    const val STALEMATE_FEN = "k7/8/1Q6/8/8/8/8/7K b - - 0 1"

    /** Start position, used to check the en passant target after a double push. */
    const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun board(fen: String): TacticsBoard = TacticsBoard.fromFen(fen)

    fun move(uci: String): ChessMove = ChessMove.fromUci(uci)

    fun puzzle(
        id: String,
        fen: String,
        mateIn: Int,
        line: List<String>,
        pack: Pack,
        grade: String,
    ): TacticsPuzzle {
        val start = board(fen)
        return TacticsPuzzle(
            id = id,
            initialBoard = start,
            sideToMove = start.sideToMove,
            mateIn = mateIn,
            solutionLine = line.map { move(it) },
            pack = pack,
            storedGrade = grade,
        )
    }

    fun mateInOneUnique(): TacticsPuzzle =
        puzzle("spec-mate1-unique", MATE_IN_ONE_FEN, 1, listOf("d7g7"), Pack.EASY, "checkmate:1")

    fun mateInOneWithSecondMate(): TacticsPuzzle =
        puzzle("spec-mate1-two", TWO_MATES_IN_ONE_FEN, 1, listOf("e7g7"), Pack.EASY, "checkmate:1")

    fun mateInTwoForcedDefence(): TacticsPuzzle =
        puzzle(
            "spec-mate2-forced", MATE_IN_TWO_FEN, 2,
            listOf("d3d8", "c6d8", "e1e8"), Pack.MEDIUM, "back-rank-mate:2",
        )

    fun mateInTwoTwoOpeners(): TacticsPuzzle =
        puzzle(
            "spec-mate2-two-openers", MATE_IN_TWO_TWO_OPENERS_FEN, 2,
            listOf("d1d8", "c6d8", "a5d8"), Pack.MEDIUM, "back-rank-mate:2",
        )

    fun mateInTwoSecondMoveDouble(): TacticsPuzzle =
        puzzle(
            "spec-mate2-second-double", MATE_IN_TWO_SECOND_MOVE_DOUBLE_FEN, 2,
            listOf("a3f8", "h8h7", "f8g7"), Pack.MEDIUM, "checkmate:2",
        )

    fun promotionMate(): TacticsPuzzle =
        puzzle("spec-promotion-mate", PROMOTION_MATE_FEN, 1, listOf("a7a8"), Pack.EASY, "checkmate:1")

    fun noMateQuietLine(): TacticsPuzzle =
        puzzle("spec-no-mate", NO_MATE_FEN, 1, listOf("e2e4"), Pack.EASY, "none:1")
}
