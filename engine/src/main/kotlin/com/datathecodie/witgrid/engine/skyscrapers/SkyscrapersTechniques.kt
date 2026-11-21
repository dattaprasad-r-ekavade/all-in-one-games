package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Skyscrapers technique IDs, display names and relative difficulty ranks. */
object SkyscrapersTechniques {
    val CLUE_1_AND_N = Technique("clue-1-and-n", "Clue 1 and clue n on an edge", 0)
    val NAKED_SINGLE = Technique("naked-single", "Naked single", 1)
    val HIDDEN_SINGLE = Technique("hidden-single", "Hidden single", 2)
    val HEIGHT_BOUNDS = Technique("height-bounds", "Height bounds from clues", 3)

    val all: List<Technique> = listOf(CLUE_1_AND_N, NAKED_SINGLE, HIDDEN_SINGLE, HEIGHT_BOUNDS)

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique and hint-count metadata in Skyscrapers bank lines. */
object SkyscrapersGradeCodec {
    /** Encodes the hardest technique and total hint count; a solved board uses `none:0`. */
    fun encode(grade: Grade): String {
        require(grade.hardestTechnique == null || SkyscrapersTechniques.byId(grade.hardestTechnique.id) != null) {
            "Unknown Skyscrapers technique ID"
        }
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Skyscrapers grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Skyscrapers grade step count" }
        require(stepCount >= 0) { "Skyscrapers grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Skyscrapers grade must use the none technique" }
        } else {
            require(SkyscrapersTechniques.byId(fields[0]) != null) { "Unknown Skyscrapers technique ID" }
        }
    }
}
