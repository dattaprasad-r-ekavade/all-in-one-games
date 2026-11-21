package com.datathecodie.witgrid.engine.skyscrapers

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Skyscrapers rules and bank model exposed through the shared engine contract. */
object SkyscrapersEngine : GameEngine<SkyscrapersPuzzle, SkyscrapersState> {
    override val id: String = "skyscrapers"

    override fun decodePuzzle(line: String): SkyscrapersPuzzle = SkyscrapersCodec.decode(line)

    override fun encodePuzzle(puzzle: SkyscrapersPuzzle): String = SkyscrapersCodec.encode(puzzle)

    override fun initialState(puzzle: SkyscrapersPuzzle): SkyscrapersState = SkyscrapersState.initial(puzzle)

    override fun applyMove(state: SkyscrapersState, move: Move): SkyscrapersState = state.withMove(move)

    override fun isSolved(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): Boolean =
        SkyscrapersRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): List<Cell> =
        SkyscrapersRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: SkyscrapersPuzzle, limit: Int): Int =
        SkyscrapersRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: SkyscrapersPuzzle, state: SkyscrapersState): HintStep? =
        SkyscrapersTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: SkyscrapersPuzzle): Grade =
        SkyscrapersTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): SkyscrapersPuzzle = unavailable()

    override fun encodeState(state: SkyscrapersState): ByteArray = SkyscrapersStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): SkyscrapersState = SkyscrapersStateCodec.decode(bytes)

    private fun unavailable(): Nothing = throw NotImplementedError("Skyscrapers x3")
}
