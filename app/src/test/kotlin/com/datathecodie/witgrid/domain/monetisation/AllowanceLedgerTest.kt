package com.datathecodie.witgrid.domain.monetisation

import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.MonetisationData
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.data.settings.toMonetisationData
import com.datathecodie.witgrid.domain.config.Config
import com.datathecodie.witgrid.domain.config.ConfigKeys
import com.datathecodie.witgrid.domain.config.DefaultConfig
import com.datathecodie.witgrid.domain.daily.RepairableStreakMiss
import com.datathecodie.witgrid.domain.monetisation.InterstitialPolicy.shouldShow
import com.datathecodie.witgrid.play.HintAllowanceDecision
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AllowanceLedgerTest {
    @Test
    fun hintsAreSharedAndUnchangedAdviceIsChargedOnlyOnce() = runBlocking {
        val repository = FakePlayerDataRepository()
        val day = AtomicLong(20_000)
        val firstGame = ledger(repository, day, isOnline = { true })
        val secondGame = ledger(repository, day, isOnline = { true })

        assertEquals(HintAllowanceDecision.Allowed, firstGame.allowHint("same-board-hint"))
        assertEquals(1, firstGame.grantReward("reward-b", RewardType.HINT))
        assertEquals(HintAllowanceDecision.Allowed, secondGame.allowHint("different-board-hint"))
        assertEquals(HintAllowanceDecision.AlreadyCharged, secondGame.allowHint("same-board-hint"))
        assertEquals(HintAllowanceDecision.Denied, secondGame.allowHint("third-board-hint"))
        assertEquals(1, repository.current.allowanceUsage.hintsUsedToday)

        day.incrementAndGet()
        assertEquals(HintAllowanceDecision.AlreadyCharged, secondGame.allowHint("same-board-hint"))
        assertEquals(0, repository.current.allowanceUsage.hintsUsedToday)
        assertEquals(HintAllowanceDecision.Allowed, secondGame.allowHint("new-day-hint"))
        assertEquals(1, repository.current.allowanceUsage.hintsUsedToday)
    }

    @Test
    fun offlineHintsAddTheConfiguredDailyAllowance() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(100), isOnline = { false })

        repeat(4) { index ->
            assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("hint-$index"))
        }
        assertEquals(HintAllowanceDecision.Denied, ledger.allowHint("hint-over-limit"))
        assertEquals(1, repository.current.allowanceUsage.hintsUsedToday)
        assertEquals(3, repository.current.allowanceUsage.offlineHintsUsedToday)
    }

    @Test
    fun rewardedHintsAreStoredAndConsumedAtomicallyAfterFreeAllowance() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(100), isOnline = { true })

        assertEquals(1, ledger.grantReward("reward-hint", RewardType.HINT))
        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("free-hint"))
        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("rewarded-hint"))
        assertEquals(0, repository.current.rewardBalances[RewardType.HINT])
    }

    @Test
    fun retryingTheSameRewardGrantDoesNotDuplicateItsCredit() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(100), isOnline = { true })

        assertEquals(1, ledger.grantReward("reward-callback-1", RewardType.CHECK))
        assertEquals(1, ledger.grantReward("reward-callback-1", RewardType.CHECK))
        assertEquals(2, ledger.grantReward("reward-callback-2", RewardType.CHECK))
    }

    @Test
    fun easyChecksAreFreeAndOtherChecksSpendOneStoredReward() = runBlocking {
        val repository = FakePlayerDataRepository()
        val easyLedger = ledger(repository, AtomicLong(100), isOnline = { true }, easy = true)
        val standardLedger = ledger(repository, AtomicLong(100), isOnline = { true })

        assertTrue(easyLedger.allowCheck())
        assertFalse(standardLedger.allowCheck())
        ledger(repository, AtomicLong(100), isOnline = { true }).grantReward("check-grant", RewardType.CHECK)
        assertTrue(standardLedger.allowCheck())
        assertEquals(0, repository.current.rewardBalances[RewardType.CHECK])
    }

    @Test
    fun premiumAllowsHintCheckContinueAndEligibleRepairWithoutSpendingCredits() = runBlocking {
        val miss = RepairableStreakMiss(dateEpochDay = 99, deadlineEpochMillis = 200)
        val repository = FakePlayerDataRepository(
            PlayerData(
                premiumEnabled = true,
                streak = StreakData(
                    latestMissedEpochDay = miss.dateEpochDay,
                    latestMissedRepairDeadlineEpochMilli = miss.deadlineEpochMillis,
                ),
            ),
        )
        val ledger = ledger(repository, AtomicLong(100), isOnline = { true }, now = AtomicLong(100))

        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("premium-hint"))
        assertTrue(ledger.allowCheck())
        assertTrue(ledger.allowContinue())
        assertFalse(ledger.allowStreakRepair(null))
        assertTrue(ledger.allowStreakRepair(miss))
        assertEquals(0, repository.current.rewardBalances.values.sum())
    }

    @Test
    fun streakRepairRequiresTheCurrentMissAndAnUnexpiredDeadline() = runBlocking {
        val miss = RepairableStreakMiss(dateEpochDay = 99, deadlineEpochMillis = 200)
        val repository = FakePlayerDataRepository(
            PlayerData(
                rewardBalances = RewardType.entries.associateWith { 1 },
                streak = StreakData(
                    latestMissedEpochDay = miss.dateEpochDay,
                    latestMissedRepairDeadlineEpochMilli = miss.deadlineEpochMillis,
                ),
            ),
        )
        val expiredLedger = ledger(repository, AtomicLong(100), isOnline = { true }, now = AtomicLong(200))

        assertFalse(expiredLedger.allowStreakRepair(miss))
        assertEquals(1, repository.current.rewardBalances[RewardType.STREAK_REPAIR])

        val mismatchedMiss = miss.copy(dateEpochDay = 98)
        val eligibleLedger = ledger(repository, AtomicLong(100), isOnline = { true }, now = AtomicLong(100))
        assertFalse(eligibleLedger.allowStreakRepair(mismatchedMiss))
        assertTrue(eligibleLedger.allowStreakRepair(miss))
        assertEquals(0, repository.current.rewardBalances[RewardType.STREAK_REPAIR])
    }

    @Test
    fun streakRepairChecksDeadlineAfterWaitingForItsAtomicUpdate() = runBlocking {
        val miss = RepairableStreakMiss(dateEpochDay = 99, deadlineEpochMillis = 200)
        val transactionEntered = CompletableDeferred<Unit>()
        val allowTransaction = CompletableDeferred<Unit>()
        val repository = FakePlayerDataRepository(
            initial = PlayerData(
                rewardBalances = RewardType.entries.associateWith { 1 },
                streak = StreakData(
                    latestMissedEpochDay = miss.dateEpochDay,
                    latestMissedRepairDeadlineEpochMilli = miss.deadlineEpochMillis,
                ),
            ),
            beforeMonetisationUpdate = {
                transactionEntered.complete(Unit)
                allowTransaction.await()
            },
        )
        val now = AtomicLong(100)
        val ledger = ledger(repository, AtomicLong(100), isOnline = { true }, now = now)
        val repairAttempt = async { ledger.allowStreakRepair(miss) }

        transactionEntered.await()
        now.set(miss.deadlineEpochMillis)
        allowTransaction.complete(Unit)

        assertFalse(repairAttempt.await())
        assertEquals(1, repository.current.rewardBalances[RewardType.STREAK_REPAIR])
    }

    @Test
    fun continuesAndRepairsConsumeTheirOwnRewardTypes() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(100), isOnline = { true })
        ledger.grantReward("life-grant", RewardType.EXTRA_LIFE)
        ledger.grantReward("repair-grant", RewardType.STREAK_REPAIR)

        assertTrue(ledger.allowContinue())
        val miss = RepairableStreakMiss(dateEpochDay = 99, deadlineEpochMillis = 200)
        repository.updateStreak(
            StreakData(
                latestMissedEpochDay = miss.dateEpochDay,
                latestMissedRepairDeadlineEpochMilli = miss.deadlineEpochMillis,
            ),
        )
        assertTrue(ledger(repository, AtomicLong(100), isOnline = { true }, now = AtomicLong(100)).allowStreakRepair(miss))
        assertFalse(ledger.allowContinue())
        assertFalse(ledger.allowStreakRepair(null))
    }

    @Test
    fun concurrentRewardGrantsDoNotOverwriteEachOther() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(100), isOnline = { true })

        (1..20).map { index -> async { ledger.grantReward("check-$index", RewardType.CHECK) } }.awaitAll()

        assertEquals(20, repository.current.rewardBalances[RewardType.CHECK])
    }

    @Test
    fun showingAnInterstitialResetsEveryGamesSinceLastCount() = runBlocking {
        val repository = FakePlayerDataRepository()
        val tracker = InterstitialTracker(repository, DefaultConfig) { 100_000 }
        tracker.recordLevelCompleted("binary")
        tracker.recordLevelCompleted("crowns")

        tracker.recordInterstitialShown("crowns")

        val binaryCount = repository.current.levelsSinceLastInterstitial["binary"] ?: 0
        assertEquals(0, binaryCount)
    }

    @Test
    fun configDefaultsMatchThePlan() {
        assertEquals(1, DefaultConfig.int(ConfigKeys.FREE_HINTS_PER_DAY))
        assertEquals(3, DefaultConfig.int(ConfigKeys.OFFLINE_FREE_HINTS_PER_DAY))
        assertEquals(90, DefaultConfig.int(ConfigKeys.INTERSTITIAL_MIN_GAP_SEC))
        assertEquals(1, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("crowns", 6)))
        assertEquals(2, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("binary", 8)))
        assertEquals(2, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("nonogram", 5)))
        assertEquals(1, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("nonogram", 10)))
        assertEquals(2, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("minisudoku", 6)))
        assertEquals(14, ConfigKeys.allDefaults().size)
    }

    @Test
    fun interstitialRequiresEveryPolicyCondition() {
        val eligible = InterstitialRequest(
            levelCompleted = true,
            resultNextTapped = true,
            finishedLevelsSinceLast = 2,
            levelsPerInterstitial = 2,
            nowEpochMillis = 200_000,
            lastInterstitialShownEpochMillis = 100_000,
            minimumGapSeconds = 90,
            firstSessionAfterInstall = false,
            skipFirstSession = true,
            premiumEnabled = false,
            adLoaded = true,
            enabled = true,
        )

        assertTrue(shouldShow(eligible))
        listOf(
            eligible.copy(levelCompleted = false),
            eligible.copy(resultNextTapped = false),
            eligible.copy(finishedLevelsSinceLast = 1),
            eligible.copy(levelsPerInterstitial = 3),
            eligible.copy(nowEpochMillis = 99_999),
            eligible.copy(nowEpochMillis = 189_999),
            eligible.copy(firstSessionAfterInstall = true),
            eligible.copy(premiumEnabled = true),
            eligible.copy(adLoaded = false),
            eligible.copy(enabled = false),
        ).forEach { request -> assertFalse(shouldShow(request)) }
    }

    @Test
    fun interstitialTrackerPersistsSessionCounterAndMinimumGap() = runBlocking {
        val repository = FakePlayerDataRepository()
        val now = AtomicLong(100_000)
        val tracker = InterstitialTracker(repository, DefaultConfig, now::get)

        assertTrue(tracker.beginSession())
        assertFalse(tracker.beginSession())
        tracker.recordLevelCompleted("crowns")
        assertTrue(
            tracker.shouldShow(
                gameId = "crowns",
                boardSize = 6,
                levelCompleted = true,
                resultNextTapped = true,
                firstSessionAfterInstall = false,
                adLoaded = true,
            ),
        )

        tracker.recordInterstitialShown("crowns")
        assertFalse(
            tracker.shouldShow(
                gameId = "crowns",
                boardSize = 6,
                levelCompleted = true,
                resultNextTapped = true,
                firstSessionAfterInstall = false,
                adLoaded = true,
            ),
        )
        tracker.recordLevelCompleted("crowns")
        now.addAndGet(89_999)
        assertFalse(
            tracker.shouldShow(
                gameId = "crowns",
                boardSize = 6,
                levelCompleted = true,
                resultNextTapped = true,
                firstSessionAfterInstall = false,
                adLoaded = true,
            ),
        )
        now.incrementAndGet()
        assertTrue(
            tracker.shouldShow(
                gameId = "crowns",
                boardSize = 6,
                levelCompleted = true,
                resultNextTapped = true,
                firstSessionAfterInstall = false,
                adLoaded = true,
            ),
        )
    }

    private fun ledger(
        repository: PlayerDataRepository,
        day: AtomicLong,
        isOnline: () -> Boolean,
        easy: Boolean = false,
        config: Config = DefaultConfig,
        now: AtomicLong = AtomicLong(100),
    ) = AllowanceLedger(
        playerDataRepository = repository,
        config = config,
        localEpochDay = day::get,
        isOnline = isOnline,
        freeChecksInEasyPack = easy,
        nowEpochMillis = now::get,
    )

    private class FakePlayerDataRepository(
        initial: PlayerData = PlayerData(),
        private val beforeMonetisationUpdate: suspend () -> Unit = {},
    ) : PlayerDataRepository {
        private val mutableData = MutableStateFlow(initial)
        private val updateMutex = Mutex()
        val current: PlayerData get() = mutableData.value

        override val playerData: Flow<PlayerData> = mutableData.asStateFlow()

        override suspend fun updateSettings(settings: AppSettings) {
            mutableData.value = mutableData.value.copy(settings = settings)
        }

        override suspend fun updateStreak(streak: StreakData) {
            mutableData.value = mutableData.value.copy(streak = streak)
        }

        override suspend fun setPremiumEnabled(enabled: Boolean) {
            mutableData.value = mutableData.value.copy(premiumEnabled = enabled)
        }

        override suspend fun setRewardBalance(type: RewardType, balance: Int) {
            mutableData.value = mutableData.value.copy(
                rewardBalances = mutableData.value.rewardBalances + (type to balance),
            )
        }

        override suspend fun updateMonetisationData(
            update: (MonetisationData) -> MonetisationData,
        ): MonetisationData = updateMutex.withLock {
            beforeMonetisationUpdate()
            val next = update(mutableData.value.toMonetisationData())
            mutableData.value = mutableData.value.copy(
                premiumEnabled = next.premiumEnabled,
                rewardBalances = next.rewardBalances,
                allowanceUsage = next.allowanceUsage,
                processedRewardGrantIds = next.processedRewardGrantIds,
                levelsSinceLastInterstitial = next.levelsSinceLastInterstitial,
                lastInterstitialShownEpochMillis = next.lastInterstitialShownEpochMillis,
                hasStartedAppSession = next.hasStartedAppSession,
            )
            next
        }
    }
}
