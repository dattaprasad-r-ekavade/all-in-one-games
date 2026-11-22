package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/**
 * Stable technique definitions for Chess Tactics.
 *
 * Ranks represent relative difficulty within the Chess Tactics lane, from obvious checks
 * up to specific tactical mating motifs.
 */
object TacticsTechniques {
    val CHECK = Technique("check", "Check", 0)
    val CAPTURE = Technique("capture", "Forced capture", 1)
    val FORCES_MATE = Technique("forces-mate", "Forces mate", 2)
    val CHECKMATE = Technique("checkmate", "Checkmate", 3)
    val BACK_RANK_MATE = Technique("back-rank-mate", "Back-rank mate", 4)
    val SMOTHERED_MATE = Technique("smothered-mate", "Smothered mate", 5)
    val ANASTASIA_MATE = Technique("anastasia-mate", "Anastasia's mate", 6)
    val ARABIAN_MATE = Technique("arabian-mate", "Arabian mate", 7)

    val all: List<Technique> = listOf(
        CHECK,
        CAPTURE,
        FORCES_MATE,
        CHECKMATE,
        BACK_RANK_MATE,
        SMOTHERED_MATE,
        ANASTASIA_MATE,
        ARABIAN_MATE,
    )

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/**
 * Pack bands for Chess Tactics based on mate-in-N horizon and initial board piece count.
 *
 * As specified in development plan section 12.6:
 * - Easy: Mate in 1
 * - Medium: Mate in 2 with fewer pieces (up to [MAX_MEDIUM_PIECES])
 * - Hard: Mate in 2 with more pieces (exceeding [MAX_MEDIUM_PIECES])
 * - Expert: Mate in 3
 */
object TacticsPackBands {
    /** Maximum piece count for a Medium mate-in-2; boards with more pieces are categorized as Hard. */
    const val MAX_MEDIUM_PIECES: Int = 10

    /** Minimum opening checks required for a Hard mate-in-2 puzzle. */
    const val HARD_MIN_CHECKS: Int = 3

    /** Returns opening checks available to side to move from initial board. */
    fun openingChecks(board: TacticsBoard): Int =
        board.legalMoves().count { board.applyMoveInternal(it).isCheck() }

    /** Returns the canonical pack band for a puzzle with [mateIn] horizon and [pieceCount] pieces. */
    fun packFor(mateIn: Int, pieceCount: Int): Pack = when (mateIn) {
        1 -> Pack.EASY
        2 -> if (pieceCount <= MAX_MEDIUM_PIECES) Pack.MEDIUM else Pack.HARD
        3 -> Pack.EXPERT
        else -> throw IllegalArgumentException("Unsupported Tactics mate horizon: $mateIn")
    }

    /** Returns whether [puzzle] has a pack band consistent with its mate horizon, piece count, and checks. */
    fun isPuzzleInPackBand(puzzle: TacticsPuzzle): Boolean {
        val pieceCount = puzzle.initialBoard.pieces.count { it != null }
        val checks = openingChecks(puzzle.initialBoard)
        if (puzzle.pack == Pack.HARD && checks < HARD_MIN_CHECKS) return false
        return try {
            puzzle.pack == packFor(puzzle.mateIn, pieceCount)
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}

/**
 * Serializer and validator for technique grade metadata stored in bank lines.
 */
object TacticsGradeCodec {
    /** Encodes the hardest technique and step count as "technique-id:stepCount". */
    fun encode(grade: Grade): String {
        require(grade.hardestTechnique == null || TacticsTechniques.byId(grade.hardestTechnique.id) != null) {
            "Unknown Tactics technique ID: ${grade.hardestTechnique?.id}"
        }
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }

    /** Validates that [value] conforms to the "technique-id:stepCount" format with known techniques. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Tactics grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Tactics grade step count" }
        require(stepCount >= 0) { "Tactics grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Tactics grade must use the none technique" }
        } else {
            require(TacticsTechniques.byId(fields[0]) != null) { "Unknown Tactics technique ID: ${fields[0]}" }
        }
    }
}
