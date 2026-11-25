package com.datathecodie.witgrid.engine.klondike

/**
 * In-progress Klondike play state: 7 tableau columns, 4 suit foundations, stock and waste.
 */
data class KlondikeState(
    val puzzle: KlondikePuzzle,
    val tableau: List<List<Card>>,
    val foundations: Map<Suit, List<Card>>,
    val stock: List<Card>,
    val waste: List<Card>,
    val moveCount: Int = 0,
    val previousState: KlondikeState? = null,
    val extraUndos: Int = 0,
) {
    val totalFoundationCards: Int get() = foundations.values.sumOf { it.size }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KlondikeState) return false
        return puzzle == other.puzzle &&
            tableau == other.tableau &&
            foundations == other.foundations &&
            stock == other.stock &&
            waste == other.waste &&
            moveCount == other.moveCount &&
            extraUndos == other.extraUndos
    }

    override fun hashCode(): Int {
        var result = puzzle.hashCode()
        result = 31 * result + tableau.hashCode()
        result = 31 * result + foundations.hashCode()
        result = 31 * result + stock.hashCode()
        result = 31 * result + waste.hashCode()
        result = 31 * result + moveCount
        result = 31 * result + extraUndos
        return result
    }
}
