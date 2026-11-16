package com.datathecodie.witgrid.ui.screens.home

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeViewModelTest {
    @Test
    fun refreshCountsCompletedLevelsForEachGame() = runBlocking {
        val repository = FakeProgressSummaryRepository(
            mapOf(
                "crowns" to listOf(
                    progress("crowns", "easy-001", completed = true),
                    progress("crowns", "easy-002", completed = false),
                    progress("crowns", "easy-003", completed = true),
                    progress("crowns", "daily-001", completed = true),
                    progress("crowns", "removed-level", completed = true),
                ),
            ),
        )
        val viewModel = HomeViewModel(
            gameIds = listOf("crowns", "binary"),
            levelRefsByGame = mapOf(
                "crowns" to (1..3).map { LevelRef("crowns", "easy-00$it", Pack.EASY) },
                "binary" to listOf(LevelRef("binary", "easy-001", Pack.EASY)),
            ),
            progressRepository = repository,
        )

        viewModel.refresh()

        assertEquals(
            listOf(
                HomeGameProgress("crowns", completedLevels = 2, totalLevels = 3),
                HomeGameProgress("binary", completedLevels = 0, totalLevels = 1),
            ),
            viewModel.state.value.games,
        )
        assertEquals(false, viewModel.state.value.isLoading)
    }

    private class FakeProgressSummaryRepository(
        private val progressByGame: Map<String, List<LevelProgress>>,
    ) : GameProgressSummaryRepository {
        override suspend fun getProgressForGame(gameId: String): List<LevelProgress> =
            progressByGame[gameId].orEmpty()
    }

    private companion object {
        fun progress(gameId: String, levelId: String, completed: Boolean) = LevelProgress(
            gameId = gameId,
            levelId = levelId,
            stateBytes = byteArrayOf(),
            elapsedTimeMillis = 0,
            hintsUsed = 0,
            completed = completed,
            bestTimeMillis = null,
        )
    }
}
