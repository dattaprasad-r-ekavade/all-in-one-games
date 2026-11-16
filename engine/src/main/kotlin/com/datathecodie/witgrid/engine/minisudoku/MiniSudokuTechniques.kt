package com.datathecodie.witgrid.engine.minisudoku

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Mini Sudoku technique IDs, names and relative difficulty ranks. */
internal object MiniSudokuTechniques {
    val NAKED_SINGLE = Technique("naked-single", "Naked single", 0)
    val HIDDEN_SINGLE = Technique("hidden-single", "Hidden single", 1)
    val LOCKED_CANDIDATES = Technique("locked-candidates", "Locked candidates", 2)
    val NAKED_PAIR = Technique("naked-pair", "Naked pair", 3)

    val all: List<Technique> = listOf(NAKED_SINGLE, HIDDEN_SINGLE, LOCKED_CANDIDATES, NAKED_PAIR)

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique and hint-count metadata in Mini Sudoku bank lines. */
object MiniSudokuGradeCodec {
    /** Encodes the hardest technique and total hint count; a solved board uses `none:0`. */
    fun encode(grade: Grade): String {
        require(grade.hardestTechnique == null || MiniSudokuTechniques.byId(grade.hardestTechnique.id) != null) {
            "Unknown Mini Sudoku technique ID"
        }
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Mini Sudoku grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Mini Sudoku grade step count" }
        require(stepCount >= 0) { "Mini Sudoku grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Mini Sudoku grade must use the none technique" }
        } else {
            require(MiniSudokuTechniques.byId(fields[0]) != null) { "Unknown Mini Sudoku technique ID" }
        }
    }
}

/** Pack difficulty bands use the hardest technique required by the puzzle. */
object MiniSudokuPackBands {
    /** Returns whether [grade] uses the technique level assigned to [pack]. */
    fun isGradeInPackBand(pack: Pack, grade: Grade): Boolean {
        if (grade.pack != pack) return false
        val expectedRank = when (pack) {
            Pack.EASY -> MiniSudokuTechniques.NAKED_SINGLE.difficultyRank
            Pack.MEDIUM -> MiniSudokuTechniques.HIDDEN_SINGLE.difficultyRank
            Pack.HARD -> MiniSudokuTechniques.LOCKED_CANDIDATES.difficultyRank
            Pack.EXPERT -> MiniSudokuTechniques.NAKED_PAIR.difficultyRank
        }
        return grade.hardestTechnique?.difficultyRank == expectedRank
    }
}
