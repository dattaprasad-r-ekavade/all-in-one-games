package com.datathecodie.witgrid.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "witgrid_settings"

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

class DataStorePlayerDataRepository(
    private val dataStore: DataStore<Preferences>,
) : PlayerDataRepository {
    constructor(context: Context) : this(context.applicationContext.playerDataStore)

    override val playerData: Flow<PlayerData> = dataStore.data.map { preferences ->
        val monetisation = readMonetisationData(preferences)
        PlayerData(
            settings = AppSettings(
                soundEnabled = preferences[SOUND_ENABLED] ?: true,
                hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
                mistakeLimitEnabled = preferences[MISTAKE_LIMIT_ENABLED] ?: false,
            ),
            reminder = ReminderSettings(
                enabled = preferences[REMINDER_ENABLED] ?: false,
                hourOfDay = (preferences[REMINDER_HOUR_OF_DAY] ?: 9).coerceIn(0, 23),
                minute = (preferences[REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
            ),
            streak = StreakData(
                currentDays = preferences[STREAK_DAYS] ?: 0,
                lastActivityEpochDay = preferences[LAST_ACTIVITY_EPOCH_DAY],
                freezesAvailable = preferences[FREEZES_AVAILABLE] ?: 0,
                lastFreezeRefillEpochDay = preferences[LAST_FREEZE_REFILL_EPOCH_DAY],
                latestMissedEpochDay = preferences[LATEST_MISSED_EPOCH_DAY],
                latestMissedPreviousStreakDays = preferences[LATEST_MISSED_PREVIOUS_STREAK_DAYS] ?: 0,
                latestMissedPreviousActivityEpochDay = preferences[LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY],
                latestMissedRepairDeadlineEpochMilli = preferences[LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI],
            ),
            premiumEnabled = monetisation.premiumEnabled,
            rewardBalances = monetisation.rewardBalances,
            allowanceUsage = monetisation.allowanceUsage,
            processedRewardGrantIds = monetisation.processedRewardGrantIds,
            levelsSinceLastInterstitial = monetisation.levelsSinceLastInterstitial,
            lastInterstitialShownEpochMillis = monetisation.lastInterstitialShownEpochMillis,
            hasStartedAppSession = monetisation.hasStartedAppSession,
        )
    }

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = settings.soundEnabled
            preferences[HAPTICS_ENABLED] = settings.hapticsEnabled
            preferences[MISTAKE_LIMIT_ENABLED] = settings.mistakeLimitEnabled
        }
    }

    override suspend fun updateReminderSettings(settings: ReminderSettings) {
        dataStore.edit { preferences ->
            preferences[REMINDER_ENABLED] = settings.enabled
            preferences[REMINDER_HOUR_OF_DAY] = settings.hourOfDay
            preferences[REMINDER_MINUTE] = settings.minute
        }
    }

    override suspend fun updateStreak(streak: StreakData) {
        dataStore.edit { preferences ->
            preferences[STREAK_DAYS] = streak.currentDays
            preferences[FREEZES_AVAILABLE] = streak.freezesAvailable
            streak.lastActivityEpochDay?.let { day ->
                preferences[LAST_ACTIVITY_EPOCH_DAY] = day
            } ?: preferences.remove(LAST_ACTIVITY_EPOCH_DAY)
            streak.lastFreezeRefillEpochDay?.let { day ->
                preferences[LAST_FREEZE_REFILL_EPOCH_DAY] = day
            } ?: preferences.remove(LAST_FREEZE_REFILL_EPOCH_DAY)
            streak.latestMissedEpochDay?.let { day ->
                preferences[LATEST_MISSED_EPOCH_DAY] = day
                preferences[LATEST_MISSED_PREVIOUS_STREAK_DAYS] = streak.latestMissedPreviousStreakDays
                streak.latestMissedPreviousActivityEpochDay?.let { previousDay ->
                    preferences[LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY] = previousDay
                } ?: preferences.remove(LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY)
                streak.latestMissedRepairDeadlineEpochMilli?.let { deadline ->
                    preferences[LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI] = deadline
                } ?: preferences.remove(LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI)
            } ?: run {
                preferences.remove(LATEST_MISSED_EPOCH_DAY)
                preferences.remove(LATEST_MISSED_PREVIOUS_STREAK_DAYS)
                preferences.remove(LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY)
                preferences.remove(LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI)
            }
        }
    }

    override suspend fun setPremiumEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PREMIUM_ENABLED] = enabled }
    }

    override suspend fun setRewardBalance(type: RewardType, balance: Int) {
        require(balance >= 0) { "Reward balance must not be negative" }
        dataStore.edit { preferences -> preferences[rewardBalanceKey(type)] = balance }
    }

    override suspend fun updateMonetisationData(
        update: (MonetisationData) -> MonetisationData,
    ): MonetisationData {
        var updated: MonetisationData? = null
        dataStore.edit { preferences ->
            val current = readMonetisationData(preferences)
            val next = update(current)
            validate(next)
            writeMonetisationData(preferences, next, current.levelsSinceLastInterstitial.keys)
            updated = next
        }
        return requireNotNull(updated)
    }

    private fun rewardBalanceKey(type: RewardType) = intPreferencesKey("reward_${type.storageKey}")

    private fun readMonetisationData(preferences: Preferences): MonetisationData = MonetisationData(
        premiumEnabled = preferences[PREMIUM_ENABLED] ?: false,
        rewardBalances = RewardType.entries.associateWith { type -> preferences[rewardBalanceKey(type)] ?: 0 },
        allowanceUsage = AllowanceUsage(
            localDateEpochDay = preferences[ALLOWANCE_USAGE_DATE],
            hintsUsedToday = preferences[HINTS_USED_TODAY] ?: 0,
            offlineHintsUsedToday = preferences[OFFLINE_HINTS_USED_TODAY] ?: 0,
            chargedHintKeys = preferences[CHARGED_HINT_KEYS] ?: emptySet(),
        ),
        processedRewardGrantIds = preferences[PROCESSED_REWARD_GRANT_IDS] ?: emptySet(),
        levelsSinceLastInterstitial = INTERSTITIAL_GAME_IDS.mapNotNull { gameId ->
            preferences[interstitialLevelKey(gameId)]?.let { gameId to it }
        }.toMap(),
        lastInterstitialShownEpochMillis = preferences[LAST_INTERSTITIAL_SHOWN_EPOCH_MILLIS],
        hasStartedAppSession = preferences[HAS_STARTED_APP_SESSION] ?: false,
        latestMissedEpochDay = preferences[LATEST_MISSED_EPOCH_DAY],
        latestMissedRepairDeadlineEpochMillis = preferences[LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI],
    )

    private fun writeMonetisationData(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        data: MonetisationData,
        previouslyTrackedGames: Set<String>,
    ) {
        preferences[PREMIUM_ENABLED] = data.premiumEnabled
        RewardType.entries.forEach { type ->
            preferences[rewardBalanceKey(type)] = data.rewardBalances[type] ?: 0
        }
        data.allowanceUsage.localDateEpochDay?.let { preferences[ALLOWANCE_USAGE_DATE] = it }
            ?: preferences.remove(ALLOWANCE_USAGE_DATE)
        preferences[HINTS_USED_TODAY] = data.allowanceUsage.hintsUsedToday
        preferences[OFFLINE_HINTS_USED_TODAY] = data.allowanceUsage.offlineHintsUsedToday
        preferences[CHARGED_HINT_KEYS] = data.allowanceUsage.chargedHintKeys
        preferences[PROCESSED_REWARD_GRANT_IDS] = data.processedRewardGrantIds
        (previouslyTrackedGames + data.levelsSinceLastInterstitial.keys).forEach { gameId ->
            val key = interstitialLevelKey(gameId)
            val count = data.levelsSinceLastInterstitial[gameId] ?: 0
            if (count == 0) preferences.remove(key) else preferences[key] = count
        }
        data.lastInterstitialShownEpochMillis?.let { preferences[LAST_INTERSTITIAL_SHOWN_EPOCH_MILLIS] = it }
            ?: preferences.remove(LAST_INTERSTITIAL_SHOWN_EPOCH_MILLIS)
        preferences[HAS_STARTED_APP_SESSION] = data.hasStartedAppSession
    }

    private fun validate(data: MonetisationData) {
        require(data.rewardBalances.values.all { it >= 0 }) { "Reward balances must not be negative" }
        require(data.processedRewardGrantIds.none(String::isBlank)) { "Reward grant IDs must not be blank" }
        require(data.allowanceUsage.hintsUsedToday >= 0) { "Hint allowance use must not be negative" }
        require(data.allowanceUsage.offlineHintsUsedToday >= 0) {
            "Offline hint allowance use must not be negative"
        }
        require(data.allowanceUsage.chargedHintKeys.none(String::isBlank)) { "Hint keys must not be blank" }
        require(data.levelsSinceLastInterstitial.values.all { it >= 0 }) {
            "Interstitial level counts must not be negative"
        }
        require(data.levelsSinceLastInterstitial.keys.all(String::isNotBlank)) {
            "Interstitial game IDs must not be blank"
        }
    }

    private fun interstitialLevelKey(gameId: String) = intPreferencesKey("interstitial_levels_$gameId")

    private companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val MISTAKE_LIMIT_ENABLED = booleanPreferencesKey("mistake_limit_enabled")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR_OF_DAY = intPreferencesKey("reminder_hour_of_day")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_ACTIVITY_EPOCH_DAY = longPreferencesKey("last_activity_epoch_day")
        val FREEZES_AVAILABLE = intPreferencesKey("freezes_available")
        val LAST_FREEZE_REFILL_EPOCH_DAY = longPreferencesKey("last_freeze_refill_epoch_day")
        val LATEST_MISSED_EPOCH_DAY = longPreferencesKey("latest_missed_epoch_day")
        val LATEST_MISSED_PREVIOUS_STREAK_DAYS = intPreferencesKey("latest_missed_previous_streak_days")
        val LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY = longPreferencesKey("latest_missed_previous_activity_epoch_day")
        val LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI = longPreferencesKey("latest_missed_repair_deadline_epoch_milli")
        val PREMIUM_ENABLED = booleanPreferencesKey("premium_enabled")
        val ALLOWANCE_USAGE_DATE = longPreferencesKey("allowance_usage_date_epoch_day")
        val HINTS_USED_TODAY = intPreferencesKey("hints_used_today")
        val OFFLINE_HINTS_USED_TODAY = intPreferencesKey("offline_hints_used_today")
        val CHARGED_HINT_KEYS = androidx.datastore.preferences.core.stringSetPreferencesKey("charged_hint_keys")
        val PROCESSED_REWARD_GRANT_IDS =
            androidx.datastore.preferences.core.stringSetPreferencesKey("processed_reward_grant_ids")
        val LAST_INTERSTITIAL_SHOWN_EPOCH_MILLIS = longPreferencesKey("last_interstitial_shown_epoch_millis")
        val HAS_STARTED_APP_SESSION = booleanPreferencesKey("has_started_app_session")
        val INTERSTITIAL_GAME_IDS = listOf("crowns", "binary", "nonogram", "minisudoku")
    }
}
