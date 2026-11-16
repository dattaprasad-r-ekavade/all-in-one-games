package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Binary rules and bank model exposed through the shared engine contract. */
object BinaryEngine : GameEngine<BinaryPuzzle, BinaryState> {
    override val id: String = "binary"

    override fun decodePuzzle(line: String): BinaryPuzzle = BinaryCodec.decode(line)

    override fun encodePuzzle(puzzle: BinaryPuzzle): String = BinaryCodec.encode(puzzle)

    override fun initialState(puzzle: BinaryPuzzle): BinaryState = BinaryState.initial(puzzle)

    override fun applyMove(state: BinaryState, move: Move): BinaryState = state.withMove(move)

    override fun isSolved(puzzle: BinaryPuzzle, state: BinaryState): Boolean =
        BinaryRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: BinaryPuzzle, state: BinaryState): List<Cell> =
        BinaryRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: BinaryPuzzle, limit: Int): Int =
        BinaryRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: BinaryPuzzle, state: BinaryState): HintStep? =
        BinaryTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: BinaryPuzzle): Grade = BinaryTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): BinaryPuzzle = unavailable()

    override fun encodeState(state: BinaryState): ByteArray = unavailable()

    override fun decodeState(bytes: ByteArray): BinaryState = unavailable()

    private fun unavailable(): Nothing = throw NotImplementedError("BI")
}
