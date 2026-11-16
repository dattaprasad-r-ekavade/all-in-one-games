package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Binary technique IDs, display names and relative difficulty ranks. */
internal object BinaryTechniques {
    val PAIR_CAP = Technique("pair-cap", "Pair cap", 0)
    val GAP_FILL = Technique("gap-fill", "Gap fill", 1)
    val COUNT_COMPLETION = Technique("count-completion", "Count completion", 2)
    val LINE_COMPARISON = Technique("line-comparison", "Row and column comparison", 3)

    val all: List<Technique> = listOf(PAIR_CAP, GAP_FILL, COUNT_COMPLETION, LINE_COMPARISON)

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique/step-count metadata stored in Binary bank lines. */
object BinaryGradeCodec {
    /** Encodes the hardest technique ID and total hint count; a solved-in-place board uses `none:0`. */
    fun encode(grade: Grade): String {
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        require(grade.hardestTechnique == null || BinaryTechniques.byId(techniqueId) != null) {
            "Unknown Binary technique ID"
        }
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Binary grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Binary grade step count" }
        require(stepCount >= 0) { "Binary grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Binary grade must use the none technique" }
        } else {
            require(BinaryTechniques.byId(fields[0]) != null) { "Unknown Binary technique ID" }
        }
    }
}
