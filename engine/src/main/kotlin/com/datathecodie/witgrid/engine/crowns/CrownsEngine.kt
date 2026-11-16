package com.datathecodie.witgrid.engine.crowns

import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Crowns rules and bank model exposed through the shared engine contract. */
object CrownsEngine : GameEngine<CrownsPuzzle, CrownsState> {
    override val id: String = "crowns"

    override fun decodePuzzle(line: String): CrownsPuzzle = CrownsCodec.decode(line)

    override fun encodePuzzle(puzzle: CrownsPuzzle): String = CrownsCodec.encode(puzzle)

    override fun initialState(puzzle: CrownsPuzzle): CrownsState = CrownsState.initial(puzzle)

    override fun applyMove(state: CrownsState, move: Move): CrownsState = state.withMove(move)

    override fun isSolved(puzzle: CrownsPuzzle, state: CrownsState): Boolean =
        CrownsRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: CrownsPuzzle, state: CrownsState) = CrownsRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: CrownsPuzzle, limit: Int): Int =
        CrownsRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: CrownsPuzzle, state: CrownsState): HintStep? =
        CrownsTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: CrownsPuzzle): Grade = CrownsTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): CrownsPuzzle = CrownsGenerator.generate(rng, pack)

    override fun encodeState(state: CrownsState): ByteArray = CrownsStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): CrownsState = CrownsStateCodec.decode(bytes)
}
