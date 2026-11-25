package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayEngine
import com.datathecodie.witgrid.engine.core.SplitMix64

object ColourSortEngine : PlayEngine<ColourSortPuzzle, ColourSortState, ColourSortAction> {
    override val id: String = "coloursort"

    override fun decodePuzzle(line: String): ColourSortPuzzle = ColourSortCodec.decodeLine(line)

    override fun encodePuzzle(puzzle: ColourSortPuzzle): String = ColourSortCodec.encodePuzzle(puzzle)

    override fun initialState(puzzle: ColourSortPuzzle): ColourSortState =
        ColourSortState(
            puzzle = puzzle,
            tubes = puzzle.initialTubes.map { it.toList() },
            pourCount = 0,
        )

    override fun applyAction(state: ColourSortState, action: ColourSortAction): ColourSortState {
        val nextTubes = ColourSortRules.applyPour(state.tubes, action.from, action.to)
        return state.copy(tubes = nextTubes, pourCount = state.pourCount + 1)
    }

    override fun isWon(puzzle: ColourSortPuzzle, state: ColourSortState): Boolean =
        ColourSortRules.isWon(state.tubes)

    override fun isLost(puzzle: ColourSortPuzzle, state: ColourSortState): Boolean =
        ColourSortRules.isStuckAfterPlay(state.tubes, state.pourCount)

    override fun hintAction(puzzle: ColourSortPuzzle, state: ColourSortState): ColourSortAction? {
        if (isWon(puzzle, state) || isLost(puzzle, state)) return null
        val solution = ColourSortSolver.solve(state.tubes) ?: return null
        return solution.firstOrNull()
    }

    override fun generate(rng: SplitMix64, pack: Pack): ColourSortPuzzle =
        ColourSortGenerator.generate(rng, pack)

    override fun encodeState(state: ColourSortState): ByteArray = ColourSortStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): ColourSortState = ColourSortStateCodec.decode(bytes)

    override fun applyContinue(state: ColourSortState): ColourSortState {
        require(isLost(state.puzzle, state)) { "Continue only applies after failure" }
        return state.copy(tubes = state.tubes + listOf(emptyList()), pourCount = 0)
    }
}