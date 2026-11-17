package com.datathecodie.witgrid.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val DATASTORE_NAME = "witgrid_settings"

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

private object PlayerSettingsWriteOwner {
    private data class Write(
        val persist: suspend () -> Unit,
        val completion: CompletableDeferred<Unit>,
    )

    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            for (write in writes) {
                try {
                    write.persist()
                    write.completion.complete(Unit)
                } catch (cancellationException: CancellationException) {
                    write.completion.completeExceptionally(cancellationException)
                    if (!scope.isActive) throw cancellationException
                } catch (exception: Exception) {
                    write.completion.completeExceptionally(exception)
                }
            }
        }
    }

    suspend fun submit(persist: suspend () -> Unit) {
        val completion = CompletableDeferred<Unit>()
        writes.send(Write(persist, completion))
        completion.await()
    }
}

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
                themeMode = ThemeMode.entries.firstOrNull {
                    it.storageId == preferences[THEME_MODE]
                } ?: ThemeMode.SYSTEM,
                colourwayId = preferences[COLOURWAY_ID] ?: "indigo",
                crownsShowRegionAids = preferences[CROWNS_SHOW_REGION_AIDS] ?: AppSettings().crownsShowRegionAids,
                crownsAutoXEnabled = preferences[CROWNS_AUTO_X_ENABLED] ?: false,
                nonogramAutoCrossEnabled = preferences[NONOGRAM_AUTO_CROSS_ENABLED] ?: false,
                reduceMotion = preferences[REDUCE_MOTION] ?: false,
                welcomeSeen = preferences[WELCOME_SEEN] ?: false,
            ),
            reminder = ReminderSettings(
                enabled = preferences[REMINDER_ENABLED] ?: false,
                hourOfDay = (preferences[REMINDER_HOUR_OF_DAY] ?: 9).coerceIn(0, 23),
                minute = (preferences[REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
            ),
            streak = readStreak(preferences),
            premiumEnabled = monetisation.premiumEnabled,
            rewardBalances = monetisation.rewardBalances,
            allowanceUsage = monetisation.allowanceUsage,
            processedRewardGrantIds = monetisation.processedRewardGrantIds,
            processedContinueTransactionIds = monetisation.processedContinueTransactionIds,
            levelsSinceLastInterstitial = monetisation.levelsSinceLastInterstitial,
            lastInterstitialShownEpochMillis = monetisation.lastInterstitialShownEpochMillis,
            hasStartedAppSession = monetisation.hasStartedAppSession,
        )
    }

    override suspend fun updateSettings(settings: AppSettings) {
        PlayerSettingsWriteOwner.submit {
            dataStore.edit { preferences ->
                preferences[SOUND_ENABLED] = settings.soundEnabled
                preferences[HAPTICS_ENABLED] = settings.hapticsEnabled
                preferences[MISTAKE_LIMIT_ENABLED] = settings.mistakeLimitEnabled
                preferences[THEME_MODE] = settings.themeMode.storageId
                preferences[COLOURWAY_ID] = settings.colourwayId
                preferences[CROWNS_SHOW_REGION_AIDS] = settings.crownsShowRegionAids
                preferences[CROWNS_AUTO_X_ENABLED] = settings.crownsAutoXEnabled
                preferences[NONOGRAM_AUTO_CROSS_ENABLED] = settings.nonogramAutoCrossEnabled
                preferences[REDUCE_MOTION] = settings.reduceMotion
                preferences[WELCOME_SEEN] = settings.welcomeSeen
            }
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
        dataStore.edit { preferences -> writeStreak(preferences, streak) }
    }

    override suspend fun commitStreakRepair(
        expectedStreak: StreakData,
        repairedStreak: StreakData,
        expectedMissedEpochDay: Long,
        expectedRepairDeadlineEpochMillis: Long,
        expectedLocalEpochDay: Long,
        expectedTimeZoneId: String,
        currentLocalEpochDay: () -> Long,
        currentTimeZoneId: () -> String,
        nowEpochMillis: () -> Long,
    ): Boolean {
        var committed = false
        dataStore.edit { preferences ->
            val currentStreak = readStreak(preferences)
            val currentMonetisation = readMonetisationData(preferences)
            val balance = currentMonetisation.rewardBalances[RewardType.STREAK_REPAIR] ?: 0
            val repairIsStillCurrent = currentStreak == expectedStreak &&
                currentStreak.latestMissedEpochDay == expectedMissedEpochDay &&
                currentStreak.latestMissedRepairDeadlineEpochMilli == expectedRepairDeadlineEpochMillis &&
                currentLocalEpochDay() == expectedLocalEpochDay &&
                currentTimeZoneId() == expectedTimeZoneId &&
                nowEpochMillis() < expectedRepairDeadlineEpochMillis
            val canPay = currentMonetisation.premiumEnabled || balance > 0

            if (repairIsStillCurrent && canPay) {
                val updatedMonetisation = if (currentMonetisation.premiumEnabled) {
                    currentMonetisation
                } else {
                    currentMonetisation.copy(
                        rewardBalances = currentMonetisation.rewardBalances +
                            (RewardType.STREAK_REPAIR to balance - 1),
                    )
                }
                validate(updatedMonetisation)
                writeStreak(preferences, repairedStreak)
                writeMonetisationData(
                    preferences,
                    updatedMonetisation,
                    currentMonetisation.levelsSinceLastInterstitial.keys,
                )
                committed = true
            }
        }
        return committed
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

    private fun readStreak(preferences: Preferences): StreakData = StreakData(
        currentDays = preferences[STREAK_DAYS] ?: 0,
        lastActivityEpochDay = preferences[LAST_ACTIVITY_EPOCH_DAY],
        freezesAvailable = preferences[FREEZES_AVAILABLE] ?: 0,
        lastFreezeRefillEpochDay = preferences[LAST_FREEZE_REFILL_EPOCH_DAY],
        latestMissedEpochDay = preferences[LATEST_MISSED_EPOCH_DAY],
        latestMissedPreviousStreakDays = preferences[LATEST_MISSED_PREVIOUS_STREAK_DAYS] ?: 0,
        latestMissedPreviousActivityEpochDay = preferences[LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY],
        latestMissedRepairDeadlineEpochMilli = preferences[LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI],
    )

    private fun writeStreak(preferences: MutablePreferences, streak: StreakData) {
        preferences[STREAK_DAYS] = streak.currentDays
        preferences[FREEZES_AVAILABLE] = streak.freezesAvailable
        preferences.setOrRemove(LAST_ACTIVITY_EPOCH_DAY, streak.lastActivityEpochDay)
        preferences.setOrRemove(LAST_FREEZE_REFILL_EPOCH_DAY, streak.lastFreezeRefillEpochDay)
        val missedDay = streak.latestMissedEpochDay
        preferences.setOrRemove(LATEST_MISSED_EPOCH_DAY, missedDay)
        preferences.setOrRemove(
            LATEST_MISSED_PREVIOUS_STREAK_DAYS,
            if (missedDay != null) streak.latestMissedPreviousStreakDays else null,
        )
        preferences.setOrRemove(
            LATEST_MISSED_PREVIOUS_ACTIVITY_EPOCH_DAY,
            if (missedDay != null) streak.latestMissedPreviousActivityEpochDay else null,
        )
        preferences.setOrRemove(
            LATEST_MISSED_REPAIR_DEADLINE_EPOCH_MILLI,
            if (missedDay != null) streak.latestMissedRepairDeadlineEpochMilli else null,
        )
    }

    // MutablePreferences.remove() returns the old value typed as non-null T. Using that value in an
    // expression (for example after ?:) unboxes null and crashes when the key is absent.
    private fun <T : Any> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value != null) {
            this[key] = value
        } else {
            remove(key)
        }
    }

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
        processedContinueTransactionIds = preferences[PROCESSED_CONTINUE_TRANSACTION_IDS] ?: emptySet(),
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
        preferences.setOrRemove(ALLOWANCE_USAGE_DATE, data.allowanceUsage.localDateEpochDay)
        preferences[HINTS_USED_TODAY] = data.allowanceUsage.hintsUsedToday
        preferences[OFFLINE_HINTS_USED_TODAY] = data.allowanceUsage.offlineHintsUsedToday
        preferences[CHARGED_HINT_KEYS] = data.allowanceUsage.chargedHintKeys
        preferences[PROCESSED_REWARD_GRANT_IDS] = data.processedRewardGrantIds
        preferences[PROCESSED_CONTINUE_TRANSACTION_IDS] = data.processedContinueTransactionIds
        (previouslyTrackedGames + data.levelsSinceLastInterstitial.keys).forEach { gameId ->
            val key = interstitialLevelKey(gameId)
            val count = data.levelsSinceLastInterstitial[gameId] ?: 0
            preferences.setOrRemove(key, if (count == 0) null else count)
        }
        preferences.setOrRemove(LAST_INTERSTITIAL_SHOWN_EPOCH_MILLIS, data.lastInterstitialShownEpochMillis)
        preferences[HAS_STARTED_APP_SESSION] = data.hasStartedAppSession
    }

    private fun validate(data: MonetisationData) {
        require(data.rewardBalances.values.all { it >= 0 }) { "Reward balances must not be negative" }
        require(data.processedRewardGrantIds.none(String::isBlank)) { "Reward grant IDs must not be blank" }
        require(data.processedContinueTransactionIds.none(String::isBlank)) {
            "Continue transaction IDs must not be blank"
        }
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
        val THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        val COLOURWAY_ID = androidx.datastore.preferences.core.stringPreferencesKey("colourway_id")
        val CROWNS_SHOW_REGION_AIDS = booleanPreferencesKey("crowns_show_region_aids")
        val CROWNS_AUTO_X_ENABLED = booleanPreferencesKey("crowns_auto_x_enabled")
        val NONOGRAM_AUTO_CROSS_ENABLED = booleanPreferencesKey("nonogram_auto_cross_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
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
        val PROCESSED_CONTINUE_TRANSACTION_IDS =
            androidx.datastore.preferences.core.stringSetPreferencesKey("processed_continue_transaction_ids")
        val LAST_INTERSTITIAL_SHOWN_EPOCH_MILLIS = longPreferencesKey("last_interstitial_shown_epoch_millis")
        val HAS_STARTED_APP_SESSION = booleanPreferencesKey("has_started_app_session")
        val INTERSTITIAL_GAME_IDS = listOf("crowns", "binary", "nonogram", "minisudoku")
    }
}
