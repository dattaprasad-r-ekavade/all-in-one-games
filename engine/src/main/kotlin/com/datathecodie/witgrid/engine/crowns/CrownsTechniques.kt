package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.Technique

/** Stable Crowns technique IDs, display names and relative difficulty ranks. */
internal object CrownsTechniques {
    val TOUCH_EXCLUSION = Technique("touch-exclusion", "Touch exclusion", 0)
    val REGION_LINE = Technique("region-line", "Region confined to one line", 1)
    val K_REGIONS_K_LINES = Technique("k-regions-k-lines", "k regions in k lines", 2)

    val all: List<Technique> = listOf(TOUCH_EXCLUSION, REGION_LINE, K_REGIONS_K_LINES)

    fun byId(id: String): Technique? = all.firstOrNull { it.id == id }
}

/** Crowns pack-to-size bands from the product plan. */
object CrownsPackBands {
    /** Returns the plan-defined pack for a Crowns board size. */
    fun packForSize(size: Int): Pack = when (size) {
        6 -> Pack.EASY
        7 -> Pack.MEDIUM
        8 -> Pack.HARD
        9, 10 -> Pack.EXPERT
        else -> throw IllegalArgumentException("Crowns pack sizes are 6 through 10")
    }
}

/** Encodes and validates the technique/step-count metadata stored in Crowns bank lines. */
object CrownsGradeCodec {
    /** Encodes the hardest technique ID and total hint count; a solved-in-place board uses `none:0`. */
    fun encode(grade: Grade): String {
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        require(techniqueId == "none" || CrownsTechniques.byId(techniqueId) != null) {
            "Unknown Crowns technique ID"
        }
        return "$techniqueId:${grade.stepCount}"
    }

    /** Rejects malformed counts and technique IDs unsupported by this lane. */
    fun validate(value: String) {
        val fields = value.split(':')
        require(fields.size == 2) { "Crowns grade must be technique-id:step-count" }
        val stepCount = requireNotNull(fields[1].toIntOrNull()) { "Invalid Crowns grade step count" }
        require(stepCount >= 0) { "Crowns grade step count must not be negative" }
        if (stepCount == 0) {
            require(fields[0] == "none") { "A zero-step Crowns grade must use the none technique" }
        } else {
            require(CrownsTechniques.byId(fields[0]) != null) { "Unknown Crowns grade technique ID" }
        }
    }
}
