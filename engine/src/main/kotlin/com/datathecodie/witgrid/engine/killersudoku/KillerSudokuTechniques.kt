package com.datathecodie.witgrid.engine.killersudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Killer Sudoku technique IDs, display names, and relative difficulty ranks. */
internal object KillerSudokuTechniques {
    val NAKED_SINGLE = Technique("naked-single", "Naked single", 0)
    val HIDDEN_SINGLE = Technique("hidden-single", "Hidden single", 1)
    val CAGE_COMBINATIONS = Technique("cage-combinations", "Cage combinations", 2)
    val RULE_45 = Technique("rule-45", "45 rule", 3)
    val LOCKED_CANDIDATES = Technique("locked-candidates", "Locked candidates", 4)
    val NAKED_PAIR = Technique("naked-pair", "Naked pair", 5)
    val INNIE_OUTIE = Technique("innie-outie", "Innie/outie", 6)
    val HIDDEN_PAIR = Technique("hidden-pair", "Hidden pair", 7)
    val NAKED_TRIPLE = Technique("naked-triple", "Naked triple", 8)
    val X_WING = Technique("x-wing", "X-Wing", 9)

    const val MAX_RANK: Int = 9

    val all: List<Technique> = listOf(
        NAKED_SINGLE,
        HIDDEN_SINGLE,
        CAGE_COMBINATIONS,
        RULE_45,
        LOCKED_CANDIDATES,
        NAKED_PAIR,
        INNIE_OUTIE,
        HIDDEN_PAIR,
        NAKED_TRIPLE,
        X_WING,
    )

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique and hint-count metadata in Killer Sudoku bank lines. */
object KillerSudokuGradeCodec {
    /** Encodes the hardest technique and total hint count; a solved board uses `none:0`. */
    fun encode(grade: Grade): String {
        require(grade.hardestTechnique == null || KillerSudokuTechniques.byId(grade.hardestTechnique.id) != null) {
            "Unknown Killer Sudoku technique ID"
        }
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Killer Sudoku grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Killer Sudoku grade step count" }
        require(stepCount >= 0) { "Killer Sudoku grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Killer Sudoku grade must use the none technique" }
        } else {
            require(KillerSudokuTechniques.byId(fields[0]) != null) { "Unknown Killer Sudoku technique ID" }
        }
    }
}

/**
 * Pack bands use the hardest technique required by the puzzle and a minimum number of placements.
 * Easy uses singles and cage combinations; Medium requires the 45 rule;
 * Hard requires locked candidates or naked pairs;
 * Expert requires innie/outie, hidden pairs, naked triples, or X-Wing.
 */
object KillerSudokuPackBands {
    /** Minimum placements in a generated level to keep it from collapsing into a trivial board. */
    const val MINIMUM_HINT_STEP_COUNT: Int = 8

    /** Returns whether [grade] uses the technique level assigned to [pack]. */
    fun isGradeInPackBand(pack: Pack, grade: Grade): Boolean {
        if (grade.pack != pack || grade.stepCount < MINIMUM_HINT_STEP_COUNT) return false
        val rank = grade.hardestTechnique?.difficultyRank ?: return false
        return when (pack) {
            Pack.EASY -> rank <= KillerSudokuTechniques.CAGE_COMBINATIONS.difficultyRank
            Pack.MEDIUM -> rank == KillerSudokuTechniques.RULE_45.difficultyRank
            Pack.HARD -> rank == KillerSudokuTechniques.LOCKED_CANDIDATES.difficultyRank ||
                rank == KillerSudokuTechniques.NAKED_PAIR.difficultyRank
            Pack.EXPERT -> rank >= KillerSudokuTechniques.INNIE_OUTIE.difficultyRank
        }
    }
}
