package com.datathecodie.witgrid.engine.nonogram

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/** Nonogram rules and bank model exposed through the shared engine contract. */
object NonogramEngine : GameEngine<NonogramPuzzle, NonogramState> {
    override val id: String = "nonogram"

    override fun decodePuzzle(line: String): NonogramPuzzle = NonogramCodec.decode(line)

    override fun encodePuzzle(puzzle: NonogramPuzzle): String = NonogramCodec.encode(puzzle)

    override fun initialState(puzzle: NonogramPuzzle): NonogramState = NonogramState.initial(puzzle)

    override fun applyMove(state: NonogramState, move: Move): NonogramState = state.withMove(move)

    override fun isSolved(puzzle: NonogramPuzzle, state: NonogramState): Boolean =
        NonogramRules.isSolved(puzzle, state)

    override fun mistakes(puzzle: NonogramPuzzle, state: NonogramState): List<Cell> =
        NonogramRules.mistakes(puzzle, state)

    override fun countSolutions(puzzle: NonogramPuzzle, limit: Int): Int =
        NonogramRules.countSolutions(puzzle, limit)

    override fun nextStep(puzzle: NonogramPuzzle, state: NonogramState): HintStep? = unavailable()

    override fun grade(puzzle: NonogramPuzzle): Grade = unavailable()

    override fun generate(rng: SplitMix64, pack: Pack): NonogramPuzzle = unavailable()

    override fun encodeState(state: NonogramState): ByteArray = unavailable()

    override fun decodeState(bytes: ByteArray): NonogramState = unavailable()

    private fun unavailable(): Nothing = throw NotImplementedError("NO")
}
