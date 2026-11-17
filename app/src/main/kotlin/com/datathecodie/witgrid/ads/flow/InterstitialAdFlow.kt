package com.datathecodie.witgrid.ads.flow

import android.app.Activity
import com.datathecodie.witgrid.ads.AdService
import com.datathecodie.witgrid.ads.AdShowResult
import com.datathecodie.witgrid.domain.monetisation.InterstitialTracker

class InterstitialAdFlow(
    private val adService: AdService,
    private val interstitialTracker: InterstitialTracker,
) {
    suspend fun afterResultNext(
        activity: Activity,
        gameId: String,
        boardSize: Int,
        firstSessionAfterInstall: Boolean,
    ): Boolean {
        val eligible = interstitialTracker.shouldShow(
            gameId = gameId,
            boardSize = boardSize,
            levelCompleted = true,
            resultNextTapped = true,
            firstSessionAfterInstall = firstSessionAfterInstall,
            adLoaded = adService.consentState.value.canRequestAds,
        )
        if (!eligible) return false

        val result = adService.showInterstitial(activity)
        if (result != AdShowResult.Shown) return false
        interstitialTracker.recordInterstitialShown(gameId)
        return true
    }
}
