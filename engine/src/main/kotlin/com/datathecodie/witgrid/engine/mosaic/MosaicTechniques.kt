package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Mosaic technique IDs, display names and relative difficulty ranks. */
internal object MosaicTechniques {
    val EMPTY_AREA = Technique("empty-area", "Empty area", 0)
    val FULL_AREA = Technique("full-area", "Full area", 1)
    val REMAINING_COUNT = Technique("remaining-count", "Remaining count", 2)
    val CLUE_OVERLAP = Technique("clue-overlap", "Clue overlap", 3)

    val all: List<Technique> = listOf(EMPTY_AREA, FULL_AREA, REMAINING_COUNT, CLUE_OVERLAP)

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique and step-count metadata stored in Mosaic bank lines. */
object MosaicGradeCodec {
    /** Encodes the hardest technique ID and total hint count; a zero-step solve uses `none:0`. */
    fun encode(grade: Grade): String {
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        require(grade.hardestTechnique == null || MosaicTechniques.byId(techniqueId) != null) {
            "Unknown Mosaic technique ID"
        }
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Mosaic grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Mosaic grade step count" }
        require(stepCount >= 0) { "Mosaic grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Mosaic grade must use the none technique" }
        } else {
            require(MosaicTechniques.byId(fields[0]) != null) { "Unknown Mosaic technique ID" }
        }
    }
}
