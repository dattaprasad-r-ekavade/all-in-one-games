package com.datathecodie.witgrid.engine.core

/**
 * Result of a complete, guess-free technique solve, with lane-defined [pack] bands.
 * [hardestTechnique] is null exactly when the initial board needs zero steps.
 */
data class Grade(val hardestTechnique: Technique?, val stepCount: Int, val pack: Pack) {
    init {
        require(stepCount >= 0) { "Step count must not be negative" }
        require((stepCount == 0) == (hardestTechnique == null)) {
            "A non-empty solve must name its hardest technique"
        }
    }
}
