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
            mistakes = 0,
            slideCount = 0,
            history = emptyList(),
        )

    override fun applyAction(state: ArrowsState, action: ArrowsAction): ArrowsState {
        require(!isWon(state.puzzle, state)) { "Cannot apply action: level is already won" }
        require(!isLost(state.puzzle, state)) { "Cannot apply action: level is already lost" }
        val cellIndex = action.cell.index
        require(cellIndex in state.arrows.indices) { "Cell index out of range: $cellIndex" }
        val direction = state.arrows[cellIndex]
            ?: throw IllegalArgumentException("No arrow at cell $cellIndex")

        return if (ArrowsRules.canSlide(state.puzzle.size, state.arrows, cellIndex)) {
            val nextArrows = ArrowsRules.applySlide(state.puzzle.size, state.arrows, action.cell)
            state.copy(
                arrows = nextArrows,
                slideCount = state.slideCount + 1,
                history = state.history + listOf(state.arrows),
            )
        } else {
            // A tap on an arrow that cannot slide is a mistake:
            // The arrow stays, the board does not change, mistakes goes up by 1.
            state.copy(
                mistakes = state.mistakes + 1,
            )
        }
    }

    override fun isWon(puzzle: ArrowsPuzzle, state: ArrowsState): Boolean =
        ArrowsRules.isWon(state.arrows)

    override fun isLost(puzzle: ArrowsPuzzle, state: ArrowsState): Boolean =
        !isWon(puzzle, state) && ArrowsRules.isLost(state.mistakes)

    override fun hintAction(puzzle: ArrowsPuzzle, state: ArrowsState): ArrowsAction? =
        throw NotImplementedError("arrows")

    override fun generate(rng: SplitMix64, pack: Pack): ArrowsPuzzle =
        throw NotImplementedError("arrows")

    override fun encodeState(state: ArrowsState): ByteArray = ArrowsStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): ArrowsState = ArrowsStateCodec.decode(bytes)

    /**
     * Applies one rewarded Continue after failure.
     * Continue gives one extra life by decrementing [ArrowsState.mistakes] by one (from 3 to 2).
     * The board stays as it is (no arrows or slides are undone).
     */
    override fun applyContinue(state: ArrowsState): ArrowsState {
        require(isLost(state.puzzle, state)) { "Continue only applies after failure" }
        return state.copy(
            mistakes = state.mistakes - 1,
        )
    }
}
