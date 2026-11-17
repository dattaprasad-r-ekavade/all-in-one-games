package com.datathecodie.witgrid.ui.screens.home

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.nextGameLevel
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeGameProgress(
    val gameId: String,
    val nextLevelRef: LevelRef?,
)

data class HomeUiState(
    val games: List<HomeGameProgress> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val gameIds: List<String>,
    private val levelRefsByGame: Map<String, List<LevelRef>>,
    private val progressRepository: GameProgressSummaryRepository,
) {
    private val mutableState = MutableStateFlow(HomeUiState())

    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    suspend fun refresh() {
        val games = gameIds.map { gameId ->
            HomeGameProgress(
                gameId = gameId,
                nextLevelRef = nextGameLevel(
                    levelRefsByGame[gameId].orEmpty(),
                    progressRepository.getProgressForGame(gameId),
                ),
            )
        }
        mutableState.value = HomeUiState(games = games, isLoading = false)
    }
}
