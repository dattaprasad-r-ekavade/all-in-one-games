package com.datathecodie.witgrid.domain.monetisation

import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.toMonetisationData
import com.datathecodie.witgrid.domain.config.Config
import com.datathecodie.witgrid.domain.config.ConfigKeys
import kotlinx.coroutines.flow.first

class InterstitialTracker(
    private val playerDataRepository: PlayerDataRepository,
    private val config: Config,
    private val clock: () -> Long,
) {
    suspend fun beginSession(): Boolean {
        var isFirstSession = false
        playerDataRepository.updateMonetisationData { current ->
            isFirstSession = !current.hasStartedAppSession
            current.copy(hasStartedAppSession = true)
        }
        return isFirstSession
    }

    suspend fun recordLevelCompleted(gameId: String) {
        require(gameId.isNotBlank()) { "Game ID must not be blank" }
        playerDataRepository.updateMonetisationData { current ->
            val nextCount = (current.levelsSinceLastInterstitial[gameId] ?: 0) + 1
            current.copy(
                levelsSinceLastInterstitial = current.levelsSinceLastInterstitial + (gameId to nextCount),
            )
        }
    }

    suspend fun shouldShow(
        gameId: String,
        boardSize: Int,
        levelCompleted: Boolean,
        resultNextTapped: Boolean,
        firstSessionAfterInstall: Boolean,
        adLoaded: Boolean,
    ): Boolean {
        val data = playerDataRepository.playerData.first().toMonetisationData()
        val intervalKey = ConfigKeys.interstitialLevelsKey(gameId, boardSize)
        return InterstitialPolicy.shouldShow(
            InterstitialRequest(
                levelCompleted = levelCompleted,
                resultNextTapped = resultNextTapped,
                finishedLevelsSinceLast = data.levelsSinceLastInterstitial[gameId] ?: 0,
                levelsPerInterstitial = config.int(intervalKey).coerceAtLeast(1),
                nowEpochMillis = clock(),
                lastInterstitialShownEpochMillis = data.lastInterstitialShownEpochMillis,
                minimumGapSeconds = config.int(ConfigKeys.INTERSTITIAL_MIN_GAP_SEC).coerceAtLeast(0),
                firstSessionAfterInstall = firstSessionAfterInstall,
                skipFirstSession = config.boolean(ConfigKeys.INTERSTITIAL_SKIP_FIRST_SESSION),
                premiumEnabled = data.premiumEnabled,
                adLoaded = adLoaded,
                enabled = config.boolean(ConfigKeys.INTERSTITIAL_ENABLED),
            ),
        )
    }

    suspend fun recordInterstitialShown(gameId: String) {
        require(gameId.isNotBlank()) { "Game ID must not be blank" }
        val shownAt = clock()
        playerDataRepository.updateMonetisationData { current ->
            current.copy(
                levelsSinceLastInterstitial = emptyMap(),
                lastInterstitialShownEpochMillis = shownAt,
            )
        }
    }
}
