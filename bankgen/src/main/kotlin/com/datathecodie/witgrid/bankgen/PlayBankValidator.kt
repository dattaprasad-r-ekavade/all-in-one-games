package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayEngine
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Bankgen-side Family B validation contract.
 * Production discovers implementations via [familyBRegistry]; tests may inject fakes.
 */
internal interface PlayBankValidator<P : Any> {
    val id: String

    fun decodePuzzle(line: String): P

    fun encodePuzzle(puzzle: P): String

    fun generate(rng: SplitMix64, pack: Pack): P

    /** True when the layout is winnable, or failure is only reachable after a wrong player action. */
    fun isWinnable(puzzle: P): Boolean

    /** Difficulty token stored in the bank line; must match after canonical encode. */
    fun difficulty(puzzle: P): String
}

/**
 * Adapts a [PlayEngine] to [PlayBankValidator] so each Family B lane can be wired with a single entry.
 * Enforces the shared deal-start invariants (`!isLost` and `!isWon`) before running [canWin].
 */
internal class PlayEngineValidator<P : Any, S : Any, A : Any>(
    private val engine: PlayEngine<P, S, A>,
    private val expectedDifficulty: (P) -> String,
    private val canWin: (P) -> Boolean,
) : PlayBankValidator<P> {
    override val id: String = engine.id

    override fun decodePuzzle(line: String): P = engine.decodePuzzle(line)

    override fun encodePuzzle(puzzle: P): String = engine.encodePuzzle(puzzle)

    override fun generate(rng: SplitMix64, pack: Pack): P = engine.generate(rng, pack)

    override fun isWinnable(puzzle: P): Boolean {
        val initialState = engine.initialState(puzzle)
        if (engine.isLost(puzzle, initialState) || engine.isWon(puzzle, initialState)) {
            return false
        }
        return canWin(puzzle)
    }

    override fun difficulty(puzzle: P): String = expectedDifficulty(puzzle)
}