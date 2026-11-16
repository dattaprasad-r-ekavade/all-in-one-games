package com.datathecodie.witgrid.domain.stats

import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletion
import com.datathecodie.witgrid.data.progress.LevelProgress

data class GameStatistics(
    val gameId: String,
    val solvedRegularLevels: Int,
    val dailyCompletions: Int,
    val bestTimeMillis: Long?,
    val averageBestTimeMillis: Long?,
)

/** Calculates distinct regular-level results and date-specific daily completions for one game. */
class StatisticsCalculator {
    fun calculate(
        gameId: String,
        regularLevelIds: Set<String>,
        levelProgress: List<LevelProgress>,
        dailyCompletions: List<DailyPuzzleCompletion>,
    ): GameStatistics {
        require(gameId.isNotBlank()) { "Game ID must not be blank" }

        val completedRegularLevels = levelProgress
            .asSequence()
            .filter { progress ->
                progress.gameId == gameId &&
                    progress.levelId in regularLevelIds &&
                    progress.completed
            }
            .distinctBy(LevelProgress::levelId)
            .toList()
        val bestTimes = completedRegularLevels.mapNotNull(LevelProgress::bestTimeMillis)
        val completedDailyDates = dailyCompletions
            .asSequence()
            .filter { completion -> completion.gameId == gameId }
            .map(DailyPuzzleCompletion::dateEpochDay)
            .distinct()
            .count()

        return GameStatistics(
            gameId = gameId,
            solvedRegularLevels = completedRegularLevels.size,
            dailyCompletions = completedDailyDates,
            bestTimeMillis = bestTimes.minOrNull(),
            averageBestTimeMillis = bestTimes.takeIf(List<Long>::isNotEmpty)
                ?.let { times -> times.sum() / times.size },
        )
    }
}
