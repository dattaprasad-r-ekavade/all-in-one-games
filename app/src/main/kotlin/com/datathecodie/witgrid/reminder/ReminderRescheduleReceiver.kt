package com.datathecodie.witgrid.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.datathecodie.witgrid.data.settings.DataStorePlayerDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in RESCHEDULE_ACTIONS) return
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = DataStorePlayerDataRepository(applicationContext).playerData.first().reminder
                val scheduler = AndroidReminderAlarmScheduler(applicationContext)
                if (settings.enabled) scheduler.schedule(settings) else scheduler.cancel()
            } catch (exception: Exception) {
                Log.e(TAG, "Could not restore the daily reminder", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
        const val TAG = "ReminderReschedule"
    }
}
