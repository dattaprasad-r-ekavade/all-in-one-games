package com.datathecodie.witgrid.reminder

import java.util.Calendar
import java.util.TimeZone

internal object ReminderSchedulePolicy {
    fun nextTriggerAtMillis(
        nowEpochMillis: Long,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long {
        require(hourOfDay in 0..23) { "Reminder hour must be between 0 and 23" }
        require(minute in 0..59) { "Reminder minute must be between 0 and 59" }

        val trigger = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (trigger.timeInMillis <= nowEpochMillis) {
            trigger.add(Calendar.DAY_OF_MONTH, 1)
            trigger.set(Calendar.HOUR_OF_DAY, hourOfDay)
            trigger.set(Calendar.MINUTE, minute)
            trigger.set(Calendar.SECOND, 0)
            trigger.set(Calendar.MILLISECOND, 0)
        }
        return trigger.timeInMillis
    }
}
