package com.datathecodie.witgrid.domain.daily

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyLocalDateTest {
    @Test
    fun repairDeadlineUsesTheFirstOccurrenceOfAnOverlappingMidnight() {
        val zoneId = ZoneId.of("America/Havana")
        val nextDateStart = LocalDate.of(2025, 11, 2).atStartOfDay(zoneId).toInstant()
        val expectedDayEnd = Instant.parse("2025-11-02T04:00:00Z")
        val missedDate = LocalDate.of(2025, 11, 1)

        assertEquals(expectedDayEnd, nextDateStart)
        assertEquals(
            expectedDayEnd.toEpochMilli() + 48L * 60L * 60L * 1000L,
            DailyLocalDate.repairDeadlineEpochMillis(
                missedDate.toEpochDay(),
                TimeZone.getTimeZone(zoneId),
            ),
        )
    }
}
