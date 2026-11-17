package com.datathecodie.witgrid.data.settings

import kotlinx.coroutines.flow.Flow

interface PlayerDataRepository {
    val playerData: Flow<PlayerData>

    /** An accepted settings write must finish even if its screen is disposed or its caller is cancelled. */
    suspend fun updateSettings(settings: AppSettings)

    suspend fun updateReminderSettings(settings: ReminderSettings) {
        throw UnsupportedOperationException("Reminder settings are not implemented")
    }

    suspend fun updateStreak(streak: StreakData)

    /** Commits the streak repair and any required credit debit as one persistent transaction. */
    suspend fun commitStreakRepair(
        expectedStreak: StreakData,
        repairedStreak: StreakData,
        expectedMissedEpochDay: Long,
        expectedRepairDeadlineEpochMillis: Long,
        expectedLocalEpochDay: Long,
        expectedTimeZoneId: String,
        currentLocalEpochDay: () -> Long,
        currentTimeZoneId: () -> String,
        nowEpochMillis: () -> Long = System::currentTimeMillis,
    ): Boolean = throw UnsupportedOperationException("Atomic streak repair is not implemented")

    suspend fun setPremiumEnabled(enabled: Boolean)

    suspend fun setRewardBalance(type: RewardType, balance: Int)

    /**
     * Applies one monetisation update atomically. Persistent implementations must keep all
     * changed balances and counters in the same transaction.
     */
    suspend fun updateMonetisationData(update: (MonetisationData) -> MonetisationData): MonetisationData =
        throw UnsupportedOperationException("Atomic monetisation updates are not implemented")
}
