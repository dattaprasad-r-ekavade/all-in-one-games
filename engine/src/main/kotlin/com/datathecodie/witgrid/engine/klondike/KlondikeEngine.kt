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

    override fun isLost(puzzle: KlondikePuzzle, state: KlondikeState): Boolean {
        if (state.moveCount == 0) return false
        if (isWon(puzzle, state)) return false
        if (state.extraUndos > 0) return false
        return KlondikeRules.isDeadlocked(state)
    }

    override fun hintAction(puzzle: KlondikePuzzle, state: KlondikeState): CardMove? =
        throw NotImplementedError("klondike")

    override fun generate(rng: SplitMix64, pack: Pack): KlondikePuzzle =
        throw NotImplementedError("klondike")

    override fun encodeState(state: KlondikeState): ByteArray = KlondikeStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): KlondikeState = KlondikeStateCodec.decode(bytes)

    override fun applyContinue(state: KlondikeState): KlondikeState {
        require(isLost(state.puzzle, state)) { "Continue only applies after failure" }
        val target = state.previousState ?: state
        return target.copy(
            extraUndos = target.extraUndos + 1,
        )
    }
}
