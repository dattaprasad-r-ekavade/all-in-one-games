package com.datathecodie.witgrid.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeBillingService(
    private val availablePrice: String? = "Test price",
    private var ownsPremium: Boolean = false,
) : BillingService {
    private val mutableState = MutableStateFlow(
        BillingUiState(
            productPrice = availablePrice,
            notice = if (ownsPremium) BillingNotice.PURCHASE_COMPLETE else BillingNotice.NONE,
        ),
    )

    override val state: StateFlow<BillingUiState> = mutableState

    override suspend fun refresh() {
        mutableState.value = mutableState.value.copy(
            productPrice = availablePrice,
            isLoading = false,
            notice = if (ownsPremium) BillingNotice.PURCHASE_COMPLETE else BillingNotice.NONE,
        )
    }

    override fun launchPurchase(activity: Activity) {
        ownsPremium = true
        mutableState.value = mutableState.value.copy(
            productPrice = availablePrice,
            isProcessing = false,
            notice = BillingNotice.PURCHASE_COMPLETE,
        )
    }

    override suspend fun restorePurchases() {
        mutableState.value = mutableState.value.copy(
            isProcessing = false,
            notice = if (ownsPremium) BillingNotice.RESTORE_COMPLETE else BillingNotice.RESTORE_EMPTY,
        )
    }

    override fun close() = Unit
}
