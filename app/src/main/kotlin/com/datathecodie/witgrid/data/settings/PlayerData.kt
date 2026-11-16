package com.datathecodie.witgrid.data.settings

data class AppSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val mistakeLimitEnabled: Boolean = false,
)

data class StreakData(
    val currentDays: Int = 0,
    val lastActivityEpochDay: Long? = null,
    val freezesAvailable: Int = 0,
    val lastFreezeRefillEpochDay: Long? = null,
    val latestMissedEpochDay: Long? = null,
    val latestMissedPreviousStreakDays: Int = 0,
    val latestMissedPreviousActivityEpochDay: Long? = null,
    val latestMissedRepairDeadlineEpochMilli: Long? = null,
)

enum class RewardType(val storageKey: String) {
    HINT("hint"),
    CHECK("check"),
    EXTRA_LIFE("extra_life"),
    STREAK_REPAIR("streak_repair"),
}

data class PlayerData(
    val settings: AppSettings = AppSettings(),
    val streak: StreakData = StreakData(),
    val premiumEnabled: Boolean = false,
    val rewardBalances: Map<RewardType, Int> = RewardType.entries.associateWith { 0 },
)
