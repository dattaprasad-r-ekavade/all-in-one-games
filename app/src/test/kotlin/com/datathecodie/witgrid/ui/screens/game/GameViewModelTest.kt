package com.datathecodie.witgrid.ui.screens.game

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameViewModelTest {
    private val intro = LevelRef("binary", "intro", Pack.EASY, introCoachLineResourceId = 1)
    private val easy = LevelRef("binary", "easy", Pack.EASY)
    private val easyTwo = LevelRef("binary", "easy-two", Pack.EASY)
    private val easyThree = LevelRef("binary", "easy-three", Pack.EASY)
    private val medium = LevelRef("binary", "medium", Pack.MEDIUM)
    private val catalog = mapOf(
        Pack.EASY to listOf(intro, easy, easyTwo, easyThree),
        Pack.MEDIUM to listOf(medium),
    )

    @Test
    fun newGameStartsWithUnnumberedIntroOutsideBankProgression() = runBlocking {
        val state = refresh(emptyList())
        assertEquals(intro, state.continueLevelRef)
        assertEquals(listOf(null, 1L, null, null), state.packs.map { it.nextLevelRef?.levelNumber })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun legacySaveAtBankIndexOneResumesLevelTwo() = runBlocking {
        val state = refresh(listOf(progress(easyTwo, false)))
        assertEquals(LevelRef.forLevel(2, catalog.getValue(Pack.EASY)), state.continueLevelRef)
    }

    @Test
    fun completingLastLegacyPuzzleLoopsWithoutResettingLevelNumber() = runBlocking {
        val state = refresh(listOf(progress(intro, true), progress(easyThree, true)))
        assertEquals(LevelRef.forLevel(4, catalog.getValue(Pack.EASY)), state.continueLevelRef)
        assertEquals(state.continueLevelRef, state.packs.first().nextLevelRef)
    }

    @Test
    fun numberedProgressKeepsAdvancingAcrossLoopsAndIgnoresOldUnfinishedAttempts() = runBlocking {
        val state = refresh(listOf(progress(intro, true), progress(easyTwo, true, 8)))
        assertEquals(LevelRef.forLevel(9, catalog.getValue(Pack.EASY)), state.continueLevelRef)
    }

    @Test
    fun activeAttemptInAnotherPackTakesContinuePriorityAndPacksAdvanceIndependently() = runBlocking {
        val state = refresh(listOf(progress(easyThree, true, 6), progress(medium, false, 12)))
        assertEquals(LevelRef.forLevel(12, catalog.getValue(Pack.MEDIUM)), state.continueLevelRef)
        assertEquals(listOf(7L, 12L, null, null), state.packs.map { it.nextLevelRef?.levelNumber })
    }

    private suspend fun refresh(progress: List<LevelProgress>): GameUiState {
        val viewModel = GameViewModel("binary", catalog, object : GameProgressSummaryRepository {
            override suspend fun getProgressForGame(gameId: String) = progress
        })
        viewModel.refresh()
        return viewModel.state.value
    }

    private fun progress(ref: LevelRef, completed: Boolean, number: Long = 0) = LevelProgress(
        gameId = ref.gameId,
        levelId = ref.levelId,
        stateBytes = byteArrayOf(),
        elapsedTimeMillis = 0,
        hintsUsed = 0,
        completed = completed,
        bestTimeMillis = null,
        attemptNumber = number,
    )
}
