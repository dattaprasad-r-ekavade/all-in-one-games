package com.datathecodie.witgrid.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

data class AdConsentState(
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
)

sealed interface AdShowResult {
    data object Shown : AdShowResult

    data object RewardGranted : AdShowResult

    data object DismissedWithoutReward : AdShowResult

    data object Unavailable : AdShowResult

    data object Failed : AdShowResult
}

interface AdService {
    val consentState: StateFlow<AdConsentState>

    suspend fun requestConsent(activity: Activity): AdConsentState

    suspend fun showPrivacyOptions(activity: Activity): AdConsentState

    suspend fun showInterstitial(activity: Activity): AdShowResult

    suspend fun showRewarded(
        activity: Activity,
        grantReward: suspend (grantId: String) -> Unit,
    ): AdShowResult
}
