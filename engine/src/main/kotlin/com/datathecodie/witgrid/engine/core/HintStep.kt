package com.datathecodie.witgrid.engine.core

/**
 * One logical deduction with a one-sentence [explanation] and ordered, absolute [moves].
 * [cells] identifies the evidence to highlight and may differ from the move targets.
 * Callers must supply immutable lists and retain no mutable aliases to them.
 */
data class HintStep(
    val technique: Technique,
    val cells: List<Cell>,
    val explanation: String,
    val moves: List<Move>,
) {
    init {
        require(cells.isNotEmpty()) { "A hint must highlight its evidence" }
        require(explanation.isNotBlank()) { "A hint must explain its deduction" }
        require(moves.isNotEmpty()) { "A hint must make progress" }
    }
}
