package com.datathecodie.witgrid.engine.core

/**
 * Reproducible SplitMix64 stream with the reference increment and mixing constants.
 *
 * [seed] is the state before the first increment. Arithmetic wraps modulo 2^64;
 * instances are mutable and must belong to one generator or worker at a time.
 */
class SplitMix64(seed: Long) {
    private var state = seed

    /** Returns the next 64 bits as a signed [Long]; use `toULong()` to inspect them unsigned. */
    fun nextLong(): Long {
        state += -7046029254386353131L
        var value = state
        value = (value xor (value ushr 30)) * -4658895280553007687L
        value = (value xor (value ushr 27)) * -7723592293110705685L
        return value xor (value ushr 31)
    }

    /**
     * Returns an unbiased integer in `0 until bound`, rejecting overflowed samples.
     * [bound] must be positive. Even a bound of one consumes an output.
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive" }
        var sample: Long
        var result: Long
        do {
            sample = nextLong() ushr 1
            result = sample % bound
        } while (sample - result + (bound - 1) < 0)
        return result.toInt()
    }
}
