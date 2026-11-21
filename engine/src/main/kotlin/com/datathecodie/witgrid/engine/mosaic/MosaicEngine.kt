package com.datathecodie.witgrid.engine.mosaic

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Mosaic rules and bank model exposed through the shared engine contract. */
object MosaicEngine : GameEngine<MosaicPuzzle, MosaicState> {
    override val id: String = "mosaic"

    override fun decodePuzzle(line: String): MosaicPuzzle = MosaicCodec.decode(line)

    override fun encodePuzzle(puzzle: MosaicPuzzle): String = MosaicCodec.encode(puzzle)

    override fun initialState(puzzle: MosaicPuzzle): MosaicState = MosaicState.initial(puzzle)

    override fun applyMove(state: MosaicState, move: Move): MosaicState = state.withMove(move)

    override fun isSolved(puzzle: MosaicPuzzle, state: MosaicState): Boolean =
        MosaicRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: MosaicPuzzle, state: MosaicState): List<Cell> =
        MosaicRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: MosaicPuzzle, limit: Int): Int =
        MosaicRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: MosaicPuzzle, state: MosaicState): HintStep? = unavailable()

    override fun grade(puzzle: MosaicPuzzle): Grade = unavailable()

    override fun generate(rng: SplitMix64, pack: Pack): MosaicPuzzle = unavailable()

    override fun encodeState(state: MosaicState): ByteArray = MosaicStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): MosaicState = MosaicStateCodec.decode(bytes)

    private fun unavailable(): Nothing = throw NotImplementedError("MO")
}
