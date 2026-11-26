package com.datathecodie.witgrid.engine.klondike.spec

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikeSolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Plan 12.5: the generator uses only the seed, and every deal it returns is winnable. */
class KlondikeGeneratorSpecTest {

    @Test
    fun sameSeedGivesTheSamePuzzle() {
        // Two generators built from seed 7 must agree on the whole puzzle, bank id included.
        val first = KlondikeEngine.generate(SplitMix64(7L), Pack.EASY)
        val second = KlondikeEngine.generate(SplitMix64(7L), Pack.EASY)
        assertEquals(first, second)
    }

    @Test
    fun generatedPuzzleIsWinnable() {
        val puzzle = KlondikeEngine.generate(SplitMix64(7L), Pack.EASY)
        assertTrue(KlondikeSolver.isWinnable(KlondikeEngine.initialState(puzzle)))
    }
}
