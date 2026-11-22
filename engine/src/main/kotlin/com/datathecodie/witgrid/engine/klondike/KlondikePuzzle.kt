package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.Pack

/**
 * Deal specification for Klondike, defined by a 64-bit seed and pack difficulty.
 */
data class KlondikePuzzle(
    val bankId: String,
    val pack: Pack,
    val difficulty: String,
    val seed: Long,
    val size: Int = 52,
)
