package com.datathecodie.witgrid.engine.klondike

import java.util.PriorityQueue

/**
 * Deterministic solver for Draw-1 Klondike.
 *
 * Uses safe auto-foundation reduction, lossless empty-column and run-splitting dominance rules,
 * and macro stock-cycle transitions so search branches only on board-changing plays while
 * still returning individual [CardMove] steps of a winning line.
 */
object KlondikeSolver {
    private const val TABLEAU_COLUMNS = KlondikeRules.TABLEAU_COLUMNS
    private const val TOTAL_CARDS = KlondikeRules.TOTAL_CARDS
    private const val SUIT_COUNT = 4
    private const val KING_RANK = KlondikeRules.KING_RANK

    private const val TIER1_MAX_NODES = 1_500
    private const val ENDGAME_EXACT_THRESHOLD_FACEDOWN = 6
    private const val ENDGAME_EXACT_THRESHOLD_STOCK_WASTE = 12
    private const val BEAM_WIDTH = 220
    private const val BEAM_MAX_DEPTH = 140
    private const val TIER3_MAX_NODES = 10_000

    private const val FACE_UP_FLAG = 64
    private const val RANK_MASK = 0x0F
    private const val SUIT_SHIFT = 4
    private const val SUIT_MASK = 0x03

    data class SolveResult(
        val moves: List<CardMove>,
        val exploredNodes: Int,
        val nonDrawMoves: Int,
        val recycleCount: Int,
    )

    internal data class DiagnosticTrace(
        val tier1Result: SolveResult?,
        val tier1Exhausted: Boolean,
        val tier1Nodes: Int,
        val tier2Result: SolveResult?,
        val tier2Exhausted: Boolean,
        val tier2Nodes: Int,
    )

    internal fun solveDiagnostic(state: KlondikeState): DiagnosticTrace {
        val initialPacked = packState(state)
        val initialSafe = collapseSafeMoves(initialPacked)
        val tier1 = runSearch(
            startState = initialSafe.state,
            initialSafeMoves = initialSafe.safeMoves,
            useProgressScore = false,
            maxNodes = TIER1_MAX_NODES,
            allowFoundationToTableau = false,
        )
        val tier2 = runBeamSearch(
            startState = initialSafe.state,
            initialSafeMoves = initialSafe.safeMoves,
            beamWidth = BEAM_WIDTH,
            maxDepth = BEAM_MAX_DEPTH,
            allowFoundationToTableau = false,
        )
        return DiagnosticTrace(
            tier1Result = tier1.result,
            tier1Exhausted = tier1.exhausted,
            tier1Nodes = tier1.explored,
            tier2Result = tier2.result,
            tier2Exhausted = tier2.exhausted,
            tier2Nodes = tier2.explored,
        )
    }

    /**
     * Returns a winning sequence of [CardMove]s starting from [state], or `null` if no winning line
     * is found. Prioritizes shortest winning paths on the acyclic primary move graph.
     */
    fun solve(state: KlondikeState): List<CardMove>? = solveDetailed(state)?.moves

    /** Returns `true` when [solve] finds a winning sequence from [state]. */
    fun isWinnable(state: KlondikeState): Boolean = solveFast(state) != null

    /**
     * Fast winnability check using beam search directly (skipping early-game exact A*),
     * falling back to Tier 3 rescue if needed.
     */
    private fun solveFast(state: KlondikeState): SolveResult? {
        if (KlondikeRules.isWon(state)) {
            return SolveResult(emptyList(), 0, 0, 0)
        }
        val initialPacked = packState(state)
        val initialSafe = collapseSafeMoves(initialPacked)
        if (initialSafe.state.isWon()) {
            return buildResult(initialSafe.safeMoves, exploredNodes = 0)
        }
        val fast = runBeamSearch(
            startState = initialSafe.state,
            initialSafeMoves = initialSafe.safeMoves,
            beamWidth = BEAM_WIDTH,
            maxDepth = BEAM_MAX_DEPTH,
            allowFoundationToTableau = false,
        )
        if (fast.result != null) return fast.result
        if (initialSafe.state.foundations.any { it >= 3 }) {
            val tier3 = runSearch(
                startState = initialSafe.state,
                initialSafeMoves = initialSafe.safeMoves,
                useProgressScore = true,
                maxNodes = TIER3_MAX_NODES,
                allowFoundationToTableau = true,
            )
            if (tier3.result != null) return tier3.result
        }
        return null
    }

    /** Solves [state] and reports the winning moves alongside path and search metrics. */
    fun solveDetailed(state: KlondikeState): SolveResult? {
        if (KlondikeRules.isWon(state)) {
            return SolveResult(
                moves = emptyList(),
                exploredNodes = 0,
                nonDrawMoves = 0,
                recycleCount = 0,
            )
        }

        val initialPacked = packState(state)
        val initialSafe = collapseSafeMoves(initialPacked)
        if (initialSafe.state.isWon()) {
            return buildResult(initialSafe.safeMoves, exploredNodes = 0)
        }

        val tryExactFirst = countFaceDown(initialSafe.state) <= ENDGAME_EXACT_THRESHOLD_FACEDOWN &&
            (initialSafe.state.stock.size + initialSafe.state.waste.size) <= ENDGAME_EXACT_THRESHOLD_STOCK_WASTE
        if (tryExactFirst) {
            val tier1 = runSearch(
                startState = initialSafe.state,
                initialSafeMoves = initialSafe.safeMoves,
                useProgressScore = false,
                maxNodes = TIER1_MAX_NODES,
                allowFoundationToTableau = false,
            )
            if (tier1.result != null) return tier1.result
            if (tier1.exhausted && initialSafe.state.foundations.none { it >= 3 }) {
                return null
            }
        }

        // Tier 2: Beam search on the cycle-free primary graph (never stalls in one dead-end subtree).
        val tier2 = runBeamSearch(
            startState = initialSafe.state,
            initialSafeMoves = initialSafe.safeMoves,
            beamWidth = BEAM_WIDTH,
            maxDepth = BEAM_MAX_DEPTH,
            allowFoundationToTableau = false,
        )
        if (tier2.result != null) return tier2.result

        // Tier 3: Bounded rescue search allowing FoundationToTableau when ranks >= 3 are on foundations.
        if (initialSafe.state.foundations.any { it >= 3 }) {
            val tier3 = runSearch(
                startState = initialSafe.state,
                initialSafeMoves = initialSafe.safeMoves,
                useProgressScore = true,
                maxNodes = TIER3_MAX_NODES,
                allowFoundationToTableau = true,
            )
            if (tier3.result != null) return tier3.result
        }

        return null
    }

    private data class SearchOutcome(
        val result: SolveResult?,
        val exhausted: Boolean,
        val explored: Int,
    )

    private class EdgeStep(
        val drawsBeforeRecycle: Int,
        val doRecycle: Boolean,
        val drawsAfterRecycle: Int,
        val primaryMove: CardMove?,
        val secondaryMove: CardMove?,
        val safeMoves: List<CardMove>,
    ) {
        val totalMoves: Int =
            drawsBeforeRecycle +
                (if (doRecycle) 1 else 0) +
                drawsAfterRecycle +
                (if (primaryMove != null) 1 else 0) +
                (if (secondaryMove != null) 1 else 0) +
                safeMoves.size

        fun appendTo(target: MutableList<CardMove>) {
            repeat(drawsBeforeRecycle) { target.add(CardMove.DrawStock) }
            if (doRecycle) target.add(CardMove.RecycleWaste)
            repeat(drawsAfterRecycle) { target.add(CardMove.DrawStock) }
            if (primaryMove != null) target.add(primaryMove)
            if (secondaryMove != null) target.add(secondaryMove)
            if (safeMoves.isNotEmpty()) target.addAll(safeMoves)
        }
    }

    private class SearchNode(
        val state: PackedState,
        val key: StateKey,
        val parent: SearchNode?,
        val edge: EdgeStep,
        val g: Int,
        val h: Int,
        val score: Int,
        val insertionOrder: Long,
    ) : Comparable<SearchNode> {
        override fun compareTo(other: SearchNode): Int {
            val scoreCompare = score.compareTo(other.score)
            if (scoreCompare != 0) return scoreCompare
            val hCompare = h.compareTo(other.h)
            if (hCompare != 0) return hCompare
            val stockWasteCompare = (state.stock.size + state.waste.size)
                .compareTo(other.state.stock.size + other.state.waste.size)
            if (stockWasteCompare != 0) return stockWasteCompare
            return insertionOrder.compareTo(other.insertionOrder)
        }
    }

    private class StateKey(private val bytes: ByteArray) {
        private val hash: Int = bytes.contentHashCode()

        override fun equals(other: Any?): Boolean =
            other is StateKey && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = hash
    }

    private class PackedState(
        val tableau: Array<IntArray>,
        val foundations: IntArray,
        val stock: IntArray,
        val waste: IntArray,
    ) {
        fun isWon(): Boolean =
            foundations[0] == KING_RANK &&
                foundations[1] == KING_RANK &&
                foundations[2] == KING_RANK &&
                foundations[3] == KING_RANK

        fun toKey(): StateKey {
            var totalBytes = SUIT_COUNT + TABLEAU_COLUMNS + 2 + stock.size + waste.size
            for (column in 0 until TABLEAU_COLUMNS) {
                totalBytes += tableau[column].size
            }
            val bytes = ByteArray(totalBytes)
            var offset = 0
            for (suit in 0 until SUIT_COUNT) {
                bytes[offset++] = foundations[suit].toByte()
            }
            for (column in 0 until TABLEAU_COLUMNS) {
                val pile = tableau[column]
                bytes[offset++] = pile.size.toByte()
                for (card in pile) {
                    bytes[offset++] = card.toByte()
                }
            }
            bytes[offset++] = stock.size.toByte()
            for (card in stock) {
                bytes[offset++] = card.toByte()
            }
            bytes[offset++] = waste.size.toByte()
            for (card in waste) {
                bytes[offset++] = card.toByte()
            }
            return StateKey(bytes)
        }
    }

    private class CollapseResult(
        val state: PackedState,
        val safeMoves: List<CardMove>,
    )

    private class CandidateTransition(
        val state: PackedState,
        val drawsBeforeRecycle: Int,
        val doRecycle: Boolean,
        val drawsAfterRecycle: Int,
        val primaryMove: CardMove,
        val secondaryMove: CardMove? = null,
    )

    private fun runSearch(
        startState: PackedState,
        initialSafeMoves: List<CardMove>,
        useProgressScore: Boolean,
        maxNodes: Int,
        allowFoundationToTableau: Boolean,
    ): SearchOutcome {
        val openSet = PriorityQueue<SearchNode>()
        val bestG = HashMap<StateKey, Int>()
        var insertionCounter = 0L

        val startEdge = EdgeStep(
            drawsBeforeRecycle = 0,
            doRecycle = false,
            drawsAfterRecycle = 0,
            primaryMove = null,
            secondaryMove = null,
            safeMoves = initialSafeMoves,
        )
        val startKey = startState.toKey()
        val startH = if (useProgressScore) progressHeuristic(startState) else heuristic(startState)
        val startNode = SearchNode(
            state = startState,
            key = startKey,
            parent = null,
            edge = startEdge,
            g = startEdge.totalMoves,
            h = startH,
            score = if (useProgressScore) startEdge.totalMoves + 2 * startH else startEdge.totalMoves + startH,
            insertionOrder = insertionCounter++,
        )
        openSet.add(startNode)
        bestG[startKey] = startNode.g

        var explored = 0
        while (openSet.isNotEmpty() && explored < maxNodes) {
            val current = openSet.poll() ?: break
            val recordedG = bestG[current.key]
            if (recordedG != null && current.g > recordedG) {
                continue
            }

            explored++
            if (current.state.isWon()) {
                return SearchOutcome(
                    result = buildResult(reconstructMoves(current), explored),
                    exhausted = false,
                    explored = explored,
                )
            }

            val candidates = generateCandidates(current.state, allowFoundationToTableau)
            for (candidate in candidates) {
                val collapsed = collapseSafeMoves(candidate.state)
                val edge = EdgeStep(
                    drawsBeforeRecycle = candidate.drawsBeforeRecycle,
                    doRecycle = candidate.doRecycle,
                    drawsAfterRecycle = candidate.drawsAfterRecycle,
                    primaryMove = candidate.primaryMove,
                    secondaryMove = candidate.secondaryMove,
                    safeMoves = collapsed.safeMoves,
                )
                val nextG = current.g + edge.totalMoves
                val nextKey = collapsed.state.toKey()

                val previousG = bestG[nextKey]
                if (previousG != null && nextG >= previousG) {
                    continue
                }
                bestG[nextKey] = nextG

                val nextH = if (useProgressScore) progressHeuristic(collapsed.state) else heuristic(collapsed.state)
                val nextNode = SearchNode(
                    state = collapsed.state,
                    key = nextKey,
                    parent = current,
                    edge = edge,
                    g = nextG,
                    h = nextH,
                    score = if (useProgressScore) nextG + 2 * nextH else nextG + nextH,
                    insertionOrder = insertionCounter++,
                )
                openSet.add(nextNode)
            }
        }

        return SearchOutcome(
            result = null,
            exhausted = openSet.isEmpty(),
            explored = explored,
        )
    }

    private fun runBeamSearch(
        startState: PackedState,
        initialSafeMoves: List<CardMove>,
        beamWidth: Int,
        maxDepth: Int,
        allowFoundationToTableau: Boolean,
    ): SearchOutcome {
        val bestG = HashMap<StateKey, Int>()
        var insertionCounter = 0L

        val startEdge = EdgeStep(
            drawsBeforeRecycle = 0,
            doRecycle = false,
            drawsAfterRecycle = 0,
            primaryMove = null,
            secondaryMove = null,
            safeMoves = initialSafeMoves,
        )
        val startKey = startState.toKey()
        val startH = progressHeuristic(startState)
        val startNode = SearchNode(
            state = startState,
            key = startKey,
            parent = null,
            edge = startEdge,
            g = startEdge.totalMoves,
            h = startH,
            score = startEdge.totalMoves + 4 * startH,
            insertionOrder = insertionCounter++,
        )
        bestG[startKey] = startNode.g

        var beam = listOf(startNode)
        var explored = 0
        var truncated = false

        for (depth in 0 until maxDepth) {
            if (beam.isEmpty()) break

            val nextLayer = ArrayList<SearchNode>(beam.size * 4)
            var bestGoal: SearchNode? = null

            for (current in beam) {
                explored++
                if (current.state.isWon()) {
                    return SearchOutcome(
                        result = buildResult(reconstructMoves(current), explored),
                        exhausted = false,
                        explored = explored,
                    )
                }

                val candidates = generateCandidates(current.state, allowFoundationToTableau)
                for (candidate in candidates) {
                    val collapsed = collapseSafeMoves(candidate.state)
                    val edge = EdgeStep(
                        drawsBeforeRecycle = candidate.drawsBeforeRecycle,
                        doRecycle = candidate.doRecycle,
                        drawsAfterRecycle = candidate.drawsAfterRecycle,
                        primaryMove = candidate.primaryMove,
                        secondaryMove = candidate.secondaryMove,
                        safeMoves = collapsed.safeMoves,
                    )
                    val nextG = current.g + edge.totalMoves
                    val nextKey = collapsed.state.toKey()

                    val previousG = bestG[nextKey]
                    if (previousG != null && nextG >= previousG) {
                        continue
                    }
                    bestG[nextKey] = nextG

                    if (collapsed.state.isWon()) {
                        val goalNode = SearchNode(
                            state = collapsed.state,
                            key = nextKey,
                            parent = current,
                            edge = edge,
                            g = nextG,
                            h = 0,
                            score = nextG,
                            insertionOrder = insertionCounter++,
                        )
                        if (bestGoal == null || goalNode.g < bestGoal.g) {
                            bestGoal = goalNode
                        }
                        continue
                    }

                    val nextH = progressHeuristic(collapsed.state)
                    nextLayer.add(
                        SearchNode(
                            state = collapsed.state,
                            key = nextKey,
                            parent = current,
                            edge = edge,
                            g = nextG,
                            h = nextH,
                            score = nextG + 4 * nextH,
                            insertionOrder = insertionCounter++,
                        ),
                    )
                }
            }

            if (bestGoal != null) {
                return SearchOutcome(
                    result = buildResult(reconstructMoves(bestGoal), explored),
                    exhausted = false,
                    explored = explored,
                )
            }

            if (nextLayer.isEmpty()) {
                beam = emptyList()
                break
            }

            nextLayer.sort()
            beam = if (nextLayer.size > beamWidth) {
                truncated = true
                nextLayer.subList(0, beamWidth).toList()
            } else {
                nextLayer
            }
        }

        return SearchOutcome(
            result = null,
            exhausted = !truncated && beam.isEmpty(),
            explored = explored,
        )
    }

    private fun reconstructMoves(goalNode: SearchNode): List<CardMove> {
        val edges = ArrayList<EdgeStep>()
        var current: SearchNode? = goalNode
        while (current != null) {
            if (current.edge.totalMoves > 0) {
                edges.add(current.edge)
            }
            current = current.parent
        }
        edges.reverse()
        val totalMoves = edges.sumOf { it.totalMoves }
        val result = ArrayList<CardMove>(totalMoves)
        for (edge in edges) {
            edge.appendTo(result)
        }
        return result
    }

    private fun buildResult(moves: List<CardMove>, exploredNodes: Int): SolveResult {
        var nonDrawMoves = 0
        var recycleCount = 0
        for (move in moves) {
            when (move) {
                is CardMove.DrawStock -> Unit
                is CardMove.RecycleWaste -> recycleCount++
                else -> nonDrawMoves++
            }
        }
        return SolveResult(
            moves = moves,
            exploredNodes = exploredNodes,
            nonDrawMoves = nonDrawMoves,
            recycleCount = recycleCount,
        )
    }

    private fun collapseSafeMoves(initialState: PackedState): CollapseResult {
        var state = initialState
        var safeMoves: MutableList<CardMove>? = null

        while (true) {
            var moved = false
            for (column in 0 until TABLEAU_COLUMNS) {
                val pile = state.tableau[column]
                if (pile.isEmpty()) continue
                val topCard = pile[pile.lastIndex]
                if (!isCardFaceUp(topCard)) continue
                val suit = cardSuit(topCard)
                val rank = cardRank(topCard)
                if (state.foundations[suit] + 1 == rank && isSafeToFoundation(state.foundations, rank, suit)) {
                    state = moveTableauTopToFoundation(state, column)
                    if (safeMoves == null) safeMoves = ArrayList(4)
                    safeMoves.add(CardMove.TableauToFoundation(column))
                    moved = true
                    break
                }
            }
            if (moved) continue

            if (state.waste.isNotEmpty()) {
                val topCard = state.waste[state.waste.lastIndex]
                val suit = cardSuit(topCard)
                val rank = cardRank(topCard)
                if (state.foundations[suit] + 1 == rank && isSafeToFoundation(state.foundations, rank, suit)) {
                    state = moveWasteTopToFoundation(state)
                    if (safeMoves == null) safeMoves = ArrayList(4)
                    safeMoves.add(CardMove.WasteToFoundation)
                    continue
                }
            }
            break
        }

        return CollapseResult(state, safeMoves ?: emptyList())
    }

    /**
     * A card of [rank] and [suit] can move safely to foundation whenever all opposite-color cards
     * of rank - 1 and the sibling same-color card of rank - 2 are already on foundations.
     */
    private fun isSafeToFoundation(foundations: IntArray, rank: Int, suit: Int): Boolean {
        if (rank <= 2) return true
        val red = isSuitRed(suit)
        val minOpposite = if (red) {
            minOf(foundations[0], foundations[3])
        } else {
            minOf(foundations[1], foundations[2])
        }
        if (minOpposite < rank - 1) return false

        val otherSame = if (red) {
            if (suit == 1) foundations[2] else foundations[1]
        } else {
            if (suit == 0) foundations[3] else foundations[0]
        }
        return otherSame >= rank - 2
    }

    private fun generateCandidates(
        state: PackedState,
        allowFoundationToTableau: Boolean,
    ): List<CandidateTransition> {
        val candidates = ArrayList<CandidateTransition>(16)
        val firstEmptyColumn = firstEmptyTableauColumn(state.tableau)

        // 1. Non-safe tableau top -> foundation moves.
        for (column in 0 until TABLEAU_COLUMNS) {
            val pile = state.tableau[column]
            if (pile.isEmpty()) continue
            val topCard = pile[pile.lastIndex]
            if (isCardFaceUp(topCard) && canPlaceOnFoundation(state.foundations, topCard)) {
                candidates.add(
                    CandidateTransition(
                        state = moveTableauTopToFoundation(state, column),
                        drawsBeforeRecycle = 0,
                        doRecycle = false,
                        drawsAfterRecycle = 0,
                        primaryMove = CardMove.TableauToFoundation(column),
                    ),
                )
            }
        }

        // 2. Top of waste (0 draws) -> foundation or tableau.
        if (state.waste.isNotEmpty()) {
            val topWaste = state.waste[state.waste.lastIndex]
            if (canPlaceOnFoundation(state.foundations, topWaste)) {
                candidates.add(
                    CandidateTransition(
                        state = moveWasteTopToFoundation(state),
                        drawsBeforeRecycle = 0,
                        doRecycle = false,
                        drawsAfterRecycle = 0,
                        primaryMove = CardMove.WasteToFoundation,
                    ),
                )
            }
            for (toColumn in 0 until TABLEAU_COLUMNS) {
                val targetPile = state.tableau[toColumn]
                if (targetPile.isEmpty() && toColumn != firstEmptyColumn) continue
                if (canPlaceOnTableau(targetPile, topWaste)) {
                    candidates.add(
                        CandidateTransition(
                            state = moveWasteTopToTableau(state, toColumn),
                            drawsBeforeRecycle = 0,
                            doRecycle = false,
                            drawsAfterRecycle = 0,
                            primaryMove = CardMove.WasteToTableau(toColumn),
                        ),
                    )
                }
            }
        }

        // 3. Tableau -> tableau moves (maximal valid run, or split only when freeing a foundation card).
        for (fromColumn in 0 until TABLEAU_COLUMNS) {
            val pile = state.tableau[fromColumn]
            if (pile.isEmpty()) continue
            val firstFaceUp = firstFaceUpIndex(pile)
            if (firstFaceUp == -1) continue
            val runBase = findRunBase(pile, firstFaceUp)

            val baseCard = pile[runBase]
            for (toColumn in 0 until TABLEAU_COLUMNS) {
                if (toColumn == fromColumn) continue
                val targetPile = state.tableau[toColumn]
                if (targetPile.isEmpty()) {
                    if (toColumn != firstEmptyColumn) continue
                    // Moving a bottom-most King to an empty column is a pure permutation.
                    if (runBase == 0) continue
                }
                if (canPlaceOnTableau(targetPile, baseCard)) {
                    candidates.add(
                        CandidateTransition(
                            state = moveTableauRun(state, fromColumn, runBase, toColumn),
                            drawsBeforeRecycle = 0,
                            doRecycle = false,
                            drawsAfterRecycle = 0,
                            primaryMove = CardMove.TableauToTableau(fromColumn, runBase, toColumn),
                        ),
                    )
                }
            }

            // Splitting a valid run is useful only when the newly exposed card immediately goes to foundation.
            for (cardIndex in (runBase + 1) until pile.size) {
                val exposedCard = pile[cardIndex - 1]
                if (!canPlaceOnFoundation(state.foundations, exposedCard)) continue
                val splitCard = pile[cardIndex]
                for (toColumn in 0 until TABLEAU_COLUMNS) {
                    if (toColumn == fromColumn) continue
                    val targetPile = state.tableau[toColumn]
                    if (targetPile.isEmpty()) continue
                    if (canPlaceOnTableau(targetPile, splitCard)) {
                        val afterRunMove = moveTableauRun(state, fromColumn, cardIndex, toColumn)
                        val afterFoundation = moveTableauTopToFoundation(afterRunMove, fromColumn)
                        candidates.add(
                            CandidateTransition(
                                state = afterFoundation,
                                drawsBeforeRecycle = 0,
                                doRecycle = false,
                                drawsAfterRecycle = 0,
                                primaryMove = CardMove.TableauToTableau(fromColumn, cardIndex, toColumn),
                                secondaryMove = CardMove.TableauToFoundation(fromColumn),
                            ),
                        )
                    }
                }
            }
        }

        // 4. Macro stock-cycle moves for cards currently in stock.
        for (stockIndex in state.stock.indices) {
            val drawnCard = makeFaceUp(state.stock[stockIndex])
            val drawsNeeded = state.stock.size - stockIndex
            val canGoFoundation = canPlaceOnFoundation(state.foundations, drawnCard)
            val tableauTargets = legalTableauTargets(state.tableau, drawnCard, firstEmptyColumn)
            if (!canGoFoundation && tableauTargets.isEmpty()) continue

            val nextStock = state.stock.copyOfRange(0, stockIndex)
            val skippedCount = state.stock.size - 1 - stockIndex
            val nextWaste = IntArray(state.waste.size + skippedCount)
            state.waste.copyInto(nextWaste, destinationOffset = 0)
            for (offset in 0 until skippedCount) {
                val skippedIndex = state.stock.size - 1 - offset
                nextWaste[state.waste.size + offset] = makeFaceUp(state.stock[skippedIndex])
            }

            if (canGoFoundation) {
                val nextFoundations = state.foundations.copyOf()
                nextFoundations[cardSuit(drawnCard)]++
                candidates.add(
                    CandidateTransition(
                        state = PackedState(state.tableau, nextFoundations, nextStock, nextWaste),
                        drawsBeforeRecycle = drawsNeeded,
                        doRecycle = false,
                        drawsAfterRecycle = 0,
                        primaryMove = CardMove.WasteToFoundation,
                    ),
                )
            }
            for (toColumn in tableauTargets) {
                val nextTableau = state.tableau.copyOf()
                nextTableau[toColumn] = appendCard(state.tableau[toColumn], drawnCard)
                candidates.add(
                    CandidateTransition(
                        state = PackedState(nextTableau, state.foundations, nextStock, nextWaste),
                        drawsBeforeRecycle = drawsNeeded,
                        doRecycle = false,
                        drawsAfterRecycle = 0,
                        primaryMove = CardMove.WasteToTableau(toColumn),
                    ),
                )
            }
        }

        // 5. Macro recycle-and-draw moves for cards buried below the top of waste.
        if (state.waste.size > 1) {
            for (wasteIndex in 0 until state.waste.size - 1) {
                val targetCard = state.waste[wasteIndex]
                val canGoFoundation = canPlaceOnFoundation(state.foundations, targetCard)
                val tableauTargets = legalTableauTargets(state.tableau, targetCard, firstEmptyColumn)
                if (!canGoFoundation && tableauTargets.isEmpty()) continue

                val remainingAfterIndex = state.waste.size - 1 - wasteIndex
                val nextStock = IntArray(state.stock.size + remainingAfterIndex)
                state.stock.copyInto(nextStock, destinationOffset = 0)
                for (offset in 0 until remainingAfterIndex) {
                    val sourceWasteIndex = state.waste.size - 1 - offset
                    nextStock[state.stock.size + offset] = makeFaceDown(state.waste[sourceWasteIndex])
                }
                val nextWaste = state.waste.copyOfRange(0, wasteIndex)

                if (canGoFoundation) {
                    val nextFoundations = state.foundations.copyOf()
                    nextFoundations[cardSuit(targetCard)]++
                    candidates.add(
                        CandidateTransition(
                            state = PackedState(state.tableau, nextFoundations, nextStock, nextWaste),
                            drawsBeforeRecycle = state.stock.size,
                            doRecycle = true,
                            drawsAfterRecycle = wasteIndex + 1,
                            primaryMove = CardMove.WasteToFoundation,
                        ),
                    )
                }
                for (toColumn in tableauTargets) {
                    val nextTableau = state.tableau.copyOf()
                    nextTableau[toColumn] = appendCard(state.tableau[toColumn], targetCard)
                    candidates.add(
                        CandidateTransition(
                            state = PackedState(nextTableau, state.foundations, nextStock, nextWaste),
                            drawsBeforeRecycle = state.stock.size,
                            doRecycle = true,
                            drawsAfterRecycle = wasteIndex + 1,
                            primaryMove = CardMove.WasteToTableau(toColumn),
                        ),
                    )
                }
            }
        }

        // 6. Optional Foundation -> Tableau rescue moves in Tier 3.
        if (allowFoundationToTableau) {
            for (suit in 0 until SUIT_COUNT) {
                val rank = state.foundations[suit]
                if (rank < 3 || isSafeToFoundation(state.foundations, rank, suit)) continue
                val card = packCard(rank, suit, faceUp = true)
                for (toColumn in 0 until TABLEAU_COLUMNS) {
                    val targetPile = state.tableau[toColumn]
                    if (targetPile.isEmpty() && toColumn != firstEmptyColumn) continue
                    if (canPlaceOnTableau(targetPile, card)) {
                        val nextFoundations = state.foundations.copyOf()
                        nextFoundations[suit] = rank - 1
                        val nextTableau = state.tableau.copyOf()
                        nextTableau[toColumn] = appendCard(targetPile, card)
                        candidates.add(
                            CandidateTransition(
                                state = PackedState(nextTableau, nextFoundations, state.stock, state.waste),
                                drawsBeforeRecycle = 0,
                                doRecycle = false,
                                drawsAfterRecycle = 0,
                                primaryMove = CardMove.FoundationToTableau(Suit.entries[suit], toColumn),
                            ),
                        )
                    }
                }
            }
        }

        return candidates
    }

    private fun heuristic(state: PackedState): Int {
        val foundationTotal =
            state.foundations[0] + state.foundations[1] + state.foundations[2] + state.foundations[3]
        var estimate = TOTAL_CARDS - foundationTotal + state.stock.size

        for (column in 0 until TABLEAU_COLUMNS) {
            val pile = state.tableau[column]
            if (hasSameSuitInversion(pile)) {
                estimate++
            }
        }
        if (hasSameSuitInversion(state.waste)) {
            estimate++
        }
        return estimate
    }

    /**
     * Progress-guided heuristic for Tier 2/3 weighted search.
     * Avoids rewarding deep stock burns; prioritizes uncovering face-down cards and building foundations.
     */
    private fun progressHeuristic(state: PackedState): Int {
        val foundationTotal =
            state.foundations[0] + state.foundations[1] + state.foundations[2] + state.foundations[3]
        return 2 * (TOTAL_CARDS - foundationTotal) +
            3 * countFaceDown(state) +
            (state.stock.size + state.waste.size)
    }

    private fun countFaceDown(state: PackedState): Int {
        var faceDownCount = 0
        for (column in 0 until TABLEAU_COLUMNS) {
            val pile = state.tableau[column]
            for (card in pile) {
                if (!isCardFaceUp(card)) faceDownCount++
            }
        }
        return faceDownCount
    }

    private fun hasSameSuitInversion(cards: IntArray): Boolean {
        if (cards.size < 2) return false
        for (lowerIndex in 0 until cards.size - 1) {
            val lowerCard = cards[lowerIndex]
            val lowerSuit = cardSuit(lowerCard)
            val lowerRank = cardRank(lowerCard)
            for (upperIndex in (lowerIndex + 1) until cards.size) {
                val upperCard = cards[upperIndex]
                if (cardSuit(upperCard) == lowerSuit && cardRank(upperCard) > lowerRank) {
                    return true
                }
            }
        }
        return false
    }

    private fun legalTableauTargets(
        tableau: Array<IntArray>,
        card: Int,
        firstEmptyColumn: Int,
    ): List<Int> {
        val targets = ArrayList<Int>(2)
        for (column in 0 until TABLEAU_COLUMNS) {
            val pile = tableau[column]
            if (pile.isEmpty()) {
                if (column == firstEmptyColumn && cardRank(card) == KING_RANK) {
                    targets.add(column)
                }
            } else if (canPlaceOnTableau(pile, card)) {
                targets.add(column)
            }
        }
        return targets
    }

    private fun moveTableauTopToFoundation(state: PackedState, column: Int): PackedState {
        val pile = state.tableau[column]
        val card = pile[pile.lastIndex]
        val nextTableau = state.tableau.copyOf()
        val remaining = pile.copyOf(pile.size - 1)
        if (remaining.isNotEmpty() && !isCardFaceUp(remaining[remaining.lastIndex])) {
            remaining[remaining.lastIndex] = makeFaceUp(remaining[remaining.lastIndex])
        }
        nextTableau[column] = remaining
        val nextFoundations = state.foundations.copyOf()
        nextFoundations[cardSuit(card)]++
        return PackedState(nextTableau, nextFoundations, state.stock, state.waste)
    }

    private fun moveWasteTopToFoundation(state: PackedState): PackedState {
        val card = state.waste[state.waste.lastIndex]
        val nextFoundations = state.foundations.copyOf()
        nextFoundations[cardSuit(card)]++
        val nextWaste = state.waste.copyOf(state.waste.size - 1)
        return PackedState(state.tableau, nextFoundations, state.stock, nextWaste)
    }

    private fun moveWasteTopToTableau(state: PackedState, toColumn: Int): PackedState {
        val card = state.waste[state.waste.lastIndex]
        val nextTableau = state.tableau.copyOf()
        nextTableau[toColumn] = appendCard(state.tableau[toColumn], card)
        val nextWaste = state.waste.copyOf(state.waste.size - 1)
        return PackedState(nextTableau, state.foundations, state.stock, nextWaste)
    }

    private fun moveTableauRun(
        state: PackedState,
        fromColumn: Int,
        startIndex: Int,
        toColumn: Int,
    ): PackedState {
        val sourcePile = state.tableau[fromColumn]
        val targetPile = state.tableau[toColumn]
        val runLength = sourcePile.size - startIndex

        val nextSource = sourcePile.copyOf(startIndex)
        if (nextSource.isNotEmpty() && !isCardFaceUp(nextSource[nextSource.lastIndex])) {
            nextSource[nextSource.lastIndex] = makeFaceUp(nextSource[nextSource.lastIndex])
        }
        val nextTarget = IntArray(targetPile.size + runLength)
        targetPile.copyInto(nextTarget, destinationOffset = 0)
        sourcePile.copyInto(nextTarget, destinationOffset = targetPile.size, startIndex = startIndex)

        val nextTableau = state.tableau.copyOf()
        nextTableau[fromColumn] = nextSource
        nextTableau[toColumn] = nextTarget
        return PackedState(nextTableau, state.foundations, state.stock, state.waste)
    }

    private fun appendCard(pile: IntArray, card: Int): IntArray {
        val next = pile.copyOf(pile.size + 1)
        next[pile.size] = card
        return next
    }

    private fun firstEmptyTableauColumn(tableau: Array<IntArray>): Int {
        for (column in 0 until TABLEAU_COLUMNS) {
            if (tableau[column].isEmpty()) return column
        }
        return -1
    }

    private fun firstFaceUpIndex(pile: IntArray): Int {
        for (index in pile.indices) {
            if (isCardFaceUp(pile[index])) return index
        }
        return -1
    }

    private fun findRunBase(pile: IntArray, firstFaceUp: Int): Int {
        var runBase = pile.lastIndex
        while (runBase > firstFaceUp) {
            val upper = pile[runBase - 1]
            val lower = pile[runBase]
            if (isCardRed(upper) != isCardRed(lower) && cardRank(upper) == cardRank(lower) + 1) {
                runBase--
            } else {
                break
            }
        }
        return runBase
    }

    private fun canPlaceOnTableau(targetPile: IntArray, card: Int): Boolean {
        if (targetPile.isEmpty()) {
            return cardRank(card) == KING_RANK
        }
        val top = targetPile[targetPile.lastIndex]
        return isCardFaceUp(top) &&
            isCardRed(top) != isCardRed(card) &&
            cardRank(top) == cardRank(card) + 1
    }

    private fun canPlaceOnFoundation(foundations: IntArray, card: Int): Boolean {
        val suit = cardSuit(card)
        val rank = cardRank(card)
        return foundations[suit] + 1 == rank
    }

    private fun packState(state: KlondikeState): PackedState {
        val tableau = Array(TABLEAU_COLUMNS) { column ->
            val pile = state.tableau[column]
            IntArray(pile.size) { index -> packCard(pile[index]) }
        }
        val foundations = IntArray(SUIT_COUNT) { suitOrdinal ->
            val pile = state.foundations[Suit.entries[suitOrdinal]] ?: emptyList()
            pile.lastOrNull()?.rank ?: 0
        }
        val stock = IntArray(state.stock.size) { index -> packCard(state.stock[index]) }
        val waste = IntArray(state.waste.size) { index -> packCard(state.waste[index]) }
        return PackedState(tableau, foundations, stock, waste)
    }

    private fun packCard(card: Card): Int = packCard(card.rank, card.suit.ordinal, card.faceUp)

    private fun packCard(rank: Int, suitOrdinal: Int, faceUp: Boolean): Int =
        (if (faceUp) FACE_UP_FLAG else 0) or (suitOrdinal shl SUIT_SHIFT) or rank

    private fun cardRank(packed: Int): Int = packed and RANK_MASK

    private fun cardSuit(packed: Int): Int = (packed ushr SUIT_SHIFT) and SUIT_MASK

    private fun isSuitRed(suitOrdinal: Int): Boolean = suitOrdinal == 1 || suitOrdinal == 2

    private fun isCardRed(packed: Int): Boolean = isSuitRed(cardSuit(packed))

    private fun isCardFaceUp(packed: Int): Boolean = (packed and FACE_UP_FLAG) != 0

    private fun makeFaceUp(packed: Int): Int = packed or FACE_UP_FLAG

    private fun makeFaceDown(packed: Int): Int = packed and FACE_UP_FLAG.inv()
}
