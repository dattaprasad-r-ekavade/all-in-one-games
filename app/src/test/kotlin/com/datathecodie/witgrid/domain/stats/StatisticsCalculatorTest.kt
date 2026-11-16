package com.datathecodie.witgrid.domain.stats

import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletion
import com.datathecodie.witgrid.data.progress.LevelProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatisticsCalculatorTest {
    private val calculator = StatisticsCalculator()

    @Test
    fun calculateCountsDistinctCompletedRegularLevelsAndDailyDates() {
        val result = calculator.calculate(
            gameId = "crowns",
            regularLevelIds = setOf("CR-EASY-01", "CR-EASY-02"),
            levelProgress = listOf(
                levelProgress("crowns", "CR-EASY-01", completed = true, bestTimeMillis = 30_000L),
                levelProgress("crowns", "CR-EASY-02", completed = true, bestTimeMillis = 60_000L),
                levelProgress("crowns", "CR-EASY-01", completed = true, bestTimeMillis = 30_000L),
                levelProgress("crowns", "CR-INTRO-01", completed = true, bestTimeMillis = 10_000L),
                levelProgress("crowns", "CR-EASY-03", completed = false, bestTimeMillis = null),
                levelProgress("binary", "BI-EASY-01", completed = true, bestTimeMillis = 15_000L),
            ),
            dailyCompletions = listOf(
                DailyPuzzleCompletion(100, "crowns", "CR-DAILY-01"),
                DailyPuzzleCompletion(100, "crowns", "CR-DAILY-01"),
                DailyPuzzleCompletion(101, "crowns", "CR-DAILY-02"),
                DailyPuzzleCompletion(100, "binary", "BI-DAILY-01"),
            ),
        )

        assertEquals(
            GameStatistics(
                gameId = "crowns",
                solvedRegularLevels = 2,
                dailyCompletions = 2,
                bestTimeMillis = 30_000L,
                averageBestTimeMillis = 45_000L,
            ),
            result,
        )
    }

    @Test
    fun calculateReturnsEmptyTimesWhenNoRegularLevelHasBestTime() {
        val result = calculator.calculate(
            gameId = "binary",
            regularLevelIds = setOf("BI-EASY-01"),
            levelProgress = listOf(
                levelProgress("binary", "BI-EASY-01", completed = true, bestTimeMillis = null),
            ),
            dailyCompletions = emptyList(),
        )

        assertEquals(1, result.solvedRegularLevels)
        assertEquals(0, result.dailyCompletions)
        assertEquals(null, result.bestTimeMillis)
        assertEquals(null, result.averageBestTimeMillis)
    }

    private fun levelProgress(
        gameId: String,
        levelId: String,
        completed: Boolean,
        bestTimeMillis: Long?,
    ): LevelProgress = LevelProgress(
        gameId = gameId,
        levelId = levelId,
        stateBytes = byteArrayOf(),
        elapsedTimeMillis = bestTimeMillis ?: 0L,
        hintsUsed = 0,
        completed = completed,
        bestTimeMillis = bestTimeMillis,
    )
}
