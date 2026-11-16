package com.datathecodie.witgrid.domain.stats

import com.datathecodie.witgrid.data.progress.DailyPuzzleCompletionRepository
import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class StatisticsState(
    val isLoading: Boolean = true,
    val games: List<GameStatistics> = emptyList(),
)

class StatisticsViewModel(
    private val gameIds: List<String>,
    private val regularLevelIdsByGame: Map<String, Set<String>>,
    private val progressRepository: GameProgressSummaryRepository,
    private val dailyPuzzleCompletionRepository: DailyPuzzleCompletionRepository,
    private val calculator: StatisticsCalculator = StatisticsCalculator(),
) {
    private val mutableState = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = mutableState.asStateFlow()

    suspend fun refresh() {
        mutableState.value = StatisticsState(isLoading = true)
        val games = withContext(Dispatchers.IO) {
            val dailyCompletions = dailyPuzzleCompletionRepository
                .observeBetween(Long.MIN_VALUE, Long.MAX_VALUE)
                .first()
            gameIds.map { gameId ->
                calculator.calculate(
                    gameId = gameId,
                    regularLevelIds = regularLevelIdsByGame[gameId].orEmpty(),
                    levelProgress = progressRepository.getProgressForGame(gameId),
                    dailyCompletions = dailyCompletions,
                )
            }
        }
        mutableState.value = StatisticsState(isLoading = false, games = games)
    }
}
