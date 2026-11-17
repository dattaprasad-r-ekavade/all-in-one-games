package com.datathecodie.witgrid.data.settings

data class AppSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val mistakeLimitEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colourwayId: String = "indigo",
    val crownsShowRegionAids: Boolean = true,
    val crownsAutoXEnabled: Boolean = false,
    val nonogramAutoCrossEnabled: Boolean = false,
    val reduceMotion: Boolean = false,
)

enum class ThemeMode(val storageId: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val hourOfDay: Int = DEFAULT_HOUR_OF_DAY,
    val minute: Int = DEFAULT_MINUTE,
) {
    init {
        require(hourOfDay in 0..23) { "Reminder hour must be between 0 and 23" }
        require(minute in 0..59) { "Reminder minute must be between 0 and 59" }
    }

    private companion object {
        const val DEFAULT_HOUR_OF_DAY = 9
        const val DEFAULT_MINUTE = 0
    }
}

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

data class AllowanceUsage(
    val localDateEpochDay: Long? = null,
    val hintsUsedToday: Int = 0,
    val offlineHintsUsedToday: Int = 0,
    val chargedHintKeys: Set<String> = emptySet(),
)

data class MonetisationData(
    val premiumEnabled: Boolean = false,
    val rewardBalances: Map<RewardType, Int> = RewardType.entries.associateWith { 0 },
    val allowanceUsage: AllowanceUsage = AllowanceUsage(),
    val processedRewardGrantIds: Set<String> = emptySet(),
    val processedContinueTransactionIds: Set<String> = emptySet(),
    val levelsSinceLastInterstitial: Map<String, Int> = emptyMap(),
    val lastInterstitialShownEpochMillis: Long? = null,
    val hasStartedAppSession: Boolean = false,
    val latestMissedEpochDay: Long? = null,
    val latestMissedRepairDeadlineEpochMillis: Long? = null,
)

data class PlayerData(
    val settings: AppSettings = AppSettings(),
    val streak: StreakData = StreakData(),
    val premiumEnabled: Boolean = false,
    val rewardBalances: Map<RewardType, Int> = RewardType.entries.associateWith { 0 },
    val allowanceUsage: AllowanceUsage = AllowanceUsage(),
    val processedRewardGrantIds: Set<String> = emptySet(),
    val processedContinueTransactionIds: Set<String> = emptySet(),
    val levelsSinceLastInterstitial: Map<String, Int> = emptyMap(),
    val lastInterstitialShownEpochMillis: Long? = null,
    val hasStartedAppSession: Boolean = false,
    val reminder: ReminderSettings = ReminderSettings(),
)

fun PlayerData.toMonetisationData(): MonetisationData = MonetisationData(
    premiumEnabled = premiumEnabled,
    rewardBalances = rewardBalances,
    allowanceUsage = allowanceUsage,
    processedRewardGrantIds = processedRewardGrantIds,
    processedContinueTransactionIds = processedContinueTransactionIds,
    levelsSinceLastInterstitial = levelsSinceLastInterstitial,
    lastInterstitialShownEpochMillis = lastInterstitialShownEpochMillis,
    hasStartedAppSession = hasStartedAppSession,
    latestMissedEpochDay = streak.latestMissedEpochDay,
    latestMissedRepairDeadlineEpochMillis = streak.latestMissedRepairDeadlineEpochMilli,
)
