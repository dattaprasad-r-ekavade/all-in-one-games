package com.datathecodie.witgrid.engine.coloursort

/** In-progress play; tubes may grow after Continue adds empty capacity. */
data class ColourSortState(
    val puzzle: ColourSortPuzzle,
    /** Current stacks bottom-first; length is at least the deal tube count. */
    val tubes: List<List<Int>>,
    /** Becomes true after the first legal pour; drives stuck detection on a fresh deal. */
    val pourCount: Int,
)