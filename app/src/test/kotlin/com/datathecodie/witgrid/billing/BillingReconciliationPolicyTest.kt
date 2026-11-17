package com.datathecodie.witgrid.billing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BillingReconciliationPolicyTest {
    @Test
    fun purchaseUpdateInvalidatesAnOlderEmptyOwnershipQuery() {
        val versions = EntitlementQueryVersion()
        val earlierQuery = versions.beginQuery()

        versions.onPurchaseUpdate()

        assertFalse(versions.isCurrent(earlierQuery))
    }

    @Test
    fun purchasePersistenceInvalidatesQueriesStartedBeforeTheWriteFinished() {
        val versions = EntitlementQueryVersion()
        versions.onPurchaseUpdate()
        val queryStartedDuringPurchaseWrite = versions.beginQuery()

        versions.onPurchasePersisted()

        assertFalse(versions.isCurrent(queryStartedDuringPurchaseWrite))
    }

    @Test
    fun failedAcknowledgementRemainsPendingUntilLaterSuccess() {
        val tracker = PurchaseAcknowledgementTracker()
        val purchaseToken = "premium-token"

        val firstRetryDelay = tracker.recordFailure(purchaseToken)
        val nextRetryDelay = tracker.recordFailure(purchaseToken)
        assertTrue(tracker.isPending(purchaseToken))
        assertTrue(nextRetryDelay > firstRetryDelay)

        tracker.markAcknowledged(purchaseToken)

        assertFalse(tracker.isPending(purchaseToken))
        assertEquals(firstRetryDelay, tracker.recordFailure(purchaseToken))
    }
}
