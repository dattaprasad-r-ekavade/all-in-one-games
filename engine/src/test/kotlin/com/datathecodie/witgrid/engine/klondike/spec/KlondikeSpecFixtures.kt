package com.datathecodie.witgrid.engine.klondike.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.klondike.Card
import com.datathecodie.witgrid.engine.klondike.KlondikePuzzle
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.klondike.Suit

/** Hand-made Klondike positions for the plan 12.5 spec tests. */
object KlondikeSpecFixtures {
    private const val COLUMN_COUNT = 7

    val puzzle = KlondikePuzzle(
        bankId = "KL-SPEC",
        pack = Pack.MEDIUM,
        difficulty = "d2",
        seed = 1L,
    )

    fun up(rank: Int, suit: Suit): Card = Card(rank, suit, faceUp = true)

    fun down(rank: Int, suit: Suit): Card = Card(rank, suit, faceUp = false)

    /** Foundation piles that hold the ranks Ace to [clubs], [diamonds], [hearts] and [spades]. */
    fun piles(clubs: Int = 0, diamonds: Int = 0, hearts: Int = 0, spades: Int = 0): Map<Suit, List<Card>> =
        mapOf(
            Suit.CLUBS to ranksUpTo(clubs, Suit.CLUBS),
            Suit.DIAMONDS to ranksUpTo(diamonds, Suit.DIAMONDS),
            Suit.HEARTS to ranksUpTo(hearts, Suit.HEARTS),
            Suit.SPADES to ranksUpTo(spades, Suit.SPADES),
        )

    /** Builds a state. Missing tableau columns are empty. The stock list ends with the top card. */
    fun state(
        tableau: List<List<Card>> = emptyList(),
        foundations: Map<Suit, List<Card>> = piles(),
        stock: List<Card> = emptyList(),
        waste: List<Card> = emptyList(),
        moveCount: Int = 0,
        previousState: KlondikeState? = null,
    ): KlondikeState = KlondikeState(
        puzzle = puzzle,
        tableau = List(COLUMN_COUNT) { index -> tableau.getOrElse(index) { emptyList() } },
        foundations = foundations,
        stock = stock,
        waste = waste,
        moveCount = moveCount,
        previousState = previousState,
    )

    private fun ranksUpTo(count: Int, suit: Suit): List<Card> = (1..count).map { up(it, suit) }
}
