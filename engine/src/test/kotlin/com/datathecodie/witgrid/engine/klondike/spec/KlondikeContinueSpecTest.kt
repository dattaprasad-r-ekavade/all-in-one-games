package com.datathecodie.witgrid.engine.klondike.spec

import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.Suit
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.state
import com.datathecodie.witgrid.engine.klondike.spec.KlondikeSpecFixtures.up
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Plan 12.5 / 8.4: Continue steps back to the position before the last action. */
class KlondikeContinueSpecTest {

    /**
     * Position before the last action. Column 0: 2 of spades. Column 1: 2 of clubs.
     * Column 2: ace of diamonds. All foundations empty. No stock or waste.
     */
    private val before = state(
        tableau = listOf(
            listOf(up(2, Suit.SPADES)),
            listOf(up(2, Suit.CLUBS)),
            listOf(up(1, Suit.DIAMONDS)),
        ),
    )

    /**
     * Ace of diamonds goes to its foundation. Column 2 is empty. No tableau top can go to a
     * foundation: 2S and 2C need an Ace first. No tableau run can move: 2S and 2C share a
     * colour. The position has no forward move, so the engine calls it lost.
     */
    private val lost = KlondikeEngine.applyAction(before, CardMove.TableauToFoundation(2))

    @Test
    fun lastMoveLeavesALostPosition() {
        assertTrue(KlondikeEngine.isLost(lost.puzzle, lost))
    }

    @Test
    fun continueRestoresThePositionBeforeTheLastAction() {
        val continued = KlondikeEngine.applyContinue(lost)

        assertEquals(before.tableau, continued.tableau)
        assertEquals(before.foundations, continued.foundations)
        assertEquals(0, continued.moveCount)
        assertEquals(1, continued.extraUndos)
        assertFalse(KlondikeEngine.isLost(continued.puzzle, continued))
    }

    @Test
    fun replayAfterContinueUsesTheRestoredPosition() {
        val continued = KlondikeEngine.applyContinue(lost)
        val replayed = KlondikeEngine.applyAction(continued, CardMove.TableauToFoundation(2))

        // The grace action is used up by the replayed move.
        assertEquals(0, replayed.extraUndos)
        assertEquals(listOf(up(1, Suit.DIAMONDS)), replayed.foundations.getValue(Suit.DIAMONDS))
        assertEquals(lost.tableau, replayed.tableau)
    }

    @Test
    fun continueOnAPositionThatIsNotLostThrows() {
        // No action yet, so the position is not lost. Continue is refused.
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeEngine.applyContinue(before)
        }
    }
}
