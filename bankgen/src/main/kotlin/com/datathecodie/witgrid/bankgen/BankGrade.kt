package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Grade

/** Generic technique grade text for bank validation without per-lane codec switches. */
internal object BankGrade {
    fun encode(grade: Grade): String {
        val techniqueId = grade.hardestTechnique?.id ?: "none"
        return "$techniqueId:${grade.stepCount}"
    }
}