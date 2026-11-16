package com.datathecodie.witgrid.domain.daily

import java.util.TimeZone

/** Assigns one of the 365 daily bank entries to a local calendar date. */
object DailyPuzzleIndex {
    const val DAYS_PER_CYCLE = 365

    fun forLocalEpochDay(epochDay: Long): Int = Math.floorMod(epochDay, DAYS_PER_CYCLE.toLong()).toInt()

    fun today(nowEpochMillis: Long, timeZone: TimeZone): Int =
        forLocalEpochDay(DailyLocalDate.epochDayAt(nowEpochMillis, timeZone))
}
