package com.datathecodie.witgrid.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.datathecodie.witgrid.data.settings.PlayerDataRepository
import com.datathecodie.witgrid.data.settings.ReminderSettings
import kotlinx.coroutines.flow.first

sealed interface ReminderEnableResult {
    data object Enabled : ReminderEnableResult

    data object NotificationPermissionRequired : ReminderEnableResult
}

fun interface ReminderPermissionStatus {
    fun isGranted(): Boolean
}

interface ReminderAlarmScheduler {
    fun schedule(settings: ReminderSettings)

    fun cancel()
}

class AndroidReminderPermissionStatus(context: Context) : ReminderPermissionStatus {
    private val applicationContext = context.applicationContext

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

class ReminderController(
    private val playerDataRepository: PlayerDataRepository,
    private val permissionStatus: ReminderPermissionStatus,
    private val alarmScheduler: ReminderAlarmScheduler,
) {
    /** The caller should request permission only after the player chooses to enable reminders. */
    suspend fun enable(settings: ReminderSettings): ReminderEnableResult {
        require(settings.enabled) { "Enabled reminder settings are required" }
        if (!permissionStatus.isGranted()) return ReminderEnableResult.NotificationPermissionRequired

        playerDataRepository.updateReminderSettings(settings)
        try {
            alarmScheduler.schedule(settings)
        } catch (exception: Exception) {
            alarmScheduler.cancel()
            playerDataRepository.updateReminderSettings(settings.copy(enabled = false))
            throw exception
        }
        return ReminderEnableResult.Enabled
    }

    suspend fun updateTime(hourOfDay: Int, minute: Int) {
        val current = playerDataRepository.playerData.first().reminder
        val updated = ReminderSettings(
            enabled = current.enabled,
            hourOfDay = hourOfDay,
            minute = minute,
        )
        playerDataRepository.updateReminderSettings(updated)
        try {
            if (updated.enabled) alarmScheduler.schedule(updated)
        } catch (exception: Exception) {
            playerDataRepository.updateReminderSettings(current)
            if (current.enabled) alarmScheduler.schedule(current) else alarmScheduler.cancel()
            throw exception
        }
    }

    suspend fun disable() {
        val current = playerDataRepository.playerData.first().reminder
        playerDataRepository.updateReminderSettings(current.copy(enabled = false))
        alarmScheduler.cancel()
    }
}
