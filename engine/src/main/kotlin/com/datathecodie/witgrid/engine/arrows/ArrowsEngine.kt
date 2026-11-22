package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayEngine
import com.datathecodie.witgrid.engine.core.SplitMix64

object ArrowsEngine : PlayEngine<ArrowsPuzzle, ArrowsState, ArrowsAction> {
    override val id: String = "arrows"

    override fun decodePuzzle(line: String): ArrowsPuzzle = ArrowsCodec.decodeLine(line)

    override fun encodePuzzle(puzzle: ArrowsPuzzle): String = ArrowsCodec.encodePuzzle(puzzle)

    override fun initialState(puzzle: ArrowsPuzzle): ArrowsState =
        ArrowsState(
            puzzle = puzzle,
            arrows = puzzle.initialArrows.toList(),
            slideCount = 0,
            history = emptyList(),
        )

    override fun applyAction(state: ArrowsState, action: ArrowsAction): ArrowsState {
        val nextArrows = ArrowsRules.applySlide(state.puzzle.size, state.arrows, action.cell)
        return state.copy(
            arrows = nextArrows,
            slideCount = state.slideCount + 1,
            history = state.history + listOf(state.arrows),
        )
    }

    override fun isWon(puzzle: ArrowsPuzzle, state: ArrowsState): Boolean =
        ArrowsRules.isWon(state.arrows)

    override fun isLost(puzzle: ArrowsPuzzle, state: ArrowsState): Boolean =
        ArrowsRules.isLost(puzzle.size, state.arrows, state.slideCount)

    override fun hintAction(puzzle: ArrowsPuzzle, state: ArrowsState): ArrowsAction? =
        throw NotImplementedError("arrows")

    override fun generate(rng: SplitMix64, pack: Pack): ArrowsPuzzle =
        throw NotImplementedError("arrows")

    override fun encodeState(state: ArrowsState): ByteArray = ArrowsStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): ArrowsState = ArrowsStateCodec.decode(bytes)

    override fun applyContinue(state: ArrowsState): ArrowsState {
        require(isLost(state.puzzle, state)) { "Continue only applies after failure" }
        val restoredArrows = state.history.lastOrNull() ?: state.puzzle.initialArrows
        val newHistory = if (state.history.isNotEmpty()) state.history.dropLast(1) else emptyList()
        return state.copy(
            arrows = restoredArrows,
            slideCount = 0,
            history = newHistory,
        )
    }
}
