package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Futoshiki technique IDs, display names and relative difficulty ranks. */
object FutoshikiTechniques {
    val NAKED_SINGLE = Technique("naked-single", "Naked single", 0)
    val HIDDEN_SINGLE = Technique("hidden-single", "Hidden single", 1)
    val SIGN_BOUNDS = Technique("sign-bounds", "Sign bounds", 2)
    val SIGN_CHAINS = Technique("sign-chains", "Sign chains", 3)
    val NAKED_PAIR = Technique("naked-pair", "Naked pair", 4)

    val all: List<Technique> = listOf(
        NAKED_SINGLE,
        HIDDEN_SINGLE,
        SIGN_BOUNDS,
        SIGN_CHAINS,
        NAKED_PAIR,
    )

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique and hint-count metadata in Futoshiki bank lines. */
object FutoshikiGradeCodec {
    /** Encodes the hardest technique and total hint count; a solved board uses `none:0`. */
    fun encode(grade: Grade): String {
        require(grade.hardestTechnique == null || FutoshikiTechniques.byId(grade.hardestTechnique.id) != null) {
            "Unknown Futoshiki technique ID"
        }
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Futoshiki grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Futoshiki grade step count" }
        require(stepCount >= 0) { "Futoshiki grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Futoshiki grade must use the none technique" }
        } else {
            require(FutoshikiTechniques.byId(fields[0]) != null) { "Unknown Futoshiki technique ID" }
        }
    }
}
