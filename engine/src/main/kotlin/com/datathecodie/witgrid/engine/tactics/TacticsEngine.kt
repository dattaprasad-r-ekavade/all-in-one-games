package com.datathecodie.witgrid.engine.tactics

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Chess Tactics rules and bank model exposed through the shared engine contract. */
object TacticsEngine : GameEngine<TacticsPuzzle, TacticsState> {
    override val id: String = "tactics"

    override fun decodePuzzle(line: String): TacticsPuzzle = TacticsCodec.decode(line)

    override fun encodePuzzle(puzzle: TacticsPuzzle): String = TacticsCodec.encode(puzzle)

    override fun initialState(puzzle: TacticsPuzzle): TacticsState = TacticsState.initial(puzzle)

    override fun applyMove(state: TacticsState, move: Move): TacticsState = state.withMove(move)

    override fun isSolved(puzzle: TacticsPuzzle, state: TacticsState): Boolean =
        TacticsRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: TacticsPuzzle, state: TacticsState): List<Cell> =
        TacticsRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: TacticsPuzzle, limit: Int): Int =
        TacticsRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: TacticsPuzzle, state: TacticsState): HintStep? =
        throw NotImplementedError("tactics")

    override fun grade(puzzle: TacticsPuzzle): Grade =
        throw NotImplementedError("tactics")

    override fun generate(rng: SplitMix64, pack: Pack): TacticsPuzzle =
        throw NotImplementedError("tactics")

    override fun encodeState(state: TacticsState): ByteArray = TacticsStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): TacticsState = TacticsStateCodec.decode(bytes)
}
