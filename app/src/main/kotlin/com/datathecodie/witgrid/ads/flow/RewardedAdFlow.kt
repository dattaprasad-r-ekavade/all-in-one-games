package com.datathecodie.witgrid.ads.flow

import android.app.Activity
import com.datathecodie.witgrid.ads.AdService
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.domain.monetisation.AllowanceLedger

class RewardedAdFlow(
    private val adService: AdService,
    private val allowanceLedger: AllowanceLedger,
) {
    suspend fun grantReward(activity: Activity, rewardType: RewardType): AdShowResult {
        if (!adService.consentState.value.canRequestAds) return AdShowResult.Unavailable
        return adService.showRewarded(activity) { grantId ->
            allowanceLedger.grantReward(grantId, rewardType)
        }
    }
}
