package com.datathecodie.witgrid.engine.futoshiki

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Futoshiki rules and bank model exposed through the shared engine contract. */
object FutoshikiEngine : GameEngine<FutoshikiPuzzle, FutoshikiState> {
    override val id: String = "futoshiki"

    override fun decodePuzzle(line: String): FutoshikiPuzzle = FutoshikiCodec.decode(line)

    override fun encodePuzzle(puzzle: FutoshikiPuzzle): String = FutoshikiCodec.encode(puzzle)

    override fun initialState(puzzle: FutoshikiPuzzle): FutoshikiState = FutoshikiState.initial(puzzle)

    override fun applyMove(state: FutoshikiState, move: Move): FutoshikiState = state.withMove(move)

    override fun isSolved(puzzle: FutoshikiPuzzle, state: FutoshikiState): Boolean =
        FutoshikiRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: FutoshikiPuzzle, state: FutoshikiState): List<Cell> =
        FutoshikiRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: FutoshikiPuzzle, limit: Int): Int =
        FutoshikiRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: FutoshikiPuzzle, state: FutoshikiState): HintStep? =
        FutoshikiTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: FutoshikiPuzzle): Grade =
        FutoshikiTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): FutoshikiPuzzle =
        FutoshikiGenerator.generate(rng, pack)

    override fun encodeState(state: FutoshikiState): ByteArray = FutoshikiStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): FutoshikiState = FutoshikiStateCodec.decode(bytes)
}
