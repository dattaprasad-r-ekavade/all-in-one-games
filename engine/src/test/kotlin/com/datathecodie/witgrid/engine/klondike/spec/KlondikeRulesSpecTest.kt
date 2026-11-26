package com.datathecodie.witgrid.engine.klondike.spec

import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikeRules
import com.datathecodie.witgrid.engine.klondike.Suit
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.down
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.piles
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.state
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.up
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Plan 12.5: placement, run, stock and win rules for standard draw-1 Klondike. */
class KlondikeRulesSpecTest {

    @Test
    fun onlyAKingGoesToAnEmptyColumn() {
        // Empty column. King (13) is legal. Queen (12) and Ace (1) are not.
        assertTrue(KlondikeRules.isTableauPlacementLegal(emptyList(), up(13, Suit.HEARTS)))
        assertFalse(KlondikeRules.isTableauPlacementLegal(emptyList(), up(12, Suit.HEARTS)))
        assertFalse(KlondikeRules.isTableauPlacementLegal(emptyList(), up(1, Suit.CLUBS)))
    }

    @Test
    fun aKingFromWasteMovesToAnEmptyColumn() {
        // King of hearts on waste. Column 0 is empty, so the move is legal.
        val start = state(waste = listOf(up(13, Suit.HEARTS)))
        val after = KlondikeEngine.applyAction(start, CardMove.WasteToTableau(0))
        assertEquals(listOf(up(13, Suit.HEARTS)), after.tableau[0])
        assertTrue(after.waste.isEmpty())
        assertEquals(1, after.moveCount)
    }

    @Test
    fun aQueenFromWasteIsRefusedForAnEmptyColumn() {
        // Queen of hearts on waste. Column 0 is empty, so the move throws.
        val start = state(waste = listOf(up(12, Suit.HEARTS)))
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyAction(start, CardMove.WasteToTableau(0))
        }
    }

    @Test
    fun tableauNeedsOppositeColourAndOneLowerRank() {
        // Target top: 9 of spades (black, face up).
        val target = listOf(up(9, Suit.SPADES))
        assertTrue(KlondikeRules.isTableauPlacementLegal(target, up(8, Suit.HEARTS)))    // red, 8 = 9 - 1
        assertTrue(KlondikeRules.isTableauPlacementLegal(target, up(8, Suit.DIAMONDS)))  // red, 8 = 9 - 1
        assertFalse(KlondikeRules.isTableauPlacementLegal(target, up(8, Suit.CLUBS)))    // black, same colour
        assertFalse(KlondikeRules.isTableauPlacementLegal(target, up(10, Suit.HEARTS)))  // 10 is not 9 - 1
        assertFalse(KlondikeRules.isTableauPlacementLegal(target, up(7, Suit.HEARTS)))   // 7 is two lower
    }

    @Test
    fun tableauTargetMustBeFaceUp() {
        // Target top is 9 of spades but face down. Even 8 of hearts is refused.
        val target = listOf(down(9, Suit.SPADES))
        assertFalse(KlondikeRules.isTableauPlacementLegal(target, up(8, Suit.HEARTS)))
    }

    @Test
    fun wasteCardOntoTableauFollowsTheSameRule() {
        // 8 of hearts on 9 of spades is legal. 8 of clubs on the same 9 is not.
        val legal = state(tableau = listOf(listOf(up(9, Suit.SPADES))), waste = listOf(up(8, Suit.HEARTS)))
        val after = KlondikeEngine.applyAction(legal, CardMove.WasteToTableau(0))
        assertEquals(listOf(up(9, Suit.SPADES), up(8, Suit.HEARTS)), after.tableau[0])
        assertTrue(after.waste.isEmpty())

        val illegal = state(tableau = listOf(listOf(up(9, Suit.SPADES))), waste = listOf(up(8, Suit.CLUBS)))
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyAction(illegal, CardMove.WasteToTableau(0))
        }
    }

    @Test
    fun foundationNeedsSameSuitAndOneHigherRankStartingWithAce() {
        // Clubs pile holds A and 2, so the next legal card is 3 of clubs.
        val clubs2 = piles(clubs = 2)
        assertTrue(KlondikeRules.isFoundationPlacementLegal(clubs2, up(3, Suit.CLUBS)))
        assertFalse(KlondikeRules.isFoundationPlacementLegal(clubs2, up(3, Suit.HEARTS)))  // other suit
        assertFalse(KlondikeRules.isFoundationPlacementLegal(clubs2, up(4, Suit.CLUBS)))   // skips 3
        assertFalse(KlondikeRules.isFoundationPlacementLegal(clubs2, up(2, Suit.CLUBS)))   // 2 is already there

        // Empty hearts pile accepts only an Ace.
        assertTrue(KlondikeRules.isFoundationPlacementLegal(piles(), up(1, Suit.HEARTS)))
        assertFalse(KlondikeRules.isFoundationPlacementLegal(piles(), up(2, Suit.HEARTS)))
    }

    @Test
    fun twoOfClubsCannotStartAnEmptyFoundation() {
        // Column 0 holds 2 of clubs. The foundation is empty, so the move throws.
        val start = state(tableau = listOf(listOf(up(2, Suit.CLUBS))))
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyAction(start, CardMove.TableauToFoundation(0))
        }

        // Column 0 holds 3 of clubs. The clubs pile holds A and 2, so the move is legal.
        val ready = state(tableau = listOf(listOf(up(3, Suit.CLUBS))), foundations = piles(clubs = 2))
        val after = KlondikeEngine.applyAction(ready, CardMove.TableauToFoundation(0))
        assertEquals(3, after.foundations.getValue(Suit.CLUBS).last().rank)
        assertTrue(after.tableau[0].isEmpty())
    }

    @Test
    fun movingARunRevealsAndFlipsTheCardBelow() {
        // Column 0: 5 of diamonds (face down), 8 of hearts, 7 of clubs (face up).
        // Column 1: 9 of spades. The run 8H, 7C sits on 9S: 8 is red, 9 is black, 8 = 9 - 1.
        val start = state(
            tableau = listOf(
                listOf(down(5, Suit.DIAMONDS), up(8, Suit.HEARTS), up(7, Suit.CLUBS)),
                listOf(up(9, Suit.SPADES)),
            ),
        )
        val after = KlondikeEngine.applyAction(start, CardMove.TableauToTableau(fromColumn = 0, cardIndex = 1, toColumn = 1))

        assertEquals(listOf(up(5, Suit.DIAMONDS)), after.tableau[0])
        assertEquals(listOf(up(9, Suit.SPADES), up(8, Suit.HEARTS), up(7, Suit.CLUBS)), after.tableau[1])
        assertEquals(1, after.moveCount)
    }

    @Test
    fun runThatIncludesAFaceDownCardIsRefused() {
        // Run from index 0 would include 5 of diamonds, which is face down. The move throws.
        val start = state(
            tableau = listOf(
                listOf(down(5, Suit.DIAMONDS), up(8, Suit.HEARTS), up(7, Suit.CLUBS)),
                listOf(up(9, Suit.SPADES)),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyAction(start, CardMove.TableauToTableau(fromColumn = 0, cardIndex = 0, toColumn = 1))
        }
    }

    @Test
    fun drawTakesOneCardFromStockToWaste() {
        // Stock top is the last item: 3 of diamonds. It moves to waste face up.
        val start = state(stock = listOf(down(2, Suit.CLUBS), down(3, Suit.DIAMONDS)))
        val after = KlondikeEngine.applyAction(start, CardMove.DrawStock)

        assertEquals(listOf(down(2, Suit.CLUBS)), after.stock)
        assertEquals(listOf(up(3, Suit.DIAMONDS)), after.waste)
        assertEquals(1, after.moveCount)
    }

    @Test
    fun recycleIsRefusedWhileStockHasCards() {
        // Stock still holds 2 of clubs, so waste cannot be recycled yet.
        val start = state(stock = listOf(down(2, Suit.CLUBS)), waste = listOf(up(3, Suit.DIAMONDS)))
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyAction(start, CardMove.RecycleWaste)
        }
    }

    @Test
    fun drawWithEmptyStockRecyclesWasteInReverseOrder() {
        // Waste bottom to top: 2C, 3D, 4S. Recycling reverses the order, face down.
        // Stock becomes 4S, 3D, 2C with 2C on top. The next draw gives 2C again.
        val start = state(waste = listOf(up(2, Suit.CLUBS), up(3, Suit.DIAMONDS), up(4, Suit.SPADES)))
        val recycled = KlondikeEngine.applyAction(start, CardMove.DrawStock)

        assertTrue(recycled.waste.isEmpty())
        assertEquals(listOf(down(4, Suit.SPADES), down(3, Suit.DIAMONDS), down(2, Suit.CLUBS)), recycled.stock)
        assertEquals(1, recycled.moveCount)

        val drawn = KlondikeEngine.applyAction(recycled, CardMove.DrawStock)
        assertEquals(listOf(up(2, Suit.CLUBS)), drawn.waste)
    }

    @Test
    fun drawWithStockAndWasteBothEmptyThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyAction(state(), CardMove.DrawStock)
        }
    }

    @Test
    fun gameIsWonOnlyWhenEveryFoundationHasThirteenCards() {
        // All four piles hold A to K. The game is won.
        val full = state(foundations = piles(13, 13, 13, 13))
        assertTrue(KlondikeEngine.isWon(full.puzzle, full))

        // Spades holds A to Q. King of spades is alone in column 0. Not yet won.
        val almost = state(tableau = listOf(listOf(up(13, Suit.SPADES))), foundations = piles(13, 13, 13, 12))
        assertFalse(KlondikeEngine.isWon(almost.puzzle, almost))

        // King of spades goes to the foundation. Thirteen cards there now, so won.
        val after = KlondikeEngine.applyAction(almost, CardMove.TableauToFoundation(0))
        assertTrue(KlondikeEngine.isWon(after.puzzle, after))
    }
}
