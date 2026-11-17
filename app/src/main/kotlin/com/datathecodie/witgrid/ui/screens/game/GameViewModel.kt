package com.datathecodie.witgrid.ui.screens.game

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.nextGameLevel
import com.datathecodie.witgrid.data.progress.nextPackLevel
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PackProgress(
    val pack: Pack,
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
        val orderedRefs = Pack.entries.flatMap { levelRefsByPack[it].orEmpty() }
        val continueLevelRef = nextGameLevel(orderedRefs, progress)
        val packs = Pack.entries.map { pack ->
            val refs = levelRefsByPack[pack].orEmpty()
            PackProgress(
                pack = pack,
                nextLevelRef = nextPackLevel(refs, progress),
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
