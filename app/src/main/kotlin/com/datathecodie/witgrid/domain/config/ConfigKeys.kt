package com.datathecodie.witgrid.domain.config

object ConfigKeys {
    const val INTERSTITIAL_ENABLED = "interstitial_enabled"
    const val INTERSTITIAL_MIN_GAP_SEC = "interstitial_min_gap_sec"
    const val INTERSTITIAL_SKIP_FIRST_SESSION = "interstitial_skip_first_session"
    const val FREE_HINTS_PER_DAY = "free_hints_per_day"
    const val OFFLINE_FREE_HINTS_PER_DAY = "offline_free_hints_per_day"
    const val MISTAKE_LIMIT_DEFAULT = "mistake_limit_default"
    const val MISTAKE_LIMIT_COUNT = "mistake_limit_count"
    const val STREAK_FREE_FREEZES_PER_WEEK = "streak_free_freezes_per_week"
    const val PREMIUM_OFFER_ON_AD_FAIL = "premium_offer_on_ad_fail"

    private val booleanDefaults = mapOf(
        INTERSTITIAL_ENABLED to true,
        INTERSTITIAL_SKIP_FIRST_SESSION to true,
        MISTAKE_LIMIT_DEFAULT to false,
        PREMIUM_OFFER_ON_AD_FAIL to true,
    )

    private val integerDefaults = mapOf(
        INTERSTITIAL_MIN_GAP_SEC to 90,
        "interstitial_levels_crowns" to 1,
        "interstitial_levels_binary" to 2,
        "interstitial_levels_nonogram" to 1,
        "interstitial_levels_nonogram_5x5" to 2,
        "interstitial_levels_minisudoku" to 2,
        FREE_HINTS_PER_DAY to 1,
        OFFLINE_FREE_HINTS_PER_DAY to 3,
        MISTAKE_LIMIT_COUNT to 3,
        STREAK_FREE_FREEZES_PER_WEEK to 1,
    )

    fun interstitialLevelsKey(gameId: String, boardSize: Int): String = when {
        gameId == "nonogram" && boardSize == 5 -> "interstitial_levels_nonogram_5x5"
        else -> "interstitial_levels_$gameId"
    }

    fun booleanDefault(key: String): Boolean = booleanDefaults[key]
        ?: throw IllegalArgumentException("Unknown Boolean config key: $key")

    fun intDefault(key: String): Int = integerDefaults[key]
        ?: throw IllegalArgumentException("Unknown integer config key: $key")

    fun allDefaults(): Map<String, Any> = booleanDefaults + integerDefaults
}
