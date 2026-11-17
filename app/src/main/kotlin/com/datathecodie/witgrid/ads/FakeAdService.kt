package com.datathecodie.witgrid.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAdService(
    initialConsentState: AdConsentState = AdConsentState(canRequestAds = true),
) : AdService {
    private val mutableConsentState = MutableStateFlow(initialConsentState)

    override val consentState = mutableConsentState.asStateFlow()

    var interstitialResult: AdShowResult = AdShowResult.Unavailable

    var rewardedResult: AdShowResult = AdShowResult.Unavailable

    var nextRewardGrantId: String = "fake-reward-grant"

    override suspend fun requestConsent(activity: Activity): AdConsentState = consentState.value

    override suspend fun showPrivacyOptions(activity: Activity): AdConsentState = consentState.value

    override suspend fun showInterstitial(activity: Activity): AdShowResult = interstitialResult

    override suspend fun showRewarded(
        activity: Activity,
        grantReward: suspend (grantId: String) -> Unit,
    ): AdShowResult {
        if (rewardedResult == AdShowResult.RewardGranted) {
            grantReward(nextRewardGrantId)
        }
        return rewardedResult
    }

    fun setConsentState(state: AdConsentState) {
        mutableConsentState.value = state
    }
}
