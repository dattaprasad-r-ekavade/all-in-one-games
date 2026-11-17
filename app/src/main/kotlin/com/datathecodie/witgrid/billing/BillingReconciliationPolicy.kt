package com.datathecodie.witgrid.billing

import java.util.concurrent.atomic.AtomicLong

internal class EntitlementQueryVersion {
    private val value = AtomicLong(0)

    fun beginQuery(): Long = value.incrementAndGet()

    fun onPurchaseUpdate() {
        value.incrementAndGet()
    }

    fun onPurchasePersisted() {
        value.incrementAndGet()
    }

    fun isCurrent(queryVersion: Long): Boolean = queryVersion == value.get()
}

internal class PurchaseAcknowledgementTracker {
    private val pendingTokens = mutableSetOf<String>()
    private val attemptsByToken = mutableMapOf<String, Int>()

    fun recordFailure(purchaseToken: String): Long {
        pendingTokens += purchaseToken
        val attempt = (attemptsByToken[purchaseToken] ?: 0) + 1
        attemptsByToken[purchaseToken] = attempt
        val multiplier = 1L shl (attempt - 1).coerceAtMost(MAX_BACKOFF_SHIFT)
        return (BASE_RETRY_DELAY_MILLIS * multiplier).coerceAtMost(MAX_RETRY_DELAY_MILLIS)
    }

    fun markAcknowledged(purchaseToken: String) {
        pendingTokens.remove(purchaseToken)
        attemptsByToken.remove(purchaseToken)
    }

    fun isPending(purchaseToken: String): Boolean = purchaseToken in pendingTokens

    private companion object {
        const val BASE_RETRY_DELAY_MILLIS = 60_000L
        const val MAX_RETRY_DELAY_MILLIS = 21_600_000L
        const val MAX_BACKOFF_SHIFT = 10
    }
}
