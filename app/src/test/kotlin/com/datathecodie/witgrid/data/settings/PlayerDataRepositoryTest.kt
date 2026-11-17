package com.datathecodie.witgrid.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerDataRepositoryTest {
    @Test
    fun cancelledDataStoreUpdateDoesNotStopSettingsWriter() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-cancelled-store-update")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val backingStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file },
        )
        val repository = DataStorePlayerDataRepository(CancelFirstUpdateDataStore(backingStore))
        val laterSettings = AppSettings(themeMode = ThemeMode.LIGHT, colourwayId = "forest")

        try {
            val firstWriteFailed = try {
                repository.updateSettings(AppSettings(themeMode = ThemeMode.DARK))
                false
            } catch (_: CancellationException) {
                true
            }
            assertTrue(firstWriteFailed)

            repository.updateSettings(laterSettings)
            val persistedPlayerData = withTimeout(2_000) {
                repository.playerData.first { it.settings == laterSettings }
            }
            assertEquals(laterSettings, persistedPlayerData.settings)
        } finally {
            dataStoreScope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun acceptedSettingsWriteCompletesAfterCallerIsCancelled() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-cancelled-settings-write")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file },
        )
        val repository = DataStorePlayerDataRepository(dataStore)
        val requestedSettings = AppSettings(
            soundEnabled = false,
            hapticsEnabled = false,
            themeMode = ThemeMode.LIGHT,
            colourwayId = "plum",
        )

        try {
            val caller = launch(start = CoroutineStart.UNDISPATCHED) {
                repository.updateSettings(requestedSettings)
            }
            caller.cancelAndJoin()

            val persistedPlayerData = withTimeout(2_000) {
                repository.playerData.first { it.settings == requestedSettings }
            }
            assertEquals(requestedSettings, persistedPlayerData.settings)
        } finally {
            dataStoreScope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }

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
                    themeMode = ThemeMode.DARK,
                    colourwayId = "forest",
                    crownsShowRegionAids = false,
                    crownsAutoXEnabled = true,
                    nonogramAutoCrossEnabled = true,
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
                    themeMode = ThemeMode.DARK,
                    colourwayId = "forest",
                    crownsShowRegionAids = false,
                    crownsAutoXEnabled = true,
                    nonogramAutoCrossEnabled = true,
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

private class CancelFirstUpdateDataStore(
    private val delegate: DataStore<Preferences>,
) : DataStore<Preferences> {
    override val data: Flow<Preferences> = delegate.data
    private var shouldCancelNextUpdate = true

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        if (shouldCancelNextUpdate) {
            shouldCancelNextUpdate = false
            throw CancellationException("Simulated cancelled storage update")
        }
        return delegate.updateData(transform)
    }
}
