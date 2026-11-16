package com.datathecodie.witgrid.domain.daily

import com.datathecodie.witgrid.data.settings.StreakData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyStreakPolicyTest {
    @Test
    fun completingMultipleDailyPuzzlesOnOneLocalDateOnlyCountsOnce() {
        val date = LocalDate.of(2025, 6, 2)
        val policy = policyAt(date)

        val first = policy.completeDailyPuzzle(StreakData())
        val second = policy.completeDailyPuzzle(first.streak)

        assertEquals(1, first.streak.currentDays)
        assertEquals(first.streak, second.streak)
        assertFalse(second.freezeUsed)
    }

    @Test
    fun consecutiveCompletionsBuildASevenDayOfflineStreak() {
        val startDate = LocalDate.of(2025, 6, 2)
        var streak = StreakData()

        (0L..6L).forEach { offset ->
            streak = policyAt(startDate.plusDays(offset)).completeDailyPuzzle(streak).streak
        }

        assertEquals(7, streak.currentDays)
        assertEquals(startDate.plusDays(6).toEpochDay(), streak.lastActivityEpochDay)
    }

    @Test
    fun freeFreezeCoversOneMissedLocalDay() {
        val monday = LocalDate.of(2025, 6, 2)
        val first = policyAt(monday).completeDailyPuzzle(StreakData()).streak

        val afterMissedTuesday = policyAt(monday.plusDays(2)).completeDailyPuzzle(first)

        assertEquals(3, afterMissedTuesday.streak.currentDays)
        assertEquals(0, afterMissedTuesday.streak.freezesAvailable)
        assertTrue(afterMissedTuesday.freezeUsed)
        assertNull(afterMissedTuesday.streak.latestMissedEpochDay)
    }

    @Test
    fun oneFreezeCannotProtectAStreakAcrossTwoMissedDays() {
        val monday = LocalDate.of(2025, 6, 2)
        val first = policyAt(monday).completeDailyPuzzle(StreakData()).streak

        val afterTwoMissedDays = policyAt(monday.plusDays(3)).completeDailyPuzzle(first)

        assertEquals(1, afterTwoMissedDays.streak.currentDays)
        assertEquals(0, afterTwoMissedDays.streak.freezesAvailable)
        assertTrue(afterTwoMissedDays.freezeUsed)
        assertEquals(monday.plusDays(2).toEpochDay(), afterTwoMissedDays.streak.latestMissedEpochDay)
    }

    @Test
    fun missedDayWithoutFreezeBreaksStreakAndCanBeRepaired() {
        val monday = LocalDate.of(2025, 6, 2)
        val first = policyAt(monday).completeDailyPuzzle(StreakData()).streak.copy(freezesAvailable = 0)
        val onWednesday = policyAt(monday.plusDays(2))

        val afterMiss = onWednesday.completeDailyPuzzle(first).streak
        val repairableMiss = onWednesday.latestRepairableMiss(afterMiss)
        val repaired = onWednesday.repairLatestMiss(afterMiss)

        assertEquals(1, afterMiss.currentDays)
        assertEquals(monday.plusDays(1).toEpochDay(), afterMiss.latestMissedEpochDay)
        assertEquals(monday.plusDays(1).toEpochDay(), repairableMiss?.dateEpochDay)
        assertNotNull(repaired)
        assertEquals(3, repaired?.streak?.currentDays)
        assertEquals(monday.plusDays(2).toEpochDay(), repaired?.streak?.lastActivityEpochDay)
        assertNull(repaired?.streak?.latestMissedEpochDay)
    }

    @Test
    fun repairAfterLaterCompletionsRestoresTheConnectedStreak() {
        val monday = LocalDate.of(2025, 6, 2)
        val first = StreakData(
            currentDays = 5,
            lastActivityEpochDay = monday.toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = monday.toEpochDay(),
        )
        var streak = policyAt(monday.plusDays(2)).completeDailyPuzzle(first).streak
        streak = policyAt(monday.plusDays(3)).completeDailyPuzzle(streak).streak
        val onThursday = policyAt(monday.plusDays(3))

        val repaired = onThursday.repairLatestMiss(streak)

        assertNotNull(repaired)
        assertEquals(8, repaired?.streak?.currentDays)
        assertEquals(monday.plusDays(3).toEpochDay(), repaired?.streak?.lastActivityEpochDay)
    }

    @Test
    fun reconcileAppliesFreezeWithoutAwardingTodaysPuzzle() {
        val monday = LocalDate.of(2025, 6, 2)
        val first = policyAt(monday).completeDailyPuzzle(StreakData()).streak

        val reconciled = policyAt(monday.plusDays(2)).reconcile(first)

        assertEquals(2, reconciled.streak.currentDays)
        assertEquals(monday.plusDays(1).toEpochDay(), reconciled.streak.lastActivityEpochDay)
        assertEquals(0, reconciled.streak.freezesAvailable)
        assertTrue(reconciled.freezeUsed)
        assertNull(policyAt(monday.plusDays(2)).latestRepairableMiss(reconciled.streak))
    }

    @Test
    fun reconcileMarksUnprotectedMissWithoutAwardingToday() {
        val monday = LocalDate.of(2025, 6, 2)
        val first = policyAt(monday).completeDailyPuzzle(StreakData()).streak.copy(freezesAvailable = 0)

        val reconciled = policyAt(monday.plusDays(2)).reconcile(first)

        assertEquals(0, reconciled.streak.currentDays)
        assertEquals(monday.toEpochDay(), reconciled.streak.lastActivityEpochDay)
        assertEquals(monday.plusDays(1).toEpochDay(), reconciled.streak.latestMissedEpochDay)
        assertEquals(
            monday.plusDays(1).toEpochDay(),
            policyAt(monday.plusDays(2)).latestRepairableMiss(reconciled.streak)?.dateEpochDay,
        )
    }

    @Test
    fun mondayRefillRunsBeforeAFreezeCoversTheEarliestDateInALongerGap() {
        val previousMonday = LocalDate.of(2025, 6, 2)
        val monday = previousMonday.plusWeeks(1)
        val streak = StreakData(
            currentDays = 5,
            lastActivityEpochDay = monday.minusDays(3).toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = previousMonday.toEpochDay(),
        )

        val reconciled = policyAt(monday).reconcile(streak)

        assertTrue(reconciled.freezeUsed)
        assertEquals(0, reconciled.streak.currentDays)
        assertEquals(monday.minusDays(2).toEpochDay(), reconciled.streak.lastActivityEpochDay)
        assertEquals(monday.minusDays(1).toEpochDay(), reconciled.streak.latestMissedEpochDay)
        assertEquals(0, reconciled.streak.freezesAvailable)
        assertEquals(monday.toEpochDay(), reconciled.streak.lastFreezeRefillEpochDay)
    }

    @Test
    fun repairAfterALongerGapStartsANewStreakInsteadOfReconnectingTheOldOne() {
        val previousMonday = LocalDate.of(2025, 6, 2)
        val monday = previousMonday.plusWeeks(1)
        val streak = StreakData(
            currentDays = 8,
            lastActivityEpochDay = monday.minusDays(4).toEpochDay(),
            freezesAvailable = 1,
            lastFreezeRefillEpochDay = previousMonday.toEpochDay(),
        )
        val policy = policyAt(monday)
        val reconciled = policy.reconcile(streak).streak

        val repaired = policy.repairLatestMiss(reconciled)

        assertNotNull(repaired)
        assertEquals(1, repaired?.streak?.currentDays)
        assertEquals(monday.minusDays(1).toEpochDay(), repaired?.streak?.lastActivityEpochDay)
    }

    @Test
    fun repairAfterMultipleMissesExtendsTheLaterConsecutiveRun() {
        val monday = LocalDate.of(2025, 6, 2)
        val streak = StreakData(
            currentDays = 5,
            lastActivityEpochDay = monday.toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = monday.toEpochDay(),
        )
        var current = policyAt(monday.plusDays(3)).completeDailyPuzzle(streak).streak
        current = policyAt(monday.plusDays(4)).completeDailyPuzzle(current).streak
        val onFriday = policyAt(monday.plusDays(4))

        val repaired = onFriday.repairLatestMiss(current)

        assertNotNull(repaired)
        assertEquals(3, repaired?.streak?.currentDays)
        assertEquals(monday.plusDays(4).toEpochDay(), repaired?.streak?.lastActivityEpochDay)
    }

    @Test
    fun multipleMissesWithoutFreezeBreakTheStreakAndKeepOnlyTheLatestRepairTarget() {
        val monday = LocalDate.of(2025, 6, 2)
        val streak = StreakData(
            currentDays = 5,
            lastActivityEpochDay = monday.toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = monday.toEpochDay(),
        )
        val thursday = policyAt(monday.plusDays(3))

        val reconciled = thursday.reconcile(streak)

        assertEquals(0, reconciled.streak.currentDays)
        assertEquals(monday.toEpochDay(), reconciled.streak.lastActivityEpochDay)
        assertEquals(monday.plusDays(2).toEpochDay(), reconciled.streak.latestMissedEpochDay)
        assertEquals(monday.plusDays(2).toEpochDay(), thursday.latestRepairableMiss(reconciled.streak)?.dateEpochDay)
    }

    @Test
    fun mondayRefillsFromZeroAndDoesNotStackWhenAlreadyHoldingOne() {
        val previousMonday = LocalDate.of(2025, 6, 2)
        val monday = previousMonday.plusWeeks(1)
        val noFreeze = StreakData(
            currentDays = 5,
            lastActivityEpochDay = monday.minusDays(1).toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = previousMonday.toEpochDay(),
        )
        val holdingOne = noFreeze.copy(freezesAvailable = 1)

        val refilled = policyAt(monday).reconcile(noFreeze).streak
        val notStacked = policyAt(monday).reconcile(holdingOne).streak

        assertEquals(1, refilled.freezesAvailable)
        assertEquals(monday.toEpochDay(), refilled.lastFreezeRefillEpochDay)
        assertEquals(1, notStacked.freezesAvailable)
    }

    @Test
    fun repairExpiresAt48HoursAfterTheMissedLocalDayEnds() {
        val missedDate = LocalDate.of(2025, 6, 3)
        val deadline = missedDate.plusDays(1).atStartOfDay(UTC).toInstant().plusSeconds(48 * 60 * 60L)
        val streak = StreakData(
            currentDays = 1,
            lastActivityEpochDay = deadline.atZone(UTC).toLocalDate().toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = LocalDate.of(2025, 6, 2).toEpochDay(),
            latestMissedEpochDay = missedDate.toEpochDay(),
            latestMissedPreviousStreakDays = 1,
            latestMissedPreviousActivityEpochDay = missedDate.minusDays(1).toEpochDay(),
            latestMissedRepairDeadlineEpochMilli = deadline.toEpochMilli(),
        )

        assertNotNull(policyAt(deadline.minusNanos(1)).latestRepairableMiss(streak))
        assertNull(policyAt(deadline).latestRepairableMiss(streak))
    }

    @Test
    fun mondayRefillsToOneWithoutAccumulatingUnusedFreezes() {
        val previousMonday = LocalDate.of(2025, 6, 2)
        val monday = previousMonday.plusWeeks(1)
        val streak = StreakData(
            currentDays = 7,
            lastActivityEpochDay = monday.minusDays(1).toEpochDay(),
            freezesAvailable = 1,
            lastFreezeRefillEpochDay = previousMonday.toEpochDay(),
        )

        val updated = policyAt(monday).completeDailyPuzzle(streak).streak

        assertEquals(1, updated.freezesAvailable)
        assertEquals(monday.toEpochDay(), updated.lastFreezeRefillEpochDay)
    }

    @Test
    fun sameInstantAfterTimeZoneChangeUsesLocalDateWithoutRewindingStreak() {
        val instant = Instant.parse("2025-05-02T01:00:00Z")
        var zoneId = UTC
        val policy = DailyStreakPolicy(DailyClock { instant.toEpochMilli() }) {
            TimeZone.getTimeZone(zoneId)
        }
        val utc = policy.completeDailyPuzzle(StreakData()).streak
        zoneId = ZoneId.of("America/Los_Angeles")
        val afterZoneChange = policy.completeDailyPuzzle(utc).streak

        assertEquals(LocalDate.of(2025, 5, 2).toEpochDay(), utc.lastActivityEpochDay)
        assertEquals(utc, afterZoneChange)
        assertTrue(
            policy.dailyPuzzleIndex() != DailyPuzzleIndex.forLocalEpochDay(LocalDate.of(2025, 5, 2).toEpochDay()),
        )
    }

    @Test
    fun backwardClockDoesNotRewindOrDuplicateAStreakDay() {
        val lastActivity = LocalDate.of(2025, 6, 10)
        val streak = StreakData(
            currentDays = 6,
            lastActivityEpochDay = lastActivity.toEpochDay(),
            freezesAvailable = 0,
            lastFreezeRefillEpochDay = LocalDate.of(2025, 6, 9).toEpochDay(),
        )

        val updated = policyAt(lastActivity.minusDays(1)).completeDailyPuzzle(streak).streak

        assertEquals(streak, updated)
    }

    private fun policyAt(date: LocalDate, zoneId: ZoneId = UTC): DailyStreakPolicy =
        policyAt(date.atTime(12, 0).atZone(zoneId).toInstant(), zoneId)

    private fun policyAt(instant: Instant, zoneId: ZoneId = UTC): DailyStreakPolicy =
        DailyStreakPolicy(
            clock = DailyClock { instant.toEpochMilli() },
            timeZone = TimeZone.getTimeZone(zoneId),
        )

    private companion object {
        val UTC: ZoneId = ZoneId.of("UTC")
    }
}
