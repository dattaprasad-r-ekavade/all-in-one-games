package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

object KlondikeDeal {
    const val DECK_SIZE = 52
    const val TABLEAU_COLUMNS = 7

    fun createStandardDeck(): List<Card> {
        val suits = listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)
        val deck = ArrayList<Card>(DECK_SIZE)
        for (suit in suits) {
            for (rank in 1..13) {
                deck.add(Card(rank = rank, suit = suit, faceUp = false))
            }
        }
        return deck
    }

    fun deal(puzzle: KlondikePuzzle): KlondikeState {
        val deck = createStandardDeck().toMutableList()
        val rng = SplitMix64(puzzle.seed)
        // Deterministic Fisher-Yates shuffle
        for (index in deck.lastIndex downTo 1) {
            val other = rng.nextInt(index + 1)
            val temporary = deck[index]
            deck[index] = deck[other]
            deck[other] = temporary
        }

        // Deal 28 cards round-robin across 7 tableau columns
        val tableau = List(TABLEAU_COLUMNS) { ArrayList<Card>() }
        var cardIndex = 0
        for (round in 0 until TABLEAU_COLUMNS) {
            for (column in round until TABLEAU_COLUMNS) {
                tableau[column].add(deck[cardIndex++])
            }
        }

        // Easy has more face-up cards (top 2 per column); Medium/Hard/Expert have 1 face-up (the top card)
        val faceUpPerColumn = if (puzzle.pack == Pack.EASY) 2 else 1
        for (column in 0 until TABLEAU_COLUMNS) {
            val pile = tableau[column]
            val faceUpCount = minOf(pile.size, faceUpPerColumn)
            val firstFaceUpIndex = pile.size - faceUpCount
            for (index in pile.indices) {
                if (index >= firstFaceUpIndex) {
                    pile[index] = pile[index].copy(faceUp = true)
                }
            }
        }

        // Remaining 24 cards form the stock; top card is drawn first
        val stockCards = ArrayList<Card>(DECK_SIZE - cardIndex)
        while (cardIndex < DECK_SIZE) {
            stockCards.add(deck[cardIndex++].copy(faceUp = false))
        }
        // Reversing ensures deck[28] is at stock.last() and drawn first
        val stock = stockCards.reversed()

        val foundations = Suit.entries.associateWith { emptyList<Card>() }

        return KlondikeState(
            puzzle = puzzle,
            tableau = tableau.map { it.toList() },
            foundations = foundations,
            stock = stock,
            waste = emptyList(),
            moveCount = 0,
        )
    }
}
