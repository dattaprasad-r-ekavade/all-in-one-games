package com.datathecodie.witgrid.engine.klondike

object KlondikeRules {
    const val KING_RANK = 13
    const val ACE_RANK = 1
    const val TABLEAU_COLUMNS = 7
    const val TOTAL_CARDS = 52

    fun isTableauPlacementLegal(targetColumn: List<Card>, card: Card): Boolean {
        if (targetColumn.isEmpty()) {
            return card.rank == KING_RANK
        }
        val top = targetColumn.last()
        return top.faceUp && top.isRed != card.isRed && top.rank == card.rank + 1
    }

    fun isFoundationPlacementLegal(foundations: Map<Suit, List<Card>>, card: Card): Boolean {
        val pile = foundations[card.suit] ?: emptyList()
        if (pile.isEmpty()) {
            return card.rank == ACE_RANK
        }
        return pile.last().rank + 1 == card.rank
    }

    fun isValidRun(run: List<Card>): Boolean {
        if (run.isEmpty()) return false
        if (run.any { !it.faceUp }) return false
        for (index in 0 until run.lastIndex) {
            val current = run[index]
            val next = run[index + 1]
            if (current.isRed == next.isRed || current.rank != next.rank + 1) {
                return false
            }
        }
        return true
    }

    fun applyAction(state: KlondikeState, action: CardMove): KlondikeState {
        val nextUndos = if (state.extraUndos > 0) state.extraUndos - 1 else 0
        return when (action) {
            is CardMove.DrawStock -> applyDrawStock(state, nextUndos)
            is CardMove.RecycleWaste -> applyRecycleWaste(state, nextUndos)
            is CardMove.WasteToTableau -> applyWasteToTableau(state, action.toColumn, nextUndos)
            is CardMove.WasteToFoundation -> applyWasteToFoundation(state, nextUndos)
            is CardMove.TableauToTableau -> applyTableauToTableau(state, action.fromColumn, action.cardIndex, action.toColumn, nextUndos)
            is CardMove.TableauToFoundation -> applyTableauToFoundation(state, action.fromColumn, nextUndos)
            is CardMove.FoundationToTableau -> applyFoundationToTableau(state, action.suit, action.toColumn, nextUndos)
        }
    }

    private fun applyDrawStock(state: KlondikeState, nextUndos: Int): KlondikeState {
        if (state.stock.isNotEmpty()) {
            val drawn = state.stock.last().copy(faceUp = true)
            return state.copy(
                stock = state.stock.dropLast(1),
                waste = state.waste + drawn,
                moveCount = state.moveCount + 1,
                previousState = state,
                extraUndos = nextUndos,
            )
        }
        if (state.waste.isNotEmpty()) {
            return applyRecycleWaste(state, nextUndos)
        }
        throw IllegalArgumentException("Cannot draw: both stock and waste are empty")
    }

    private fun applyRecycleWaste(state: KlondikeState, nextUndos: Int): KlondikeState {
        require(state.stock.isEmpty()) { "Cannot recycle waste while stock is not empty" }
        require(state.waste.isNotEmpty()) { "Cannot recycle waste: waste is empty" }
        val recycled = state.waste.reversed().map { it.copy(faceUp = false) }
        return state.copy(
            stock = recycled,
            waste = emptyList(),
            moveCount = state.moveCount + 1,
            previousState = state,
            extraUndos = nextUndos,
        )
    }

    private fun applyWasteToTableau(state: KlondikeState, toColumn: Int, nextUndos: Int): KlondikeState {
        require(toColumn in 0 until TABLEAU_COLUMNS) { "Invalid target column: $toColumn" }
        require(state.waste.isNotEmpty()) { "Waste is empty" }
        val card = state.waste.last()
        require(isTableauPlacementLegal(state.tableau[toColumn], card)) {
            "Cannot place $card onto tableau column $toColumn"
        }
        val nextTableau = state.tableau.toMutableList()
        nextTableau[toColumn] = state.tableau[toColumn] + card
        return state.copy(
            tableau = nextTableau,
            waste = state.waste.dropLast(1),
            moveCount = state.moveCount + 1,
            previousState = state,
            extraUndos = nextUndos,
        )
    }

    private fun applyWasteToFoundation(state: KlondikeState, nextUndos: Int): KlondikeState {
        require(state.waste.isNotEmpty()) { "Waste is empty" }
        val card = state.waste.last()
        require(isFoundationPlacementLegal(state.foundations, card)) {
            "Cannot place $card onto foundation"
        }
        val nextFoundations = state.foundations.toMutableMap()
        val currentPile = nextFoundations[card.suit] ?: emptyList()
        nextFoundations[card.suit] = currentPile + card
        return state.copy(
            foundations = nextFoundations,
            waste = state.waste.dropLast(1),
            moveCount = state.moveCount + 1,
            previousState = state,
            extraUndos = nextUndos,
        )
    }

    private fun applyTableauToTableau(
        state: KlondikeState,
        fromColumn: Int,
        cardIndex: Int,
        toColumn: Int,
        nextUndos: Int,
    ): KlondikeState {
        require(fromColumn in 0 until TABLEAU_COLUMNS) { "Invalid source column: $fromColumn" }
        require(toColumn in 0 until TABLEAU_COLUMNS) { "Invalid target column: $toColumn" }
        require(fromColumn != toColumn) { "Source and target column must differ" }
        val sourcePile = state.tableau[fromColumn]
        require(sourcePile.isNotEmpty()) { "Source column $fromColumn is empty" }
        val resolvedIndex = if (cardIndex == -1) sourcePile.lastIndex else cardIndex
        require(resolvedIndex in sourcePile.indices) { "Card index $resolvedIndex out of range for column $fromColumn" }
        val run = sourcePile.subList(resolvedIndex, sourcePile.size)
        require(isValidRun(run)) { "Selected cards do not form a valid face-up run: $run" }
        val baseCard = run.first()
        require(isTableauPlacementLegal(state.tableau[toColumn], baseCard)) {
            "Cannot place run starting with $baseCard onto tableau column $toColumn"
        }

        val nextTableau = state.tableau.toMutableList()
        val remainingSource = sourcePile.subList(0, resolvedIndex).toMutableList()
        if (remainingSource.isNotEmpty() && !remainingSource.last().faceUp) {
            remainingSource[remainingSource.lastIndex] = remainingSource.last().copy(faceUp = true)
        }
        nextTableau[fromColumn] = remainingSource.toList()
        nextTableau[toColumn] = state.tableau[toColumn] + run

        return state.copy(
            tableau = nextTableau,
            moveCount = state.moveCount + 1,
            previousState = state,
            extraUndos = nextUndos,
        )
    }

    private fun applyTableauToFoundation(state: KlondikeState, fromColumn: Int, nextUndos: Int): KlondikeState {
        require(fromColumn in 0 until TABLEAU_COLUMNS) { "Invalid source column: $fromColumn" }
        val sourcePile = state.tableau[fromColumn]
        require(sourcePile.isNotEmpty()) { "Source column $fromColumn is empty" }
        val card = sourcePile.last()
        require(card.faceUp) { "Top card of column $fromColumn is not face-up" }
        require(isFoundationPlacementLegal(state.foundations, card)) {
            "Cannot place $card onto foundation"
        }

        val nextTableau = state.tableau.toMutableList()
        val remainingSource = sourcePile.dropLast(1).toMutableList()
        if (remainingSource.isNotEmpty() && !remainingSource.last().faceUp) {
            remainingSource[remainingSource.lastIndex] = remainingSource.last().copy(faceUp = true)
        }
        nextTableau[fromColumn] = remainingSource.toList()

        val nextFoundations = state.foundations.toMutableMap()
        val currentPile = nextFoundations[card.suit] ?: emptyList()
        nextFoundations[card.suit] = currentPile + card

        return state.copy(
            tableau = nextTableau,
            foundations = nextFoundations,
            moveCount = state.moveCount + 1,
            previousState = state,
            extraUndos = nextUndos,
        )
    }

    private fun applyFoundationToTableau(state: KlondikeState, suit: Suit, toColumn: Int, nextUndos: Int): KlondikeState {
        require(toColumn in 0 until TABLEAU_COLUMNS) { "Invalid target column: $toColumn" }
        val pile = state.foundations[suit] ?: emptyList()
        require(pile.isNotEmpty()) { "Foundation for $suit is empty" }
        val card = pile.last()
        require(isTableauPlacementLegal(state.tableau[toColumn], card)) {
            "Cannot place $card from foundation onto column $toColumn"
        }

        val nextFoundations = state.foundations.toMutableMap()
        nextFoundations[suit] = pile.dropLast(1)

        val nextTableau = state.tableau.toMutableList()
        nextTableau[toColumn] = state.tableau[toColumn] + card

        return state.copy(
            tableau = nextTableau,
            foundations = nextFoundations,
            moveCount = state.moveCount + 1,
            previousState = state,
            extraUndos = nextUndos,
        )
    }

    fun isWon(state: KlondikeState): Boolean =
        state.foundations.values.all { it.size == 13 }

    fun totalCardCount(state: KlondikeState): Int =
        state.tableau.sumOf { it.size } +
            state.foundations.values.sumOf { it.size } +
            state.stock.size +
            state.waste.size

    /**
     * True when at least one non-oscillating forward move exists: playing to foundation,
     * playing a stock/waste card onto tableau or foundation, moving a complete face-up run
     * to another column (excluding King-to-empty permutations), or splitting a run only to
     * uncover a card that can immediately move to foundation.
     */
    fun hasForwardProgressMove(state: KlondikeState): Boolean {
        if (isWon(state)) return false

        for (col in 0 until TABLEAU_COLUMNS) {
            val card = state.tableau[col].lastOrNull() ?: continue
            if (card.faceUp && isFoundationPlacementLegal(state.foundations, card)) {
                return true
            }
        }

        val availableCards = state.stock + state.waste
        for (card in availableCards) {
            if (isFoundationPlacementLegal(state.foundations, card)) {
                return true
            }
            for (to in 0 until TABLEAU_COLUMNS) {
                if (isTableauPlacementLegal(state.tableau[to], card)) {
                    return true
                }
            }
        }

        for (from in 0 until TABLEAU_COLUMNS) {
            val pile = state.tableau[from]
            if (pile.isEmpty()) continue
            val firstFaceUp = pile.indexOfFirst { it.faceUp }
            if (firstFaceUp == -1) continue

            var runBase = pile.lastIndex
            while (runBase > firstFaceUp) {
                val upper = pile[runBase - 1]
                val lower = pile[runBase]
                if (upper.isRed != lower.isRed && upper.rank == lower.rank + 1) {
                    runBase--
                } else {
                    break
                }
            }

            val baseCard = pile[runBase]
            for (to in 0 until TABLEAU_COLUMNS) {
                if (to == from) continue
                if (isTableauPlacementLegal(state.tableau[to], baseCard)) {
                    val isRedundantKingMove = runBase == 0 && baseCard.rank == KING_RANK && state.tableau[to].isEmpty()
                    if (!isRedundantKingMove) {
                        return true
                    }
                }
            }

            for (splitIndex in (runBase + 1) until pile.size) {
                val exposed = pile[splitIndex - 1]
                if (!isFoundationPlacementLegal(state.foundations, exposed)) continue
                val splitCard = pile[splitIndex]
                for (to in 0 until TABLEAU_COLUMNS) {
                    if (to == from || state.tableau[to].isEmpty()) continue
                    if (isTableauPlacementLegal(state.tableau[to], splitCard)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * True only when no legal move exists among tableau and foundations,
     * and cycling through stock and waste cannot introduce any playable card.
     */
    fun isDeadlocked(state: KlondikeState): Boolean {
        if (isWon(state)) return false

        // 1. Can any tableau card move to a foundation?
        for (col in 0 until TABLEAU_COLUMNS) {
            val card = state.tableau[col].lastOrNull() ?: continue
            if (card.faceUp && isFoundationPlacementLegal(state.foundations, card)) {
                return false
            }
        }

        // 2. Can any tableau card or run move to another tableau column?
        for (from in 0 until TABLEAU_COLUMNS) {
            val pile = state.tableau[from]
            if (pile.isEmpty()) continue
            val firstFaceUp = pile.indexOfFirst { it.faceUp }
            if (firstFaceUp == -1) continue

            for (start in firstFaceUp until pile.size) {
                val run = pile.subList(start, pile.size)
                if (!isValidRun(run)) continue
                val base = run.first()

                for (to in 0 until TABLEAU_COLUMNS) {
                    if (to == from) continue
                    if (isTableauPlacementLegal(state.tableau[to], base)) {
                        // Moving a single King from a column with no cards beneath it to an empty column
                        // does not change game progress; any other move does.
                        val isRedundantKingMove = start == 0 && base.rank == KING_RANK && state.tableau[to].isEmpty()
                        if (!isRedundantKingMove) {
                            return false
                        }
                    }
                }
            }
        }

        // 3. Can any card from foundation move back to tableau?
        for (suit in Suit.entries) {
            val card = state.foundations[suit]?.lastOrNull() ?: continue
            for (to in 0 until TABLEAU_COLUMNS) {
                if (isTableauPlacementLegal(state.tableau[to], card)) {
                    return false
                }
            }
        }

        // 4. Can any card in stock or waste be played to foundation or tableau?
        val availableCards = state.stock + state.waste
        if (availableCards.isNotEmpty()) {
            for (card in availableCards) {
                if (isFoundationPlacementLegal(state.foundations, card)) {
                    return false
                }
                for (to in 0 until TABLEAU_COLUMNS) {
                    if (isTableauPlacementLegal(state.tableau[to], card)) {
                        return false
                    }
                }
            }
        }

        return true
    }
}
