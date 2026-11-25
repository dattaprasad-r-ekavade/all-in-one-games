package com.datathecodie.witgrid.engine.klondike

/**
 * Family B player actions for standard Draw-1 Klondike.
 */
sealed interface CardMove {
    /** Draw one card from stock to waste, or recycle waste back into stock if stock is empty. */
    data object DrawStock : CardMove

    /** Recycle all waste cards back into stock face-down when stock is empty. */
    data object RecycleWaste : CardMove

    /** Move top card of waste to tableau column [toColumn] (0..6). */
    data class WasteToTableau(val toColumn: Int) : CardMove

    /** Move top card of waste to its suit foundation. */
    data object WasteToFoundation : CardMove

    /**
     * Move a face-up run from [fromColumn] to [toColumn].
     * [cardIndex] is the index of the start card of the run in [fromColumn].
     * If omitted or -1, moves just the top card of [fromColumn].
     */
    data class TableauToTableau(
        val fromColumn: Int,
        val cardIndex: Int = -1,
        val toColumn: Int,
    ) : CardMove

    /** Move top card of tableau column [fromColumn] to its suit foundation. */
    data class TableauToFoundation(val fromColumn: Int) : CardMove

    /** Move top card of foundation for [suit] back to tableau column [toColumn]. */
    data class FoundationToTableau(val suit: Suit, val toColumn: Int) : CardMove
}
