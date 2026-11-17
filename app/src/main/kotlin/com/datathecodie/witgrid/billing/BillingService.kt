package com.datathecodie.witgrid.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

enum class BillingNotice {
    NONE,
    STORE_UNAVAILABLE,
    PRODUCT_UNAVAILABLE,
    PURCHASE_PENDING,
    PURCHASE_CANCELLED,
    PURCHASE_COMPLETE,
    PURCHASE_FAILED,
    RESTORE_COMPLETE,
    RESTORE_EMPTY,
    RESTORE_FAILED,
}

data class BillingUiState(
    val productPrice: String? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val notice: BillingNotice = BillingNotice.NONE,
)

interface BillingService {
    val state: StateFlow<BillingUiState>

    suspend fun refresh()

    fun launchPurchase(activity: Activity)

    suspend fun restorePurchases()

    fun close()
}
