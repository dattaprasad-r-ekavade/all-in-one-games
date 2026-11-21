package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Bankgen-side Family B contract until [com.datathecodie.witgrid.engine.core.PlayEngine] lands in engine/.
 * Production discovers implementations via [familyBRegistry]; tests inject fakes.
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