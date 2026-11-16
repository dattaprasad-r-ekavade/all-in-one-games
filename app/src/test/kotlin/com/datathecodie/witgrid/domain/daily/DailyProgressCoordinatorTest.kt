package com.datathecodie.witgrid.domain.daily

import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletion
import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.data.settings.StreakData
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.awaitAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone

class DailyProgressCoordinatorTest {
    @Test
    fun reconcileRecoversTodayStreakWhenCompletionWasSavedFirst() = runBlocking {
        val playerDataRepository = FakePlayerDataRepository()
        val completionRepository = FakeDailyPuzzleCompletionRepository()
        val now = Instant.parse("2025-04-05T12:00:00Z").toEpochMilli()
        val timeZone = TimeZone.getTimeZone("UTC")
        val day = DailyProgressCoordinator(
            playerDataRepository = playerDataRepository,
            dailyPuzzleCompletionRepository = completionRepository,
            clock = DailyClock { now },
            timeZoneProvider = { timeZone },
        ).today()
        completionRepository.recordCompletion(day.epochDay, "crowns", "daily-001")
        val coordinator = DailyProgressCoordinator(
            playerDataRepository = playerDataRepository,
            dailyPuzzleCompletionRepository = completionRepository,
            clock = DailyClock { now },
            timeZoneProvider = { timeZone },
        )

        val recovered = coordinator.reconcile()

        assertEquals(1, recovered.streak.currentDays)
        assertEquals(day.epochDay, recovered.streak.lastActivityEpochDay)
    }

    @Test
    fun reconcileRecoversMultiplePastCompletionDaysBeforeApplyingMisses() = runBlocking {
        val playerDataRepository = FakePlayerDataRepository()
        val completionRepository = FakeDailyPuzzleCompletionRepository()
        val thursday = LocalDate.parse("2025-04-03").toEpochDay()
        val friday = thursday + 1
        val saturday = thursday + 2
        val lastRefillMonday = LocalDate.parse("2025-03-31").toEpochDay()
        playerDataRepository.state.value = PlayerData(
            streak = StreakData(
                currentDays = 2,
                lastActivityEpochDay = thursday,
                freezesAvailable = 0,
                lastFreezeRefillEpochDay = lastRefillMonday,
            ),
        )
        completionRepository.recordCompletion(friday, "crowns", "daily-001")
        completionRepository.recordCompletion(saturday, "binary", "daily-001")
        val coordinator = DailyProgressCoordinator(
            playerDataRepository = playerDataRepository,
            dailyPuzzleCompletionRepository = completionRepository,
            clock = DailyClock { Instant.parse("2025-04-06T12:00:00Z").toEpochMilli() },
            timeZoneProvider = { TimeZone.getTimeZone("UTC") },
        )

        val recovered = coordinator.reconcile()

        assertEquals(4, recovered.streak.currentDays)
        assertEquals(saturday, recovered.streak.lastActivityEpochDay)
        assertEquals(0, recovered.streak.freezesAvailable)
        assertEquals(null, recovered.streak.latestMissedEpochDay)
    }

    @Test
    fun reconcileClearsLegacyMissWhenThatDateHasACompletionRecord() = runBlocking {
        val playerDataRepository = FakePlayerDataRepository()
        val completionRepository = FakeDailyPuzzleCompletionRepository()
        val friday = LocalDate.parse("2025-04-04").toEpochDay()
        val saturday = friday + 1
        val lastRefillMonday = LocalDate.parse("2025-03-31").toEpochDay()
        playerDataRepository.state.value = PlayerData(
            streak = StreakData(
                currentDays = 0,
                lastActivityEpochDay = friday,
                freezesAvailable = 0,
                lastFreezeRefillEpochDay = lastRefillMonday,
                latestMissedEpochDay = saturday,
                latestMissedPreviousStreakDays = 2,
                latestMissedPreviousActivityEpochDay = friday,
                latestMissedRepairDeadlineEpochMilli =
                    Instant.parse("2025-04-07T00:00:00Z").toEpochMilli(),
            ),
        )
        completionRepository.recordCompletion(saturday, "crowns", "daily-001")
        val coordinator = DailyProgressCoordinator(
            playerDataRepository = playerDataRepository,
            dailyPuzzleCompletionRepository = completionRepository,
            clock = DailyClock { Instant.parse("2025-04-06T12:00:00Z").toEpochMilli() },
            timeZoneProvider = { TimeZone.getTimeZone("UTC") },
        )

        val recovered = coordinator.reconcile()

        assertEquals(3, recovered.streak.currentDays)
        assertEquals(saturday, recovered.streak.lastActivityEpochDay)
        assertEquals(null, recovered.streak.latestMissedEpochDay)
        assertEquals(null, recovered.repairableMiss)
    }

    @Test
    fun concurrentDailyCompletionsShareOneStreakDayAndPersistEachGame() = runBlocking {
        val playerDataRepository = FakePlayerDataRepository()
        val completionRepository = FakeDailyPuzzleCompletionRepository()
        val coordinator = DailyProgressCoordinator(
            playerDataRepository = playerDataRepository,
            dailyPuzzleCompletionRepository = completionRepository,
            clock = DailyClock { Instant.parse("2025-04-05T12:00:00Z").toEpochMilli() },
            timeZoneProvider = { TimeZone.getTimeZone("America/Los_Angeles") },
        )

        listOf("crowns", "binary", "nonogram", "minisudoku").map { gameId ->
            async { coordinator.completeDailyPuzzle(gameId, "daily-001") }
        }.awaitAll()

        assertEquals(1, playerDataRepository.current.streak.currentDays)
        assertEquals(4, completionRepository.completions.value.size)
        assertEquals(setOf("crowns", "binary", "nonogram", "minisudoku"),
            completionRepository.completions.value.map { it.gameId }.toSet())
    }

    private class FakePlayerDataRepository : PlayerDataRepository {
        val state = MutableStateFlow(PlayerData())
        val current: PlayerData get() = state.value

        override val playerData: Flow<PlayerData> = state

        override suspend fun updateSettings(settings: AppSettings) {
            state.value = state.value.copy(settings = settings)
        }

        override suspend fun updateStreak(streak: StreakData) {
            state.value = state.value.copy(streak = streak)
        }

        override suspend fun setPremiumEnabled(enabled: Boolean) {
            state.value = state.value.copy(premiumEnabled = enabled)
        }

        override suspend fun setRewardBalance(type: RewardType, balance: Int) {
            state.value = state.value.copy(
                rewardBalances = state.value.rewardBalances + (type to balance),
            )
        }
    }

    private class FakeDailyPuzzleCompletionRepository : DailyPuzzleCompletionRepository {
        val completions = MutableStateFlow<List<DailyPuzzleCompletion>>(emptyList())

        override fun observeBetween(firstEpochDay: Long, lastEpochDay: Long): Flow<List<DailyPuzzleCompletion>> =
            completions.map { rows -> rows.filter { it.dateEpochDay in firstEpochDay..lastEpochDay } }

        override suspend fun recordCompletion(dateEpochDay: Long, gameId: String, levelId: String) {
            val completion = DailyPuzzleCompletion(dateEpochDay, gameId, levelId)
            completions.value = completions.value
                .filterNot { it.dateEpochDay == dateEpochDay && it.gameId == gameId } + completion
        }
    }
}
