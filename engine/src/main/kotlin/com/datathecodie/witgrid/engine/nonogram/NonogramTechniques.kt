package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Nonogram technique IDs, display names and relative difficulty ranks. */
internal object NonogramTechniques {
    val OVERLAP = Technique("overlap", "Overlap", 0)
    val EDGE_RULES = Technique("edge-rules", "Edge rules", 1)
    val LINE_BY_LINE = Technique("line-by-line", "Line by line", 2)

    val all: List<Technique> = listOf(OVERLAP, EDGE_RULES, LINE_BY_LINE)

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Encodes and validates the technique/line-pass metadata stored in Nonogram bank lines. */
object NonogramGradeCodec {
    /** Encodes the hardest rule and full line-pass count; a zero-step grade uses `none:0`. */
    fun encode(grade: Grade): String {
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        require(grade.hardestTechnique == null || NonogramTechniques.byId(techniqueId) != null) {
            "Unknown Nonogram technique ID"
        }
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed pass counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Nonogram grade must be technique-id:line-passes" }
        val passCount = requireNotNull(fields[1].toIntOrNull()) {
            "Invalid Nonogram line-pass count"
        }
        require(passCount >= 0) { "Nonogram line-pass count must not be negative" }
        if (passCount == 0) {
            require(fields[0] == "none") { "A zero-pass Nonogram grade must use the none technique" }
        } else {
            require(NonogramTechniques.byId(fields[0]) != null) { "Unknown Nonogram technique ID" }
        }
    }
}
