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

internal const val ACTION_DAILY_REMINDER = "com.datathecodie.witgrid.action.DAILY_REMINDER"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != REMINDER_ACTION) return
        val pendingResult = goAsync()
        val applicationContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = DataStorePlayerDataRepository(applicationContext).playerData.first().reminder
                if (!settings.enabled) return@launch

                AndroidReminderAlarmScheduler(applicationContext).schedule(settings)
                ReminderNotification.show(applicationContext)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not deliver the daily reminder", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val REMINDER_ACTION = ACTION_DAILY_REMINDER
        const val TAG = "ReminderReceiver"
    }
}
