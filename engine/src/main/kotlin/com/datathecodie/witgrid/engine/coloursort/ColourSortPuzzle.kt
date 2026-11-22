package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.Pack

/** Fixed deal: initial tube stacks (bottom-first) and metadata from the bank line. */
data class ColourSortPuzzle(
    val bankId: String,
    val pack: Pack,
    val difficulty: String,
    /** Initial tubes at deal time; each inner list is bottom to top. */
    val initialTubes: List<List<Int>>,
)