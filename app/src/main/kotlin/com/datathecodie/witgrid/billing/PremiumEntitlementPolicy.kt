package com.datathecodie.witgrid.billing

data class PurchaseRecord(
    val productIds: Set<String>,
    val state: PurchaseState,
)

enum class PurchaseState {
    PURCHASED,
    PENDING,
    OTHER,
}

object PremiumEntitlementPolicy {
    fun isOwned(productId: String, purchases: Iterable<PurchaseRecord>): Boolean =
        purchases.any { purchase ->
            productId in purchase.productIds && purchase.state == PurchaseState.PURCHASED
        }

    fun isPending(productId: String, purchases: Iterable<PurchaseRecord>): Boolean =
        purchases.any { purchase ->
            productId in purchase.productIds && purchase.state == PurchaseState.PENDING
        }
}
