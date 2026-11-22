package com.datathecodie.witgrid.engine.path

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Path rules and bank model exposed through the shared engine contract. */
object PathEngine : GameEngine<PathPuzzle, PathState> {
    override val id: String = "path"

    override fun decodePuzzle(line: String): PathPuzzle = PathCodec.decode(line)

    override fun encodePuzzle(puzzle: PathPuzzle): String = PathCodec.encode(puzzle)

    override fun initialState(puzzle: PathPuzzle): PathState = PathState.initial(puzzle)

    override fun applyMove(state: PathState, move: Move): PathState = state.withMove(move)

    override fun isSolved(puzzle: PathPuzzle, state: PathState): Boolean =
        PathRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: PathPuzzle, state: PathState): List<Cell> =
        PathRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: PathPuzzle, limit: Int): Int =
        PathRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: PathPuzzle, state: PathState): HintStep? =
        PathTechniqueSolver.nextStep(puzzle, state)

    override fun grade(puzzle: PathPuzzle): Grade =
        PathTechniqueSolver.grade(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): PathPuzzle =
        PathGenerator.generate(rng, pack)

    override fun encodeState(state: PathState): ByteArray = PathStateCodec.encode(state)

    override fun decodeState(bytes: ByteArray): PathState = PathStateCodec.decode(bytes)
}
