package com.datathecodie.witgrid.data.settings

import kotlinx.coroutines.flow.Flow

interface PlayerDataRepository {
    val playerData: Flow<PlayerData>

    suspend fun updateSettings(settings: AppSettings)

    suspend fun updateStreak(streak: StreakData)

    suspend fun setPremiumEnabled(enabled: Boolean)

    suspend fun setRewardBalance(type: RewardType, balance: Int)

    /**
     * Applies one monetisation update atomically. Persistent implementations must keep all
     * changed balances and counters in the same transaction.
     */
    suspend fun updateMonetisationData(update: (MonetisationData) -> MonetisationData): MonetisationData =
        throw UnsupportedOperationException("Atomic monetisation updates are not implemented")
}
