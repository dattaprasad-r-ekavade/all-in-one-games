package com.datathecodie.witgrid.ads

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdPreloadAgePolicyTest {
    @Test
    fun preloadedCacheIsFreshUntilOneHourAndExpiredAtTheBoundary() {
        val policy = AdPreloadAgePolicy()
        val startedAt = 50_000L

        assertFalse(policy.shouldDrop(startedAt))
        policy.markStarted(startedAt)

        assertFalse(policy.shouldDrop(startedAt))
        assertFalse(policy.shouldDrop(startedAt + AdPreloadAgePolicy.DEFAULT_MAXIMUM_AGE_MILLIS - 1))
        assertTrue(policy.shouldDrop(startedAt + AdPreloadAgePolicy.DEFAULT_MAXIMUM_AGE_MILLIS))
    }

    @Test
    fun clockMovingBackwardsExpiresThePreloadedCache() {
        val policy = AdPreloadAgePolicy()
        policy.markStarted(50_000)

        assertTrue(policy.shouldDrop(49_999))
    }

    @Test
    fun droppingTheCacheClearsItsAge() {
        val policy = AdPreloadAgePolicy()
        policy.markStarted(50_000)
        policy.clear()

        assertFalse(policy.shouldDrop(4_000_000))
        assertFalse(policy.hasStarted())
    }
}
