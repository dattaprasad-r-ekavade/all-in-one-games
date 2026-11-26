package com.datathecodie.witgrid.games.klondike

import com.datathecodie.witgrid.engine.klondike.Card
import com.datathecodie.witgrid.engine.klondike.CardMove
import com.datathecodie.witgrid.engine.klondike.KlondikeRules
import com.datathecodie.witgrid.engine.klondike.KlondikeState
import com.datathecodie.witgrid.engine.klondike.Suit

internal enum class KlondikeTapFeedback {
    NONE,
    SELECTED,
    DESELECTED,
    MOVED,
}

internal data class KlondikeTapResult(
    val selection: KlondikeSelection?,
    val action: CardMove?,
    val feedback: KlondikeTapFeedback,
)

/** The face-up card, waste card, or foundation card the next tap will move. */
internal sealed interface KlondikeSelection {
    data class Tableau(val column: Int, val cardIndex: Int) : KlondikeSelection
    data object Waste : KlondikeSelection
    data class Foundation(val suit: Suit) : KlondikeSelection
}

/**
 * One pile or one card the player tapped.
 * [Tableau.cardIndex] is null when the column is empty.
 */
internal sealed interface KlondikeHit {
    data object Stock : KlondikeHit
    data object Waste : KlondikeHit
    data class Foundation(val suit: Suit) : KlondikeHit
    data class Tableau(val column: Int, val cardIndex: Int?) : KlondikeHit
}

internal sealed interface KlondikePile {
    data object Stock : KlondikePile
    data object Waste : KlondikePile
    data class Foundation(val suit: Suit) : KlondikePile

    /** [column] is 0-based. TalkBack speaks it as 1-based. */
    data class Tableau(val column: Int) : KlondikePile
}

internal data class KlondikePileLabel(
    val pile: KlondikePile,
    val topCard: Card?,
    val faceDownCount: Int,
    val selected: Boolean,
)

/** What TalkBack should say for the top of a pile. A face-down top does not reveal its rank. */
internal sealed interface KlondikeTopSpeech {
    data object Empty : KlondikeTopSpeech
    data object FaceDown : KlondikeTopSpeech
    data class FaceUp(val card: Card) : KlondikeTopSpeech
}

/**
 * Converts taps into a selection or one [CardMove].
 * Tap a face-up card to select its run. Tap a destination to move.
 * Tap the selected card again to send it to its foundation when that play is legal.
 * Tap the stock to draw, or to recycle the waste when the stock is empty.
 */
internal object KlondikeBoardInput {
    fun tap(state: KlondikeState, selection: KlondikeSelection?, hit: KlondikeHit): KlondikeTapResult {
        validate(state, hit)
        if (isClosed(state)) {
            return KlondikeTapResult(selection = null, action = null, feedback = KlondikeTapFeedback.NONE)
        }
        val current = activeSelection(state, selection)
        return when (hit) {
            KlondikeHit.Stock -> tapStock(state, current)
            KlondikeHit.Waste -> tapWaste(state, current)
            is KlondikeHit.Foundation -> tapFoundation(state, current, hit.suit)
            is KlondikeHit.Tableau -> tapTableau(state, current, hit)
        }
    }

    fun activeSelection(state: KlondikeState, selection: KlondikeSelection?): KlondikeSelection? {
        return when (selection) {
            null -> null
            KlondikeSelection.Waste -> if (state.waste.isNotEmpty()) selection else null
            is KlondikeSelection.Foundation -> {
                val pile = state.foundations[selection.suit].orEmpty()
                if (pile.isNotEmpty()) selection else null
            }
            is KlondikeSelection.Tableau -> {
                val pile = state.tableau.getOrNull(selection.column) ?: return null
                if (!isSelectable(pile, selection.cardIndex)) return null
                selection
            }
        }
    }

    fun isLegalDestination(state: KlondikeState, selection: KlondikeSelection?, hit: KlondikeHit): Boolean {
        if (isClosed(state)) return false
        val current = activeSelection(state, selection) ?: return false
        return when (hit) {
            KlondikeHit.Stock, KlondikeHit.Waste -> false
            is KlondikeHit.Foundation -> foundationMove(state, current, hit.suit) != null
            is KlondikeHit.Tableau -> tableauMove(state, current, hit.column) != null
        }
    }

    fun pileLabel(state: KlondikeState, pile: KlondikePile, selection: KlondikeSelection?): KlondikePileLabel {
        val active = activeSelection(state, selection)
        val cards = cardsOf(state, pile)
        val selected = when (pile) {
            KlondikePile.Stock -> false
            KlondikePile.Waste -> active is KlondikeSelection.Waste
            is KlondikePile.Foundation -> active is KlondikeSelection.Foundation && active.suit == pile.suit
            is KlondikePile.Tableau -> active is KlondikeSelection.Tableau && active.column == pile.column
        }
        return KlondikePileLabel(
            pile = pile,
            topCard = cards.lastOrNull(),
            faceDownCount = cards.count { !it.faceUp },
            selected = selected,
        )
    }

    fun topCardSpeech(card: Card?): KlondikeTopSpeech = when {
        card == null -> KlondikeTopSpeech.Empty
        !card.faceUp -> KlondikeTopSpeech.FaceDown
        else -> KlondikeTopSpeech.FaceUp(card)
    }

    /**
     * Lowest face-up card of the descending run that ends on the top card.
     * Null when the column is empty or its top card is face down.
     * TalkBack activates this card, because a pile has one label.
     */
    fun selectableRunBase(pile: List<Card>): Int? {
        if (pile.isEmpty() || !pile.last().faceUp) return null
        var base = pile.lastIndex
        while (base > 0 && isSelectable(pile, base - 1)) {
            base--
        }
        return base
    }

    fun accessibilityHit(state: KlondikeState, pile: KlondikePile): KlondikeHit = when (pile) {
        KlondikePile.Stock -> KlondikeHit.Stock
        KlondikePile.Waste -> KlondikeHit.Waste
        is KlondikePile.Foundation -> KlondikeHit.Foundation(pile.suit)
        is KlondikePile.Tableau -> {
            val cards = state.tableau.getOrNull(pile.column).orEmpty()
            KlondikeHit.Tableau(pile.column, selectableRunBase(cards))
        }
    }

    private fun tapStock(state: KlondikeState, current: KlondikeSelection?): KlondikeTapResult {
        if (state.stock.isNotEmpty()) return moved(CardMove.DrawStock)
        if (state.waste.isNotEmpty()) return moved(CardMove.RecycleWaste)
        return none(current)
    }

    private fun tapWaste(state: KlondikeState, current: KlondikeSelection?): KlondikeTapResult {
        if (state.waste.isEmpty()) return none(current)
        if (current is KlondikeSelection.Waste) {
            val card = state.waste.last()
            return if (KlondikeRules.isFoundationPlacementLegal(state.foundations, card)) {
                moved(CardMove.WasteToFoundation)
            } else {
                deselected()
            }
        }
        return selected(KlondikeSelection.Waste)
    }

    private fun tapFoundation(
        state: KlondikeState,
        current: KlondikeSelection?,
        suit: Suit,
    ): KlondikeTapResult {
        val move = foundationMove(state, current, suit)
        if (move != null) return moved(move)
        if (current is KlondikeSelection.Foundation && current.suit == suit) return deselected()
        val pile = state.foundations[suit].orEmpty()
        if (pile.isNotEmpty()) return selected(KlondikeSelection.Foundation(suit))
        return if (current != null) deselected() else none(null)
    }

    private fun tapTableau(
        state: KlondikeState,
        current: KlondikeSelection?,
        hit: KlondikeHit.Tableau,
    ): KlondikeTapResult {
        val pile = state.tableau[hit.column]
        val index = hit.cardIndex
        if (index != null && !pile[index].faceUp) {
            val move = tableauMove(state, current, hit.column)
            return if (move != null) moved(move) else none(current)
        }
        if (index == null) {
            if (pile.isNotEmpty()) {
                val top = pile.indexOfLast { it.faceUp }
                if (top >= 0) {
                    return tapTableau(state, current, KlondikeHit.Tableau(hit.column, top))
                }
                val move = tableauMove(state, current, hit.column)
                return if (move != null) moved(move) else none(current)
            }
            val move = tableauMove(state, current, hit.column)
            return if (move != null) moved(move) else if (current != null) deselected() else none(null)
        }
        val sameCard = current is KlondikeSelection.Tableau &&
            current.column == hit.column &&
            current.cardIndex == index
        if (sameCard) {
            val card = pile[index]
            if (index == pile.lastIndex && KlondikeRules.isFoundationPlacementLegal(state.foundations, card)) {
                return moved(CardMove.TableauToFoundation(hit.column))
            }
            return deselected()
        }
        val move = tableauMove(state, current, hit.column)
        if (move != null) return moved(move)
        if (isSelectable(pile, index)) return selected(KlondikeSelection.Tableau(hit.column, index))
        return none(current)
    }

    private fun foundationMove(state: KlondikeState, selection: KlondikeSelection?, suit: Suit): CardMove? {
        val card = singleTopCard(state, selection) ?: return null
        if (card.suit != suit) return null
        if (!KlondikeRules.isFoundationPlacementLegal(state.foundations, card)) return null
        return when (selection) {
            KlondikeSelection.Waste -> CardMove.WasteToFoundation
            is KlondikeSelection.Tableau -> CardMove.TableauToFoundation(selection.column)
            else -> null
        }
    }

    private fun tableauMove(state: KlondikeState, selection: KlondikeSelection?, toColumn: Int): CardMove? {
        if (selection == null) return null
        if (toColumn !in state.tableau.indices) return null
        return when (selection) {
            KlondikeSelection.Waste -> {
                val card = state.waste.lastOrNull() ?: return null
                if (KlondikeRules.isTableauPlacementLegal(state.tableau[toColumn], card)) {
                    CardMove.WasteToTableau(toColumn)
                } else {
                    null
                }
            }
            is KlondikeSelection.Foundation -> {
                val card = state.foundations[selection.suit]?.lastOrNull() ?: return null
                if (KlondikeRules.isTableauPlacementLegal(state.tableau[toColumn], card)) {
                    CardMove.FoundationToTableau(selection.suit, toColumn)
                } else {
                    null
                }
            }
            is KlondikeSelection.Tableau -> {
                if (selection.column == toColumn) return null
                val pile = state.tableau.getOrNull(selection.column) ?: return null
                if (!isSelectable(pile, selection.cardIndex)) return null
                val base = pile[selection.cardIndex]
                if (KlondikeRules.isTableauPlacementLegal(state.tableau[toColumn], base)) {
                    CardMove.TableauToTableau(selection.column, selection.cardIndex, toColumn)
                } else {
                    null
                }
            }
        }
    }

    private fun singleTopCard(state: KlondikeState, selection: KlondikeSelection?): Card? = when (selection) {
        KlondikeSelection.Waste -> state.waste.lastOrNull()
        is KlondikeSelection.Tableau -> {
            val pile = state.tableau.getOrNull(selection.column) ?: return null
            if (selection.cardIndex != pile.lastIndex) return null
            pile.lastOrNull()?.takeIf { it.faceUp }
        }
        else -> null
    }

    private fun isSelectable(pile: List<Card>, index: Int): Boolean {
        if (index !in pile.indices) return false
        if (!pile[index].faceUp) return false
        return KlondikeRules.isValidRun(pile.subList(index, pile.size))
    }

    private fun cardsOf(state: KlondikeState, pile: KlondikePile): List<Card> = when (pile) {
        KlondikePile.Stock -> state.stock
        KlondikePile.Waste -> state.waste
        is KlondikePile.Foundation -> state.foundations[pile.suit].orEmpty()
        is KlondikePile.Tableau -> state.tableau.getOrNull(pile.column).orEmpty()
    }

    private fun isClosed(state: KlondikeState): Boolean {
        if (KlondikeRules.isWon(state)) return true
        // A fresh deal is not a loss. Extra undo is the one-action grace after Continue.
        // Deadlock is the loss a tap can see. The full solver search stays with the play host,
        // so a tap does not search the deal.
        if (state.moveCount == 0 || state.extraUndos > 0) return false
        return KlondikeRules.isDeadlocked(state)
    }

    private fun validate(state: KlondikeState, hit: KlondikeHit) {
        if (hit !is KlondikeHit.Tableau) return
        require(hit.column in state.tableau.indices) { "Tapped column ${hit.column} outside board" }
        if (hit.cardIndex != null) {
            require(hit.cardIndex in state.tableau[hit.column].indices) {
                "Tapped card ${hit.cardIndex} outside column ${hit.column}"
            }
        }
    }

    private fun moved(action: CardMove) = KlondikeTapResult(null, action, KlondikeTapFeedback.MOVED)

    private fun selected(selection: KlondikeSelection) =
        KlondikeTapResult(selection, null, KlondikeTapFeedback.SELECTED)

    private fun deselected() = KlondikeTapResult(null, null, KlondikeTapFeedback.DESELECTED)

    private fun none(selection: KlondikeSelection?) =
        KlondikeTapResult(selection, null, KlondikeTapFeedback.NONE)
}
