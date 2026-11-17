package com.datathecodie.witgrid.reminder

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulePolicyTest {
    @Test
    fun schedulesAtTheSelectedTimeWhenItIsStillAheadToday() {
        val timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val now = localTime(timeZone, 2026, Calendar.MARCH, 10, 8, 59)

        val next = ReminderSchedulePolicy.nextTriggerAtMillis(now, 9, 15, timeZone)

        assertEquals(localTime(timeZone, 2026, Calendar.MARCH, 10, 9, 15), next)
    }

    @Test
    fun schedulesForTomorrowWhenSelectedTimeHasPassed() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val now = localTime(timeZone, 2026, Calendar.JUNE, 4, 20, 0)

        val next = ReminderSchedulePolicy.nextTriggerAtMillis(now, 9, 0, timeZone)

        assertEquals(localTime(timeZone, 2026, Calendar.JUNE, 5, 9, 0), next)
    }

    @Test
    fun schedulesTomorrowWhenCurrentTimeEqualsSelectedTime() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val now = localTime(timeZone, 2026, Calendar.JUNE, 4, 9, 0)

        val next = ReminderSchedulePolicy.nextTriggerAtMillis(now, 9, 0, timeZone)

        assertEquals(localTime(timeZone, 2026, Calendar.JUNE, 5, 9, 0), next)
    }

    @Test
    fun restoresSelectedTimeForTheDayAfterASpringForwardGap() {
        val timeZone = TimeZone.getTimeZone("America/New_York")
        val beforeSpringForward = localTime(timeZone, 2026, Calendar.MARCH, 7, 23, 0)
        val springForwardOccurrence = ReminderSchedulePolicy.nextTriggerAtMillis(
            beforeSpringForward,
            2,
            30,
            timeZone,
        )

        assertEquals(localTime(timeZone, 2026, Calendar.MARCH, 8, 3, 30), springForwardOccurrence)

        val followingOccurrence = ReminderSchedulePolicy.nextTriggerAtMillis(
            springForwardOccurrence,
            2,
            30,
            timeZone,
        )

        assertEquals(localTime(timeZone, 2026, Calendar.MARCH, 9, 2, 30), followingOccurrence)
    }

    private fun localTime(
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance(timeZone).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis
}
