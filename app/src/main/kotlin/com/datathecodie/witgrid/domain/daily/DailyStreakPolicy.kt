package com.datathecodie.witgrid.domain.daily

import com.datathecodie.witgrid.data.settings.StreakData
import java.util.TimeZone

fun interface DailyClock {
    fun nowEpochMillis(): Long
}

data class StreakTransition(
    val streak: StreakData,
    val freezeUsed: Boolean = false,
)

data class RepairableStreakMiss(
    val dateEpochDay: Long,
    val deadlineEpochMillis: Long,
)

/** Applies local-date streak, weekly freeze and rewarded repair rules. */
class DailyStreakPolicy(
    private val clock: DailyClock,
    private val timeZoneProvider: () -> TimeZone,
) {
    constructor(clock: DailyClock, timeZone: TimeZone) : this(clock, { timeZone })

    fun dailyPuzzleIndex(): Int {
        val now = clock.nowEpochMillis()
        return DailyPuzzleIndex.today(now, currentTimeZone())
    }

    /** Refill the weekly freeze and apply it to missed dates without counting today's puzzle. */
    fun reconcile(current: StreakData): StreakTransition {
        val now = clock.nowEpochMillis()
        val timeZone = currentTimeZone()
        return reconcile(current, now, DailyLocalDate.epochDayAt(now, timeZone), timeZone)
    }

    /** Record one of today's daily puzzles. Repeated calls for the same local date are idempotent. */
    fun completeDailyPuzzle(current: StreakData): StreakTransition {
        val now = clock.nowEpochMillis()
        val timeZone = currentTimeZone()
        val todayEpochDay = DailyLocalDate.epochDayAt(now, timeZone)
        val reconciled = reconcile(current, now, todayEpochDay, timeZone)
        val streak = reconciled.streak
        val lastActivity = streak.lastActivityEpochDay

        val updated = when {
            lastActivity == null -> streak.withoutRepairableMiss().copy(
                currentDays = 1,
                lastActivityEpochDay = todayEpochDay,
            )

            todayEpochDay <= lastActivity -> streak

            todayEpochDay == lastActivity + 1 -> streak.withExpiredRepairableMiss(now).copy(
                currentDays = maxOf(1, streak.currentDays + 1),
                lastActivityEpochDay = todayEpochDay,
            )

            else -> streak.withExpiredRepairableMiss(now).copy(
                currentDays = 1,
                lastActivityEpochDay = todayEpochDay,
            )
        }
        return StreakTransition(updated, reconciled.freezeUsed)
    }

    /**
     * Returns the latest eligible miss only when the caller has reconciled and persisted current
     * date effects. This prevents showing a rewarded repair when a free freeze should apply.
     */
    fun latestRepairableMiss(current: StreakData): RepairableStreakMiss? {
        val now = clock.nowEpochMillis()
        val timeZone = currentTimeZone()
        val todayEpochDay = DailyLocalDate.epochDayAt(now, timeZone)
        val reconciled = reconcile(current, now, todayEpochDay, timeZone).streak
        if (reconciled != current) return null
        return findRepairableMiss(current, now, timeZone)
    }

    /** Restores the latest eligible local date. Reconcile and persist before offering the repair. */
    fun repairLatestMiss(current: StreakData): StreakTransition? {
        val now = clock.nowEpochMillis()
        val timeZone = currentTimeZone()
        val todayEpochDay = DailyLocalDate.epochDayAt(now, timeZone)
        val reconciled = reconcile(current, now, todayEpochDay, timeZone).streak
        if (reconciled != current) return null
        val miss = findRepairableMiss(current, now, timeZone) ?: return null
        val missedEpochDay = miss.dateEpochDay
        val lastActivity = current.lastActivityEpochDay
        val previousActivity = if (current.latestMissedEpochDay == missedEpochDay) {
            current.latestMissedPreviousActivityEpochDay
        } else {
            lastActivity?.takeIf { it == missedEpochDay - 1 }
        }
        val previousStreakDays = if (current.latestMissedEpochDay == missedEpochDay) {
            current.latestMissedPreviousStreakDays
        } else if (previousActivity != null) {
            current.currentDays
        } else {
            0
        }
        val updatedDays = when {
            lastActivity == null -> 1
            previousActivity == missedEpochDay - 1 && lastActivity >= missedEpochDay ->
                previousStreakDays + (lastActivity - previousActivity).toInt()
            lastActivity == missedEpochDay - 1 -> maxOf(1, previousStreakDays + 1)
            lastActivity > missedEpochDay -> current.currentDays + 1
            lastActivity < missedEpochDay - 1 -> 1
            else -> current.currentDays
        }

        return StreakTransition(
            streak = current.withoutRepairableMiss().copy(
                currentDays = updatedDays,
                lastActivityEpochDay = maxOf(lastActivity ?: missedEpochDay, missedEpochDay),
            ),
        )
    }

    private fun reconcile(
        current: StreakData,
        nowEpochMillis: Long,
        todayEpochDay: Long,
        timeZone: TimeZone,
    ): StreakTransition {
        val streak = refillFreeze(current, nowEpochMillis, timeZone).withExpiredRepairableMiss(nowEpochMillis)
        val lastActivity = streak.lastActivityEpochDay ?: return StreakTransition(streak)
        val missedDates = todayEpochDay - lastActivity - 1
        if (missedDates <= 0) return StreakTransition(streak)

        val latestMissedEpochDay = todayEpochDay - 1
        if (streak.freezesAvailable > 0) {
            // Advancing through the covered date makes automatic freeze use idempotent on refresh.
            val coveredEpochDay = lastActivity + 1
            val remainingMissedDates = missedDates - 1
            val withFreeze = streak.copy(
                freezesAvailable = streak.freezesAvailable - 1,
                lastActivityEpochDay = coveredEpochDay,
            )
            return if (remainingMissedDates == 0L) {
                StreakTransition(
                    streak = withFreeze.withoutRepairableMiss().copy(
                        currentDays = streak.currentDays + 1,
                    ),
                    freezeUsed = true,
                )
            } else {
                StreakTransition(
                    streak = withMiss(
                        withFreeze.copy(currentDays = 0),
                        latestMissedEpochDay,
                        previousStreakDays = 0,
                        previousActivityEpochDay = null,
                        timeZone = timeZone,
                    ),
                    freezeUsed = true,
                )
            }
        }

        val existingMissIsLatest = streak.latestMissedEpochDay == latestMissedEpochDay
        val previousActivity = if (missedDates == 1L) {
            if (existingMissIsLatest) streak.latestMissedPreviousActivityEpochDay else lastActivity
        } else {
            null
        }
        val previousStreakDays = if (missedDates == 1L) {
            if (existingMissIsLatest) streak.latestMissedPreviousStreakDays else streak.currentDays
        } else {
            0
        }
        return StreakTransition(
            streak = withMiss(
                streak.copy(currentDays = 0),
                latestMissedEpochDay,
                previousStreakDays,
                previousActivity,
                timeZone,
            ),
        )
    }

    private fun findRepairableMiss(
        current: StreakData,
        nowEpochMillis: Long,
        timeZone: TimeZone,
    ): RepairableStreakMiss? {
        val missedEpochDay = current.latestMissedEpochDay ?: return null
        val deadline = current.latestMissedRepairDeadlineEpochMilli
            ?: DailyLocalDate.repairDeadlineEpochMillis(missedEpochDay, timeZone)
        return if (nowEpochMillis < deadline) {
            RepairableStreakMiss(missedEpochDay, deadline)
        } else {
            null
        }
    }

    private fun refillFreeze(current: StreakData, nowEpochMillis: Long, timeZone: TimeZone): StreakData {
        val mondayEpochDay = DailyLocalDate.mondayEpochDayAt(nowEpochMillis, timeZone)
        val lastRefillEpochDay = current.lastFreezeRefillEpochDay
        return if (lastRefillEpochDay == null || mondayEpochDay > lastRefillEpochDay) {
            current.copy(
                freezesAvailable = 1,
                lastFreezeRefillEpochDay = mondayEpochDay,
            )
        } else {
            current.copy(freezesAvailable = current.freezesAvailable.coerceIn(0, 1))
        }
    }

    private fun withMiss(
        current: StreakData,
        missedEpochDay: Long,
        previousStreakDays: Int,
        previousActivityEpochDay: Long?,
        timeZone: TimeZone,
    ): StreakData {
        val savedDeadline = if (current.latestMissedEpochDay == missedEpochDay) {
            current.latestMissedRepairDeadlineEpochMilli
        } else {
            null
        }
        return current.copy(
            latestMissedEpochDay = missedEpochDay,
            latestMissedPreviousStreakDays = previousStreakDays,
            latestMissedPreviousActivityEpochDay = previousActivityEpochDay,
            latestMissedRepairDeadlineEpochMilli = savedDeadline
                ?: DailyLocalDate.repairDeadlineEpochMillis(missedEpochDay, timeZone),
        )
    }

    private fun StreakData.withExpiredRepairableMiss(nowEpochMillis: Long): StreakData =
        latestMissedRepairDeadlineEpochMilli
            ?.takeIf { deadline -> nowEpochMillis >= deadline }
            ?.let { withoutRepairableMiss() }
            ?: this

    private fun StreakData.withoutRepairableMiss(): StreakData = copy(
        latestMissedEpochDay = null,
        latestMissedPreviousStreakDays = 0,
        latestMissedPreviousActivityEpochDay = null,
        latestMissedRepairDeadlineEpochMilli = null,
    )

    private fun currentTimeZone(): TimeZone = timeZoneProvider().clone() as TimeZone
}
