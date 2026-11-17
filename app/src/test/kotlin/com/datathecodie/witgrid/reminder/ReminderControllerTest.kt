package com.datathecodie.witgrid.reminder

import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.ReminderSettings
import com.datathecodie.witgrid.data.settings.RewardType
import com.datathecodie.witgrid.data.settings.StreakData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderControllerTest {
    @Test
    fun reminderStartsDisabledByDefault() = runBlocking {
        val repository = FakePlayerDataRepository()

        assertFalse(repository.playerData.first().reminder.enabled)
    }

    @Test
    fun enablingWithoutNotificationPermissionDoesNotSaveOrSchedule() = runBlocking {
        val repository = FakePlayerDataRepository()
        val scheduler = FakeReminderAlarmScheduler()
        val controller = ReminderController(repository, ReminderPermissionStatus { false }, scheduler)

        val result = controller.enable(ReminderSettings(enabled = true, hourOfDay = 18, minute = 30))

        assertEquals(ReminderEnableResult.NotificationPermissionRequired, result)
        assertEquals(ReminderSettings(), repository.playerData.first().reminder)
        assertNull(scheduler.scheduledSettings)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun enablingWithPermissionPersistsAndSchedulesTheSelectedLocalTime() = runBlocking {
        val repository = FakePlayerDataRepository()
        val scheduler = FakeReminderAlarmScheduler()
        val controller = ReminderController(repository, ReminderPermissionStatus { true }, scheduler)
        val settings = ReminderSettings(enabled = true, hourOfDay = 18, minute = 30)

        val result = controller.enable(settings)

        assertEquals(ReminderEnableResult.Enabled, result)
        assertEquals(settings, repository.playerData.first().reminder)
        assertEquals(settings, scheduler.scheduledSettings)
    }

    @Test
    fun changingTimeWhileDisabledPersistsWithoutScheduling() = runBlocking {
        val repository = FakePlayerDataRepository()
        val scheduler = FakeReminderAlarmScheduler()
        val controller = ReminderController(repository, ReminderPermissionStatus { true }, scheduler)

        controller.updateTime(hourOfDay = 21, minute = 45)

        assertEquals(ReminderSettings(hourOfDay = 21, minute = 45), repository.playerData.first().reminder)
        assertNull(scheduler.scheduledSettings)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun changingTimeWhileEnabledReschedulesTheReminder() = runBlocking {
        val initial = PlayerData(reminder = ReminderSettings(enabled = true, hourOfDay = 7, minute = 15))
        val repository = FakePlayerDataRepository(initial)
        val scheduler = FakeReminderAlarmScheduler()
        val controller = ReminderController(repository, ReminderPermissionStatus { true }, scheduler)

        controller.updateTime(hourOfDay = 8, minute = 45)

        val expected = ReminderSettings(enabled = true, hourOfDay = 8, minute = 45)
        assertEquals(expected, repository.playerData.first().reminder)
        assertEquals(expected, scheduler.scheduledSettings)
    }

    @Test
    fun disablingPersistsAndCancelsTheAlarm() = runBlocking {
        val initial = PlayerData(reminder = ReminderSettings(enabled = true, hourOfDay = 7, minute = 15))
        val repository = FakePlayerDataRepository(initial)
        val scheduler = FakeReminderAlarmScheduler()
        val controller = ReminderController(repository, ReminderPermissionStatus { true }, scheduler)

        controller.disable()

        assertEquals(ReminderSettings(hourOfDay = 7, minute = 15), repository.playerData.first().reminder)
        assertEquals(1, scheduler.cancelCount)
    }

    private class FakeReminderAlarmScheduler : ReminderAlarmScheduler {
        var scheduledSettings: ReminderSettings? = null
        var cancelCount = 0

        override fun schedule(settings: ReminderSettings) {
            scheduledSettings = settings
        }

        override fun cancel() {
            cancelCount += 1
            scheduledSettings = null
        }
    }

    private class FakePlayerDataRepository(
        initial: PlayerData = PlayerData(),
    ) : PlayerDataRepository {
        private val state = MutableStateFlow(initial)

        override val playerData: Flow<PlayerData> = state

        override suspend fun updateSettings(settings: AppSettings) {
            state.value = state.value.copy(settings = settings)
        }

        override suspend fun updateReminderSettings(settings: ReminderSettings) {
            state.value = state.value.copy(reminder = settings)
        }

        override suspend fun updateStreak(streak: StreakData) {
            state.value = state.value.copy(streak = streak)
        }

        override suspend fun setPremiumEnabled(enabled: Boolean) {
            state.value = state.value.copy(premiumEnabled = enabled)
        }

        override suspend fun setRewardBalance(type: RewardType, balance: Int) {
            val balances = state.value.rewardBalances + (type to balance)
            state.value = state.value.copy(rewardBalances = balances)
        }

    }
}
