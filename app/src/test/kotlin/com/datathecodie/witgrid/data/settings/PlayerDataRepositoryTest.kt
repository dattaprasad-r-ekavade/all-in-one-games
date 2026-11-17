package com.datathecodie.witgrid.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerDataRepositoryTest {
    @Test
    fun savedPlayerDataSurvivesRepositoryRecreation() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-player-data")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )

        try {
            val writer = DataStorePlayerDataRepository(dataStore)
            writer.updateSettings(
                AppSettings(
                    soundEnabled = false,
                    hapticsEnabled = true,
                    mistakeLimitEnabled = true,
                ),
            )
            writer.updateStreak(
                StreakData(
                    currentDays = 9,
                    lastActivityEpochDay = 20_000,
                    freezesAvailable = 1,
                    lastFreezeRefillEpochDay = 19_997,
                    latestMissedEpochDay = 19_999,
                    latestMissedPreviousStreakDays = 8,
                    latestMissedPreviousActivityEpochDay = 19_998,
                    latestMissedRepairDeadlineEpochMilli = 1_800_000_000_000,
                ),
            )
            writer.setPremiumEnabled(true)
            writer.setRewardBalance(RewardType.HINT, 3)
            writer.updateMonetisationData { monetisation ->
                monetisation.copy(
                    allowanceUsage = AllowanceUsage(
                        localDateEpochDay = 20_000,
                        hintsUsedToday = 1,
                        offlineHintsUsedToday = 2,
                        chargedHintKeys = setOf("hint-key"),
                    ),
                    processedRewardGrantIds = setOf("reward-grant"),
                    levelsSinceLastInterstitial = mapOf("crowns" to 2),
                    lastInterstitialShownEpochMillis = 1_700_000_000_000,
                    hasStartedAppSession = true,
                )
            }

            val expected = PlayerData(
                settings = AppSettings(
                    soundEnabled = false,
                    hapticsEnabled = true,
                    mistakeLimitEnabled = true,
                ),
                streak = StreakData(
                    currentDays = 9,
                    lastActivityEpochDay = 20_000,
                    freezesAvailable = 1,
                    lastFreezeRefillEpochDay = 19_997,
                    latestMissedEpochDay = 19_999,
                    latestMissedPreviousStreakDays = 8,
                    latestMissedPreviousActivityEpochDay = 19_998,
                    latestMissedRepairDeadlineEpochMilli = 1_800_000_000_000,
                ),
                premiumEnabled = true,
                rewardBalances = mapOf(
                    RewardType.HINT to 3,
                    RewardType.CHECK to 0,
                    RewardType.EXTRA_LIFE to 0,
                    RewardType.STREAK_REPAIR to 0,
                ),
                allowanceUsage = AllowanceUsage(
                    localDateEpochDay = 20_000,
                    hintsUsedToday = 1,
                    offlineHintsUsedToday = 2,
                    chargedHintKeys = setOf("hint-key"),
                ),
                processedRewardGrantIds = setOf("reward-grant"),
                levelsSinceLastInterstitial = mapOf("crowns" to 2),
                lastInterstitialShownEpochMillis = 1_700_000_000_000,
                hasStartedAppSession = true,
            )
            val recreated = DataStorePlayerDataRepository(dataStore)

            assertEquals(expected, writer.playerData.first())
            assertEquals(expected, recreated.playerData.first())
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }
}
