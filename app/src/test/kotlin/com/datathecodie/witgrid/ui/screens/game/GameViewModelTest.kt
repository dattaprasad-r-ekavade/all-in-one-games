package com.datathecodie.witgrid.ui.screens.game

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameViewModelTest {
    @Test
    fun refreshCountsByPackAndChoosesFirstUnfinishedLevel() = runBlocking {
        val easyFirst = LevelRef("binary", "easy-001", Pack.EASY)
        val easySecond = LevelRef("binary", "easy-002", Pack.EASY)
        val mediumFirst = LevelRef("binary", "medium-001", Pack.MEDIUM)
        val expertFirst = LevelRef("binary", "expert-001", Pack.EXPERT)
        val repository = FakeProgressSummaryRepository(
            listOf(
                progress("easy-001", completed = true),
                progress("medium-001", completed = false),
                progress("expert-001", completed = true),
            ),
        )
        val viewModel = GameViewModel(
            gameId = "binary",
            levelRefsByPack = mapOf(
                Pack.EASY to listOf(easyFirst, easySecond),
                Pack.MEDIUM to listOf(mediumFirst),
                Pack.HARD to emptyList(),
                Pack.EXPERT to listOf(expertFirst),
            ),
            progressRepository = repository,
        )

        viewModel.refresh()

        assertEquals(
            listOf(
                PackProgress(Pack.EASY, completedLevels = 1, totalLevels = 2, nextLevelRef = easySecond),
                PackProgress(Pack.MEDIUM, completedLevels = 0, totalLevels = 1, nextLevelRef = mediumFirst),
                PackProgress(Pack.HARD, completedLevels = 0, totalLevels = 0, nextLevelRef = null),
                PackProgress(Pack.EXPERT, completedLevels = 1, totalLevels = 1, nextLevelRef = expertFirst),
            ),
            viewModel.state.value.packs,
        )
        assertEquals(mediumFirst, viewModel.state.value.continueLevelRef)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun continueSkipsFinishedPacksAndResumesSavedLevelBeforeUnplayedLevels() = runBlocking {
        val easy = LevelRef("binary", "easy-001", Pack.EASY)
        val mediumFirst = LevelRef("binary", "medium-001", Pack.MEDIUM)
        val mediumSecond = LevelRef("binary", "medium-002", Pack.MEDIUM)
        val viewModel = GameViewModel(
            gameId = "binary",
            levelRefsByPack = mapOf(
                Pack.EASY to listOf(easy),
                Pack.MEDIUM to listOf(mediumFirst, mediumSecond),
            ),
            progressRepository = FakeProgressSummaryRepository(
                listOf(progress("easy-001", true), progress("medium-002", false)),
            ),
        )

        viewModel.refresh()

        assertEquals(mediumSecond, viewModel.state.value.continueLevelRef)
        assertEquals(mediumSecond, viewModel.state.value.packs[1].nextLevelRef)
    }

    @Test
    fun continueChoosesNextUnplayedPackWhenEarlierPackIsComplete() = runBlocking {
        val easy = LevelRef("binary", "easy-001", Pack.EASY)
        val medium = LevelRef("binary", "medium-001", Pack.MEDIUM)
        val viewModel = GameViewModel(
            "binary",
            mapOf(Pack.EASY to listOf(easy), Pack.MEDIUM to listOf(medium)),
            FakeProgressSummaryRepository(listOf(progress("easy-001", true))),
        )

        viewModel.refresh()

        assertEquals(medium, viewModel.state.value.continueLevelRef)
    }

    @Test
    fun allCompletedLevelsRemainAvailableForReplay() = runBlocking {
        val easy = LevelRef("binary", "easy-001", Pack.EASY)
        val viewModel = GameViewModel(
            "binary",
            mapOf(Pack.EASY to listOf(easy)),
            FakeProgressSummaryRepository(listOf(progress("easy-001", true))),
        )

        viewModel.refresh()

        assertEquals(easy, viewModel.state.value.continueLevelRef)
        assertEquals(easy, viewModel.state.value.packs.first().nextLevelRef)
    }

    private class FakeProgressSummaryRepository(
        private val progress: List<LevelProgress>,
    ) : GameProgressSummaryRepository {
        override suspend fun getProgressForGame(gameId: String): List<LevelProgress> =
            progress.filter { it.gameId == gameId }
    }

    private companion object {
        fun progress(levelId: String, completed: Boolean) = LevelProgress(
            gameId = "binary",
            levelId = levelId,
            stateBytes = byteArrayOf(),
            elapsedTimeMillis = 0,
            hintsUsed = 0,
            completed = completed,
            bestTimeMillis = null,
        )
    }
}
