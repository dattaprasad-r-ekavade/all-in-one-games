package com.datathecodie.witgrid.ui.screens.game

import com.datathecodie.witgrid.data.progress.GameProgressSummaryRepository
import com.datathecodie.witgrid.data.progress.LevelProgress
import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.BankSource
import com.datathecodie.witgrid.games.LevelRef
import java.io.ByteArrayInputStream
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

    @Test
    fun continueStatusUsesCurrentAttemptAndNeverEarlierLoop() = runBlocking {
        val active = progress(easyTwo, false, 8).copy(elapsedTimeMillis = 102_000)
        assertEquals(active, refresh(listOf(active)).continueProgress)
        assertEquals(null, refresh(listOf(progress(easyTwo, true, 8))).continueProgress)
        assertEquals(null, refresh(listOf(progress(easyTwo, false, 2), progress(easyThree, true, 6))).continueProgress)
    }

    @Test
    fun continueStatusRecognizesExistingUnnumberedSave() = runBlocking {
        val legacy = progress(easyTwo, false).copy(elapsedTimeMillis = 23_000)
        assertEquals(legacy, refresh(listOf(legacy)).continueProgress)
    }

    @Test
    fun packSizeRangesComeFromBankEnvelopesWithoutDecodingPuzzles() = runBlocking {
        val lines = mapOf(
            "bank/binary-easy.txt.gz" to listOf(
                bankLine("easy", 6, Pack.EASY),
                bankLine("easy-large", 8, Pack.EASY),
            ),
            "bank/binary-medium.txt.gz" to listOf(bankLine("medium", 7, Pack.MEDIUM)),
        )
        val bank = object : BankSource {
            override fun open(path: String) = ByteArrayInputStream(
                lines.getValue(path).joinToString("\n").toByteArray(),
            )
        }
        val viewModel = GameViewModel(
            "binary",
            catalog,
            object : GameProgressSummaryRepository {
                override suspend fun getProgressForGame(gameId: String) = emptyList<LevelProgress>()
            },
            bank,
        )

        viewModel.refresh()

        assertEquals(6..8, viewModel.state.value.packs.first().boardSizes)
        assertEquals(7..7, viewModel.state.value.packs[1].boardSizes)
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

    private fun bankLine(id: String, size: Int, pack: Pack) =
        BankLine(id, size, "payload", "solution", pack, "grade").encode()
}
