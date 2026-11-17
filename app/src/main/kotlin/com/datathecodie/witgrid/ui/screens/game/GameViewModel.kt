package com.datathecodie.witgrid.ui.screens.game

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.data.progress.nextGameLevel
import com.datathecodie.witgrid.data.progress.nextPackLevel
import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.GameRegistry
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class PackProgress(
    val pack: Pack,
    val nextLevelRef: LevelRef?,
    val boardSizes: IntRange? = null,
)

data class GameUiState(
    val gameId: String,
    val packs: List<PackProgress> = emptyList(),
    val continueLevelRef: LevelRef? = null,
    val isLoading: Boolean = true,
    val continueProgress: LevelProgress? = null,
    val continueState: Any? = null,
)

class GameViewModel(
    private val gameId: String,
    private val levelRefsByPack: Map<Pack, List<LevelRef>>,
    private val progressRepository: GameProgressSummaryRepository,
    private val bankSource: BankSource? = null,
) {
    private val mutableState = MutableStateFlow(GameUiState(gameId = gameId))
    val state: StateFlow<GameUiState> = mutableState.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val progress = progressRepository.getProgressForGame(gameId).filter { it.gameId == gameId }
        val orderedRefs = Pack.entries.flatMap { levelRefsByPack[it].orEmpty() }
        val continueLevelRef = nextGameLevel(orderedRefs, progress)
        val saved = continueLevelRef?.let { ref ->
            progress.firstOrNull { saved ->
                !saved.completed && saved.levelId == ref.levelId &&
                    (ref.isIntro || saved.attemptNumber == ref.levelNumber ||
                        (saved.attemptNumber == 0L && ref.levelNumber == (ref.bankIndex ?: -1) + 1L))
            }
        }
        val module = GameRegistry.find(gameId)
        val packs = Pack.entries.map { pack ->
            val refs = levelRefsByPack[pack].orEmpty()
            val sizes = if (bankSource != null && refs.any { !it.isIntro }) {
                bankSource.open(bankAssetPath(gameId, pack)).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.map { BankLine.decode(it).size }.toList()
                }
            } else emptyList()
            PackProgress(pack, nextPackLevel(refs, progress),
                if (sizes.isEmpty()) null else sizes.min()..sizes.max())
        }
        mutableState.value = GameUiState(
            gameId = gameId,
            packs = packs,
            continueLevelRef = continueLevelRef,
            isLoading = false,
            continueProgress = saved,
            continueState = saved?.let { runCatching { module?.decodeState(it.stateBytes) }.getOrNull() },
        )
    }

    private fun bankAssetPath(gameId: String, pack: Pack): String =
        "bank/$gameId-${pack.name.lowercase()}.txt.gz"
}
