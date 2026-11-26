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
import org.junit.Assert.assertFalse
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
    fun regionAidsStartOffAndPersistAnExplicitOptIn() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-region-aids")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repository = DataStorePlayerDataRepository(dataStore)
            val freshSettings = withTimeout(2_000) { repository.playerData.first() }.settings
            assertFalse(freshSettings.crownsShowRegionAids)
            repository.updateSettings(freshSettings.copy(crownsShowRegionAids = true))
            val reopened = DataStorePlayerDataRepository(dataStore)
            assertTrue(withTimeout(2_000) { reopened.playerData.first() }.settings.crownsShowRegionAids)
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }

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
                    reduceMotion = true,
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
                    processedContinueTransactionIds = setOf("continue-transaction"),
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
                    reduceMotion = true,
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
                    RewardType.EXTRA_TUBE to 0,
                    RewardType.STREAK_REPAIR to 0,
                ),
                allowanceUsage = AllowanceUsage(
                    localDateEpochDay = 20_000,
                    hintsUsedToday = 1,
                    offlineHintsUsedToday = 2,
                    chargedHintKeys = setOf("hint-key"),
                ),
                processedRewardGrantIds = setOf("reward-grant"),
                processedContinueTransactionIds = setOf("continue-transaction"),
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

    @Test
    fun streakRepairAndCreditDebitCommitAtomically() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-streak-repair")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val repository = DataStorePlayerDataRepository(dataStore)
        val expectedStreak = repairableStreak(deadlineEpochMillis = 10_000)
        val repairedStreak = repairedStreak()

        try {
            repository.updateStreak(expectedStreak)
            repository.setRewardBalance(RewardType.STREAK_REPAIR, 1)

            val committed = repository.commitStreakRepair(
                expectedStreak = expectedStreak,
                repairedStreak = repairedStreak,
                expectedMissedEpochDay = 20_000,
                expectedRepairDeadlineEpochMillis = 10_000,
                expectedLocalEpochDay = 20_001,
                expectedTimeZoneId = "UTC",
                currentLocalEpochDay = { 20_001 },
                currentTimeZoneId = { "UTC" },
                nowEpochMillis = { 9_999 },
            )

            assertTrue(committed)
            val persisted = DataStorePlayerDataRepository(dataStore).playerData.first()
            assertEquals(repairedStreak, persisted.streak)
            assertEquals(0, persisted.rewardBalances[RewardType.STREAK_REPAIR])

            assertFalse(
                repository.commitStreakRepair(
                    expectedStreak = expectedStreak,
                    repairedStreak = repairedStreak,
                    expectedMissedEpochDay = 20_000,
                    expectedRepairDeadlineEpochMillis = 10_000,
                    expectedLocalEpochDay = 20_001,
                    expectedTimeZoneId = "UTC",
                    currentLocalEpochDay = { 20_001 },
                    currentTimeZoneId = { "UTC" },
                    nowEpochMillis = { 9_999 },
                ),
            )
            assertEquals(0, repository.playerData.first().rewardBalances[RewardType.STREAK_REPAIR])
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun expiredRepairKeepsTheCreditAndStreakUnchanged() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-expired-streak-repair")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val repository = DataStorePlayerDataRepository(dataStore)
        val expectedStreak = repairableStreak(deadlineEpochMillis = 10_000)

        try {
            repository.updateStreak(expectedStreak)
            repository.setRewardBalance(RewardType.STREAK_REPAIR, 1)

            val committed = repository.commitStreakRepair(
                expectedStreak = expectedStreak,
                repairedStreak = repairedStreak(),
                expectedMissedEpochDay = 20_000,
                expectedRepairDeadlineEpochMillis = 10_000,
                expectedLocalEpochDay = 20_001,
                expectedTimeZoneId = "UTC",
                currentLocalEpochDay = { 20_001 },
                currentTimeZoneId = { "UTC" },
                nowEpochMillis = { 10_000 },
            )

            assertFalse(committed)
            val persisted = repository.playerData.first()
            assertEquals(expectedStreak, persisted.streak)
            assertEquals(1, persisted.rewardBalances[RewardType.STREAK_REPAIR])
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun localDateOrTimeZoneChangeRejectsAStaleRepairWithoutSpendingCredit() = runBlocking {
        val directory = Files.createTempDirectory("witgrid-stale-streak-repair")
        val file = directory.resolve("witgrid_settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val repository = DataStorePlayerDataRepository(dataStore)
        val expectedStreak = repairableStreak(deadlineEpochMillis = 10_000)

        try {
            repository.updateStreak(expectedStreak)
            repository.setRewardBalance(RewardType.STREAK_REPAIR, 1)

            assertFalse(
                repository.commitStreakRepair(
                    expectedStreak = expectedStreak,
                    repairedStreak = repairedStreak(),
                    expectedMissedEpochDay = 20_000,
                    expectedRepairDeadlineEpochMillis = 10_000,
                    expectedLocalEpochDay = 20_001,
                    expectedTimeZoneId = "UTC",
                    currentLocalEpochDay = { 20_002 },
                    currentTimeZoneId = { "UTC" },
                    nowEpochMillis = { 9_999 },
                ),
            )
            assertFalse(
                repository.commitStreakRepair(
                    expectedStreak = expectedStreak,
                    repairedStreak = repairedStreak(),
                    expectedMissedEpochDay = 20_000,
                    expectedRepairDeadlineEpochMillis = 10_000,
                    expectedLocalEpochDay = 20_001,
                    expectedTimeZoneId = "UTC",
                    currentLocalEpochDay = { 20_001 },
                    currentTimeZoneId = { "Asia/Kolkata" },
                    nowEpochMillis = { 9_999 },
                ),
            )

            val persisted = repository.playerData.first()
            assertEquals(expectedStreak, persisted.streak)
            assertEquals(1, persisted.rewardBalances[RewardType.STREAK_REPAIR])
        } finally {
            scope.cancel()
            Files.deleteIfExists(file.toPath())
            Files.deleteIfExists(directory)
        }
    }
}

private fun repairableStreak(deadlineEpochMillis: Long) = StreakData(
    currentDays = 4,
    lastActivityEpochDay = 19_999,
    latestMissedEpochDay = 20_000,
    latestMissedPreviousStreakDays = 4,
    latestMissedPreviousActivityEpochDay = 19_999,
    latestMissedRepairDeadlineEpochMilli = deadlineEpochMillis,
)

private fun repairedStreak() = StreakData(
    currentDays = 5,
    lastActivityEpochDay = 20_000,
)

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
