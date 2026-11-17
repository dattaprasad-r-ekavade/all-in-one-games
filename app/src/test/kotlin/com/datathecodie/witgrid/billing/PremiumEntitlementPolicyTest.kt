package com.datathecodie.witgrid.billing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PremiumEntitlementPolicyTest {
    @Test
    fun purchasedMatchingProductGrantsOwnership() {
        assertTrue(
            PremiumEntitlementPolicy.isOwned(
                PREMIUM_PRODUCT_ID,
                listOf(PurchaseRecord(setOf(PREMIUM_PRODUCT_ID), PurchaseState.PURCHASED)),
            ),
        )
    }

    @Test
    fun pendingPurchaseDoesNotGrantOwnership() {
        val purchases = listOf(PurchaseRecord(setOf(PREMIUM_PRODUCT_ID), PurchaseState.PENDING))

        assertFalse(PremiumEntitlementPolicy.isOwned(PREMIUM_PRODUCT_ID, purchases))
        assertTrue(PremiumEntitlementPolicy.isPending(PREMIUM_PRODUCT_ID, purchases))
    }

    @Test
    fun unrelatedOrUnspecifiedPurchasesDoNotGrantOwnership() {
        val purchases = listOf(
            PurchaseRecord(setOf("another_product"), PurchaseState.PURCHASED),
            PurchaseRecord(setOf(PREMIUM_PRODUCT_ID), PurchaseState.OTHER),
        )

        assertFalse(PremiumEntitlementPolicy.isOwned(PREMIUM_PRODUCT_ID, purchases))
        assertFalse(PremiumEntitlementPolicy.isPending(PREMIUM_PRODUCT_ID, purchases))
    }

    private companion object {
        const val PREMIUM_PRODUCT_ID = "witgrid_premium_lifetime"
    }
}
