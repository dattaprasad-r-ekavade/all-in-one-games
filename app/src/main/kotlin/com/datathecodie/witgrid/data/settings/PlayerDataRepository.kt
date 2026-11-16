package com.datathecodie.witgrid.data.settings

import kotlinx.coroutines.flow.Flow

interface PlayerDataRepository {
    val playerData: Flow<PlayerData>

    suspend fun updateSettings(settings: AppSettings)

    suspend fun updateStreak(streak: StreakData)

    suspend fun setPremiumEnabled(enabled: Boolean)

    suspend fun setRewardBalance(type: RewardType, balance: Int)
}
