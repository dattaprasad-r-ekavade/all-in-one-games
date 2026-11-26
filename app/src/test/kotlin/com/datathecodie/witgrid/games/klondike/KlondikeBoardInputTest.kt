package com.datathecodie.witgrid.games.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.klondike.Card
import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikePuzzle
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.klondike.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KlondikeBoardInputTest {
    @Test
    fun followsEasyDealHintsToWinThroughTaps() {
        val puzzle = KlondikeEngine.generate(SplitMix64(1), Pack.EASY)
        assertEquals(Pack.EASY, puzzle.pack)
        var state = KlondikeEngine.initialState(puzzle)
        assertFalse(KlondikeEngine.isWon(puzzle, state))

        var steps = 0
        while (!KlondikeEngine.isWon(puzzle, state)) {
            assertTrue(steps < 300, "No win after $steps taps, seed ${puzzle.seed}")
            assertFalse(
                KlondikeEngine.isLost(puzzle, state),
                "Lost at step $steps, seed ${puzzle.seed}",
            )
            val hinted = KlondikeEngine.hintAction(puzzle, state)
            assertTrue(hinted != null, "No hint at step $steps, seed ${puzzle.seed}")
            val hint = hinted!!
            var selection: KlondikeSelection? = null
            val actions = mutableListOf<CardMove>()
            for (hit in hitsFor(state, hint)) {
                val result = KlondikeBoardInput.tap(state, selection, hit)
                selection = result.selection
                if (result.action != null) actions += result.action
            }
            assertEquals(listOf(canonical(state, hint)), actions, "Step $steps hint $hint seed ${puzzle.seed}")
            state = KlondikeEngine.applyAction(state, actions.single())
            steps++
        }

        assertTrue(KlondikeEngine.isWon(puzzle, state))
        assertFalse(KlondikeEngine.isLost(puzzle, state))
        assertEquals(52, state.totalFoundationCards)
        assertTrue(steps > 0)
        val afterWin = KlondikeBoardInput.tap(state, selection = null, hit = KlondikeHit.Stock)
        assertEquals(KlondikeTapFeedback.NONE, afterWin.feedback)
        assertNull(afterWin.action)
    }

    @Test
    fun tapSelectsARunAndTapDestinationMovesIt() {
        val deal = state(
            tableau = columns(
                listOf(card(11, Suit.SPADES), card(10, Suit.HEARTS)),
                listOf(card(12, Suit.DIAMONDS)),
            ),
        )

        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 0))
        assertEquals(KlondikeSelection.Tableau(0, 0), select.selection)
        assertEquals(KlondikeTapFeedback.SELECTED, select.feedback)
        assertNull(select.action)
        assertTrue(
            KlondikeBoardInput.isLegalDestination(deal, select.selection, KlondikeHit.Tableau(1, 0)),
        )

        val move = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(1, 0))
        assertEquals(CardMove.TableauToTableau(0, 0, 1), move.action)
        assertEquals(KlondikeTapFeedback.MOVED, move.feedback)
        assertNull(move.selection)
    }

    @Test
    fun secondTapSendsTheSelectedTopCardToItsFoundation() {
        val deal = state(tableau = columns(listOf(card(1, Suit.HEARTS))))

        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 0))
        val send = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(0, 0))

        assertEquals(CardMove.TableauToFoundation(0), send.action)
        assertEquals(KlondikeTapFeedback.MOVED, send.feedback)
        val next = KlondikeEngine.applyAction(deal, send.action!!)
        assertEquals(1, next.foundations.getValue(Suit.HEARTS).size)
    }

    @Test
    fun secondTapOnABuriedRunCardDeselectsInsteadOfMovingIt() {
        val deal = state(
            tableau = columns(listOf(card(11, Suit.SPADES), card(10, Suit.HEARTS))),
        )

        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 0))
        val again = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(0, 0))

        assertNull(again.action)
        assertNull(again.selection)
        assertEquals(KlondikeTapFeedback.DESELECTED, again.feedback)
    }

    @Test
    fun kingMovesOntoAnEmptyColumn() {
        val deal = state(tableau = columns(listOf(card(13, Suit.HEARTS), card(12, Suit.SPADES))))

        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 0))
        val move = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(3, null))

        assertEquals(CardMove.TableauToTableau(0, 0, 3), move.action)
    }

    @Test
    fun stockDrawsAndEmptyStockRecyclesTheWaste() {
        val stockDeal = state(stock = listOf(card(4, Suit.CLUBS, faceUp = false)))
        val draw = KlondikeBoardInput.tap(stockDeal, null, KlondikeHit.Stock)
        assertEquals(CardMove.DrawStock, draw.action)

        val wasteDeal = state(waste = listOf(card(4, Suit.CLUBS)))
        val recycle = KlondikeBoardInput.tap(wasteDeal, null, KlondikeHit.Stock)
        assertEquals(CardMove.RecycleWaste, recycle.action)

        val selected = KlondikeSelection.Tableau(0, 0)
        val idle = state(tableau = columns(listOf(card(13, Suit.SPADES))))
        val kept = KlondikeBoardInput.tap(idle, selected, KlondikeHit.Stock)
        assertEquals(KlondikeTapFeedback.NONE, kept.feedback)
        assertEquals(selected, kept.selection)
        assertNull(kept.action)
    }

    @Test
    fun wasteDoubleTapAndFoundationTapBothBuildTheFoundation() {
        val deal = state(waste = listOf(card(1, Suit.CLUBS)))

        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Waste)
        assertEquals(KlondikeSelection.Waste, select.selection)
        val doubled = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Waste)
        assertEquals(CardMove.WasteToFoundation, doubled.action)

        val toPile = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Foundation(Suit.CLUBS))
        assertEquals(CardMove.WasteToFoundation, toPile.action)
        assertTrue(KlondikeBoardInput.isLegalDestination(deal, select.selection, KlondikeHit.Foundation(Suit.CLUBS)))
        assertFalse(KlondikeBoardInput.isLegalDestination(deal, select.selection, KlondikeHit.Foundation(Suit.HEARTS)))
    }

    @Test
    fun wasteMovesOntoATableauCardAndAWrongFoundationClearsTheSelection() {
        val deal = state(
            tableau = columns(listOf(card(10, Suit.SPADES))),
            waste = listOf(card(9, Suit.HEARTS)),
        )
        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Waste)
        val move = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(0, 0))
        assertEquals(CardMove.WasteToTableau(0), move.action)

        val ace = state(waste = listOf(card(1, Suit.HEARTS)))
        val selected = KlondikeBoardInput.tap(ace, null, KlondikeHit.Waste).selection
        val wrong = KlondikeBoardInput.tap(ace, selected, KlondikeHit.Foundation(Suit.CLUBS))
        assertEquals(KlondikeTapFeedback.DESELECTED, wrong.feedback)
        assertNull(wrong.action)
    }

    @Test
    fun foundationCardMovesBackToTheTableau() {
        val deal = state(
            tableau = columns(listOf(card(3, Suit.SPADES))),
            foundations = emptyFoundations(Suit.HEARTS to listOf(card(1, Suit.HEARTS), card(2, Suit.HEARTS))),
        )
        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Foundation(Suit.HEARTS))
        assertEquals(KlondikeSelection.Foundation(Suit.HEARTS), select.selection)
        val move = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(0, 0))
        assertEquals(CardMove.FoundationToTableau(Suit.HEARTS, 0), move.action)
    }

    @Test
    fun illegalColumnSwitchesSelectionAndAFaceDownCardDoesNothing() {
        val deal = state(
            tableau = columns(
                listOf(card(5, Suit.CLUBS)),
                listOf(card(9, Suit.HEARTS)),
            ),
        )
        val select = KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 0))
        assertFalse(KlondikeBoardInput.isLegalDestination(deal, select.selection, KlondikeHit.Tableau(1, 0)))
        val switched = KlondikeBoardInput.tap(deal, select.selection, KlondikeHit.Tableau(1, 0))
        assertEquals(KlondikeSelection.Tableau(1, 0), switched.selection)
        assertEquals(KlondikeTapFeedback.SELECTED, switched.feedback)

        val covered = state(
            tableau = columns(listOf(card(5, Suit.CLUBS, faceUp = false), card(4, Suit.HEARTS))),
        )
        val faceDown = KlondikeBoardInput.tap(covered, null, KlondikeHit.Tableau(0, 0))
        assertEquals(KlondikeTapFeedback.NONE, faceDown.feedback)
        assertNull(faceDown.selection)
        assertNull(faceDown.action)
    }

    @Test
    fun pileLabelNamesTheTopCardAndHidesAFaceDownRank() {
        val hidden = card(1, Suit.HEARTS, faceUp = false)
        val deal = state(
            tableau = columns(listOf(hidden, hidden.copy(rank = 12, faceUp = false), card(1, Suit.HEARTS))),
            stock = listOf(card(4, Suit.CLUBS, false), card(5, Suit.SPADES, false), card(6, Suit.DIAMONDS, false)),
        )
        val column = KlondikeBoardInput.pileLabel(deal, KlondikePile.Tableau(0), selection = null)
        assertEquals(2, column.faceDownCount)
        assertEquals(KlondikeTopSpeech.FaceUp(card(1, Suit.HEARTS)), KlondikeBoardInput.topCardSpeech(column.topCard))
        assertFalse(column.selected)

        val selected = KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 2)).selection
        val selectedLabel = KlondikeBoardInput.pileLabel(deal, KlondikePile.Tableau(0), selected)
        assertTrue(selectedLabel.selected)

        val stock = KlondikeBoardInput.pileLabel(deal, KlondikePile.Stock, selection = null)
        assertEquals(3, stock.faceDownCount)
        assertEquals(KlondikeTopSpeech.FaceDown, KlondikeBoardInput.topCardSpeech(stock.topCard))
        assertEquals(KlondikeTopSpeech.Empty, KlondikeBoardInput.topCardSpeech(null))
        assertFalse(stock.selected)
    }

    @Test
    fun accessibilityHitSelectsTheWholeFaceUpRun() {
        val pile = listOf(card(11, Suit.SPADES), card(10, Suit.HEARTS))
        assertEquals(0, KlondikeBoardInput.selectableRunBase(pile))
        val broken = listOf(card(5, Suit.CLUBS), card(3, Suit.HEARTS))
        assertEquals(1, KlondikeBoardInput.selectableRunBase(broken))

        val deal = state(tableau = columns(pile))
        assertEquals(
            KlondikeHit.Tableau(0, 0),
            KlondikeBoardInput.accessibilityHit(deal, KlondikePile.Tableau(0)),
        )
        assertEquals(KlondikeHit.Stock, KlondikeBoardInput.accessibilityHit(deal, KlondikePile.Stock))
    }

    @Test
    fun staleSelectionIsIgnoredAndABadTapThrows() {
        val deal = state(
            tableau = columns(
                listOf(card(13, Suit.SPADES)),
                listOf(card(1, Suit.HEARTS)),
            ),
        )
        val stale = KlondikeSelection.Tableau(column = 0, cardIndex = 4)
        val tap = KlondikeBoardInput.tap(deal, stale, KlondikeHit.Tableau(1, 0))
        assertEquals(KlondikeSelection.Tableau(1, 0), tap.selection)
        assertEquals(KlondikeTapFeedback.SELECTED, tap.feedback)

        assertThrows(IllegalArgumentException::class.java) {
            KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(7, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            KlondikeBoardInput.tap(deal, null, KlondikeHit.Tableau(0, 5))
        }
    }

    @Test
    fun wonAndDeadlockedBoardsIgnoreTaps() {
        val wonFoundations = Suit.entries.associateWith { suit -> (1..13).map { rank -> card(rank, suit) } }
        val won = state(foundations = wonFoundations, moveCount = 20)
        assertTrue(KlondikeEngine.isWon(won.puzzle, won))
        val wonTap = KlondikeBoardInput.tap(won, null, KlondikeHit.Tableau(0, null))
        assertEquals(KlondikeTapFeedback.NONE, wonTap.feedback)
        assertNull(wonTap.action)

        val stuck = state(
            tableau = columns(listOf(card(5, Suit.CLUBS))),
            moveCount = 1,
        )
        assertTrue(KlondikeEngine.isLost(stuck.puzzle, stuck))
        val stuckTap = KlondikeBoardInput.tap(stuck, null, KlondikeHit.Tableau(0, 0))
        assertEquals(KlondikeTapFeedback.NONE, stuckTap.feedback)
        assertNull(stuckTap.action)

        val grace = stuck.copy(extraUndos = 1)
        val graceTap = KlondikeBoardInput.tap(grace, null, KlondikeHit.Tableau(0, 0))
        assertEquals(KlondikeTapFeedback.SELECTED, graceTap.feedback)
    }

    private fun hitsFor(state: KlondikeState, move: CardMove): List<KlondikeHit> {
        fun destination(column: Int): KlondikeHit.Tableau {
            val index = state.tableau[column].indexOfLast { it.faceUp }.takeIf { it >= 0 }
            return KlondikeHit.Tableau(column, index)
        }
        return when (move) {
            CardMove.DrawStock,
            CardMove.RecycleWaste,
            -> listOf(KlondikeHit.Stock)
            is CardMove.WasteToTableau -> listOf(KlondikeHit.Waste, destination(move.toColumn))
            CardMove.WasteToFoundation -> listOf(KlondikeHit.Waste, KlondikeHit.Waste)
            is CardMove.TableauToTableau -> {
                val pile = state.tableau[move.fromColumn]
                val index = if (move.cardIndex < 0) pile.lastIndex else move.cardIndex
                listOf(KlondikeHit.Tableau(move.fromColumn, index), destination(move.toColumn))
            }
            is CardMove.TableauToFoundation -> {
                val hit = KlondikeHit.Tableau(move.fromColumn, state.tableau[move.fromColumn].lastIndex)
                listOf(hit, hit)
            }
            is CardMove.FoundationToTableau -> listOf(
                KlondikeHit.Foundation(move.suit),
                destination(move.toColumn),
            )
        }
    }

    private fun canonical(state: KlondikeState, move: CardMove): CardMove = when (move) {
        is CardMove.TableauToTableau -> {
            val index = if (move.cardIndex < 0) state.tableau[move.fromColumn].lastIndex else move.cardIndex
            move.copy(cardIndex = index)
        }
        else -> move
    }

    private fun card(rank: Int, suit: Suit, faceUp: Boolean = true) = Card(rank, suit, faceUp)

    private fun columns(vararg piles: List<Card>): List<List<Card>> {
        require(piles.size <= 7)
        return List(7) { index -> piles.getOrNull(index) ?: emptyList() }
    }

    private fun emptyFoundations(vararg placed: Pair<Suit, List<Card>>): Map<Suit, List<Card>> {
        val map = Suit.entries.associateWith { emptyList<Card>() }.toMutableMap()
        for ((suit, cards) in placed) {
            map[suit] = cards
        }
        return map
    }

    private fun state(
        tableau: List<List<Card>> = columns(),
        stock: List<Card> = emptyList(),
        waste: List<Card> = emptyList(),
        foundations: Map<Suit, List<Card>> = emptyFoundations(),
        moveCount: Int = 0,
        extraUndos: Int = 0,
    ): KlondikeState = KlondikeState(
        puzzle = KlondikePuzzle(bankId = "kl-test", pack = Pack.EASY, difficulty = "d1", seed = 1L),
        tableau = tableau,
        foundations = foundations,
        stock = stock,
        waste = waste,
        moveCount = moveCount,
        extraUndos = extraUndos,
    )
}
