package com.datathecodie.witgrid.ui.screens.game

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PackProgress(
    val pack: Pack,
    val completedLevels: Int,
    val totalLevels: Int,
    val nextLevelRef: LevelRef?,
)

data class GameUiState(
    val gameId: String,
    val packs: List<PackProgress> = emptyList(),
    val continueLevelRef: LevelRef? = null,
    val isLoading: Boolean = true,
)

class GameViewModel(
    private val gameId: String,
    private val levelRefsByPack: Map<Pack, List<LevelRef>>,
    private val progressRepository: GameProgressSummaryRepository,
) {
    private val mutableState = MutableStateFlow(GameUiState(gameId = gameId))

    val state: StateFlow<GameUiState> = mutableState.asStateFlow()

    suspend fun refresh() {
        val progress = progressRepository.getProgressForGame(gameId).filter { it.gameId == gameId }
        val completedLevelIds = progress.filter { it.completed }.map { it.levelId }.toSet()
        val unfinishedSavedIds = progress.filterNot { it.completed }.map { it.levelId }.toSet()
        val orderedRefs = Pack.entries.flatMap { levelRefsByPack[it].orEmpty() }
        val continueLevelRef = orderedRefs.firstOrNull { it.levelId in unfinishedSavedIds }
            ?: orderedRefs.firstOrNull { it.isIntro && it.levelId !in completedLevelIds }
            ?: orderedRefs.firstOrNull { it.levelId !in completedLevelIds }
            ?: orderedRefs.firstOrNull()
        val packs = Pack.entries.map { pack ->
            val refs = levelRefsByPack[pack].orEmpty()
            val nextLevelRef = refs.firstOrNull { it.levelId in unfinishedSavedIds }
                ?: refs.firstOrNull { it.levelId !in completedLevelIds }
                ?: refs.firstOrNull()
            PackProgress(
                pack = pack,
                completedLevels = refs.count { it.levelId in completedLevelIds },
                totalLevels = refs.size,
                nextLevelRef = nextLevelRef,
            )
        }
        mutableState.value = GameUiState(
            gameId = gameId,
            packs = packs,
            continueLevelRef = continueLevelRef,
            isLoading = false,
        )
    }
}
