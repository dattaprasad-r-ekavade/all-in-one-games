package com.datathecodie.witgrid.domain.monetisation

data class InterstitialRequest(
    val levelCompleted: Boolean,
    val resultNextTapped: Boolean,
    val finishedLevelsSinceLast: Int,
    val levelsPerInterstitial: Int,
    val nowEpochMillis: Long,
    val lastInterstitialShownEpochMillis: Long?,
    val minimumGapSeconds: Int,
    val firstSessionAfterInstall: Boolean,
    val skipFirstSession: Boolean,
    val premiumEnabled: Boolean,
    val adLoaded: Boolean,
    val enabled: Boolean,
)

object InterstitialPolicy {
    fun shouldShow(request: InterstitialRequest): Boolean {
        require(request.finishedLevelsSinceLast >= 0) { "Finished level count must not be negative" }
        require(request.levelsPerInterstitial > 0) { "Interstitial level interval must be positive" }
        require(request.minimumGapSeconds >= 0) { "Interstitial gap must not be negative" }

        val minimumGapMillis = request.minimumGapSeconds * MILLIS_PER_SECOND
        val gapPassed = request.lastInterstitialShownEpochMillis?.let { lastShown ->
            request.nowEpochMillis >= lastShown && request.nowEpochMillis - lastShown >= minimumGapMillis
        } ?: true

        return request.enabled &&
            request.levelCompleted &&
            request.resultNextTapped &&
            request.finishedLevelsSinceLast >= request.levelsPerInterstitial &&
            gapPassed &&
            !(request.skipFirstSession && request.firstSessionAfterInstall) &&
            !request.premiumEnabled &&
            request.adLoaded
    }

    private const val MILLIS_PER_SECOND = 1_000L
}
