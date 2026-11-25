package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayEngine
import com.datathecodie.witgrid.engine.core.SplitMix64

object KlondikeEngine : PlayEngine<KlondikePuzzle, KlondikeState, CardMove> {
    override val id: String = "klondike"

    override fun decodePuzzle(line: String): KlondikePuzzle = KlondikeCodec.decodeLine(line)

    override fun encodePuzzle(puzzle: KlondikePuzzle): String = KlondikeCodec.encodePuzzle(puzzle)

    override fun initialState(puzzle: KlondikePuzzle): KlondikeState = KlondikeDeal.deal(puzzle)

    override fun applyAction(state: KlondikeState, action: CardMove): KlondikeState =
        KlondikeRules.applyAction(state, action)

    override fun isWon(puzzle: KlondikePuzzle, state: KlondikeState): Boolean =
        KlondikeRules.isWon(state)

    /**
     * True after at least one action when no winning line remains from [state] (for full 52-card games)
     * or only reversible/deadlocked moves remain (for partial fixtures).
     * Suppressed while the one-action Continue grace ([KlondikeState.extraUndos] > 0) is active.
     */
    override fun isLost(puzzle: KlondikePuzzle, state: KlondikeState): Boolean {
        if (state.moveCount == 0) return false
        if (isWon(puzzle, state)) return false
        if (state.extraUndos > 0) return false
        if (KlondikeRules.isDeadlocked(state)) return true
        if (KlondikeRules.totalCardCount(state) == KlondikeRules.TOTAL_CARDS) {
            return !KlondikeSolver.isWinnable(state)
        }
        return !KlondikeRules.hasForwardProgressMove(state)
    }

    override fun hintAction(puzzle: KlondikePuzzle, state: KlondikeState): CardMove? {
        if (isWon(puzzle, state)) return null
        if (state.moveCount > 0 && state.extraUndos == 0) {
            if (KlondikeRules.isDeadlocked(state)) return null
            if (KlondikeRules.totalCardCount(state) != KlondikeRules.TOTAL_CARDS &&
                !KlondikeRules.hasForwardProgressMove(state)
            ) {
                return null
            }
        }
        return KlondikeSolver.solve(state)?.firstOrNull()
    }

    override fun generate(rng: SplitMix64, pack: Pack): KlondikePuzzle =
        KlondikeGenerator.generate(rng, pack)

    override fun encodeState(state: KlondikeState): ByteArray = KlondikeStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): KlondikeState = KlondikeStateCodec.decode(bytes)

    /**
     * Applies one rewarded Continue after failure by restoring [KlondikeState.previousState]
     * and granting a one-action grace in [KlondikeState.extraUndos] so [isLost] clears for the next action.
     */
    override fun applyContinue(state: KlondikeState): KlondikeState {
        require(isLost(state.puzzle, state)) { "Continue only applies after failure" }
        val target = state.previousState ?: state
        return target.copy(
            extraUndos = target.extraUndos + 1,
        )
    }
}
