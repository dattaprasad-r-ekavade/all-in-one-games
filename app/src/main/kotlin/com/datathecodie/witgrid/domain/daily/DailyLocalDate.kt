package com.datathecodie.witgrid.domain.daily

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

internal object DailyLocalDate {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun epochDayAt(nowEpochMillis: Long, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            timeInMillis = nowEpochMillis
        }
        return epochDay(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun mondayEpochDayAt(nowEpochMillis: Long, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            timeInMillis = nowEpochMillis
        }
        val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return epochDayAt(nowEpochMillis, timeZone) - daysSinceMonday
    }

    fun repairDeadlineEpochMillis(missedEpochDay: Long, timeZone: TimeZone): Long {
        val nextLocalDayStart = firstInstantOnOrAfterLocalDate(missedEpochDay + 1, timeZone)
        return nextLocalDayStart + REPAIR_WINDOW_MILLIS
    }

    private fun firstInstantOnOrAfterLocalDate(targetEpochDay: Long, timeZone: TimeZone): Long {
        val approximateUtcStart = targetEpochDay * MILLIS_PER_DAY
        var before = approximateUtcStart - THREE_DAYS_MILLIS
        var after = approximateUtcStart + THREE_DAYS_MILLIS
        while (epochDayAt(before, timeZone) >= targetEpochDay) before -= THREE_DAYS_MILLIS
        while (epochDayAt(after, timeZone) < targetEpochDay) after += THREE_DAYS_MILLIS

        while (after - before > 1L) {
            val middle = before + (after - before) / 2L
            if (epochDayAt(middle, timeZone) >= targetEpochDay) {
                after = middle
            } else {
                before = middle
            }
        }
        return after
    }

    private fun epochDay(year: Int, month: Int, dayOfMonth: Int): Long {
        val calendar = GregorianCalendar(UTC, Locale.ROOT).apply {
            clear()
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Math.floorDiv(calendar.timeInMillis, MILLIS_PER_DAY)
    }

    private fun utcCalendarAtEpochDay(epochDay: Long): Calendar =
        GregorianCalendar(UTC, Locale.ROOT).apply {
            clear()
            timeInMillis = epochDay * MILLIS_PER_DAY
        }

    private const val REPAIR_WINDOW_MILLIS = 48L * 60L * 60L * 1000L
    private const val THREE_DAYS_MILLIS = 3L * MILLIS_PER_DAY
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
}
