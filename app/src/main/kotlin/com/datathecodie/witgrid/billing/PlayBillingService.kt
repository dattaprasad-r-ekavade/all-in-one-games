package com.datathecodie.witgrid.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class PlayBillingService(
    context: Context,
    private val playerDataRepository: PlayerDataRepository,
) : BillingService {
    private val serviceJob: Job = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val connectionMutex = Mutex()
    private val entitlementMutex = Mutex()
    private val acknowledgementMutex = Mutex()
    private val acknowledgedPurchaseTokens = mutableSetOf<String>()
    private val pendingAcknowledgements = mutableMapOf<String, Purchase>()
    private val acknowledgementTracker = PurchaseAcknowledgementTracker()
    private val scheduledAcknowledgementRetries = mutableSetOf<String>()
    private val entitlementQueryVersion = EntitlementQueryVersion()
    private val mutableState = MutableStateFlow(BillingUiState())
    private val billingClient: BillingClient
    private var productDetails: ProductDetails? = null
    private var productOffer: ProductDetails.OneTimePurchaseOfferDetails? = null

    override val state: StateFlow<BillingUiState> = mutableState

    init {
        val listener = PurchasesUpdatedListener(::onPurchasesUpdated)
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .enableAutoServiceReconnection()
            .build()
    }

    override suspend fun refresh() {
        mutableState.value = mutableState.value.copy(isLoading = true, notice = BillingNotice.NONE)
        try {
            if (!connect()) {
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    notice = BillingNotice.STORE_UNAVAILABLE,
                )
                return
            }
            refreshProductDetails()
            refreshOwnedPurchases()
        } catch (exception: CancellationException) {
            mutableState.value = mutableState.value.copy(isLoading = false)
            throw exception
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(
                isLoading = false,
                notice = BillingNotice.STORE_UNAVAILABLE,
            )
        }
    }

    override fun launchPurchase(activity: Activity) {
        if (mutableState.value.isProcessing) return
        mutableState.value = mutableState.value.copy(isProcessing = true, notice = BillingNotice.NONE)
        scope.launch {
            try {
                if (!connect()) {
                    mutableState.value = mutableState.value.copy(
                        isProcessing = false,
                        notice = BillingNotice.STORE_UNAVAILABLE,
                    )
                    return@launch
                }
                refreshProductDetails()
                val details = productDetails
                val offer = productOffer
                val offerToken = offer?.offerToken
                if (details == null || offerToken.isNullOrBlank()) {
                    mutableState.value = mutableState.value.copy(
                        isProcessing = false,
                        notice = BillingNotice.PRODUCT_UNAVAILABLE,
                    )
                    return@launch
                }
                if (activity.isFinishing || activity.isDestroyed) {
                    mutableState.value = mutableState.value.copy(
                        isProcessing = false,
                        notice = BillingNotice.PURCHASE_FAILED,
                    )
                    return@launch
                }
                val params = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .setOfferToken(offerToken)
                                .build(),
                        ),
                    )
                    .build()
                val result = billingClient.launchBillingFlow(activity, params)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    mutableState.value = mutableState.value.copy(
                        isProcessing = false,
                        notice = result.toPurchaseNotice(),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    isProcessing = false,
                    notice = BillingNotice.PURCHASE_FAILED,
                )
            }
        }
    }

    override suspend fun restorePurchases() {
        mutableState.value = mutableState.value.copy(isProcessing = true, notice = BillingNotice.NONE)
        try {
            if (!connect()) {
                mutableState.value = mutableState.value.copy(
                    isProcessing = false,
                    notice = BillingNotice.RESTORE_FAILED,
                )
                return
            }
            val queryVersion = entitlementQueryVersion.beginQuery()
            val purchases = queryInAppPurchases()
            if (purchases == null) {
                mutableState.value = mutableState.value.copy(
                    isProcessing = false,
                    notice = BillingNotice.RESTORE_FAILED,
                )
                return
            }
            val ownsPremium = purchases.arePremiumOwned()
            if (!persistOwnedPurchases(queryVersion, purchases)) {
                mutableState.value = mutableState.value.copy(isProcessing = false)
                return
            }
            purchases.filter(::isPremiumPurchase).forEach { acknowledgeIfNeeded(it) }
            mutableState.value = mutableState.value.copy(
                isProcessing = false,
                notice = if (ownsPremium) BillingNotice.RESTORE_COMPLETE else BillingNotice.RESTORE_EMPTY,
            )
        } catch (exception: CancellationException) {
            mutableState.value = mutableState.value.copy(isProcessing = false)
            throw exception
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(
                isProcessing = false,
                notice = BillingNotice.RESTORE_FAILED,
            )
        }
    }

    override fun close() {
        serviceJob.cancel()
        billingClient.endConnection()
    }

    private suspend fun connect(): Boolean = connectionMutex.withLock {
        if (billingClient.isReady) return@withLock true
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : com.android.billingclient.api.BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    private suspend fun refreshProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        val (result, queryResult) = suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, detailsResult ->
                if (continuation.isActive) continuation.resume(billingResult to detailsResult)
            }
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            productDetails = null
            productOffer = null
            mutableState.value = mutableState.value.copy(productPrice = null)
            return
        }
        val details = queryResult.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
        val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: details?.oneTimePurchaseOfferDetails
        productDetails = details
        productOffer = offer
        mutableState.value = mutableState.value.copy(productPrice = offer?.formattedPrice)
    }

    private suspend fun refreshOwnedPurchases() {
        val queryVersion = entitlementQueryVersion.beginQuery()
        val purchases = queryInAppPurchases()
        if (purchases == null) {
            mutableState.value = mutableState.value.copy(
                isLoading = false,
                notice = BillingNotice.STORE_UNAVAILABLE,
            )
            return
        }
        val matchingPurchases = purchases.filter(::isPremiumPurchase)
        val ownsPremium = purchases.arePremiumOwned()
        val hasPendingPurchase = PremiumEntitlementPolicy.isPending(PRODUCT_ID, purchases.toRecords())
        val ownershipWasApplied = persistOwnedPurchases(queryVersion, purchases)
        if (ownershipWasApplied) {
            matchingPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .forEach { acknowledgeIfNeeded(it) }
        }
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            notice = when {
                !ownershipWasApplied -> mutableState.value.notice
                ownsPremium -> BillingNotice.PURCHASE_COMPLETE
                hasPendingPurchase -> BillingNotice.PURCHASE_PENDING
                else -> mutableState.value.notice
            },
        )
    }

    private suspend fun queryInAppPurchases(): List<Purchase>? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val (result, purchases) = suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
                if (continuation.isActive) continuation.resume(billingResult to purchaseList)
            }
        }
        return purchases.takeIf { result.responseCode == BillingClient.BillingResponseCode.OK }
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                entitlementQueryVersion.onPurchaseUpdate()
                mutableState.value = mutableState.value.copy(isProcessing = true)
                scope.launch { applyPurchaseUpdates(purchases) }
            }

            else -> mutableState.value = mutableState.value.copy(
                isProcessing = false,
                notice = billingResult.toPurchaseNotice(),
            )
        }
    }

    private suspend fun applyPurchaseUpdates(purchases: List<Purchase>) {
        try {
            val matchingPurchases = purchases.filter(::isPremiumPurchase)
            val ownsPremium = purchases.arePremiumOwned()
            val hasPendingPurchase = PremiumEntitlementPolicy.isPending(PRODUCT_ID, purchases.toRecords())
            entitlementMutex.withLock {
                if (ownsPremium) {
                    playerDataRepository.setPremiumEnabled(true)
                    entitlementQueryVersion.onPurchasePersisted()
                }
            }
            matchingPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .forEach { acknowledgeIfNeeded(it) }
            mutableState.value = mutableState.value.copy(
                isProcessing = false,
                notice = when {
                    ownsPremium -> BillingNotice.PURCHASE_COMPLETE
                    hasPendingPurchase -> BillingNotice.PURCHASE_PENDING
                    else -> BillingNotice.PRODUCT_UNAVAILABLE
                },
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(
                isProcessing = false,
                notice = BillingNotice.PURCHASE_FAILED,
            )
        }
    }

    private suspend fun persistOwnedPurchases(queryVersion: Long, purchases: List<Purchase>): Boolean =
        entitlementMutex.withLock {
            if (!entitlementQueryVersion.isCurrent(queryVersion)) {
                false
            } else {
                playerDataRepository.setPremiumEnabled(purchases.arePremiumOwned())
                true
            }
        }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val token = purchase.purchaseToken
        val acknowledged = acknowledgementMutex.withLock {
            if (purchase.isAcknowledged || token in acknowledgedPurchaseTokens) {
                acknowledgedPurchaseTokens += token
                pendingAcknowledgements.remove(token)
                acknowledgementTracker.markAcknowledged(token)
                return@withLock true
            }
            pendingAcknowledgements[token] = purchase
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(token)
                .build()
            val result = try {
                suspendCancellableCoroutine { continuation ->
                    billingClient.acknowledgePurchase(params) { billingResult ->
                        if (continuation.isActive) {
                            continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                false
            }
            if (result) {
                acknowledgedPurchaseTokens += token
                pendingAcknowledgements.remove(token)
                acknowledgementTracker.markAcknowledged(token)
            }
            result
        }
        if (!acknowledged) {
            scheduleAcknowledgementRetry(token, acknowledgementTracker.recordFailure(token))
        }
    }

    private fun scheduleAcknowledgementRetry(token: String, retryDelayMillis: Long) {
        if (!scheduledAcknowledgementRetries.add(token)) return
        scope.launch {
            delay(retryDelayMillis)
            scheduledAcknowledgementRetries.remove(token)
            pendingAcknowledgements[token]?.let { acknowledgeIfNeeded(it) }
        }
    }

    private fun isPremiumPurchase(purchase: Purchase): Boolean = PRODUCT_ID in purchase.products

    private fun List<Purchase>.arePremiumOwned(): Boolean =
        PremiumEntitlementPolicy.isOwned(PRODUCT_ID, toRecords())

    private fun List<Purchase>.toRecords(): List<PurchaseRecord> = map { purchase ->
        PurchaseRecord(
            productIds = purchase.products.toSet(),
            state = when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                else -> PurchaseState.OTHER
            },
        )
    }

    private fun BillingResult.toPurchaseNotice(): BillingNotice = when (responseCode) {
        BillingClient.BillingResponseCode.USER_CANCELED -> BillingNotice.PURCHASE_CANCELLED
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingNotice.PRODUCT_UNAVAILABLE
        BillingClient.BillingResponseCode.OK -> BillingNotice.PURCHASE_COMPLETE
        else -> BillingNotice.PURCHASE_FAILED
    }

    private companion object {
        const val PRODUCT_ID = "witgrid_premium_lifetime"
    }
}
