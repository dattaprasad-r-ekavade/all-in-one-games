package com.datathecodie.witgrid.domain.daily

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyPuzzleIndexTest {
    @Test
    fun cyclesEvery365LocalDatesFromTheUnixEpoch() {
        assertEquals(0, DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(1970, 1, 1).toEpochDay()))
        assertEquals(364, DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(1970, 12, 31).toEpochDay()))
        assertEquals(0, DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(1971, 1, 1).toEpochDay()))
        assertEquals(364, DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(1969, 12, 31).toEpochDay()))
    }

    @Test
    fun changesAtLocalMidnightAndUsesTheInjectedZone() {
        val beforeMidnight = Instant.parse("2025-05-02T06:59:59Z").toEpochMilli()
        val afterMidnight = Instant.parse("2025-05-02T07:00:00Z").toEpochMilli()
        val zone = ZoneId.of("America/Los_Angeles")

        assertEquals(
            DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(2025, 5, 1).toEpochDay()),
            DailyPuzzleIndex.today(beforeMidnight, TimeZone.getTimeZone(zone)),
        )
        assertEquals(
            DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(2025, 5, 2).toEpochDay()),
            DailyPuzzleIndex.today(afterMidnight, TimeZone.getTimeZone(zone)),
        )
    }

    @Test
    fun sameInstantCanHaveDifferentDailyIndexInDifferentZones() {
        val instant = Instant.parse("2025-05-02T01:00:00Z").toEpochMilli()

        assertEquals(
            DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(2025, 5, 2).toEpochDay()),
            DailyPuzzleIndex.today(instant, TimeZone.getTimeZone("UTC")),
        )
        assertEquals(
            DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(2025, 5, 1).toEpochDay()),
            DailyPuzzleIndex.today(instant, TimeZone.getTimeZone("America/Los_Angeles")),
        )
    }
}
