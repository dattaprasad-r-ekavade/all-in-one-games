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
        PlayerData(
            settings = AppSettings(
                soundEnabled = preferences[SOUND_ENABLED] ?: true,
                hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
                mistakeLimitEnabled = preferences[MISTAKE_LIMIT_ENABLED] ?: false,
            ),
            streak = StreakData(
                currentDays = preferences[STREAK_DAYS] ?: 0,
                lastActivityEpochDay = preferences[LAST_ACTIVITY_EPOCH_DAY],
                freezesAvailable = preferences[FREEZES_AVAILABLE] ?: 0,
            ),
            premiumEnabled = preferences[PREMIUM_ENABLED] ?: false,
            rewardBalances = RewardType.entries.associateWith { type ->
                preferences[rewardBalanceKey(type)] ?: 0
            },
        )
    }

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = settings.soundEnabled
            preferences[HAPTICS_ENABLED] = settings.hapticsEnabled
            preferences[MISTAKE_LIMIT_ENABLED] = settings.mistakeLimitEnabled
        }
    }

    override suspend fun updateStreak(streak: StreakData) {
        dataStore.edit { preferences ->
            preferences[STREAK_DAYS] = streak.currentDays
            preferences[FREEZES_AVAILABLE] = streak.freezesAvailable
            streak.lastActivityEpochDay?.let { day ->
                preferences[LAST_ACTIVITY_EPOCH_DAY] = day
            } ?: preferences.remove(LAST_ACTIVITY_EPOCH_DAY)
        }
    }

    override suspend fun setPremiumEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PREMIUM_ENABLED] = enabled }
    }

    override suspend fun setRewardBalance(type: RewardType, balance: Int) {
        dataStore.edit { preferences -> preferences[rewardBalanceKey(type)] = balance }
    }

    private fun rewardBalanceKey(type: RewardType) = intPreferencesKey("reward_${type.storageKey}")

    private companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val MISTAKE_LIMIT_ENABLED = booleanPreferencesKey("mistake_limit_enabled")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_ACTIVITY_EPOCH_DAY = longPreferencesKey("last_activity_epoch_day")
        val FREEZES_AVAILABLE = intPreferencesKey("freezes_available")
        val PREMIUM_ENABLED = booleanPreferencesKey("premium_enabled")
    }
}
