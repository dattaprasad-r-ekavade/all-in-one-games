package com.datathecodie.witgrid.domain.monetisation.spec

import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.MonetisationData
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.data.settings.StreakData
import com.datathecodie.witgrid.data.settings.toMonetisationData
import com.datathecodie.witgrid.domain.config.ConfigKeys
import com.datathecodie.witgrid.domain.config.DefaultConfig
import com.datathecodie.witgrid.domain.daily.RepairableStreakMiss
import com.datathecodie.witgrid.domain.monetisation.AllowanceLedger
import com.datathecodie.witgrid.domain.monetisation.InterstitialPolicy
import com.datathecodie.witgrid.domain.monetisation.InterstitialRequest
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Mark
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Technique
import com.datathecodie.witgrid.play.HintAllowanceDecision
import com.datathecodie.witgrid.play.HintController
import com.datathecodie.witgrid.play.HintRequest
import java.util.concurrent.atomic.AtomicLong
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

class MonetisationSpecTest {
    @Test
    fun interstitialRequiresACompletedLevelThenNextAndEnoughFinishedLevels() {
        val eligible = eligibleInterstitial()

        assertTrue(InterstitialPolicy.shouldShow(eligible))
        assertTrue(InterstitialPolicy.shouldShow(eligible.copy(finishedLevelsSinceLast = 3)))
        assertFalse(InterstitialPolicy.shouldShow(eligible.copy(levelCompleted = false)))
        assertFalse(InterstitialPolicy.shouldShow(eligible.copy(resultNextTapped = false)))
        assertFalse(InterstitialPolicy.shouldShow(eligible.copy(finishedLevelsSinceLast = 1)))
    }

    @Test
    fun interstitialWaitsUntilTheMinimumGapBoundary() {
        val eligible = eligibleInterstitial()

        assertFalse(InterstitialPolicy.shouldShow(eligible.copy(nowEpochMillis = 189_999)))
        assertTrue(InterstitialPolicy.shouldShow(eligible.copy(nowEpochMillis = 190_000)))
        assertTrue(InterstitialPolicy.shouldShow(eligible.copy(nowEpochMillis = 190_001)))
        assertFalse(
            InterstitialPolicy.shouldShow(
                eligible.copy(nowEpochMillis = 100_000, lastInterstitialShownEpochMillis = 100_001),
            ),
        )
        assertTrue(InterstitialPolicy.shouldShow(eligible.copy(lastInterstitialShownEpochMillis = null)))
    }

    @Test
    fun interstitialFrequencyDefaultsMatchEachLaunchGameAndBoardSize() {
        assertEquals(1, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("crowns", 6)))
        assertEquals(2, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("binary", 8)))
        assertEquals(1, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("nonogram", 10)))
        assertEquals(2, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("nonogram", 5)))
        assertEquals(2, DefaultConfig.int(ConfigKeys.interstitialLevelsKey("minisudoku", 6)))
    }

    @Test
    fun interstitialPolicyRejectsRequestsWithoutCompletionOrNextTap() {
        val eligible = eligibleInterstitial()
        val forbiddenEvents = listOf(
            eligible.copy(levelCompleted = false, resultNextTapped = false), // app start
            eligible.copy(levelCompleted = false, resultNextTapped = false), // level start
            eligible.copy(levelCompleted = false, resultNextTapped = false), // active puzzle
            eligible.copy(resultNextTapped = false), // back from the result screen
        )

        forbiddenEvents.forEach { assertFalse(InterstitialPolicy.shouldShow(it)) }
    }

    @Test
    fun interstitialPolicyRejectsDisabledPremiumFirstSessionAndUnloadedAd() {
        val eligible = eligibleInterstitial()

        listOf(
            eligible.copy(enabled = false),
            eligible.copy(premiumEnabled = true),
            eligible.copy(firstSessionAfterInstall = true),
            eligible.copy(adLoaded = false),
        ).forEach { assertFalse(InterstitialPolicy.shouldShow(it)) }
    }

    @Test
    fun oneDailyHintIsSharedAcrossGamesAndRefreshesOnTheNextLocalDate() = runBlocking {
        val repository = FakePlayerDataRepository()
        val day = AtomicLong(20_000)
        val ledger = ledger(repository, day)

        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("crowns:level-1:step-a"))
        assertEquals(HintAllowanceDecision.Denied, ledger.allowHint("binary:level-1:step-b"))

        day.incrementAndGet()
        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("binary:level-1:step-b"))
    }

    @Test
    fun unchangedHintAdviceIsNotChargedAgainAfterOtherHintsOrMidnight() = runBlocking {
        val repository = FakePlayerDataRepository()
        val day = AtomicLong(20_000)
        val ledger = ledger(repository, day)

        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("crowns:level-1:board-a:step"))
        ledger.grantReward("hint-credit-1", RewardType.HINT)
        assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("binary:level-1:board-b:step"))
        assertEquals(HintAllowanceDecision.AlreadyCharged, ledger.allowHint("crowns:level-1:board-a:step"))

        day.incrementAndGet()
        assertEquals(HintAllowanceDecision.AlreadyCharged, ledger.allowHint("crowns:level-1:board-a:step"))
        assertEquals(0, repository.current.allowanceUsage.hintsUsedToday)
    }

    @Test
    fun noUsefulHintAdviceDoesNotSpendAnAllowance() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(20_000))
        val controller = HintController(
            currentMistakes = { emptyList() },
            nextStep = { null },
            applyStep = { false },
            recordHintUse = {},
            allowance = ledger,
            boardKey = { "unchanged-board" },
        )

        assertEquals(HintRequest.NoStep, controller.requestHint())
        assertEquals(0, repository.current.allowanceUsage.hintsUsedToday)
    }

    @Test
    fun usefulHintIsChargedOnRevealOnlyAndNotOnRepeat() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(20_000))
        var recordedUses = 0
        val controller = HintController(
            currentMistakes = { emptyList() },
            nextStep = ::sampleHint,
            applyStep = { true },
            recordHintUse = { recordedUses += 1 },
            allowance = ledger,
            boardKey = { "same-board" },
        )

        assertTrue(controller.requestHint() is HintRequest.Available)
        assertTrue(controller.requestHint() is HintRequest.Available)
        assertEquals(1, recordedUses)
        assertEquals(1, repository.current.allowanceUsage.hintsUsedToday)
    }

    @Test
    fun offlineAllowanceAddsThreeHintsBeyondDailyAllowance() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(20_000), online = false)

        repeat(4) { index ->
            assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("offline-board-$index"))
        }
        assertEquals(HintAllowanceDecision.Denied, ledger.allowHint("offline-board-over-limit"))
        assertEquals(1, repository.current.allowanceUsage.hintsUsedToday)
        assertEquals(3, repository.current.allowanceUsage.offlineHintsUsedToday)
    }

    @Test
    fun checksAreFreeInEasyAndConsumeOneRewardElsewhere() = runBlocking {
        val repository = FakePlayerDataRepository()
        val easy = ledger(repository, AtomicLong(20_000), easyChecks = true)
        val standard = ledger(repository, AtomicLong(20_000))

        assertTrue(easy.allowCheck())
        assertFalse(standard.allowCheck())
        ledger(repository, AtomicLong(20_000)).grantReward("check-credit-1", RewardType.CHECK)
        assertTrue(standard.allowCheck())
        assertFalse(standard.allowCheck())
    }

    @Test
    fun rewardGrantIdPreventsDuplicateCredits() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(20_000))

        assertEquals(1, ledger.grantReward("sdk-reward-17", RewardType.HINT))
        assertEquals(1, ledger.grantReward("sdk-reward-17", RewardType.HINT))
        assertEquals(1, repository.current.rewardBalances[RewardType.HINT])
        assertTrue("sdk-reward-17" in repository.current.processedRewardGrantIds)
    }

    @Test
    fun continueConsumesOneCreditOrIsFreeWithPremium() = runBlocking {
        val repository = FakePlayerDataRepository()
        val ledger = ledger(repository, AtomicLong(20_000))

        assertFalse(ledger.allowContinue())
        ledger.grantReward("continue-credit-1", RewardType.EXTRA_LIFE)
        assertTrue(ledger.allowContinue())
        assertFalse(ledger.allowContinue())

        repository.setPremiumEnabled(true)
        assertTrue(ledger.allowContinue())
        assertTrue(ledger.allowContinue())
        assertEquals(0, repository.current.rewardBalances[RewardType.EXTRA_LIFE])
    }

    @Test
    fun repairNeedsTheLatestUncoveredMissAndMustFinishBeforeItsDeadline() = runBlocking {
        val miss = RepairableStreakMiss(dateEpochDay = 19_999, deadlineEpochMillis = 200)
        val repository = FakePlayerDataRepository(
            PlayerData(
                rewardBalances = RewardType.entries.associateWith { 1 },
                streak = StreakData(
                    latestMissedEpochDay = miss.dateEpochDay,
                    latestMissedRepairDeadlineEpochMilli = miss.deadlineEpochMillis,
                ),
            ),
        )
        val ledger = ledger(repository, AtomicLong(20_000), now = AtomicLong(199))

        assertFalse(ledger.allowStreakRepair(null))
        assertFalse(ledger.allowStreakRepair(miss.copy(dateEpochDay = miss.dateEpochDay - 1)))
        assertTrue(ledger.allowStreakRepair(miss))
        assertEquals(0, repository.current.rewardBalances[RewardType.STREAK_REPAIR])

        ledger.grantReward("repair-credit-2", RewardType.STREAK_REPAIR)
        val deadlineLedger = ledger(repository, AtomicLong(20_000), now = AtomicLong(200))
        assertFalse(deadlineLedger.allowStreakRepair(miss))
        assertEquals(1, repository.current.rewardBalances[RewardType.STREAK_REPAIR])
    }

    @Test
    fun premiumAllowsUnlimitedHintsChecksAndContinueOfflineButNotIneligibleRepairs() = runBlocking {
        val miss = RepairableStreakMiss(dateEpochDay = 19_999, deadlineEpochMillis = 200)
        val repository = FakePlayerDataRepository(
            PlayerData(
                premiumEnabled = true,
                streak = StreakData(
                    latestMissedEpochDay = miss.dateEpochDay,
                    latestMissedRepairDeadlineEpochMilli = miss.deadlineEpochMillis,
                ),
            ),
        )
        val ledger = ledger(repository, AtomicLong(20_000), online = false, now = AtomicLong(100))

        repeat(5) { index ->
            assertEquals(HintAllowanceDecision.Allowed, ledger.allowHint("premium-hint-$index"))
            assertTrue(ledger.allowCheck())
            assertTrue(ledger.allowContinue())
        }
        assertTrue(ledger.allowStreakRepair(miss))
        assertTrue(ledger.allowStreakRepair(miss))
        val olderMiss = miss.copy(dateEpochDay = miss.dateEpochDay - 1)
        assertFalse(ledger.allowStreakRepair(olderMiss))
        val expiredMiss = miss.copy(deadlineEpochMillis = 100)
        repository.updateStreak(
            StreakData(
                latestMissedEpochDay = expiredMiss.dateEpochDay,
                latestMissedRepairDeadlineEpochMilli = expiredMiss.deadlineEpochMillis,
            ),
        )
        assertFalse(ledger.allowStreakRepair(expiredMiss))
        repository.updateStreak(StreakData())
        assertFalse(ledger.allowStreakRepair(null))
        assertEquals(0, repository.current.rewardBalances.values.sum())
        assertFalse(InterstitialPolicy.shouldShow(eligibleInterstitial().copy(premiumEnabled = true)))
    }

    private fun eligibleInterstitial() = InterstitialRequest(
        levelCompleted = true,
        resultNextTapped = true,
        finishedLevelsSinceLast = 2,
        levelsPerInterstitial = 2,
        nowEpochMillis = 190_000,
        lastInterstitialShownEpochMillis = 100_000,
        minimumGapSeconds = 90,
        firstSessionAfterInstall = false,
        skipFirstSession = true,
        premiumEnabled = false,
        adLoaded = true,
        enabled = true,
    )

    private fun sampleHint() = HintStep(
        technique = Technique("test", "Single cell", 0),
        cells = listOf(Cell(0)),
        explanation = "Only one value fits this cell.",
        moves = listOf(Move.SetMark(Cell(0), Mark.FILLED)),
    )

    private fun ledger(
        repository: PlayerDataRepository,
        day: AtomicLong,
        online: Boolean = true,
        easyChecks: Boolean = false,
        now: AtomicLong = AtomicLong(100),
    ) = AllowanceLedger(
        playerDataRepository = repository,
        config = DefaultConfig,
        localEpochDay = day::get,
        isOnline = { online },
        freeChecksInEasyPack = easyChecks,
        nowEpochMillis = now::get,
    )

    private class FakePlayerDataRepository(initial: PlayerData = PlayerData()) : PlayerDataRepository {
        private val mutablePlayerData = MutableStateFlow(initial)
        private val updateMutex = Mutex()

        val current: PlayerData
            get() = mutablePlayerData.value

        override val playerData: Flow<PlayerData> = mutablePlayerData.asStateFlow()

        override suspend fun updateSettings(settings: AppSettings) {
            mutablePlayerData.value = mutablePlayerData.value.copy(settings = settings)
        }

        override suspend fun updateStreak(streak: StreakData) {
            mutablePlayerData.value = mutablePlayerData.value.copy(streak = streak)
        }

        override suspend fun setPremiumEnabled(enabled: Boolean) {
            mutablePlayerData.value = mutablePlayerData.value.copy(premiumEnabled = enabled)
        }

        override suspend fun setRewardBalance(type: RewardType, balance: Int) {
            mutablePlayerData.value = mutablePlayerData.value.copy(
                rewardBalances = mutablePlayerData.value.rewardBalances + (type to balance),
            )
        }

        override suspend fun updateMonetisationData(
            update: (MonetisationData) -> MonetisationData,
        ): MonetisationData = updateMutex.withLock {
            val next = update(mutablePlayerData.value.toMonetisationData())
            mutablePlayerData.value = mutablePlayerData.value.copy(
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
