package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Generates deterministic, solver-verified Draw-1 Klondike deals partitioned by pack difficulty.
 *
 * Easy uses two face-up cards per tableau column and caps solution length; Medium, Hard, and
 * Expert use the standard single face-up card per column and escalate required solution length
 * and non-draw move complexity.
 */
object KlondikeGenerator {
    private const val MAX_ATTEMPTS = 2_000

    data class PackConfig(
        val difficulty: String,
        val minTotalMoves: Int,
        val maxTotalMoves: Int,
        val minNonDrawMoves: Int,
        val minRecycleCount: Int,
    )

    fun configFor(pack: Pack): PackConfig = when (pack) {
        Pack.EASY -> PackConfig(
            difficulty = "d1",
            minTotalMoves = 0,
            maxTotalMoves = 135,
            minNonDrawMoves = 0,
            minRecycleCount = 0,
        )
        Pack.MEDIUM -> PackConfig(
            difficulty = "d2",
            minTotalMoves = 76,
            maxTotalMoves = 135,
            minNonDrawMoves = 52,
            minRecycleCount = 0,
        )
        Pack.HARD -> PackConfig(
            difficulty = "d3",
            minTotalMoves = 134,
            maxTotalMoves = Int.MAX_VALUE,
            minNonDrawMoves = 80,
            minRecycleCount = 2,
        )
        Pack.EXPERT -> PackConfig(
            difficulty = "d4",
            minTotalMoves = 140,
            maxTotalMoves = Int.MAX_VALUE,
            minNonDrawMoves = 83,
            minRecycleCount = 2,
        )
    }

    fun generate(rng: SplitMix64, pack: Pack): KlondikePuzzle {
        val config = configFor(pack)

        repeat(MAX_ATTEMPTS) {
            val seed = rng.nextLong()
            val candidate = KlondikePuzzle(
                bankId = "KL-${pack.name}-pending",
                pack = pack,
                difficulty = config.difficulty,
                seed = seed,
                size = KlondikeCodec.DECK_SIZE,
            )
            val initial = KlondikeDeal.deal(candidate)
            val solved = KlondikeSolver.solveForGeneration(initial)
            if (solved != null && matchesPackDifficulty(config, solved)) {
                val hex = rng.nextLong().toULong().toString(16).padStart(16, '0')
                return candidate.copy(bankId = "KL-${pack.name}-$hex")
            }
        }

        throw IllegalStateException("Failed to generate a winnable Klondike puzzle for pack $pack")
    }

    internal fun matchesPackDifficulty(config: PackConfig, result: KlondikeSolver.SolveResult): Boolean {
        val totalMoves = result.moves.size
        return totalMoves in config.minTotalMoves..config.maxTotalMoves &&
            result.nonDrawMoves >= config.minNonDrawMoves &&
            result.recycleCount >= config.minRecycleCount
    }
}
