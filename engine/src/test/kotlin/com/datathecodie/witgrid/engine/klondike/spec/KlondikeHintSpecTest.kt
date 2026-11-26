package com.datathecodie.witgrid.engine.klondike.spec

import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.Suit
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.piles
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.state
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.up
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Plan 12.5: a hint is one legal move on the winning line. */
class KlondikeHintSpecTest {

    /**
     * Nearly won. Foundations hold A to K of clubs, diamonds and hearts, and A to Q of spades.
     * King of spades is alone in column 0. That is 13 + 13 + 13 + 12 + 1 = 52 cards.
     * The only legal move is King of spades to its foundation.
     */
    private val nearlyWon = state(
        tableau = listOf(listOf(up(13, Suit.SPADES))),
        foundations = piles(13, 13, 13, 12),
    )

    @Test
    fun hintOnANearlyWonStateIsTheOnlyLegalMove() {
        val hint = requireNotNull(KlondikeEngine.hintAction(nearlyWon.puzzle, nearlyWon))
        assertEquals(CardMove.TableauToFoundation(0), hint)
    }

    @Test
    fun hintMakesProgressAndWinsTheGame() {
        val hint = requireNotNull(KlondikeEngine.hintAction(nearlyWon.puzzle, nearlyWon))
        val after = KlondikeEngine.applyAction(nearlyWon, hint)

        // Foundation count goes from 51 to 52. The game is won.
        assertEquals(nearlyWon.totalFoundationCards + 1, after.totalFoundationCards)
        assertTrue(KlondikeEngine.isWon(after.puzzle, after))
    }
}
