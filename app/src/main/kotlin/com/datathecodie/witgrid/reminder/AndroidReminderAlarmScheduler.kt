package com.datathecodie.witgrid.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.datathecodie.witgrid.data.settings.ReminderSettings

class AndroidReminderAlarmScheduler(
    context: Context,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : ReminderAlarmScheduler {
    private val applicationContext = context.applicationContext

    override fun schedule(settings: ReminderSettings) {
        if (!settings.enabled) {
            cancel()
            return
        }

        val alarmManager = requireNotNull(applicationContext.getSystemService(AlarmManager::class.java))
        val triggerAtMillis = ReminderSchedulePolicy.nextTriggerAtMillis(
            nowEpochMillis = nowEpochMillis(),
            hourOfDay = settings.hourOfDay,
            minute = settings.minute,
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            reminderPendingIntent(),
        )
    }

    override fun cancel() {
        val pendingIntent = reminderPendingIntent()
        applicationContext.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun reminderPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        REMINDER_REQUEST_CODE,
        Intent(applicationContext, ReminderReceiver::class.java).setAction(ACTION_DAILY_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REMINDER_REQUEST_CODE = 4107
    }
}
