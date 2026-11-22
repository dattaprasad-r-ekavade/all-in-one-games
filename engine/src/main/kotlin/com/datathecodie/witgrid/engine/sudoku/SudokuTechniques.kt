package com.datathecodie.witgrid.engine.sudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Sudoku technique IDs, names and relative difficulty ranks. */
internal object SudokuTechniques {
    val NAKED_SINGLE = Technique("naked-single", "Naked single", 0)
    val HIDDEN_SINGLE = Technique("hidden-single", "Hidden single", 1)
    val LOCKED_CANDIDATES = Technique("locked-candidates", "Locked candidates", 2)
    val NAKED_PAIR = Technique("naked-pair", "Naked pair", 3)
    val HIDDEN_PAIR = Technique("hidden-pair", "Hidden pair", 4)
    val NAKED_TRIPLE = Technique("naked-triple", "Naked triple", 5)
    val X_WING = Technique("x-wing", "X-Wing", 6)

    val all: List<Technique> = listOf(
        NAKED_SINGLE,
        HIDDEN_SINGLE,
        LOCKED_CANDIDATES,
        NAKED_PAIR,
        HIDDEN_PAIR,
        NAKED_TRIPLE,
        X_WING,
    )

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique and hint-count metadata in Sudoku bank lines. */
object SudokuGradeCodec {
    /** Encodes the hardest technique and total hint count; a solved board uses `none:0`. */
    fun encode(grade: Grade): String {
        require(grade.hardestTechnique == null || SudokuTechniques.byId(grade.hardestTechnique.id) != null) {
            "Unknown Sudoku technique ID"
        }
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Sudoku grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Sudoku grade step count" }
        require(stepCount >= 0) { "Sudoku grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Sudoku grade must use the none technique" }
        } else {
            require(SudokuTechniques.byId(fields[0]) != null) { "Unknown Sudoku technique ID" }
        }
    }
}

/** Pack bands use the hardest technique required by the puzzle and a minimum number of placements. */
object SudokuPackBands {
    /** Minimum placements in a generated level to keep it from collapsing into a trivial board. */
    const val MINIMUM_HINT_STEP_COUNT: Int = 8

    /** Returns whether [grade] uses the technique level assigned to [pack]. */
    fun isGradeInPackBand(pack: Pack, grade: Grade): Boolean {
        if (grade.pack != pack || grade.stepCount < MINIMUM_HINT_STEP_COUNT) return false
        val rank = grade.hardestTechnique?.difficultyRank ?: return false
        return when (pack) {
            Pack.EASY -> rank == SudokuTechniques.NAKED_SINGLE.difficultyRank
            Pack.MEDIUM -> rank == SudokuTechniques.HIDDEN_SINGLE.difficultyRank
            Pack.HARD -> rank == SudokuTechniques.LOCKED_CANDIDATES.difficultyRank ||
                rank == SudokuTechniques.NAKED_PAIR.difficultyRank
            Pack.EXPERT -> rank == SudokuTechniques.HIDDEN_PAIR.difficultyRank ||
                rank == SudokuTechniques.NAKED_TRIPLE.difficultyRank ||
                rank == SudokuTechniques.X_WING.difficultyRank
        }
    }
}
