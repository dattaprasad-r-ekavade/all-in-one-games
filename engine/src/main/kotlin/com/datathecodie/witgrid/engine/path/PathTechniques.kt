package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Path technique IDs, display names and relative difficulty ranks. */
internal object PathTechniques {
    val ENDPOINT_EXTENSION = Technique("endpoint-extension", "Endpoint extension", 0)
    val FORCED_NEIGHBOUR = Technique("forced-neighbour", "Forced neighbour", 1)
    val DEAD_END_AVOIDANCE = Technique("dead-end-avoidance", "Dead-end avoidance", 2)
    val LAST_EMPTY_CELL = Technique("last-empty-cell", "Last empty cell in a region", 3)

    val all: List<Technique> = listOf(
        ENDPOINT_EXTENSION,
        FORCED_NEIGHBOUR,
        DEAD_END_AVOIDANCE,
        LAST_EMPTY_CELL,
    )

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique/step-count metadata stored in Path bank lines. */
object PathGradeCodec {
    /** Encodes the hardest technique ID and total hint count; a solved-in-place board uses `none:0`. */
    fun encode(grade: Grade): String {
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        require(grade.hardestTechnique == null || PathTechniques.byId(techniqueId) != null) {
            "Unknown Path technique ID"
        }
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Path grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Path grade step count" }
        require(stepCount >= 0) { "Path grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Path grade must use the none technique" }
        } else {
            require(PathTechniques.byId(fields[0]) != null) { "Unknown Path grade technique ID" }
        }
    }
}
