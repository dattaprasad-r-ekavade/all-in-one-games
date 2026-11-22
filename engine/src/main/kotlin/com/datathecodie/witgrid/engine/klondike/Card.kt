package com.datathecodie.witgrid.engine.klondike

enum class Suit(val symbol: Char) {
    CLUBS('C'),
    DIAMONDS('D'),
    HEARTS('H'),
    SPADES('S');

    val isRed: Boolean get() = this == DIAMONDS || this == HEARTS
    val isBlack: Boolean get() = this == CLUBS || this == SPADES
}

enum class Rank(val value: Int, val symbol: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    companion object {
        fun fromValue(value: Int): Rank =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid rank value: $value")
    }
}

/**
 * Standard playing card with integer rank 1 (Ace) to 13 (King), suit, and visibility.
 */
data class Card(
    val rank: Int,
    val suit: Suit,
    val faceUp: Boolean = false,
) {
    constructor(rank: Rank, suit: Suit, faceUp: Boolean = false) : this(rank.value, suit, faceUp)

    init {
        require(rank in 1..13) { "Rank must be 1..13, got $rank" }
    }

    val isRed: Boolean get() = suit.isRed
    val isBlack: Boolean get() = suit.isBlack
    val rankEnum: Rank get() = Rank.fromValue(rank)

    fun withFaceUp(faceUp: Boolean): Card =
        if (this.faceUp == faceUp) this else copy(faceUp = faceUp)
}
