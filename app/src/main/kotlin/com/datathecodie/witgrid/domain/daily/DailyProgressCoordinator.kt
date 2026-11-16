package com.datathecodie.witgrid.domain.daily

import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.StreakData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone

data class DailyPuzzleDay(
    val epochDay: Long,
    val bankIndex: Int,
    val timeZoneId: String,
)

data class DailyProgressSnapshot(
    val today: DailyPuzzleDay,
    val streak: StreakData,
    val repairableMiss: RepairableStreakMiss?,
)

/** Serializes daily completion and streak read-transition-write operations. */
class DailyProgressCoordinator(
    private val playerDataRepository: PlayerDataRepository,
    private val dailyPuzzleCompletionRepository: DailyPuzzleCompletionRepository,
    private val clock: DailyClock,
    private val timeZoneProvider: () -> TimeZone,
) {
    private val mutex = Mutex()

    fun today(): DailyPuzzleDay = currentSession().day

    suspend fun reconcile(): DailyProgressSnapshot = mutex.withLock {
        val session = currentSession()
        val policy = session.policy()
        val current = playerDataRepository.playerData.first().streak
        val firstUnappliedDay = current.lastActivityEpochDay?.let { it + 1L } ?: Long.MIN_VALUE
        val firstRecoveryDay = minOf(firstUnappliedDay, current.latestMissedEpochDay ?: firstUnappliedDay)
        val savedCompletions = dailyPuzzleCompletionRepository
            .observeBetween(firstRecoveryDay, session.day.epochDay)
            .first()
        val completedDays = savedCompletions
            .asSequence()
            .map { it.dateEpochDay }
            .distinct()
            .sorted()
            .toList()
        val baseline = current.latestMissedEpochDay
            ?.takeIf { it in completedDays }
            ?.let { completedMiss -> policy.restoreCompletedMiss(current, completedMiss) }
            ?: current
        val recovered = completedDays.fold(baseline) { streak, completedEpochDay ->
                session.policyForEpochDay(completedEpochDay).completeDailyPuzzle(streak).streak
            }
        val updated = policy.reconcile(recovered).streak
        persistIfChanged(current, updated)
        snapshot(session.day, updated, policy)
    }

    suspend fun completeDailyPuzzle(gameId: String, levelId: String): DailyProgressSnapshot = mutex.withLock {
        require(gameId.isNotBlank()) { "Game ID must not be blank" }
        require(levelId.isNotBlank()) { "Level ID must not be blank" }
        val session = currentSession()
        dailyPuzzleCompletionRepository.recordCompletion(session.day.epochDay, gameId, levelId)

        val policy = session.policy()
        val current = playerDataRepository.playerData.first().streak
        val transition = policy.completeDailyPuzzle(current)
        persistIfChanged(current, transition.streak)
        snapshot(session.day, transition.streak, policy)
    }

    suspend fun repairLatestMiss(): DailyProgressSnapshot? = mutex.withLock {
        val session = currentSession()
        val policy = session.policy()
        val current = playerDataRepository.playerData.first().streak
        val transition = policy.repairLatestMiss(current) ?: return@withLock null
        persistIfChanged(current, transition.streak)
        snapshot(session.day, transition.streak, policy)
    }

    private suspend fun persistIfChanged(current: StreakData, updated: StreakData) {
        if (current != updated) playerDataRepository.updateStreak(updated)
    }

    private fun snapshot(
        today: DailyPuzzleDay,
        streak: StreakData,
        policy: DailyStreakPolicy,
    ): DailyProgressSnapshot = DailyProgressSnapshot(
        today = today,
        streak = streak,
        repairableMiss = policy.latestRepairableMiss(streak),
    )

    private fun currentSession(): DailySession {
        val nowEpochMillis = clock.nowEpochMillis()
        val timeZone = timeZoneProvider().clone() as TimeZone
        val epochDay = DailyLocalDate.epochDayAt(nowEpochMillis, timeZone)
        return DailySession(
            day = DailyPuzzleDay(
                epochDay = epochDay,
                bankIndex = DailyPuzzleIndex.forLocalEpochDay(epochDay),
                timeZoneId = timeZone.id,
            ),
            nowEpochMillis = nowEpochMillis,
            timeZone = timeZone,
        )
    }

    private data class DailySession(
        val day: DailyPuzzleDay,
        private val nowEpochMillis: Long,
        private val timeZone: TimeZone,
    ) {
        fun policy(): DailyStreakPolicy = DailyStreakPolicy(
            clock = DailyClock { nowEpochMillis },
            timeZone = timeZone,
        )

        fun policyForEpochDay(epochDay: Long): DailyStreakPolicy = DailyStreakPolicy(
            clock = DailyClock { DailyLocalDate.firstInstantOnOrAfterEpochDay(epochDay, timeZone) },
            timeZone = timeZone,
        )
    }
}
