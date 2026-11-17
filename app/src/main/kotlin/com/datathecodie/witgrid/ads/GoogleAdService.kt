package com.datathecodie.witgrid.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine

class GoogleAdService(
    context: Context,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : AdService, AutoCloseable {
    private val applicationContext = context.applicationContext
    private val connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)
    private val consentInformation = UserMessagingPlatform.getConsentInformation(applicationContext)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Reward callbacks can outlive ad-service cleanup, so accepted credits must have an independent scope.
    private val rewardPersistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sdkInitializationMutex = Mutex()
    private val preloadAgePolicy = AdPreloadAgePolicy()
    private val mutableConsentState = MutableStateFlow(AdConsentState())
    private var sdkInitialized = false
    private var networkCallbackRegistered = false

    override val consentState = mutableConsentState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (isValidatedInternet(capabilities)) {
                serviceScope.launch { startPreloadingIfAllowed() }
            }
        }
    }

    init {
        registerConnectivityCallback()
    }

    override suspend fun requestConsent(activity: Activity): AdConsentState {
        val state = withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                consentInformation.requestConsentInfoUpdate(
                    activity,
                    ConsentRequestParameters.Builder().build(),
                    {
                        refreshPrivacyOptionsState()
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                            val updated = currentConsentState()
                            if (continuation.isActive) continuation.resume(updated)
                        }
                    },
                    {
                        val updated = currentConsentState()
                        if (continuation.isActive) continuation.resume(updated)
                    },
                )
            }
        }
        mutableConsentState.value = state
        if (state.canRequestAds) {
            initializeSdkIfNeeded()
            startPreloadingIfAllowed()
        } else {
            dropPreloadedAds()
        }
        return state
    }

    override suspend fun showPrivacyOptions(activity: Activity): AdConsentState {
        if (!consentState.value.privacyOptionsRequired) return consentState.value
        val state = withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                UserMessagingPlatform.showPrivacyOptionsForm(activity) {
                    val updated = currentConsentState()
                    if (continuation.isActive) continuation.resume(updated)
                }
            }
        }
        mutableConsentState.value = state
        if (state.canRequestAds) {
            initializeSdkIfNeeded()
            startPreloadingIfAllowed()
        } else {
            dropPreloadedAds()
        }
        return state
    }

    override suspend fun showInterstitial(activity: Activity): AdShowResult {
        if (!consentState.value.canRequestAds || !sdkInitialized) return AdShowResult.Unavailable
        return try {
            withContext(Dispatchers.Main.immediate) {
                if (!refreshPreloadsOnMainThread()) return@withContext AdShowResult.Unavailable
                val ad = InterstitialAdPreloader.pollAd(TestAdIds.INTERSTITIAL)
                    ?: return@withContext AdShowResult.Unavailable
                val result = CompletableDeferred<AdShowResult>()
                val completed = AtomicBoolean(false)
                ad.adEventCallback = object : InterstitialAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        if (completed.compareAndSet(false, true)) result.complete(AdShowResult.Shown)
                    }

                    override fun onAdFailedToShowFullScreenContent(
                        fullScreenContentError: FullScreenContentError,
                    ) {
                        if (completed.compareAndSet(false, true)) result.complete(AdShowResult.Failed)
                    }
                }
                try {
                    ad.show(activity)
                } catch (_: Exception) {
                    if (completed.compareAndSet(false, true)) result.complete(AdShowResult.Failed)
                }
                result.await()
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            AdShowResult.Failed
        }
    }

    override suspend fun showRewarded(
        activity: Activity,
        grantReward: suspend (grantId: String) -> Unit,
    ): AdShowResult {
        if (!consentState.value.canRequestAds || !sdkInitialized) return AdShowResult.Unavailable
        return try {
            withContext(Dispatchers.Main.immediate) {
                if (!refreshPreloadsOnMainThread()) return@withContext AdShowResult.Unavailable
                val ad = RewardedAdPreloader.pollAd(TestAdIds.REWARDED)
                    ?: return@withContext AdShowResult.Unavailable
                val grantId = UUID.randomUUID().toString()
                val grantStarted = AtomicBoolean(false)
                val rewardCallbackReceived = CompletableDeferred<Unit>()
                val grantPersisted = CompletableDeferred<Boolean>()
                val result = CompletableDeferred<AdShowResult>()
                val completed = AtomicBoolean(false)
                ad.adEventCallback = object : RewardedAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        if (completed.compareAndSet(false, true)) {
                            rewardPersistenceScope.launch {
                                val wasRewarded = withTimeoutOrNull(REWARD_CALLBACK_GRACE_MILLIS) {
                                    rewardCallbackReceived.await()
                                    true
                                } ?: false
                                val grantWasPersisted = wasRewarded && grantPersisted.await()
                                result.complete(
                                    when {
                                        !wasRewarded -> AdShowResult.DismissedWithoutReward
                                        grantWasPersisted -> AdShowResult.RewardGranted
                                        else -> AdShowResult.Failed
                                    },
                                )
                            }
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(
                        fullScreenContentError: FullScreenContentError,
                    ) {
                        if (completed.compareAndSet(false, true)) result.complete(AdShowResult.Failed)
                    }
                }
                try {
                    ad.show(activity) {
                        if (grantStarted.compareAndSet(false, true)) {
                            rewardCallbackReceived.complete(Unit)
                            rewardPersistenceScope.launch {
                                val persisted = runCatching { grantReward(grantId) }.isSuccess
                                grantPersisted.complete(persisted)
                            }
                        }
                    }
                } catch (_: Exception) {
                    if (completed.compareAndSet(false, true)) result.complete(AdShowResult.Failed)
                }
                result.await()
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            AdShowResult.Failed
        }
    }

    private suspend fun initializeSdkIfNeeded() = sdkInitializationMutex.withLock {
        if (sdkInitialized) return@withLock
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                MobileAds.initialize(
                    applicationContext,
                    InitializationConfig.Builder(TestAdIds.APPLICATION).build(),
                ) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        sdkInitialized = true
    }

    private suspend fun startPreloadingIfAllowed() {
        if (!consentState.value.canRequestAds || !sdkInitialized) return
        withContext(Dispatchers.Main.immediate) {
            refreshPreloadsOnMainThread()
        }
    }

    private fun refreshPreloadsOnMainThread(): Boolean {
        val now = nowEpochMillis()
        if (preloadAgePolicy.shouldDrop(now)) {
            dropPreloadedAdsOnMainThread()
        }
        if (!preloadAgePolicy.hasStarted()) {
            if (!hasValidatedInternet()) return false
            val interstitialRequest = AdRequest.Builder(TestAdIds.INTERSTITIAL).build()
            val rewardedRequest = AdRequest.Builder(TestAdIds.REWARDED).build()
            InterstitialAdPreloader.start(
                TestAdIds.INTERSTITIAL,
                PreloadConfiguration(interstitialRequest, bufferSize = 1),
            )
            RewardedAdPreloader.start(
                TestAdIds.REWARDED,
                PreloadConfiguration(rewardedRequest, bufferSize = 1),
            )
            preloadAgePolicy.markStarted(now)
        }
        return true
    }

    private fun dropPreloadedAds() {
        serviceScope.launch {
            withContext(Dispatchers.Main.immediate) {
                dropPreloadedAdsOnMainThread()
            }
        }
    }

    private fun dropPreloadedAdsOnMainThread() {
        if (!preloadAgePolicy.hasStarted()) return
        InterstitialAdPreloader.destroy(TestAdIds.INTERSTITIAL)
        RewardedAdPreloader.destroy(TestAdIds.REWARDED)
        preloadAgePolicy.clear()
    }

    private fun currentConsentState(): AdConsentState = AdConsentState(
        canRequestAds = consentInformation.canRequestAds(),
        privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
    )

    private fun refreshPrivacyOptionsState() {
        mutableConsentState.value = currentConsentState()
    }

    private fun hasValidatedInternet(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return isValidatedInternet(capabilities)
    }

    private fun isValidatedInternet(capabilities: NetworkCapabilities): Boolean =
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    private fun registerConnectivityCallback() {
        val manager = connectivityManager ?: return
        runCatching { manager.registerDefaultNetworkCallback(networkCallback) }
            .onSuccess { networkCallbackRegistered = true }
    }

    override fun close() {
        if (networkCallbackRegistered) {
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        }
        serviceScope.launch {
            withContext(Dispatchers.Main.immediate) { dropPreloadedAdsOnMainThread() }
            serviceScope.cancel()
        }
    }

    private companion object {
        const val REWARD_CALLBACK_GRACE_MILLIS = 1_000L
    }
}
