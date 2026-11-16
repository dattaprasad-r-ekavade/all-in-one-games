package com.datathecodie.witgrid.engine.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SplitMix64Test {
    @Test
    fun matchesReferenceOutputsIncludingUnsignedHighBit() {
        val rng = SplitMix64(1234567)
        assertEquals(
            listOf(6457827717110365317uL, 3203168211198807973uL, 9817491932198370423uL),
            List(3) { rng.nextLong().toULong() },
        )
    }

    @Test
    fun matchesZeroSeedReferenceVectorAcrossStateOverflow() {
        val rng = SplitMix64(0)
        assertEquals(
            listOf(0xe220a8397b1dcdafuL, 0x6e789e6aa1b965f4uL, 0x06c45d188009454fuL),
            List(3) { rng.nextLong().toULong() },
        )
    }

    @Test
    fun allSeedBitsAreReproducible() {
        for (seed in listOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)) {
            val first = SplitMix64(seed)
            val second = SplitMix64(seed)
            repeat(1000) { assertEquals(first.nextLong(), second.nextLong()) }
        }
    }

    @Test
    fun boundedOutputsHaveStableMapping() {
        val rng = SplitMix64(1234567)
        assertEquals(listOf(8, 6, 1), List(3) { rng.nextInt(10) })
    }

    @Test
    fun boundsIncludeOnePowersOfTwoAndLargestInt() {
        for (bound in listOf(1, 2, 3, 16, 100, Int.MAX_VALUE)) {
            val rng = SplitMix64(-1)
            repeat(1000) { assertTrue(rng.nextInt(bound) in 0 until bound) }
        }
        val rng = SplitMix64(1234567)
        assertEquals(0, rng.nextInt(1))
        assertEquals(3203168211198807973L, rng.nextLong())
    }

    @Test
    fun rejectsNonPositiveBoundsWithoutAdvancing() {
        val rng = SplitMix64(1234567)
        for (bound in listOf(0, -1, Int.MIN_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { rng.nextInt(bound) }
        }
        assertEquals(6457827717110365317L, rng.nextLong())
    }
}
