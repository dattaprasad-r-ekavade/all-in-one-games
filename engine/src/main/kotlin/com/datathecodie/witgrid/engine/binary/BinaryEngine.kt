package com.datathecodie.witgrid.engine.binary

import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.Grade
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * BI lane placeholder. Unit types deliberately promise no puzzle or state model;
 * the lane replaces them with its own immutable types when implementing its rules.
 */
object BinaryEngine : GameEngine<Unit, Unit> {
    override val id: String = "binary"

    override fun decodePuzzle(line: String): Unit = unavailable()

    override fun encodePuzzle(puzzle: Unit): String = unavailable()

    override fun initialState(puzzle: Unit): Unit = unavailable()

    override fun applyMove(state: Unit, move: Move): Unit = unavailable()

    override fun isSolved(puzzle: Unit, state: Unit): Boolean = unavailable()

    override fun mistakes(puzzle: Unit, state: Unit): List<Cell> = unavailable()

    override fun countSolutions(puzzle: Unit, limit: Int): Int = unavailable()

    override fun nextStep(puzzle: Unit, state: Unit): HintStep? = unavailable()

    override fun grade(puzzle: Unit): Grade = unavailable()

    override fun generate(rng: SplitMix64, pack: Pack): Unit = unavailable()

    override fun encodeState(state: Unit): ByteArray = unavailable()

    override fun decodeState(bytes: ByteArray): Unit = unavailable()

    private fun unavailable(): Nothing = throw NotImplementedError("BI")
}
